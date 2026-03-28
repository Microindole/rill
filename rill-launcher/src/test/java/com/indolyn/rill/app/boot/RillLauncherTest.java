package com.indolyn.rill.app.boot;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class RillLauncherTest {

    @Test
    void helpModeShouldPrintUnifiedUsage() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
            RillLauncher.main(new String[] {"help"});
        } finally {
            System.setOut(originalOut);
        }

        String usage = output.toString(StandardCharsets.UTF_8);
        assertTrue(usage.contains("Modes:"));
        assertTrue(usage.contains("server"));
        assertTrue(usage.contains("mysql-server"));
        assertTrue(usage.contains("sql"));
        assertTrue(usage.contains("data"));
        assertTrue(usage.contains("log"));
    }

    @Test
    void unknownModeShouldPrintErrorAndUsage() throws Exception {
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        PrintStream originalOut = System.out;
        try {
            System.setErr(new PrintStream(error, true, StandardCharsets.UTF_8));
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
            RillLauncher.main(new String[] {"unknown-mode"});
        } finally {
            System.setErr(originalErr);
            System.setOut(originalOut);
        }

        assertTrue(error.toString(StandardCharsets.UTF_8).contains("Unknown mode: unknown-mode"));
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Usage:"));
    }
}
