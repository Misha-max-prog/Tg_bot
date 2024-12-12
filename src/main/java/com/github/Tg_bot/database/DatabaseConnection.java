package com.github.Tg_bot.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseConnection {
    private static final String URL = "jdbc:sqlite:src/main/resources/bot_database.db";
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);

    public static Connection connect() {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(URL);
            logger.info("Подключение к SQLite установлено.");
        } catch (SQLException e) {
            logger.error("Ошибка подключения к базе данных: {}", e.getMessage(), e);
        }
        return connection;
    }
}