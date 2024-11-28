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

import java.util.ArrayList;
import java.util.List;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Bot extends TelegramLongPollingBot {

    private final AdminService adminService = new AdminService(this);

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

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        var msg = update.getMessage();
        if (msg == null || !msg.hasText()) {
            return;
        }

        var user = msg.getFrom();
        var id = user.getId();
        var userName = user.getFirstName();
        var lastUsed = now.format(formatter);

        System.out.println(user.getFirstName() + " wrote " + msg.getText());

        UserState currentState = UserDatabase.getUserStateFromDatabase(id);

        if (adminService.isAdmin(id)) {
            switch (msg.getText()) {
                case "/admin":
                    handleAdmin(id, userName, currentState);
                    adminService.showAdminPanel(id);
                    break;
                case "Просмотреть всех пользователей":
                    adminService.showAllUsers(id);
                    break;
                case "Назад":
                    handleBack(id, userName,currentState, lastUsed);
                    break;
            }
        }
        switch (msg.getText()) {
            case "/start":
                handleStart(id, userName, currentState, lastUsed);
                break;
            case "План питания":
                handleFoodPlan(id, userName,currentState, lastUsed);
                break;
            case "Тренировки":
                handleTraining(id, userName,currentState, lastUsed);
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
                    handleBack(id, userName, currentState, lastUsed);
                    break;
                }
            default:
                if (!adminService.isAdmin(id)) {
                    sendInvalidCommandMessage(id);
                    break;
                }
        }
    }

    // Методы для обработки сообщений
    private void handleAdmin(Long id,String userName, UserState currentState) {
        UserDatabase.saveUserStateToDatabase(id, userName, UserState.ADMIN, null, null);
    }
    private void handleStart(Long id,String userName, UserState currentState, String lastUsed) {
        sendMessage(id, MessageType.WELCOME);
        UserDatabase.saveUserStateToDatabase(id, userName, UserState.NEW_USER, lastUsed, null);
    }

    private void handleFoodPlan(Long id, String userName, UserState currentState, String lastUsed) {
        if (currentState == UserState.NEW_USER) {
            sendMessage(id, MessageType.FOOD_PLAN);
            UserDatabase.saveUserStateToDatabase(id, userName, UserState.FOOD_PLAN, lastUsed, null);
        } else {
            sendInvalidCommandMessage(id);
        }
    }

    private void handleTraining(Long id, String userName, UserState currentState, String lastUsed) {
        if (currentState == UserState.NEW_USER) {
            sendMessage(id, MessageType.TRAINING);
            UserDatabase.saveUserStateToDatabase(id, userName, UserState.TRAINING, lastUsed, null);
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

    private void handleBack(Long id, String userName, UserState currentState, String lastUsed) {
        if (currentState == UserState.FOOD_PLAN || currentState == UserState.TRAINING || currentState == UserState.ADMIN) {
            sendMessage(id, MessageType.BACK);
            UserDatabase.saveUserStateToDatabase(id, userName, UserState.NEW_USER, lastUsed, null);
        } else {
            sendInvalidCommandMessage(id);
        }
    }

    public void sendMessage(Long chatId, MessageType messageType) {
        SendMessage sm = SendMessage.builder()
                .chatId(chatId.toString())
                .text(messageType.getText())
                .replyMarkup(createKeyboard(messageType))
                .build();
        executeMessage(sm);
    }

    private ReplyKeyboardMarkup createKeyboard(MessageType messageType) {
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
                row2.add(new KeyboardButton("Назад"));
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

    public static void main(String[] args) throws TelegramApiException {
        UserDatabase.createTable(); // Создание таблицы при запуске бота
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        Bot bot = new Bot();
        botsApi.registerBot(bot);
        UserDatabase.printUserStates(); // Печать данных в консоль
    }
}