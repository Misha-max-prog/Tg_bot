package database;

import bot.UserState;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UserDatabase {

    // Метод для создания таблицы user_states
    public static void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS user_states (" +
                "user_id INTEGER PRIMARY KEY," +
                "user_name TEXT NOT NULL," +
                "state TEXT NOT NULL," +
                "Reminder_time TEXT," +  // Новый столбец для времени последнего использования
                "last_paid TEXT" +   // Новый столбец для времени последней оплаты
                ");";

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
        String reminderTime = null;
        String lastPaid = null;

        String query = "SELECT user_name, state, Reminder_time, last_paid FROM user_states WHERE user_id = ?";
        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setLong(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                userName = resultSet.getString("user_name");
                String stateStr = resultSet.getString("state");
                state = UserState.valueOf(stateStr);
                reminderTime = resultSet.getString("Reminder_time");
                lastPaid = resultSet.getString("last_paid");
            }
            if (lastPaid != null) {
                //функция для проверки сколько времени прошло с оплаты
                //DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

                // LocalDateTime lastPaidTime = LocalDateTime.parse(lastPaid, formatter);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate lastPaidTime = LocalDate.parse(lastPaid, formatter);
                LocalDate now = LocalDate.now();

                long monthsBetween = ChronoUnit.MONTHS.between(lastPaidTime, now);
                System.out.println("TIME: "+ monthsBetween);

                if (monthsBetween > 1) {
                    // Если прошло больше месяца, уведомляем пользователя
                    state = UserState.NOT_PAID; // Переводим состояние пользователя в "NOT_PAID"
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("ID:" + userId + " | Пользователь: " + userName + " | Состояние: " + state +
                " | времня напоминания " + reminderTime + " | Последняя оплата: " + lastPaid);
        return state;
    }

    // Метод для сохранения состояния пользователя в базе данных
    public static void saveUserStateToDatabase(Long userId, String userName, UserState state, String reminderTime) {
        String insertOrUpdate = "INSERT INTO user_states (user_id, user_name, state, Reminder_time, last_paid) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT(user_id) DO UPDATE SET user_name = excluded.user_name, " +
                "state = excluded.state, Reminder_time = excluded.Reminder_time, last_paid = excluded.last_paid";

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement preparedStatement = connection.prepareStatement(insertOrUpdate)) {
            preparedStatement.setLong(1, userId);
            preparedStatement.setString(2, userName);
            preparedStatement.setString(3, state.name());
            preparedStatement.setString(4, reminderTime);  // Время последнего использования
            int rowsAffected = preparedStatement.executeUpdate();
            System.out.println("Состояние пользователя " + userId + " обновлено в базе данных. Затронуто строк: " +
                    rowsAffected + " | Состояние: " + state + " | время напоминания: " + reminderTime);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void printUserStates() {

        String query = "SELECT user_id, user_name, state, Reminder_time, last_paid FROM user_states";

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            System.out.println("Список пользователей и их состояния:");

            while (resultSet.next()) {
                Long userId = resultSet.getLong("user_id");
                String userName = resultSet.getString("user_name");
                String state = resultSet.getString("state");
                String reminderTime = resultSet.getString("Reminder_time");
                String lastPaid = resultSet.getString("last_paid");

                System.out.println("ID: " + userId + ", Имя: " + userName + ", " +
                        "Состояние: " + state + "время напоминания: " + reminderTime + " Последняя оплата: " + lastPaid);
            }
        } catch (SQLException e) {
            System.out.println("Ошибка при получении данных пользователей: " + e.getMessage());
        }
    }
    public static void saveReminderTime(Long userId, String reminderTime) {
        String query = "UPDATE user_states SET reminder_time = ? WHERE user_id = ?";

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, reminderTime);  // Сохраняем время напоминания
            preparedStatement.setLong(2, userId);  // Указываем ID пользователя
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static String getReminderTime(Long userId) {
        String query = "SELECT reminder_time FROM user_states WHERE user_id = ?";

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setLong(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getString("reminder_time");  // Возвращаем строку с временем напоминания
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;  // Если время не найдено, возвращаем null
    }
    public static List<Long> getUsersWithReminderTime() {
        List<Long> userIds = new ArrayList<>();
        String query = "SELECT user_id FROM user_states WHERE reminder_time IS NOT NULL AND reminder_time != ''";

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                userIds.add(resultSet.getLong("user_id"));  // Добавляем пользователей с установленным временем
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return userIds;
    }
}