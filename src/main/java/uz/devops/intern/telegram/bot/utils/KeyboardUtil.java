package uz.devops.intern.telegram.bot.utils;

import org.checkerframework.checker.units.qual.K;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class KeyboardUtil {

    private final static List<String> LANGUAGES = List.of(
        "\uD83C\uDDFA\uD83C\uDDFF O`zbekcha", "\uD83C\uDDF7\uD83C\uDDFA Русский");

    public static List<String> availableLanguages(){
        return LANGUAGES;
    }

    public static ReplyKeyboardMarkup language(){
        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        for(String language: LANGUAGES){
            row.add(new KeyboardButton(language));
            if(row.size() == 2){
                rows.add(row);
                row = new KeyboardRow();
            }
        }
        if(!row.isEmpty()) rows.add(row);

        row.add(new KeyboardButton("\uD83C\uDDFA\uD83C\uDDFF O`zbekcha"));
        row.add(new KeyboardButton("\uD83C\uDDF7\uD83C\uDDFA Русский"));

        ReplyKeyboardMarkup markup =  new ReplyKeyboardMarkup(rows);
        markup.setResizeKeyboard(true);
        return markup;
    }

    public static ReplyKeyboardMarkup phoneNumber(){
        KeyboardButton button = new KeyboardButton("\uD83D\uDCF2 Telefon raqamni jo'natish");
        button.setRequestContact(true);

        KeyboardRow row = new KeyboardRow();
        row.add(button);

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(List.of(row));
        markup.setResizeKeyboard(true);
        return markup;
    }

    public static ReplyKeyboardMarkup sendMarkup(){
        KeyboardButton button = new KeyboardButton("\uD83D\uDCF1 Telefon raqam");
        button.setRequestContact(true);

        KeyboardRow row = new KeyboardRow();
        row.add(button);

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        markup.setKeyboard(List.of(row));

        return markup;
    }
}
