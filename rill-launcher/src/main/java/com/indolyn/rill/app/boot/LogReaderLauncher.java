package com.indolyn.rill.app.boot;

import com.indolyn.rill.core.storage.database.DatabaseManager;
import com.indolyn.rill.tools.LogInspectionService;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

final class LogReaderLauncher {

    private LogReaderLauncher() {
    }

    static void launch(String[] args) throws IOException {
        LogOptions options = LogOptions.parse(args);
        if (options.help()) {
            printUsage();
            return;
        }

        DatabaseManager dbManager = new DatabaseManager();
        List<String> databases = dbManager.listDatabases();
        if (databases.isEmpty()) {
            System.out.println("在 'data' 目录下没有找到任何数据库。");
            return;
        }
        if (options.listOnly()) {
            System.out.println("可用的数据库: " + databases);
            return;
        }

        String dbName = resolveDatabaseName(options, databases);
        if (dbName == null) {
            return;
        }

        LogInspectionService inspectionService = new LogInspectionService();
        System.out.println(inspectionService.renderConsoleReport(dbName, options.details()));
    }

    private static String resolveDatabaseName(LogOptions options, List<String> databases) {
        if (options.dbName() != null) {
            if (!databases.contains(options.dbName())) {
                System.err.println("错误: 数据库 '" + options.dbName() + "' 不存在。可用数据库: " + databases);
                return null;
            }
            return options.dbName();
        }

        System.out.println("--- rill 日志工具 ---");
        System.out.println("可用的数据库: " + databases);
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("请输入要查看日志的数据库名称: ");
                if (!scanner.hasNextLine()) {
                    System.err.println("未收到数据库名称输入。可以使用 --db=<name> 非交互执行。");
                    return null;
                }
                String dbName = scanner.nextLine().trim();
                if (databases.contains(dbName)) {
                    return dbName;
                }
                System.err.println("错误: 数据库 '" + dbName + "' 不存在，请重试。");
            }
        }
    }

    private static void printUsage() {
        System.out.println(
            """
                Usage:
                  rill log [--db=<name>] [--details] [--list]

                Options:
                  --db=<name>    Select the database to inspect
                  --details      Include full record details in output
                  --list         List available databases and exit
                  --help         Show this help
                """);
    }

    private record LogOptions(String dbName, boolean details, boolean listOnly, boolean help) {
        private static LogOptions parse(String[] args) {
            String dbName = null;
            boolean details = false;
            boolean listOnly = false;
            boolean help = false;

            if (args != null) {
                for (String arg : args) {
                    if (arg == null || arg.isBlank()) {
                        continue;
                    }
                    String normalized = arg.trim();
                    String lower = normalized.toLowerCase(Locale.ROOT);
                    if (lower.equals("--help") || lower.equals("-h")) {
                        help = true;
                    } else if (lower.equals("--details")) {
                        details = true;
                    } else if (lower.equals("--list")) {
                        listOnly = true;
                    } else if (lower.startsWith("--db=")) {
                        dbName = normalized.substring("--db=".length()).trim();
                    }
                }
            }

            return new LogOptions(dbName, details, listOnly, help);
        }
    }
}
