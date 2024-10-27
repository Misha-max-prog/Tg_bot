import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDatabase {

    // Метод для создания таблицы user_states
    public static void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS user_states (" +
                "user_id INTEGER PRIMARY KEY," +
                "state TEXT NOT NULL);";

        try (Connection connection = DatabaseConnection.connect(); // Подключаемся к базе данных
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.execute(); // Выполняем SQL-запрос
            System.out.println("Таблица user_states создана или уже существует.");
        } catch (SQLException e) {
            System.out.println("Ошибка создания таблицы: " + e.getMessage());
        }
    }

    // Метод для получения состояния пользователя из базы данных
    public static UserState getUserStateFromDatabase(Long userId) {
        UserState state = UserState.NO_USER; // Состояние по умолчанию
        String query = "SELECT state FROM user_states WHERE user_id = ?";
        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setLong(1, userId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String stateStr = resultSet.getString("state");
                state = UserState.valueOf(stateStr); // Преобразование строки в UserState
                System.out.println("Получено состояние пользователя " + userId + ": " + state);
            } else {
                System.out.println("Состояние для пользователя " + userId + " не найдено в базе данных.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return state;
    }

    // Метод для сохранения состояния пользователя в базе данных
    public static void saveUserStateToDatabase(Long userId, UserState state) {
        String insertOrUpdate = "INSERT INTO user_states (user_id, state) VALUES (?, ?) " +
                "ON CONFLICT(user_id) DO UPDATE SET state = ?;";

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement preparedStatement = connection.prepareStatement(insertOrUpdate)) {
            preparedStatement.setLong(1, userId); // Устанавливаем user_id для вставки
            preparedStatement.setString(2, state.name()); // Устанавливаем state для вставки
            preparedStatement.setString(3, state.name()); // Устанавливаем state для обновления

            int rowsAffected = preparedStatement.executeUpdate();
            System.out.println("Состояние пользователя " + userId + " обновлено в базе данных. Затронуто строк: " + rowsAffected + " Стало: " + state);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static void printUserStates() {
        String query = "SELECT * FROM user_states;";

        try (Connection connection = DatabaseConnection.connect();
             PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            System.out.println("user_id\t\tstate");
            while (resultSet.next()) {
                long userId = resultSet.getLong("user_id");
                String state = resultSet.getString("state");
                System.out.println(userId + "\t\t" + state);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}