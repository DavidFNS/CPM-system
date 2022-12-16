package uz.devops.intern.telegram.bot.service.commands;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import uz.devops.intern.feign.AdminFeign;
import uz.devops.intern.service.CustomerTelegramService;
import uz.devops.intern.service.dto.CustomerTelegramDTO;
import uz.devops.intern.service.dto.ResponseDTO;
import uz.devops.intern.service.utils.ResourceBundleUtils;
import uz.devops.intern.telegram.bot.service.BotCommandAbs;
import uz.devops.intern.telegram.bot.utils.KeyboardUtil;
import uz.devops.intern.telegram.bot.utils.TelegramsUtil;

import java.util.ResourceBundle;

@Service
public class StartCommand extends BotCommandAbs {

    private final String COMMAND = "/start";

    private final CustomerTelegramService customerTelegramService;

    protected StartCommand(AdminFeign adminFeign, CustomerTelegramService customerTelegramService) {
        super(adminFeign);
        this.customerTelegramService = customerTelegramService;
    }

    @Override
    public boolean executeCommand(Update update, Long userId) {
        if(!update.hasMessage() || !update.getMessage().hasText()){
//            wrongValue(userId, "");
            log.warn("User didn't send message");
            return false;
        }

        Message message = update.getMessage();
        String messageText = message.getText();

        ResponseDTO<CustomerTelegramDTO> response =
            customerTelegramService.findByTelegramId(message.getFrom().getId());

        if(response.getSuccess() && response.getResponseData() != null) {
            ResourceBundle bundle = ResourceBundleUtils.getResourceBundleByCustomerTgDTO(response.getResponseData());
            wrongValue(update.getMessage().getFrom().getId(), bundle.getString("bot.admin.user.is.already.exists"));
            return false;
        }
        startProcess(message, null);
        return true;
    }

    void startProcess(Message message, Long userId){
        ResourceBundle bundle = ResourceBundleUtils.getResourceBundleByUserLanguageCode("uz");
        String newMessage = bundle.getString("bot.message.choice.language");

        bundle = ResourceBundleUtils.getResourceBundleByUserLanguageCode("ru");
        newMessage = newMessage + "\n\n" + bundle.getString("bot.message.choice.language");

        bundle = ResourceBundleUtils.getResourceBundleByUserLanguageCode("en");
        newMessage = newMessage + "\n\n" + bundle.getString("bot.message.choice.language");

        ReplyKeyboardMarkup markup = KeyboardUtil.language();
        SendMessage sendMessage = TelegramsUtil.sendMessage(message.getFrom().getId(), newMessage, markup);
        Update newUpdate = adminFeign.sendMessage(sendMessage);
        log.info("Message send successfully! User id: {} | Update: {}",
            message.getFrom().getId(), newUpdate);
        CustomerTelegramDTO customer = TelegramsUtil.createCustomerTelegramDTO(message.getFrom(), userId);
        customerTelegramService.update(customer);
    }

    @Override
    public String getCommand() {
        return COMMAND;
    }
}
