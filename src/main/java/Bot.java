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

    private  Map<Long, Integer> userState = new HashMap<>();

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

        int currentState = userState.getOrDefault(id, 0);

        switch (msg.getText()) {
            case "/start":
                if (currentState == 0) {
                    sendWelcomeMessage(id, 1);
                    userState.put(id, 1);
                }
                else sendInvalidCommandMessage(id);
                break;
            case "План питания":
                if (currentState == 1) {
                    sendText(id, "Выстраивание гибкого и здорового рациона в соответсвии с твоими задачами. И вкусное, и полезное по заветаи гибкой диеты.");
                    sendMessageWithKeyboard(id, 2);
                    userState.put(id, 2);
                }
                else sendInvalidCommandMessage(id);
                break;
            case "Тренировки":
                if (currentState == 1) {
                    sendText(id, "Грамотная программа упражнений с описанием техники их выполнения и видеоинструкций. Обсудим твои спортивные задачи и придём к согласию по программе тренировок и количеству занятий.");
                    sendMessageWithKeyboard(id, 3);
                    userState.put(id, 3);
                }
                else sendInvalidCommandMessage(id);

                break;
            case "Инвентарь для питания":
                if (currentState == 2) {
                    sendText(id, "Кухонные весы, напольные весы, метровая лента, шагомер, счетчик калорий FatSecret.");
                }
                else sendInvalidCommandMessage(id);
                break;
            case "Задачи":
                if (currentState == 2) {
                    sendText(id, "Необходимо взвешивать себя каждый день. Взвешивать еду и фиксировать её в приложении, считать шаги, отправлять Маше отчеты.");
                }
                else sendInvalidCommandMessage(id);
                break;
            case "Инвентарь для тренировок":
                if (currentState == 3) {
                    sendText(id, "Телефон, чтобы снимать себя на видео, абонемент в любой тренажерный зал.");
                }
                else sendInvalidCommandMessage(id);
                break;
            case "Созвон":
                if (currentState == 3) {
                    sendText(id, "Подробно обсудим все твои вопросы в формате видеозвонка в удобное для тебя время за деньги.");
                }
                else sendInvalidCommandMessage(id);
                break;
            case "Танцы":
                if (currentState == 3) {
                    sendText(id, "Персональные и групповые занятия пока только очно.");
                }
                else sendInvalidCommandMessage(id);
                break;
            case "Назад":
                if (currentState == 2 || currentState == 3) {
                    sendMessageWithKeyboard(id, 4);
                    sendWelcomeMessage(id, 1);
                    userState.put(id, 1);
                }
                else sendInvalidCommandMessage(id);
                break;
            default:
                sendInvalidCommandMessage(id);
                break;
        }
    }
    // Метод для отправки приветственного сообщения с клавиатурой
    public void sendMessageWithKeyboard(Long chatId, int key) {
        SendMessage sm = SendMessage.builder()
                .chatId(chatId.toString())
                .text(sendMessage(key))
                .replyMarkup(createKeyboard(key)) // Добавляем клавиатуру к сообщению
                .build();
        try {
            execute(sm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
    public String sendMessage(int value){
        String AskMessage = (String) "Вопросы по работе?";
        String HelloMessage = (String) "Привет, я бот Марии Кицы Плюшки рассказываю как начать взаимодействие и отвечаю на основные вопросы.";
        String BackMessage = (String) "Хорошо, вернёмся.";
        if (value==1){
            return HelloMessage;
        }
        if (value==2 || value==3){
            return AskMessage;
        }
        if (value==4){
            return BackMessage;
        }
        return "";
    }

    // Метод для отправки приветственного сообщения с клавиатурой
    public void sendWelcomeMessage(Long chatId, int val) {
        SendMessage sm = SendMessage.builder()
                .chatId(chatId.toString())
                .text(sendMessage(val))
                .replyMarkup(createKeyboard(1)) // Добавляем клавиатуру к сообщению
                .build();
        try {
            execute(sm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    // Создаем клавиатуру
    private ReplyKeyboardMarkup createKeyboard(int value) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true); // Клавиатура будет подстраиваться под размер экрана

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        KeyboardRow row2 = new KeyboardRow();
        // Первый ряд кнопок
        if (value==1) { //после приветствия
            row1.add(new KeyboardButton("План питания"));
            row1.add(new KeyboardButton("Тренировки"));
            // Добавляем ряды в список
            keyboardRows.add(row1);
        }
        if (value==2) { //после питания
            row1.add(new KeyboardButton("Инвентарь для питания"));
            row1.add(new KeyboardButton("Задачи"));
            row2.add(new KeyboardButton("Назад"));
            //Тут нужна кнопка назад!!!!!
            // Добавляем ряды в список
            keyboardRows.add(row1);
            keyboardRows.add(row2);
        }
        if (value==3) { //после трень
            row1.add(new KeyboardButton("Инвентарь для тренировок"));
            row1.add(new KeyboardButton("Созвон"));
            row2.add(new KeyboardButton("Танцы"));
            row2.add(new KeyboardButton("Назад"));
            //Тут нужна кнопка назад!!!!!
            // Добавляем ряды в список
            keyboardRows.add(row1);
            keyboardRows.add(row2);
        }

        // Устанавливаем клавиатуру
        keyboardMarkup.setKeyboard(keyboardRows);

        return keyboardMarkup;
    }

    private void sendInvalidCommandMessage(Long chatId) {
        sendText(chatId, "Не могу вам помочь, пожалуйста, выберите сообщение с клавиатуры.");
    }

    public void sendText(Long who, String what) {
        SendMessage sm = SendMessage.builder()
                .chatId(who.toString())
                .text(what)
                .build();
        try {
            execute(sm);
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