package com.indolyn.rill.app.boot;

import com.indolyn.rill.core.storage.database.DatabaseManager;
import com.indolyn.rill.tools.DataReader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

final class DataToolLauncher {

    private DataToolLauncher() {
    }

    static void launch(String[] args) throws IOException {
        DataOptions options = DataOptions.parse(args);
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

        if (options.exportPath() != null) {
            DataReader.exportDatabaseToFile(dbName, options.exportPath().toFile());
            System.out.println("--- 导出成功！ ---");
            System.out.println("文件已保存到: " + options.exportPath().toAbsolutePath());
            return;
        }

        System.out.println(DataReader.renderDatabaseReport(dbName));
    }

    private static String resolveDatabaseName(DataOptions options, List<String> databases) {
        if (options.dbName() != null) {
            if (!databases.contains(options.dbName())) {
                System.err.println("错误: 数据库 '" + options.dbName() + "' 不存在。可用数据库: " + databases);
                return null;
            }
            return options.dbName();
        }

        System.out.println("--- rill 数据工具 ---");
        System.out.println("可用的数据库: " + databases);
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("请输入要操作的数据库名称: ");
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
                  rill data [--db=<name>] [--export=<file>] [--list]

                Options:
                  --db=<name>       Select the database to inspect
                  --export=<file>   Export the database as SQL to the target file
                  --list            List available databases and exit
                  --help            Show this help
                """);
    }

    private record DataOptions(String dbName, Path exportPath, boolean listOnly, boolean help) {
        private static DataOptions parse(String[] args) {
            String dbName = null;
            Path exportPath = null;
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
                    } else if (lower.equals("--list")) {
                        listOnly = true;
                    } else if (lower.startsWith("--db=")) {
                        dbName = normalized.substring("--db=".length()).trim();
                    } else if (lower.startsWith("--export=")) {
                        exportPath = Path.of(normalized.substring("--export=".length()).trim());
                    }
                }
            }

            return new DataOptions(dbName, exportPath, listOnly, help);
        }
    }
}
