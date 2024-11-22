package database;

import bot.UserState;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDatabase {

    // Метод для создания таблицы user_states
    public static void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS user_states (" +
                "user_id INTEGER PRIMARY KEY," +
                "user_name TEXT NOT NULL," +
                "state TEXT NOT NULL);";

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.execute();
            System.out.println("Таблица user_states создана или уже существует.");
        } catch (SQLException e) {
            System.out.println("Ошибка создания таблицы: " + e.getMessage());
        }
    }

    // Метод для получения состояния пользователя из базы данных
    public static UserState getUserStateFromDatabase(Long userId) {
        UserState state = UserState.NO_USER;
        String userName = null;

        String query = "SELECT user_name, state FROM user_states WHERE user_id = ?";
        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setLong(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                userName = resultSet.getString("user_name");
                String stateStr = resultSet.getString("state");
                state = UserState.valueOf(stateStr);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("ID:"+ userId+ " | Пользователь: " + userName + " | Состояние: " + state);
        return state;
    }

    // Метод для сохранения состояния пользователя в базе данных
    public static void saveUserStateToDatabase(Long userId, String userName, UserState state) {
        String insertOrUpdate = "INSERT INTO user_states (user_id, user_name, state) VALUES (?, ?, ?) " +
                "ON CONFLICT(user_id) DO UPDATE SET user_name = excluded.user_name, state = excluded.state";

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement preparedStatement = connection.prepareStatement(insertOrUpdate)) {
            preparedStatement.setLong(1, userId);
            preparedStatement.setString(2, userName);
            preparedStatement.setString(3, state.name());
            int rowsAffected = preparedStatement.executeUpdate();
            System.out.println("Состояние пользователя " + userId + " обновлено в базе данных. Затронуто строк: " + rowsAffected + " стало: " + state);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static void printUserStates() {
        String query = "SELECT user_id, user_name, state FROM user_states";

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            System.out.println("Список пользователей и их состояния:");

            while (resultSet.next()) {
                Long userId = resultSet.getLong("user_id");
                String userName = resultSet.getString("user_name");
                String state = resultSet.getString("state");

                System.out.println("ID: " + userId + ", Имя: " + userName + ", Состояние: " + state);
            }
        } catch (SQLException e) {
            System.out.println("Ошибка при получении данных пользователей: " + e.getMessage());
        }
    }
}