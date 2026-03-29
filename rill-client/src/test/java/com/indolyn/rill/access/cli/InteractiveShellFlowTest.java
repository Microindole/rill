package com.indolyn.rill.access.cli;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.access.protocol.ServerHost;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class InteractiveShellFlowTest {

    @Test
    void interactiveShellShouldRunSqlCommandsAgainstNativeServer() throws Exception {
        int port = findFreePort();
        Thread serverThread =
            new Thread(
                () -> {
                    try {
                        ServerHost.main(new String[] {"--port=" + port});
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                },
                "cli-shell-smoke");
        serverThread.setDaemon(true);
        serverThread.start();

        waitForServer(port);

        String dbName = "interactive_shell_smoke_" + port;
        String commands =
            String.join(
                System.lineSeparator(),
                "use " + dbName + ";",
                "create table users (id int, name varchar);",
                "insert into users (id, name) values (1, 'alice');",
                "select * from users;",
                "exit;")
                + System.lineSeparator();

        ByteArrayInputStream testInput =
            new ByteArrayInputStream(commands.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream testOutput = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        java.io.InputStream originalIn = System.in;

        try {
            System.setIn(testInput);
            System.setOut(new PrintStream(testOutput, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(testOutput, true, StandardCharsets.UTF_8));

            InteractiveShell.main(
                new String[] {"--host=127.0.0.1", "--port=" + port, "--user=root"});
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
            System.setErr(originalErr);
        }

        String output = testOutput.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Login successful"));
        assertTrue(output.contains("alice"));
        assertTrue(output.contains("Bye!"));
    }

    private int findFreePort() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        }
    }

    private void waitForServer(int port) throws Exception {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            try (Socket ignored = new Socket("127.0.0.1", port)) {
                return;
            } catch (Exception ignored) {
                Thread.sleep(100);
            }
        }
        throw new IllegalStateException("Timed out waiting for server on port " + port);
    }
}
