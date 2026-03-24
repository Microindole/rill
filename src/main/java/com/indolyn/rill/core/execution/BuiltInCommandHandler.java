package com.indolyn.rill.core.execution;

import com.indolyn.rill.core.storage.buffer.BufferPoolManager;

class BuiltInCommandHandler {

    private final BufferPoolManager bufferPoolManager;

    BuiltInCommandHandler(BufferPoolManager bufferPoolManager) {
        this.bufferPoolManager = bufferPoolManager;
    }

    String tryHandle(String sql) {
        String normalizedSql = sql.trim();
        if (normalizedSql.equalsIgnoreCase("CRASH_NOW;")) {
            System.out.println("[DEBUG] Received CRASH_NOW command. Simulating unexpected shutdown...");
            System.exit(1);
        }
        if (normalizedSql.equalsIgnoreCase("FLUSH_BUFFER;")) {
            bufferPoolManager.clear();
            return "Buffer pool cleared.";
        }
        return null;
    }
}
