package com.syncron.utils;

import java.io.*;
import java.util.Properties;

/**
 * Centralized server URL configuration.
 *
 * Instead of hardcoding ServerConfig.getBaseUrl() + "" in every controller,
 * all HTTP calls should use: ServerConfig.getBaseUrl() + "/api/..."
 *
 * The URL is read from a config file (server.properties) in the project root.
 * If the file doesn't exist, it defaults to http://localhost:8080 and creates the file.
 *
 * TO CONNECT FROM ANOTHER DEVICE:
 * 1. Find the server machine's IP address (e.g., 192.168.1.5)
 * 2. Edit server.properties: server.url=http://192.168.1.5:8080
 * 3. Restart the client app — done!
 *
 * Alternatively, pass the URL as a JVM argument:
 *   mvn javafx:run -Dserver.url=http://192.168.1.5:8080
 */
public class ServerConfig {

    private static final String CONFIG_FILE = "server.properties";
    private static final String DEFAULT_URL = ServerConfig.getBaseUrl() + "";
    private static String cachedUrl = null;

    /**
     * Returns the base server URL (e.g., ServerConfig.getBaseUrl() + "" or "http://192.168.1.5:8080").
     * No trailing slash.
     */
    public static String getBaseUrl() {
        if (cachedUrl != null) return cachedUrl;

        // Priority 1: JVM argument (-Dserver.url=http://...)
        String jvmUrl = System.getProperty("server.url");
        if (jvmUrl != null && !jvmUrl.isEmpty()) {
            cachedUrl = jvmUrl.endsWith("/") ? jvmUrl.substring(0, jvmUrl.length() - 1) : jvmUrl;
            System.out.println("🌐 Server URL (from JVM arg): " + cachedUrl);
            return cachedUrl;
        }

        // Priority 2: Config file
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (InputStream in = new FileInputStream(configFile)) {
                Properties props = new Properties();
                props.load(in);
                String url = props.getProperty("server.url", DEFAULT_URL).trim();
                cachedUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
                System.out.println("🌐 Server URL (from config): " + cachedUrl);
                return cachedUrl;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Priority 3: Default + create config file for next time
        cachedUrl = DEFAULT_URL;
        try (OutputStream out = new FileOutputStream(configFile)) {
            Properties props = new Properties();
            props.setProperty("server.url", DEFAULT_URL);
            props.store(out, "Kernel Server Configuration\nChange this to your server's IP address for multi-device access.\nExample: server.url=http://192.168.1.5:8080");
            System.out.println("🌐 Created " + CONFIG_FILE + " with default URL: " + cachedUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return cachedUrl;
    }

    /**
     * Convenience: builds a full API URL.
     * Usage: ServerConfig.api("/api/dashboard/courses?role=STUDENT")
     */
    public static String api(String path) {
        String base = getBaseUrl();
        if (path.startsWith("/")) return base + path;
        return base + "/" + path;
    }

    /**
     * Force reload the config (e.g., after user edits the file).
     */
    public static void reload() {
        cachedUrl = null;
        getBaseUrl();
    }
}