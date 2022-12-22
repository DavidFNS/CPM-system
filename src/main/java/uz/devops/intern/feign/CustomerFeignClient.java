package uz.devops.intern.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import uz.devops.intern.domain.ResponseFromTelegram;
import uz.devops.intern.telegram.bot.dto.PinMessageDTO;
import uz.devops.intern.telegram.bot.dto.WebhookResponseDTO;

import java.net.URI;

@FeignClient(name = "customer-feign-client", url = "https://api.telegram.org/bot")
public interface CustomerFeignClient {

    @PostMapping("/sendMessage")
    ResponseFromTelegram<Message> sendMessage(URI uri, @RequestBody SendMessage sendMessage);

    @GetMapping("/getMe")
    ResponseFromTelegram<User> getMe(URI uri);

    @GetMapping("/getChatMember")
    ResponseFromTelegram<ChatMember> getChatMember(URI uri, @RequestParam("chat_id") String chat_id, @RequestParam("user_id") String user_id);

    @GetMapping("/setWebhook")
    WebhookResponseDTO setWebhook(URI uri, @RequestParam("url") String url);

    @GetMapping("/deleteWebhook")
    WebhookResponseDTO deleteWebhook(URI uri, @RequestParam("drop_pending_updates") Boolean drop_pending_updates);

    @GetMapping("/pinChatMessage")
    WebhookResponseDTO pinMessage(URI uri, @RequestBody PinMessageDTO pinMessageDTO);
}
