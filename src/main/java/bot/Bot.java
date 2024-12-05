package bot;

import admin.AdminService;
import database.UserDatabase;
import util.Token;

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
        System.out.println(user.getFirstName() + " wrote " + text);

        UserState currentState = UserDatabase.getUserStateFromDatabase(id);
        // Проверка на состояние "NOT_PAID" (если прошло больше месяца с последней оплаты)
        if (currentState == UserState.NOT_PAID) {
            sendMessage(id, MessageType.NOT_PAID);
            UserDatabase.saveUserStateToDatabase(id, userName, UserState.NEW_USER, null, null);
            return; // Если пользователь не оплатил, выходим из метода
        }

        if (adminService.isAdmin(id) && currentState != UserState.PAID) {
            if (currentState == UserState.ADMIN_MES) {
                handleChangeUserState(id, text);
                UserDatabase.saveUserStateToDatabase(id, userName, UserState.ADMIN, null, null);
            }
            switch (text) {
                case "/admin":
                    handleAdmin(id, userName, currentState);
                    adminService.showAdminPanel(id);
                    break;
                case "Просмотреть всех пользователей":
                    adminService.showAllUsers(id);
                    break;
                case "Сменить состояние пользователя":
                    sendMessage(id, MessageType.PROMPT_USER_ID);
                    UserDatabase.saveUserStateToDatabase(id, userName, UserState.ADMIN_MES, null, null);
                    break;
                case "Назад":
                    handleBack(id, userName, currentState);
                    break;
            }
        }
        if (currentState == UserState.PAID) {
            switch (text) {
                case "Оплачено":
                    showPaidPanel(id, currentState);
                    break;
                case "Напоминания":
                    sendMessage(id, MessageType.ENTER_REMINDER_TIME);  // Запрашиваем время
                    UserDatabase.saveUserStateToDatabase(id, userName, UserState.SET_REMINDER_TIME, null, null);
                    break;
                case "Отправить тренировку":
                    handleSendTraining(id);
                    break;
                case "Отправить фото еды":
                    handleSendFoodPhoto(id);
                    break;
                case "/start":
                    handleStart(id, userName, currentState);
                    break;
                default:
                    sendInvalidCommandMessage(id);
                    break;
            }
        }else if (currentState == UserState.SET_REMINDER_TIME) {
            // Проверка, что введено правильное время
            if (isValidTimeFormat(text)) {
                UserDatabase.saveReminderTime(id, text);  // Сохраняем в базе
                sendMessage(id, MessageType.REMINDER_SET);  // Подтверждение
                UserDatabase.saveUserStateToDatabase(id, msg.getFrom().getFirstName(), UserState.PAID, text, null);
            } else {
                sendMessage(id, MessageType.INVALID_TIME_FORMAT);  // Некорректный формат времени
            }
        }
        else{
            switch (text) {
                case "/start":
                    handleStart(id, userName, currentState);
                    break;
                case "План питания":
                    handleFoodPlan(id, userName,currentState);
                    break;
                case "Тренировки":
                    handleTraining(id, userName,currentState);
                    break;
                case "Оплатить":
                    handlePay(id,currentState);
                    break;
                case "Инвентарь для питания":
                    handleFoodInventory(id, currentState);
                    break;
                case "Задачи":
                    handleFoodTasks(id, currentState);
                    break;
                case "Инвентарь для тренировок":
                    handleTrainingInventory(id, currentState);
                    break;
                case "Созвон":
                    handleCall(id, currentState);
                    break;
                case "Танцы":
                    handleDance(id, currentState);
                    break;
                case "Назад":
                    if (!adminService.isAdmin(id)) {
                        handleBack(id, userName, currentState);
                        break;
                    }
                default:
                    if (!adminService.isAdmin(id)) {
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
            return false;
        }
    }
    // Методы для обработки сообщений
    private void handleAdmin(Long id,String userName, UserState currentState) {
        UserDatabase.saveUserStateToDatabase(id, userName, UserState.ADMIN, null, null);
    }
    private void handleChangeUserState(Long adminId, String message) {
        // Получаем ID пользователя для смены состояния
        System.out.println("id: " + message);
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
        UserDatabase.saveUserStateToDatabase(id, userName, UserState.NEW_USER, null, null);
    }

    private void handleFoodPlan(Long id, String userName, UserState currentState) {
        if (currentState == UserState.NEW_USER) {
            sendMessage(id, MessageType.FOOD_PLAN);
            UserDatabase.saveUserStateToDatabase(id, userName, UserState.FOOD_PLAN, null, null);
        } else {
            sendInvalidCommandMessage(id);
        }
    }

    private void handleTraining(Long id, String userName, UserState currentState) {
        if (currentState == UserState.NEW_USER) {
            sendMessage(id, MessageType.TRAINING);
            UserDatabase.saveUserStateToDatabase(id, userName, UserState.TRAINING, null, null);
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
            UserDatabase.saveUserStateToDatabase(id, userName, UserState.NEW_USER, null, null);
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
                row1.add(new KeyboardButton("Напоминания"));
                row2.add(new KeyboardButton("Отправить тренировку"));
                row2.add(new KeyboardButton("Отправить фото еды"));
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
        UserDatabase.printUserStates(); // Печать данных в консоль
    }
}