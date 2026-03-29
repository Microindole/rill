package com.indolyn.rill.access.protocol;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class NativeServerSocketSmokeTest {

    @Test
    void nativeServerShouldAcceptLoginAndExecuteSqlFlow() throws Exception {
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
                "native-server-smoke");
        serverThread.setDaemon(true);
        serverThread.start();

        waitForServer(port);

        String dbName = "native_socket_smoke_" + port;
        try (Socket socket = new Socket("127.0.0.1", port);
             PrintWriter out =
                 new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
             BufferedReader in =
                 new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            socket.setSoTimeout(5000);

            String welcome = in.readLine();
            assertTrue(welcome.contains("username"));

            out.println("root");
            String login = in.readLine();
            assertTrue(login.contains("Login successful"));

            out.println("use " + dbName + ";");
            assertTrue(in.readLine().contains("Database changed"));

            out.println("create table users (id int, name varchar);");
            assertTrue(in.readLine().contains("created"));

            out.println("insert into users (id, name) values (1, 'alice');");
            assertTrue(in.readLine().contains("rows affected"));

            out.println("select * from users;");
            String selectResult = in.readLine();
            assertTrue(selectResult.contains("alice"));
            assertTrue(selectResult.contains("id"));

            out.println("exit;");
        }
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
