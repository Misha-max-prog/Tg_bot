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
import java.util.List;

public class Bot extends TelegramLongPollingBot {

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

        // Проверяем, какая кнопка нажата или сообщение отправлено
        switch (msg.getText()) {
            case "/start":
                sendWelcomeMessage(id);
                break;
            case "Тык":
                sendText(id, "Не тыкай тут");
                break;
            case "Copy Message":
                sendText(id, "Это фиксированное сообщение вместо копирования!");
                break;
            default:
                sendText(id, "Вы сказали: "+msg.getText()+". Однако сейчас я не могу вам ответить, ждите апдейтов :)");
                break;
        }
    }

    // Метод для отправки приветственного сообщения с клавиатурой
    public void sendWelcomeMessage(Long chatId) {
        SendMessage sm = SendMessage.builder()
                .chatId(chatId.toString())
                .text("Добро пожаловать! Выберите действие:")
                .replyMarkup(createKeyboard()) // Добавляем клавиатуру к сообщению
                .build();
        try {
            execute(sm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    // Создаем клавиатуру
    private ReplyKeyboardMarkup createKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true); // Клавиатура будет подстраиваться под размер экрана

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        // Первый ряд кнопок
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Тык"));
        row1.add(new KeyboardButton("Copy Message"));

        // Добавляем ряды в список
        keyboardRows.add(row1);

        // Устанавливаем клавиатуру
        keyboardMarkup.setKeyboard(keyboardRows);

        return keyboardMarkup;
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
    /*
    public void copyMessage(Long who, Integer msgId) {
        CopyMessage cm = CopyMessage.builder()
                .fromChatId(who.toString())
                .chatId(who.toString())
                .messageId(msgId)
                .build();
        try {
            execute(cm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
    */
    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        Bot bot = new Bot();
        botsApi.registerBot(bot);
    }
}