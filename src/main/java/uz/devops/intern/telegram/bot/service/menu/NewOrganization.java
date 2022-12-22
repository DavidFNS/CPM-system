package uz.devops.intern.telegram.bot.service.menu;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import uz.devops.intern.domain.User;
import uz.devops.intern.service.BotTokenService;
import uz.devops.intern.service.OrganizationService;
import uz.devops.intern.service.UserService;
import uz.devops.intern.service.dto.BotTokenDTO;
import uz.devops.intern.service.dto.CustomerTelegramDTO;
import uz.devops.intern.service.dto.OrganizationDTO;
import uz.devops.intern.service.dto.ResponseDTO;
import uz.devops.intern.service.utils.ResourceBundleUtils;
import uz.devops.intern.telegram.bot.service.BotStrategyAbs;
import uz.devops.intern.telegram.bot.utils.TelegramsUtil;
import uz.devops.intern.web.rest.utils.WebUtils;

import java.util.ResourceBundle;


@Service
public class NewOrganization extends BotStrategyAbs {

    private final String STATE = "ORGANIZATION_NAME_AND_NEW_BOT_TOKEN";
    private final Integer STEP = 4;
    private final Integer NEXT_STEP = 5;

    private final UserService userService;
    private final OrganizationService organizationService;
    private final BotTokenService botTokenService;

    public NewOrganization(UserService userService, OrganizationService organizationService, BotTokenService botTokenService) {
        this.userService = userService;
        this.organizationService = organizationService;
        this.botTokenService = botTokenService;
    }

    @Override
    public boolean execute(Update update, CustomerTelegramDTO manager) {
        ResourceBundle bundle = ResourceBundleUtils.getResourceBundleByUserLanguageCode(manager.getLanguageCode());
        ResponseDTO<User> response = userService.getUserByPhoneNumber(manager.getPhoneNumber());
        if(!response.getSuccess() && response.getResponseData() == null){
            log.warn("User is not found! Manager id: {} | Manager phone number: {} | Response: {}",
                manager.getTelegramId(), manager.getPhoneNumber(), response);
            // Here should throw exception for send message to admin about error
            return false;
        }
        if(!update.hasMessage() || !update.getMessage().hasText()){
            wrongValue(manager.getTelegramId(), "bot.admin.error.send.only.text");
            log.warn("User didn't send text! User id: {} | Update: {} ", manager.getTelegramId(), update);
            return false;
        }

        Message message = update.getMessage();
        WebUtils.setUserToContextHolder(response.getResponseData());
        String organizationName = message.getText();

        ResponseDTO<OrganizationDTO> organizationResponse =
            organizationService.getOrganizationByName(organizationName);

        if(organizationResponse.getSuccess()){
            wrongValue(message.getFrom().getId(), bundle.getString("bot.admin.error.organization.is.already.exists"));
            log.warn("Organization is already exists, Organization: {}", organizationName);
            return false;
        }

        OrganizationDTO organization = OrganizationDTO.builder()
            .name(organizationName).build();
        organization = organizationService.save(organization);
        String newMessage = bundle.getString("bot.admin.send.organization.is.saved.successfully");
        adminFeign.sendMessage(TelegramsUtil.sendMessage(manager.getTelegramId(), newMessage));
//        System.out.println("Organization is saved: " + organization);

        basicFunction(manager, bundle);
        log.info("Manager is added new organization, Organization: {} | Manager id: {}: {} | Message text: {}",
            organization, manager.getTelegramId(), organizationName);
        return true;
    }

    public void basicFunction(CustomerTelegramDTO manager, ResourceBundle bundle){
        ResponseDTO<BotTokenDTO> response = botTokenService.findByManagerId(manager.getTelegramId());
        if(!response.getSuccess() || response.getResponseData() == null){
            wrongValue(manager.getTelegramId(), bundle.getString("bot.admin.error.please.connect.to.developer"));
            log.warn("Bot is not found! User id: {} ", manager.getTelegramId());
            return;
        }
        String newMessage =
            bundle.getString("bot.admin.send.add.bot.to.telegram.group") + " <pre>@" + response.getResponseData().getUsername() + "</pre>" + "\n\n" + bundle.getString("bot.admin.send.press.button.for.copying");

        ReplyKeyboardRemove remove = new ReplyKeyboardRemove(true);
        SendMessage sendMessage = TelegramsUtil.sendMessage(manager.getTelegramId(), newMessage, remove);
        adminFeign.sendMessage(sendMessage);
        manager.setStep(NEXT_STEP);
    }

    private boolean cancelProcess(Message message, CustomerTelegramDTO manager){
        ResourceBundle bundle = ResourceBundleUtils.getResourceBundleByUserLanguageCode(manager.getLanguageCode());
        if(!message.hasText() || !message.getText().equals(bundle.getString("bot.admin.keyboard.cancel.process"))){
            return false;
        }


        manager.setStep(7);
        return true;
    }

    @Override
    public String getState() {
        return STATE;
    }

    @Override
    public Integer getStep() {
        return STEP;
    }
}
