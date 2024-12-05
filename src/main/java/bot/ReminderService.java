package bot;

import database.UserDatabase;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReminderService {
    private final Bot bot;

    public ReminderService(Bot bot) {
        this.bot = bot;
    }

    // Проверка времени напоминания
    public void checkAndSendReminder() {
        List<Long> usersWithReminderTime = UserDatabase.getUsersWithReminderTime();
        LocalTime currentTime = LocalTime.now(); // Текущее время
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (Long userId : usersWithReminderTime) {
            String reminderTime = UserDatabase.getReminderTime(userId); // Получаем время напоминания
            if (reminderTime != null) {
                System.out.println("Пользователь: " + userId + ", Напоминание: " + reminderTime + ", Текущее время: " + currentTime);
                if (currentTime.format(timeFormatter).equals(reminderTime)) {
                    System.out.println("Отправляем напоминание пользователю: " + userId);
                    sendReminder(userId);  // Отправляем напоминание
                }
            }
        }
    }

    // Отправка напоминания
    private void sendReminder(Long userId) {
        bot.sendMessage(userId, MessageType.REMINDER_NOTIFICATION);  // Отправляем уведомление
    }
}