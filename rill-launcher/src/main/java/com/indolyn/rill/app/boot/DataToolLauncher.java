package com.indolyn.rill.app.boot;

import com.indolyn.rill.core.storage.database.DatabaseManager;
import com.indolyn.rill.tools.DataReader;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

final class DataToolLauncher {

    private DataToolLauncher() {
    }

    static void launch() throws IOException {
        DatabaseManager dbManager = new DatabaseManager();
        List<String> databases = dbManager.listDatabases();

        if (databases.isEmpty()) {
            System.out.println("在 'data' 目录下没有找到任何数据库。");
            return;
        }

        System.out.println("--- rill 数据工具 ---");
        System.out.println("可用的数据库: " + databases);

        try (Scanner scanner = new Scanner(System.in)) {
            String dbName = selectDatabase(scanner, databases);

            System.out.println("\n请选择操作:");
            System.out.println("  1. 在控制台查看数据");
            System.out.println("  2. 导出为 SQL 文件");
            int choice = selectChoice(scanner);

            if (choice == 1) {
                System.out.println(DataReader.renderDatabaseReport(dbName));
            } else {
                exportDatabaseToSqlInteractive(dbName);
            }
        }
    }

    private static void exportDatabaseToSqlInteractive(String dbName) throws IOException {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("请选择SQL文件的保存位置");
        chooser.setSelectedFile(new File(dbName + "_dump.sql"));
        chooser.setFileFilter(new FileNameExtensionFilter("SQL File", "sql"));

        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            DataReader.exportDatabaseToFile(dbName, file);
            System.out.println("--- 导出成功！ ---");
            System.out.println("文件已保存到: " + file.getAbsolutePath());
        } else {
            System.out.println("导出已取消。");
        }
    }

    private static String selectDatabase(Scanner scanner, List<String> databases) {
        String dbName = "";
        boolean isValidDb = false;
        while (!isValidDb) {
            System.out.print("请输入要操作的数据库名称: ");
            dbName = scanner.nextLine();
            if (databases.contains(dbName)) {
                isValidDb = true;
            } else {
                System.err.println("错误: 数据库 '" + dbName + "' 不存在，请重试。");
            }
        }
        return dbName;
    }

    private static int selectChoice(Scanner scanner) {
        int choice = 0;
        while (choice != 1 && choice != 2) {
            System.out.print("请输入你的选择 (1 或 2): ");
            try {
                choice = Integer.parseInt(scanner.nextLine());
                if (choice != 1 && choice != 2) {
                    System.err.println("无效输入，请输入 1 或 2。");
                }
            } catch (NumberFormatException e) {
                System.err.println("无效输入，请输入一个数字。");
            }
        }
        return choice;
    }
}
