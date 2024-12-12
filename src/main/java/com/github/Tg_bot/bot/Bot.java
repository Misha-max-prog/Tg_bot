package com.github.Tg_bot.bot;

import com.github.Tg_bot.admin.AdminService;
import com.github.Tg_bot.database.UserDatabase;
import com.github.Tg_bot.util.Token;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class Bot extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(Bot.class);
    private final AdminService adminService = new AdminService(this);
    private final ReminderService reminderService = new ReminderService(this);

    @Override
    public String getBotUsername() {
        return "ShMariaSportikbot";
    }

    @Override
    public String getBotToken() {
        return Token.ReadToken();
    }
    @Override
    public void onUpdateReceived(Update update) {

        var msg = update.getMessage();
        if (msg == null || !msg.hasText()) {
            return;
        }

        var user = msg.getFrom();
        var id = user.getId();
        var userName = user.getFirstName();

        var text = msg.getText();
        logger.info("User [{}] with ID [{}] wrote: {}", userName, id, text);

        UserState currentState = UserDatabase.getUserStateFromDatabase(id);

        // Проверка на состояние "NOT_PAID" (если прошло больше месяца с последней оплаты)
        if (currentState == UserState.NOT_PAID) {
            logger.warn("User [{}] with ID [{}] is NOT_PAID. Sending notification and resetting state.", userName, id);
            sendMessage(id, MessageType.NOT_PAID);
            UserDatabase.saveUserStateToDatabase(id, userName, UserState.NEW_USER, null);
            return; // Если пользователь не оплатил, выходим из метода
        }

        if (adminService.isAdmin(id) && currentState != UserState.PAID) {
            if (currentState == UserState.ADMIN_MES) {
                handleChangeUserState(id, text);
                UserDatabase.saveUserStateToDatabase(id, userName, UserState.ADMIN, null);
            }
            switch (text) {
                case "/admin":
                    logger.info("Admin [{}] accessed admin panel", id);
                    handleAdmin(id, userName, currentState);
                    adminService.showAdminPanel(id);
                    break;
                case "Просмотреть всех пользователей":
                    logger.info("Admin [{}] requested to view all users", id);
                    adminService.showAllUsers(id);
                    break;
                case "Сменить состояние пользователя":
                    logger.info("Admin [{}] requested to change user state", id);
                    sendMessage(id, MessageType.PROMPT_USER_ID);
                    UserDatabase.saveUserStateToDatabase(id, userName, UserState.ADMIN_MES, null);
                    break;
                case "Назад":
                    logger.info("Admin [{}] navigated back", id);
                    handleBack(id, userName, currentState);
                    break;
            }
        }
        if (currentState == UserState.PAID) {
            switch (text) {
                case "Оплачено":
                    logger.info("User [{}] selected 'Оплачено'", id);
                    showPaidPanel(id, currentState);
                    break;
                case "Установить напоминание":
                    logger.info("User [{}] requested reminders", id);
                    sendMessage(id, MessageType.ENTER_REMINDER_TIME);  // Запрашиваем время
                    UserDatabase.saveUserStateToDatabase(id, userName, UserState.SET_REMINDER_TIME, null);
                    break;
                case "Удалить напоминание":
                    var time = UserDatabase.getReminderTime(id);
                    if (time != null){
                        logger.info("User [{}] deleted reminders", id);
                        sendMessage(id, MessageType.DELETE_REMINDER_TIME);
                        UserDatabase.saveUserStateToDatabase(id, userName, UserState.PAID, null);
                    }
                    else {
                        logger.info("User [{}] try to deleted reminders again", id);
                        sendMessage(id, MessageType.DELETE_REMINDER_TIME_AGAIN);
                    }
                    break;
                case "Отправить тренировку":
                    logger.info("User [{}] selected 'Отправить тренировку'", id);
                    handleSendTraining(id);
                    break;
                case "Отправить фото еды":
                    logger.info("User [{}] selected 'Отправить фото еды'", id);
                    handleSendFoodPhoto(id);
                    break;
                default:
                    logger.warn("User [{}] entered an invalid command", id);
                    sendInvalidCommandMessage(id);
                    break;
            }
        }else if (currentState == UserState.SET_REMINDER_TIME) {
            // Проверка, что введено правильное время
            if (isValidTimeFormat(text)) {
                logger.info("User [{}] set a valid reminder time: {}", id, text);
                UserDatabase.saveReminderTime(id, text);  // Сохраняем в базе
                sendMessage(id, MessageType.REMINDER_SET);  // Подтверждение
                UserDatabase.saveUserStateToDatabase(id, msg.getFrom().getFirstName(), UserState.PAID, text);
            } else {
                logger.warn("User [{}] entered an invalid time format: {}", id, text);
                sendMessage(id, MessageType.INVALID_TIME_FORMAT);  // Некорректный формат времени
            }
        }
        else{
            switch (text) {
                case "/start":
                    logger.info("User [{}] started the bot", id);
                    handleStart(id, userName, currentState);
                    break;
                case "План питания":
                    logger.info("User [{}] selected 'План питания'", id);
                    handleFoodPlan(id, userName,currentState);
                    break;
                case "Тренировки":
                    logger.info("User [{}] selected 'Тренировки'", id);
                    handleTraining(id, userName,currentState);
                    break;
                case "Оплатить":
                    logger.info("User [{}] selected 'Оплатить'", id);
                    handlePay(id,currentState);
                    break;
                case "Инвентарь для питания":
                    logger.info("User [{}] selected 'Инвентарь для питания'", id);
                    handleFoodInventory(id, currentState);
                    break;
                case "Задачи":
                    logger.info("User [{}] selected 'Задачи'", id);
                    handleFoodTasks(id, currentState);
                    break;
                case "Инвентарь для тренировок":
                    logger.info("User [{}] selected 'Инвентарь для тренировок'", id);
                    handleTrainingInventory(id, currentState);
                    break;
                case "Созвон":
                    logger.info("User [{}] selected 'Созвон'", id);
                    handleCall(id, currentState);
                    break;
                case "Танцы":
                    logger.info("User [{}] selected 'Танцы'", id);
                    handleDance(id, currentState);
                    break;
                case "Назад":
                    if (!adminService.isAdmin(id)) {
                        logger.info("User [{}] navigated back", id);
                        handleBack(id, userName, currentState);
                        break;
                    }
                default:
                    if (!adminService.isAdmin(id)) {
                        logger.warn("User [{}] entered an invalid command", id);
                        sendInvalidCommandMessage(id);
                        break;
                    }
            }
        }
    }
    private boolean isValidTimeFormat(String time) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            LocalTime.parse(time, formatter);  // Используем LocalTime вместо LocalDateTime
            return true;
        } catch (Exception e) {
            logger.error("Time format validation failed for: {}", time, e);
            return false;
        }
    }
    // Методы для обработки сообщений
    private void handleAdmin(Long id,String userName, UserState currentState) {
        UserDatabase.saveUserStateToDatabase(id, userName, UserState.ADMIN, null);
    }
    private void handleChangeUserState(Long adminId, String message) {
        // Получаем ID пользователя для смены состояния
        try {
            Long userId = Long.parseLong(message.trim());
            // Запрос на новый статус пользователя
            sendMessage(adminId, MessageType.PROMPT_USER_STATE);
            // Сохранение состояния в базе данных
            adminService.updateUserStateToPaid(adminId, userId);
        } catch (NumberFormatException e) {
            sendMessage(adminId, MessageType.INVALID_USER_ID);
        }
    }
    // панель админа
    public void showPaidPanel(Long id, UserState State) {
        if (State == UserState.PAID) {
            sendMessage(id, MessageType.PAID_USER);
        }
    }


    private void handleStart(Long id,String userName, UserState currentState) {
        sendMessage(id, MessageType.WELCOME);
        UserDatabase.saveUserStateToDatabase(id, userName, UserState.NEW_USER, null);
    }

    private void handleFoodPlan(Long id, String userName, UserState currentState) {
        if (currentState == UserState.NEW_USER) {
            sendMessage(id, MessageType.FOOD_PLAN);
            UserDatabase.saveUserStateToDatabase(id, userName, UserState.FOOD_PLAN, null);
        } else {
            sendInvalidCommandMessage(id);
        }
    }

    private void handleTraining(Long id, String userName, UserState currentState) {
        if (currentState == UserState.NEW_USER) {
            sendMessage(id, MessageType.TRAINING);
            UserDatabase.saveUserStateToDatabase(id, userName, UserState.TRAINING, null);
        } else {
            sendInvalidCommandMessage(id);
        }
    }

    private void handlePay(Long id, UserState currentState) {
        if (currentState == UserState.NEW_USER) {
            sendMessage(id, MessageType.PAY);
        } else {
            sendInvalidCommandMessage(id);
        }
    }


    private void handleFoodInventory(Long id, UserState currentState) {
        if (currentState == UserState.FOOD_PLAN) {
            sendMessage(id, MessageType.FOOD_INVENTORY);
        } else {
            sendInvalidCommandMessage(id);
        }
    }

    private void handleFoodTasks(Long id, UserState currentState) {
        if (currentState == UserState.FOOD_PLAN) {
            sendMessage(id, MessageType.FOOD_TASKS);
        } else {
            sendInvalidCommandMessage(id);
        }
    }

    private void handleTrainingInventory(Long id, UserState currentState) {
        if (currentState == UserState.TRAINING) {
            sendMessage(id, MessageType.TRAINING_INVENTORY);
        } else {
            sendInvalidCommandMessage(id);
        }
    }

    private void handleCall(Long id, UserState currentState) {
        if (currentState == UserState.TRAINING) {
            sendMessage(id, MessageType.CALL);
        } else {
            sendInvalidCommandMessage(id);
        }
    }

    private void handleDance(Long id, UserState currentState) {
        if (currentState == UserState.TRAINING) {
            sendMessage(id, MessageType.DANCE);
        } else {
            sendInvalidCommandMessage(id);
        }
    }

    private void handleBack(Long id, String userName, UserState currentState) {
        if (currentState == UserState.FOOD_PLAN || currentState == UserState.TRAINING || currentState == UserState.ADMIN) {
            sendMessage(id, MessageType.BACK);
            UserDatabase.saveUserStateToDatabase(id, userName, UserState.NEW_USER, null);
        } else {
            sendInvalidCommandMessage(id);
        }
    }


    private void handleSendTraining(Long id) {
        sendMessage(id, MessageType.SEND_TRAINING); //прием фото или текста не реализован
    }

    private void handleSendFoodPhoto(Long id) {
        sendMessage(id, MessageType.SEND_FOOD_PHOTO); //прием фото или текста не реализован
    }

    public void sendMessage(Long chatId, MessageType messageType) {
        SendMessage sm = SendMessage.builder()
                .chatId(chatId.toString())
                .text(messageType.getText())
                .replyMarkup(createKeyboard(messageType))
                .build();
        executeMessage(sm);
    }


    private static ReplyKeyboardMarkup createKeyboard(MessageType messageType) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        KeyboardRow row2 = new KeyboardRow();

        switch (messageType) {
            case WELCOME, BACK:
                row1.add(new KeyboardButton("План питания"));
                row1.add(new KeyboardButton("Тренировки"));
                row2.add(new KeyboardButton("Оплатить"));
                keyboardRows.add(row1);
                keyboardRows.add(row2);
                break;
            case FOOD_PLAN:
                row1.add(new KeyboardButton("Инвентарь для питания"));
                row1.add(new KeyboardButton("Задачи"));
                row2.add(new KeyboardButton("Назад"));
                keyboardRows.add(row1);
                keyboardRows.add(row2);
                break;
            case TRAINING:
                row1.add(new KeyboardButton("Инвентарь для тренировок"));
                row1.add(new KeyboardButton("Созвон"));
                row2.add(new KeyboardButton("Танцы"));
                row2.add(new KeyboardButton("Назад"));
                keyboardRows.add(row1);
                keyboardRows.add(row2);
                break;
            case ADMIN_PANEL:
                row1.add(new KeyboardButton("Просмотреть всех пользователей"));
                row1.add(new KeyboardButton("Сменить состояние пользователя"));
                row2.add(new KeyboardButton("Назад"));
                keyboardRows.add(row1);
                keyboardRows.add(row2);
                break;
            case PAID_USER: //панель для пользователя с состоянием PAID
                row1.add(new KeyboardButton("Установить напоминание"));
                row2.add(new KeyboardButton("Отправить тренировку"));
                row2.add(new KeyboardButton("Отправить фото еды"));
                row2.add(new KeyboardButton("Удалить напоминание"));
                keyboardRows.add(row1);
                keyboardRows.add(row2);
                break;

        }
        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    private void sendInvalidCommandMessage(Long chatId) {
        sendMessage(chatId, MessageType.INVALID_COMMAND);
    }

    public void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    // Метод для запуска напоминаний каждую минуту
    public void startReminderScheduler() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        scheduler.scheduleAtFixedRate(() -> {
            reminderService.checkAndSendReminder();
        }, 0, 1, TimeUnit.MINUTES);
    }

    public static void main(String[] args) throws TelegramApiException {
        UserDatabase.createTable(); // Создание таблицы при запуске бота
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        Bot bot = new Bot();
        botsApi.registerBot(bot);
        bot.startReminderScheduler();
        logger.info("Bot started successfully.");
        UserDatabase.printUserStates(); // Печать данных в консоль
    }
}