package com.indolyn.rill.app.boot;

import com.indolyn.rill.access.cli.InteractiveShell;
import com.indolyn.rill.access.gui.AdvancedShell;
import com.indolyn.rill.access.protocol.ServerHost;
import com.indolyn.rill.access.protocol.ServerRemote;

import java.util.Arrays;

/**
 * Unified entry point for IDE and local development of native access modes.
 */
public final class RillLauncher {

    private RillLauncher() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String mode = args[0].trim().toLowerCase();
        String[] forwardedArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (mode) {
            case "server" -> ServerHost.main(forwardedArgs);
            case "mysql-server" -> ServerRemote.main(forwardedArgs);
            case "client" -> InteractiveShell.main(forwardedArgs);
            case "gui" -> AdvancedShell.main(forwardedArgs);
            case "data-reader" -> DataToolLauncher.launch();
            case "log-reader" -> LogReaderLauncher.launch();
            case "help", "-h", "--help" -> printUsage();
            default -> {
                System.err.println("Unknown mode: " + args[0]);
                printUsage();
            }
        }
    }

    private static void printUsage() {
        System.out.println(
            """
                Usage:
                  java -jar rill-launcher.jar <mode> [args]

                Modes:
                  server        Start the native rill TCP server
                  mysql-server  Start the MySQL protocol server
                  client        Start the terminal client
                  gui           Start the Swing GUI client
                  data-reader   Inspect data files
                  log-reader    Inspect log files

                Note:
                  Spring Boot / Web UI is packaged separately as rill-app-web.
                """);
    }
}
