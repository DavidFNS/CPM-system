package uz.devops.intern.web.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.devops.intern.feign.CustomerFeign;
import uz.devops.intern.service.AdminTgService;
import uz.devops.intern.service.BotTokenService;
import uz.devops.intern.service.CustomerTelegramService;
import uz.devops.intern.service.dto.BotTokenDTO;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * REST controller for managing {@link uz.devops.intern.domain.CustomerTelegram}.
 */
@RestController
@RequestMapping("/api")
public class CustomerTelegramResource {

    private final Logger log = LoggerFactory.getLogger(CustomerTelegramResource.class);
    private final CustomerFeign customerFeign;
    private final CustomerTelegramService customerTelegramService;

    private final AdminTgService adminService;
    private final BotTokenService botTokenService;

    public CustomerTelegramResource(
        CustomerFeign customerFeign, CustomerTelegramService customerTelegramService, AdminTgService adminService, BotTokenService botTokenService) {
        this.customerFeign = customerFeign;
        this.customerTelegramService = customerTelegramService;
        this.adminService = adminService;
        this.botTokenService = botTokenService;
    }

    @PostMapping("/new-message/{botId}")
    public void sendMessage(@RequestBody Update update, @PathVariable String botId) throws URISyntaxException {
        log.info("[REST] Bot id: {} | Update: {}", botId, update);

        long idBot = Long.parseLong(botId);
        BotTokenDTO botTokenDTO = botTokenService.findByChatId(idBot);
        if (botTokenDTO != null && botTokenDTO.getToken() != null) {
            URI uri = new URI("https://api.telegram.org/bot" + botTokenDTO.getToken());
            SendMessage sendMessage = customerTelegramService.botCommands(update, uri);
            if (sendMessage.getText() != null)
                customerFeign.sendMessage(uri, sendMessage);
        }

//        if(update.getMessage() != null) {
//        if (update.getMyChatMember() != null) {
//            // Shu joyda botni gruppaga add qiganini bilsa bo'ladi
//            adminService.checkIsBotInGroup(update.getMyChatMember().getNewChatMember(), update.getMyChatMember().getChat(), botId);
//        }
//        }
//        else if(update.getMyChatMember() != null &&
//                update.getMyChatMember().getNewChatMember().getUser().getIsBot()){
//                adminService.checkIsBotAdmin(update.getMyChatMember());
//
//        }
    }
}
