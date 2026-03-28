package com.indolyn.rill.access.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class ServerEntryPointTest {

    @Test
    void serverHostParsePortShouldPreferArgsThenDefault() throws Exception {
        Method parsePort = ServerHost.class.getDeclaredMethod("parsePort", String[].class, int.class);
        parsePort.setAccessible(true);

        assertEquals(8848, parsePort.invoke(null, new Object[] {null, 8848}));
        assertEquals(9001, parsePort.invoke(null, new Object[] {new String[] {"--port=9001"}, 8848}));
    }

    @Test
    void serverRemoteParsePortShouldPreferArgsThenDefault() throws Exception {
        Method parsePort =
            ServerRemote.class.getDeclaredMethod(
                "parsePort", String[].class, int.class, String.class);
        parsePort.setAccessible(true);

        assertEquals(9999, parsePort.invoke(null, new Object[] {null, 9999, "RILL_MYSQL_PORT"}));
        assertEquals(
            3307,
            parsePort.invoke(
                null, new Object[] {new String[] {"--port=3307"}, 9999, "RILL_MYSQL_PORT"}));
    }
}
