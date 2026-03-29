package com.indolyn.rill.access.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class MysqlServerSocketSmokeTest {

    @Test
    void mysqlServerShouldEmitHandshakePacketOnConnect() throws Exception {
        int port = findFreePort();
        Thread serverThread =
            new Thread(
                () -> {
                    try {
                        ServerRemote.main(new String[] {"--port=" + port});
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                },
                "mysql-server-smoke");
        serverThread.setDaemon(true);
        serverThread.start();

        waitForServer(port);

        try (Socket socket = new Socket("127.0.0.1", port)) {
            socket.setSoTimeout(5000);
            InputStream in = socket.getInputStream();

            byte[] header = in.readNBytes(4);
            int payloadLength =
                (header[0] & 0xFF) | ((header[1] & 0xFF) << 8) | ((header[2] & 0xFF) << 16);
            byte[] payload = in.readNBytes(payloadLength);

            assertEquals(4, header.length);
            assertTrue(payloadLength > 0);
            assertEquals(10, payload[0]);

            ByteArrayOutputStream capture = new ByteArrayOutputStream();
            capture.write(payload);
            String payloadText = capture.toString(StandardCharsets.UTF_8);
            assertTrue(payloadText.contains("rill"));
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
