package uz.devops.intern.telegram.bot.utils;

import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import uz.devops.intern.domain.CustomerTelegram;
import uz.devops.intern.service.utils.ResourceBundleUtils;

import java.util.*;

import static uz.devops.intern.constants.ResourceBundleConstants.BOT_PHONE_NUMBER_BUTTON;
import static uz.devops.intern.service.utils.ResourceBundleUtils.getResourceBundleUsingCustomerTelegram;
import static uz.devops.intern.service.utils.ResourceBundleUtils.getResourceBundleUsingTelegramUser;

public class KeyboardUtil {
    public static final String UZ_LANGUAGE = "\uD83C\uDDFA\uD83C\uDDFF Uzbek";
    public static final String RU_LANGUAGE = "\uD83C\uDDF7\uD83C\uDDFA Русский";
    public static final String EN_LANGUAGE = "\uD83C\uDDEC\uD83C\uDDE7 English";

    private final static List<String> LANGUAGES = List.of(
        UZ_LANGUAGE, RU_LANGUAGE, EN_LANGUAGE
    );

    private static final Map<String, String> languageMap = Map.of(
        UZ_LANGUAGE, "uz",
        RU_LANGUAGE,"ru",
        EN_LANGUAGE ,"en"
    );

    public static Map<String, String> getLanguages(){
        return languageMap;
    }
    public static List<String> availableLanguages(){
        return LANGUAGES;
    }

    public static ReplyKeyboardMarkup language(){
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton(UZ_LANGUAGE));
        row.add(new KeyboardButton(RU_LANGUAGE));
        row.add(new KeyboardButton(EN_LANGUAGE));
        ReplyKeyboardMarkup markup =  new ReplyKeyboardMarkup(List.of(row));
        markup.setResizeKeyboard(true);
        return markup;
    }

    public static ReplyKeyboardMarkup phoneNumber(String languageCode){
        ResourceBundle bundle = ResourceBundleUtils.getResourceBundleByUserLanguageCode(languageCode);
        KeyboardButton button = new KeyboardButton(bundle.getString("bot.message.phone.number.button"));
        button.setRequestContact(true);

        KeyboardRow row = new KeyboardRow();
        row.add(button);

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(List.of(row));
        markup.setResizeKeyboard(true);
        return markup;
    }

    public static ReplyKeyboardMarkup sendMarkup(User telegramUser){
        ResourceBundle resourceBundle = getResourceBundleUsingTelegramUser(telegramUser);
        return getReplyKeyboardMarkup(resourceBundle);
    }

    public static ReplyKeyboardMarkup sendMarkup(CustomerTelegram customerTelegram){
        ResourceBundle resourceBundle = getResourceBundleUsingCustomerTelegram(customerTelegram);
        return getReplyKeyboardMarkup(resourceBundle);
    }

    private static ReplyKeyboardMarkup getReplyKeyboardMarkup(ResourceBundle resourceBundle) {
        KeyboardButton button = new KeyboardButton(resourceBundle.getString(BOT_PHONE_NUMBER_BUTTON));
        button.setRequestContact(true);

        KeyboardRow row = new KeyboardRow();
        row.add(button);

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        markup.setKeyboard(List.of(row));

        return markup;
    }

    public static ReplyKeyboardMarkup sendBackHomeButton(String text){
        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add(new KeyboardButton(text));
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup(List.of(keyboardRow));
        replyKeyboardMarkup.setResizeKeyboard(true);

        return replyKeyboardMarkup;
    }
}
