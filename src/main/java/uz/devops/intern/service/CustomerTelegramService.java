package uz.devops.intern.service;

import org.glassfish.jersey.server.Uri;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.devops.intern.domain.CustomerTelegram;
import uz.devops.intern.service.dto.CustomerTelegramDTO;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Service Interface for managing {@link uz.devops.intern.domain.CustomerTelegram}.
 */
public interface CustomerTelegramService {
    SendMessage botCommands(Update update, URI telegramUri) throws URISyntaxException;

    CustomerTelegramDTO findByTelegramId(Long telegramId);

    CustomerTelegram findEntityByTelegramId(Long telegramId);

    List<CustomerTelegramDTO> findByTelegramGroupTelegramId(Long telegramId);
    List<CustomerTelegramDTO> findAllByIsActiveTrue();
    List<CustomerTelegramDTO> findAll();
    void setFalseToTelegramCustomerProfile(List<Long> ids);
    void deleteAllTelegramCustomersIsActiveFalse(List<Long> ids);

    ResponseDTO<CustomerTelegramDTO> getCustomerByTelegramId(Long telegramId);

    ResponseDTO<CustomerTelegramDTO> update(CustomerTelegramDTO dto);

    ResponseDTO<CustomerTelegramDTO> findByBotTgId(Long botId);
}
