package com.syncron;

import com.syncron.utils.ServerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerConfigTest {

    @AfterEach
    void cleanup() {
        System.clearProperty("server.url");
        ServerConfig.reload();
    }

    @Test
    void trimsTrailingSlashFromJvmProperty() {
        System.setProperty("server.url", "http://example.org:9090/");
        ServerConfig.reload();

        assertEquals("http://example.org:9090", ServerConfig.getBaseUrl());
    }
}
