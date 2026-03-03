package sf.project.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    private static final Properties properties = new Properties();

    static {
        loadProperties();
    }

    private static void loadProperties() {
        try (InputStream input = AppConfig.class.getResourceAsStream("/application.properties")) {
            if (input == null) {
                logger.warn("Unable to find application.properties");
                return;
            }
            properties.load(input);
            logger.info("Configuration loaded successfully");
        } catch (IOException e) {
            logger.error("Error loading configuration", e);
        }
    }

    public static String getApiKey() {
        return properties.getProperty("openai.api.key", "");
    }

    public static String getModel() {
        return properties.getProperty("openai.model", "gpt-3.5-turbo");
    }

    public static String getDatabaseUrl() {
        return properties.getProperty("database.url", "safebot.db");
    }

    public static int getInsightFrequency() {
        try {
            return Integer.parseInt(properties.getProperty("insight.frequency", "10"));
        } catch (NumberFormatException e) {
            return 10;
        }
    }

    public static int getMaxTokens() {
        try {
            return Integer.parseInt(properties.getProperty("openai.max.tokens", "500"));
        } catch (NumberFormatException e) {
            return 500;
        }
    }
}
