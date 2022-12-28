package uz.devops.intern.telegram.bot.service.register;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import uz.devops.intern.domain.BotToken;
import uz.devops.intern.domain.CustomerTelegram;
import uz.devops.intern.domain.Customers;
import uz.devops.intern.domain.ResponseFromTelegram;
import uz.devops.intern.feign.AdminFeign;
import uz.devops.intern.feign.CustomerFeignClient;
import uz.devops.intern.repository.BotTokenRepository;
import uz.devops.intern.service.*;
import uz.devops.intern.service.dto.*;
import uz.devops.intern.service.utils.ResourceBundleUtils;
import uz.devops.intern.telegram.bot.AdminKeyboards;
import uz.devops.intern.telegram.bot.dto.EditMessageDTO;
import uz.devops.intern.telegram.bot.dto.PinMessageDTO;
import uz.devops.intern.telegram.bot.dto.UpdateType;
import uz.devops.intern.telegram.bot.dto.WebhookResponseDTO;
import uz.devops.intern.telegram.bot.keyboards.AdminMenuKeys;
import uz.devops.intern.telegram.bot.service.BotStrategyAbs;
import uz.devops.intern.telegram.bot.utils.TelegramsUtil;
import uz.devops.intern.web.rest.utils.WebUtils;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BotAddGroup extends BotStrategyAbs {
    private final UpdateType SUPPORTED_TYPE = UpdateType.CALLBACK_QUERY;
    private final String STATE = "MANAGER_BOT_ADD_GROUP";
    private final Integer STEP = 5;
    private final Integer NEXT_STEP = 6;

    private final BotTokenService botTokenService;
    private final UserService userService;
    private final GroupsService groupsService;
    private final TelegramGroupService telegramGroupService;
    private final CustomerTelegramService customerTelegramService;
    private final OrganizationService organizationService;
    @Value("${telegram.token}")
    private String BOT_TOKEN;

    @Override
    public String getState() {
        return STATE;
    }

    @Override
    public Integer getStep() {
        return STEP;
    }

    @Override
    public boolean execute(Update update, CustomerTelegramDTO manager) {
        ResourceBundle bundle = ResourceBundleUtils.getResourceBundleByUserLanguageCode(manager.getLanguageCode());

        boolean result = removeInlineButton(update.getCallbackQuery());
        if(!result){
            wrongValue(manager.getTelegramId(), bundle.getString("bot.admin.error.please.connect.to.developer"));
            return false;
        }
        result = basicFunctionality(manager, bundle, update.getCallbackQuery().getData());

        if(!result){
            wrongValue(manager.getTelegramId(), bundle.getString("bot.admin.error.please.connect.to.developer"));
            return false;
        }
        manager.setStep(NEXT_STEP);
        return true;
    }

    private boolean removeInlineButton(CallbackQuery callback){
        EditMessageDTO editMessageDTO = new EditMessageDTO(
          String.valueOf(callback.getFrom().getId()),
          callback.getMessage().getMessageId(),
          callback.getInlineMessageId(),
          new InlineKeyboardMarkup()
        );
        try {
            adminFeign.editMessageReplyMarkup(editMessageDTO);
            return true;
        }catch (FeignException e){
            log.error("Error while remove inline buttons! Manager id: {} | Exception: {}", callback.getFrom().getId(), e.getMessage());
            return false;
        }
    }


    public boolean basicFunctionality(CustomerTelegramDTO manager, ResourceBundle bundle, String telegramGroupId){
        String newMessage =
            bundle.getString("bot.admin.send.all.groups.attached.to.one.organization") + "\n\n" +
                bundle.getString("bot.admin.send.choose.one.organization");

        InlineKeyboardMarkup markup = createManagerOrganizationsButton(manager, telegramGroupId);
        if(markup == null){
            return false;
        }
        SendMessage sendMessage = TelegramsUtil.sendMessage(manager.getTelegramId(), newMessage, markup);
        adminFeign.sendMessage(sendMessage);
        return true;
    }

    private InlineKeyboardMarkup createManagerOrganizationsButton(CustomerTelegramDTO manager, String telegramGroupId){
        ResponseDTO<uz.devops.intern.domain.User> responseDTO = getUserByCustomerTg(manager);
        if(!responseDTO.getSuccess()){
            log.warn("User(jhi) is not found! Manager phone number: {}", manager.getPhoneNumber());
            return null;
        }
        WebUtils.setUserToContextHolder(responseDTO.getResponseData());
        List<OrganizationDTO> organizations = organizationService.getOrganizationsByUserLogin();
        if(organizations.isEmpty()){
            log.warn("Organizations not found! Manager phone number: {}", manager.getPhoneNumber());
            return null;
        }

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for(OrganizationDTO organization: organizations){
            List<InlineKeyboardButton> buttons = List.of(
                InlineKeyboardButton.builder()
                    .text(organization.getName())
                    .callbackData(organization.getId() + ":" + telegramGroupId)
                    .build());

            rows.add(buttons);
        }
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }

    public void execute(Update update, String botId){
        if(update.getMessage().getNewChatMembers().size() > 0){
            log.info("New bot, Bot id: {}", botId);
            checkIsBotInGroup(update.getMessage().getNewChatMembers().get(0), update.getMessage().getChat(), botId);
        }else if(update.getMessage().getLeftChatMember() != null && update.getMessage().getLeftChatMember().getIsBot()){
            log.info("Bot is kicked! Bot id: {}", botId);
            botIsLeft(update.getMessage().getLeftChatMember());
        }
    }

    public boolean botIsLeft(User bot){
        ResponseDTO<BotTokenDTO> responseDTO = botTokenService.findByChatId(bot.getId(), true);
        if(!responseDTO.getSuccess()){
            log.warn("Bot is not found! Bot telegram id: {} | User: {} ", bot.getId(), bot);
            return false;
        }
//        URI uri = createCustomerURI(responseDTO.getResponseData().getToken());
        return true;
    }

    public boolean checkIsBotInGroup(User user, Chat chat, String botId) {
        boolean isBot = user.getIsBot();
        if(!isBot){
            log.warn("New user is not bot! Bot id: {} | New user: {}", botId, user);
            return false;
        }

        String myBotTgId = BOT_TOKEN.split(":")[0];
        if(String.valueOf(user.getId()).equals(myBotTgId)){
            log.warn("Manager add to group not that bot! Bot id: {}", botId);
            WebhookResponseDTO response = adminFeign.leaveChat(String.valueOf(chat.getId()));
            log.info("Bot is removed from group! Response: {} ", response);
            return false;
        }

        ResponseDTO<BotTokenDTO> responseDTO = botTokenService.findByChatId(Long.parseLong(botId), true);
        if(!responseDTO.getSuccess() && responseDTO.getResponseData() == null){
            log.warn("Bot is not found! Chat id: {} | Response: {}", botId, responseDTO);
            return false;
        }

        BotTokenDTO botToken = responseDTO.getResponseData();
        ResponseDTO<CustomerTelegramDTO> customerTgResponse = customerTelegramService.findByBotTgId(Long.parseLong(botId));
        if(!customerTgResponse.getSuccess() && customerTgResponse.getResponseData() == null){
            log.warn("Owner of bot is not found! BotTokenDTO: {}", botToken);
            return false;
        }
        if(customerTgResponse.getResponseData().getStep() != 5){
            ResourceBundle bundle =
                ResourceBundleUtils.getResourceBundleByUserLanguageCode(customerTgResponse.getResponseData().getLanguageCode());
            wrongValue(customerTgResponse.getResponseData().getTelegramId(), bundle.getString("bot.admin.error.user.can.not.add.bot.to.group"));
        }
        CustomerTelegramDTO manager = customerTgResponse.getResponseData();

        sendInviteLink(user, chat.getId());
        TelegramGroupDTO telegramGroup = saveToTelegramGroup(chat, manager);
        sayThanksToManager(manager, telegramGroup, user.getUserName());
        log.info("Thanks is send to manager, Bot id: {} | Manager id: {}",
            user.getId(), manager.getTelegramId());
        return true;
    }

    private TelegramGroupDTO saveToTelegramGroup(Chat chat, CustomerTelegramDTO manager){
        ResourceBundle bundle = ResourceBundleUtils.getResourceBundleByUserLanguageCode(manager.getLanguageCode());
        TelegramGroupDTO dto = TelegramGroupDTO.builder()
            .chatId(chat.getId()).name(chat.getTitle()).build();
        TelegramGroupDTO telegramGroupDTO = telegramGroupService.findOneByChatId(chat.getId());

        if(telegramGroupDTO != null){
            return telegramGroupDTO;
        }
        ResponseDTO<TelegramGroupDTO> responseDTO = telegramGroupService.saveAndFlush(dto);
        System.out.println(responseDTO.getResponseData());
        Set<TelegramGroupDTO> telegramGroups = manager.getTelegramGroups() == null? new HashSet<>(): manager.getTelegramGroups();
        telegramGroups.add(TelegramGroupDTO.builder().id(responseDTO.getResponseData().getId()).build());
        manager.setTelegramGroups(telegramGroups);
        manager.setTelegramGroups(telegramGroups);
        customerTelegramService.update(manager);
        log.info("Telegram group is saved successfully, Chat id: {} | DTO: {}", chat.getId(), dto);
        return responseDTO.getResponseData();
    }

    private void sayThanksToManager(CustomerTelegramDTO manager, TelegramGroupDTO telegramGroup, String botName){
        ResourceBundle bundle = ResourceBundleUtils.getResourceBundleByUserLanguageCode(manager.getLanguageCode());
        String newMessage = String.format(bundle.getString("bot.admin.bot.say.thanks"), telegramGroup.getName(),botName);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(
            List.of(
                List.of(
                    InlineKeyboardButton.builder()
                        .text(bundle.getString("bot.admin.those.all"))
                        .callbackData(String.valueOf(telegramGroup.getId())).build()
                )
            )
        );

        SendMessage sendMessage = TelegramsUtil.sendMessage(manager.getTelegramId(), newMessage, markup);
        adminFeign.sendMessage(sendMessage);
    }

    private void sendInviteLink(User bot, Long groupId){
        ResourceBundle bundleUz = ResourceBundleUtils.getResourceBundleByUserLanguageCode("uz");
        ResourceBundle bundleRu = ResourceBundleUtils.getResourceBundleByUserLanguageCode("ru");
        ResourceBundle bundleEn = ResourceBundleUtils.getResourceBundleByUserLanguageCode("en");

        String href = " <a href=\"https://t.me/" + bot.getUserName() + "?start=" + groupId + "\">";
        String endHref = "</a>\n\n";
        String newMessage =
            bundleUz.getString("bot.admin.bot.send.invite.link") + href +
                bundleUz.getString("bot.admin.send.command.start.to.bot") + endHref +
                bundleRu.getString("bot.admin.bot.send.invite.link") + href +
                bundleRu.getString("bot.admin.send.command.start.to.bot") + endHref +
                bundleEn.getString("bot.admin.bot.send.invite.link") + href +
                bundleEn.getString("bot.admin.send.command.start.to.bot") + endHref;

        SendMessage sendMessage = TelegramsUtil.sendMessage(groupId, newMessage);
        ResponseDTO<BotTokenDTO> response =
            botTokenService.findByChatId(bot.getId(), true);

        if(!response.getSuccess() || response.getResponseData() == null){
            log.warn("Bot is not found! Bot id: {} | Group id: {}", bot.getId(), groupId);
            return;
        }

        URI uri = createCustomerURI(response.getResponseData().getToken());
        try {
            ResponseFromTelegram<Message> responseMessage = customerFeign.sendMessage(uri, sendMessage);
            pinMessage(response.getResponseData(), groupId, responseMessage.getResult().getMessageId());
            log.info("Link is send successfully, Bot id: {} | Groupd id: {} | Uri: {}",
                bot.getId(), groupId, uri);
        }catch (FeignException e){
            log.error("Error while sending! Error: {}", e.getMessage().toString());
            adminFeign.sendMessage(
                SendMessage.builder().chatId("736527480").text(e.getMessage().toString()).build()
            );
        }
    }

    private void pinMessage(BotTokenDTO bot, Long groupId, Integer messageId){
        URI uri = createCustomerURI(bot.getToken());
        PinMessageDTO pinMessageDTO = PinMessageDTO.builder()
            .chatId(String.valueOf(groupId)).messageId(messageId).build();
        try {
            WebhookResponseDTO response = customerFeign.pinMessage(uri, pinMessageDTO);
            log.info("Message is pinned, Message id: {} | Bot token: {} | Response: {}", messageId, bot.getToken(), response);
        }catch (FeignException e){
            log.error("Error while pinning message! Message id: {} | Bot token: {} | Exception: {}",
                messageId, bot.getToken(), e.getMessage());
        }
    }

    @Override
    public String messageOrCallback() {
        return SUPPORTED_TYPE.name();
    }

    @Override
    public String getErrorMessage(ResourceBundle bundle) {
        return bundle.getString("bot.admin.error.bot.add.group");
    }
}
