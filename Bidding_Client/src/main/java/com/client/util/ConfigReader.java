package com.client.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigReader.class);
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = ConfigReader.class.getClassLoader()
                .getResourceAsStream("client-config.properties")) {
            if (input != null) {
                properties.load(input);
                logger.info("Loaded client-config.properties successfully.");
            } else {
                logger.error("Sorry, unable to find client-config.properties");
            }
        } catch (Exception ex) {
            logger.error("Error loading client config", ex);
        }
    }

    public static String getHttpUrl() {
        return properties.getProperty("server.http.url", "http://localhost:7070");
    }

    public static String getWebSocketUrl() {
        return properties.getProperty("server.websocket.url", "ws://localhost:8080");
    }
	
	public static String getProperty(String key) {
        return properties.getProperty(key); 
    }
}