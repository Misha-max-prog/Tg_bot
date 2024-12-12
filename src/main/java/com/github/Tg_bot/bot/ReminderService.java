package com.github.Tg_bot.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.Tg_bot.database.UserDatabase;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReminderService {
    private final Bot bot;
    private static final Logger logger = LoggerFactory.getLogger(ReminderService.class);

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
                logger.info("Пользователь: {}, Напоминание: {}, Текущее время: {}", userId, reminderTime, currentTime);
                if (currentTime.format(timeFormatter).equals(reminderTime)) {
                    logger.info("Отправляем напоминание пользователю: {}", userId);
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