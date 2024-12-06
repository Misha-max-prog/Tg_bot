package admin;

import bot.Bot;
import database.DatabaseConnection;
import bot.MessageType;
import bot.UserState;
import database.UserDatabase;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;


public class AdminService {

    private final Bot bot;
    private final Set<Long> adminIds = Set.of(1716597113L, 5071170827L, 1108817976L); // id админов

    public AdminService(Bot bot) {
        this.bot = bot;
    }

    public boolean isAdmin(Long userId) {
        return adminIds.contains(userId);
    }

    // панель админа
    public void showAdminPanel(Long adminId) {
        if (isAdmin(adminId)) {
            bot.sendMessage(adminId, MessageType.ADMIN_PANEL);
        }
    }
    // чек пользователей
    public void showAllUsers(Long adminId) {
        if (isAdmin(adminId)) {
            String query = "SELECT user_id, user_name, state FROM user_states";

            StringBuilder userList = new StringBuilder("Список всех пользователей:\n");

            try (Connection connection = DatabaseConnection.connect();
                 PreparedStatement preparedStatement = connection.prepareStatement(query);
                 ResultSet resultSet = preparedStatement.executeQuery()) {

                while (resultSet.next()) {
                    Long userId = resultSet.getLong("user_id");
                    String userName = resultSet.getString("user_name");
                    String userState = resultSet.getString("state");

                    userList.append("ID: ").append(userId)
                            .append(", Имя: ").append(userName != null ? userName : "Не указано")
                            .append(", Состояние: ").append(userState)
                            .append("\n");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                userList.append("Ошибка при получении списка пользователей.");
            }
            // отправка сообщения
            SendMessage sm = SendMessage.builder()
                    .chatId(adminId.toString())
                    .text(userList.toString())
                    .build();
            bot.executeMessage(sm);
        }
    }
    // обновление состояния пользователя на PAID типо хз дописать
    public void updateUserStateToPaid(Long adminId, Long userId) {
        if (isAdmin(adminId)) {
            String query = "UPDATE user_states SET state = 'PAID', last_paid = ? WHERE user_id = ?";

            try (Connection connection = DatabaseConnection.connect();
                 PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                LocalDateTime now = LocalDateTime.now();
                preparedStatement.setString(1, now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                preparedStatement.setLong(2, userId);

                int rowsUpdated = preparedStatement.executeUpdate();

                String message = (rowsUpdated > 0)
                        ? "Состояние пользователя с ID " + userId + " успешно обновлено на PAID."
                        : "Пользователь с ID " + userId + " не найден.";

                // отправка сообщения
                SendMessage sm = SendMessage.builder()
                        .chatId(adminId.toString())
                        .text(message)
                        .build();
                bot.executeMessage(sm);
            } catch (SQLException e) {
                e.printStackTrace();
                SendMessage sm = SendMessage.builder()
                        .chatId(adminId.toString())
                        .text("Ошибка при обновлении состояния пользователя.")
                        .build();
                bot.executeMessage(sm);
            }
        }
    }
}