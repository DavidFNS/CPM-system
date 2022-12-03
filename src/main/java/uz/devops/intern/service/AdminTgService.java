package uz.devops.intern.service;

import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.devops.intern.domain.CustomerTelegram;

public interface AdminTgService {

    void main(Update update);
    void pressedStartCommand(Message message);

    void getLanguage(Message message, CustomerTelegram customer);

    void verifyAdminByLogin(Message message, CustomerTelegram customer);

    void getAdminBotToken(Message message, CustomerTelegram customer);
}
