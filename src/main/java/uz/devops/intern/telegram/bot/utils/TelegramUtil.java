package uz.devops.intern.telegram.bot.utils;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

public class TelegramUtil {
    public static saveCustomerTelegramToDatabase(User telegramUser, String phoneNumber) {
        CustomerTelegram customerTelegram = new CustomerTelegram()
            .isBot(telegramUser.getIsBot())
            .telegramId(telegramUser.getId())
            .canJoinGroups(telegramUser.getCanJoinGroups())
            .firstname(telegramUser.getFirstName())
            .username(telegramUser.getUserName())
            .languageCode(telegramUser.getLanguageCode())
            .step(1)
            .phoneNumber(phoneNumber)
            .isActive(true);

        customerTelegramRepository.save(customerTelegram);
    }


    /**
     * param: String chatId
     * param: String text

     * return: SendMessage without buttons
     */
    public static SendMessage sendMessage(String chatId, String text){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);

        return sendMessage;
    }

    /**
     * param: String chatId
     * param: String text

     * return: SendMessage with buttons
     */
    public static SendMessage sendMessage(String chatId, String text, ReplyKeyboardMarkup keyboardMarkup){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        sendMessage.setReplyMarkup(keyboardMarkup);

        return sendMessage;
    }
}
