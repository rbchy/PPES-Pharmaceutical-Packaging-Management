package com.ppes.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Loads config.properties once and opens JDBC connections to MySQL on demand. */
public final class Db {

    private static Properties props;

    private Db() {
    }

    private static synchronized Properties loadProps() {
        if (props != null) {
            return props;
        }
        Properties p = new Properties();
        try (InputStream in = Db.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in == null) {
                throw new RuntimeException("config.properties not found on classpath. " +
                        "It should be in src/main/resources/config.properties.");
            }
            p.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
        props = p;
        return props;
    }

    /** Opens a new JDBC connection. Caller is responsible for closing it (use try-with-resources). */
    public static Connection getConnection() throws SQLException {
        Properties p = loadProps();
        String host = p.getProperty("db.host", "localhost");
        String port = p.getProperty("db.port", "3306");
        String name = p.getProperty("db.name", "ppes");
        String user = p.getProperty("db.user", "root");
        String pass = p.getProperty("db.password", "");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + name
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // Modern JDBC drivers self-register via SPI; ignore if the class
            // isn't found this way and let DriverManager try anyway.
        }
        return DriverManager.getConnection(url, user, pass);
    }
}
