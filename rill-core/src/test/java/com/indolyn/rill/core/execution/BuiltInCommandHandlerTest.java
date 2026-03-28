package com.indolyn.rill.core.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

import com.indolyn.rill.core.storage.buffer.BufferPoolManager;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BuiltInCommandHandlerTest {

    @Test
    void flushBufferShouldClearBufferPoolAndReturnMessage() {
        BufferPoolManager bufferPoolManager = Mockito.mock(BufferPoolManager.class);
        BuiltInCommandHandler handler = new BuiltInCommandHandler(bufferPoolManager);

        String result = handler.tryHandle("FLUSH_BUFFER;");

        assertEquals("Buffer pool cleared.", result);
        verify(bufferPoolManager).clear();
    }

    @Test
    void unknownCommandShouldReturnNull() {
        BufferPoolManager bufferPoolManager = Mockito.mock(BufferPoolManager.class);
        BuiltInCommandHandler handler = new BuiltInCommandHandler(bufferPoolManager);

        assertNull(handler.tryHandle("SELECT 1;"));
    }
}
