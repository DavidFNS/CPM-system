package uz.devops.intern.telegram.bot.service.state;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import uz.devops.intern.feign.AdminFeign;
import uz.devops.intern.redis.ServicesRedisDTO;
import uz.devops.intern.redis.ServicesRedisRepository;
import uz.devops.intern.service.GroupsService;
import uz.devops.intern.service.ServicesService;
import uz.devops.intern.service.dto.CustomerTelegramDTO;
import uz.devops.intern.service.dto.GroupsDTO;
import uz.devops.intern.service.dto.ServicesDTO;
import uz.devops.intern.service.utils.ResourceBundleUtils;
import uz.devops.intern.telegram.bot.dto.EditMessageDTO;
import uz.devops.intern.telegram.bot.keyboards.AdminMenuKeys;
import uz.devops.intern.telegram.bot.utils.TelegramsUtil;

import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

@Service
public class ServiceGroupState extends State<ServiceFSM>{
    private ServicesRedisRepository servicesRedisRepository;
    private ServicesService servicesService;
    private GroupsService groupsService;
    private AdminFeign adminFeign;
    private AdminMenuKeys adminMenuKeys;

    public ServiceGroupState(ServiceFSM context) {
        super(context, context.getAdminFeign());
        this.servicesRedisRepository = context.getServicesRedisRepository();
        this.servicesService = context.getServicesService();
        this.adminFeign = context.getAdminFeign();
        this.groupsService = context.getGroupsService();
        this.adminMenuKeys = context.getAdminMenuKeys();
    }

    @Override
    boolean doThis(Update update, CustomerTelegramDTO manager) {
        ResourceBundle bundle = ResourceBundleUtils.getResourceBundleByUserLanguageCode(manager.getLanguageCode());
        if(!update.hasCallbackQuery()){
            wrongValue(manager.getTelegramId(), bundle.getString("bot.admin.send.organization.is.saved.successfully"));
            return false;
        }

        CallbackQuery callback = update.getCallbackQuery();
        String callbackData = callback.getData();
        Long managerId = callback.getFrom().getId();
        if(!callbackData.equals("ENOUGH")) {
            List<List<InlineKeyboardButton>> buttons = callback.getMessage().getReplyMarkup().getKeyboard();

            buttons.forEach(l -> l.forEach(k -> {
                if (k.getCallbackData().equals(callbackData)) {
                    k.setText(k.getText() + " ✅");
//                    k.setCallbackData(k.getCallbackData() + ": true");
                }
            }));

            ServicesRedisDTO redisDTO = servicesRedisRepository.findById(managerId).get();
            Set<GroupsDTO> groups = redisDTO.getServicesDTO().getGroups();

            GroupsDTO group = groupsService.findOneByTelegramId(Long.parseLong(callbackData));
            groups.add(group);
            servicesRedisRepository.save(redisDTO);

            EditMessageDTO editMessageDTO = new EditMessageDTO(
                String.valueOf(callback.getFrom().getId()),
                callback.getMessage().getMessageId(),
                callback.getInlineMessageId(),
                new InlineKeyboardMarkup());

            adminFeign.editMessageReplyMarkup(editMessageDTO);

            return false;
        }else{
            ServicesRedisDTO redisDTO = servicesRedisRepository.findById(managerId).get();
            ServicesDTO servicesDTO = redisDTO.getServicesDTO();
            if(servicesDTO.getGroups().isEmpty()){
                return false;
            }
            servicesService.save(servicesDTO);

            String newMessage = bundle.getString("bot.admin.main.menu");
            ReplyKeyboardMarkup markup = adminMenuKeys.createMenu(manager.getLanguageCode());
            SendMessage sendMessage = TelegramsUtil.sendMessage(managerId, newMessage, markup);
            adminFeign.sendMessage(sendMessage);
            context.changeState(new ServiceNameState(context));
            manager.setStep(4);
            return true;
        }
    }
}
