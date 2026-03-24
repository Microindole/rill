package com.indolyn.rill.app.boot;

import com.indolyn.rill.access.cli.InteractiveShell;
import com.indolyn.rill.access.gui.AdvancedShell;
import com.indolyn.rill.access.protocol.ServerHost;
import com.indolyn.rill.access.protocol.ServerRemote;
import com.indolyn.rill.tools.DataReader;
import com.indolyn.rill.tools.LogReader;
import java.util.Arrays;
import org.springframework.boot.SpringApplication;

/** Unified entry point for IDE, terminal, VS Code and packaged deployment. */
public final class RillLauncher {

  private RillLauncher() {}

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
      case "data-reader" -> DataReader.main(forwardedArgs);
      case "log-reader" -> LogReader.main(forwardedArgs);
      case "spring" -> SpringApplication.run(RillApplication.class, forwardedArgs);
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
                  java -jar rill.jar <mode> [args]

                Modes:
                  server        Start the native rill TCP server
                  mysql-server  Start the MySQL protocol server
                  client        Start the terminal client
                  gui           Start the Swing GUI client
                  data-reader   Inspect data files
                  log-reader    Inspect log files
                  spring        Start the Spring Boot application

                Examples:
                  java -jar target/rill-0.0.1-SNAPSHOT.jar server --port=8848
                  java -jar target/rill-0.0.1-SNAPSHOT.jar client --host=127.0.0.1 --port=8848 --user=root
                """);
  }
}
