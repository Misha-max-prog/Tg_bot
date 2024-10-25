import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class Bot extends TelegramLongPollingBot {

    private final Map<Long, UserState> userState = new HashMap<>();

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

        System.out.println(user.getFirstName() + " wrote " + msg.getText());

        UserState currentState = userState.getOrDefault(id, UserState.NO_USER);

        switch (msg.getText()) {
            case "/start":
                handleStart(id, currentState);
                break;
            case "План питания":
                handleFoodPlan(id, currentState);
                break;
            case "Тренировки":
                handleTraining(id, currentState);
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
                handleBack(id, currentState);
                break;
            default:
                sendInvalidCommandMessage(id);
                break;
        }
    }

    // Методы для обработки сообщений
    private void handleStart(Long id, UserState currentState) {
        if (currentState == UserState.NO_USER) {
            sendMessage(id, MessageType.WELCOME);
            userState.put(id, UserState.NEW_USER);
        } else {
            sendInvalidCommandMessage(id);
        }
    }

    private void handleFoodPlan(Long id, UserState currentState) {
        if (currentState == UserState.NEW_USER) {
            sendMessage(id, MessageType.FOOD_PLAN);
            userState.put(id, UserState.FOOD_PLAN);
        } else {
            sendInvalidCommandMessage(id);
        }
    }

    private void handleTraining(Long id, UserState currentState) {
        if (currentState == UserState.NEW_USER) {
            sendMessage(id, MessageType.TRAINING);
            userState.put(id, UserState.TRAINING);
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

    private void handleBack(Long id, UserState currentState) {
        if (currentState == UserState.FOOD_PLAN || currentState == UserState.TRAINING) {
            sendMessage(id, MessageType.BACK);
            userState.put(id, UserState.NEW_USER);
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
                keyboardRows.add(row1);
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

        }
        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    private void sendInvalidCommandMessage(Long chatId) {
        sendMessage(chatId, MessageType.INVALID_COMMAND);
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        Bot bot = new Bot();
        botsApi.registerBot(bot);
    }
}