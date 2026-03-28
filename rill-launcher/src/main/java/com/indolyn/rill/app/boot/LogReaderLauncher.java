package com.indolyn.rill.app.boot;

import com.indolyn.rill.tools.LogReader;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

final class LogReaderLauncher {

    private LogReaderLauncher() {
    }

    static void launch() {
        SwingUtilities.invokeLater(
            () -> {
                LogReader logReader = new LogReader();
                logReader.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                logReader.setVisible(true);
            });
    }
}
