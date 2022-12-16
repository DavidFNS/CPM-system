package uz.devops.intern.service.impl;

import java.net.URI;
import java.util.*;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import uz.devops.intern.domain.*;
import uz.devops.intern.feign.CustomerFeign;
import uz.devops.intern.redis.*;
import uz.devops.intern.repository.CustomerTelegramRepository;
import uz.devops.intern.service.*;
import uz.devops.intern.service.dto.*;
import uz.devops.intern.service.mapper.*;
import uz.devops.intern.service.utils.DateUtils;
import uz.devops.intern.telegram.bot.dto.EditMessageDTO;
import uz.devops.intern.telegram.bot.dto.EditMessageTextDTO;
import uz.devops.intern.telegram.bot.utils.KeyboardUtil;

import javax.persistence.EntityManager;

import static uz.devops.intern.service.utils.ResourceBundleUtils.getResourceBundleUsingCustomerTelegram;
import static uz.devops.intern.service.utils.ResourceBundleUtils.getResourceBundleUsingTelegramUser;
import static uz.devops.intern.telegram.bot.utils.KeyboardUtil.*;
import static uz.devops.intern.telegram.bot.utils.TelegramsUtil.*;

/**
 * Service Implementation for managing {@link CustomerTelegram}.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CustomerTelegramServiceImpl implements CustomerTelegramService {
    @Autowired
    private EntityManager entityManager;
    private static URI uri;
    private static Integer indexOfCustomerPayment;
    private static String groupReplyButton;
    private static String paymentHistoryReplyButton;
    private static String paymentReplyButton;
    private static String payReplyButton;
    private static String inlineButtonShowCurrentPayment;
    private static String myProfileReplyButton;
    private static String backHomeMenuButton;
    private static final String DATA_INLINE_CHANGE_NAME_BUTTON = "change name";
    private static final String DATA_INLINE_CHANGE_PHONE_NUMBER_BUTTON = "change phone number";
    private static final String DATA_INLINE_CHANGE_EMAIL_BUTTON = "change email";
    private static final String DATA_INLINE_REPLENISH_BALANCE_BUTTON = "change the balance";
    private static final String DATA_BACK_TO_HOME = "back to menu";
    private static Long chatIdCreatedByManager;
    private final Logger log = LoggerFactory.getLogger(CustomerTelegramServiceImpl.class);
    private final CustomerTelegramRepository customerTelegramRepository;
    private final CustomerTelegramRedisRepository customerTelegramRedisRepository;
    private final CustomerPaymentRepository customerPaymentRepository;
    private final CustomersService customersService;
    private final CustomerFeign customerFeign;
    private final PaymentService paymentService;
    private final TelegramGroupService telegramGroupService;
    private final PaymentHistoryService paymentHistoryService;
    private final CallbackRedisRepository callbackRedisRepository;
    private final PaymentHistoryMapper paymentHistoryMapper;
    private final UserService userService;
    private final BotTokenService botTokenService;
    private final CustomerTelegramMapper customerTelegramMapper;
    private final BotTokenMapper botTokenMapper;
    private static ResourceBundle resourceBundle;

    public SendMessage sendMessageIfNotExistsBotGroup(User telegramUser) {
        resourceBundle = getResourceBundleUsingTelegramUser(telegramUser);
        String sendStringMessage = resourceBundle.getString("bot.message.not.exists.bot.group");
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(telegramUser.getId());
        sendMessage.setText(sendStringMessage);

        return sendMessage;
    }

    @Override
    public SendMessage sendForbiddenMessage(Update update, URI telegramUri) {
        uri = telegramUri;
        if (update.hasCallbackQuery())
            return sendMessageIfNotExistsBotGroup(update.getCallbackQuery().getFrom());

        return sendMessageIfNotExistsBotGroup(update.getMessage().getFrom());
    }

    @Override
    public SendMessage commandWithUpdateMessage(Update update, URI telegramUri) {
        uri = telegramUri;
        Message message = update.getMessage();
        User telegramUser = message.getFrom();
        String requestMessage = message.getText();

        if (message.getText() == null) {
            requestMessage = message.getContact().getPhoneNumber();
            requestMessage = "+" + requestMessage;
        }

        return executeCommandStepByStep(telegramUser, requestMessage, update);
    }

    @Override
    public SendMessage startCommandWithoutChatId(User telegramUser, URI uri) {
        resourceBundle = getResourceBundleUsingTelegramUser(telegramUser);
        return sendMessage(telegramUser.getId(), resourceBundle.getString("bot.message.forbidden"));
    }

    @Override
    public SendMessage startCommandWithChatId(User telegramUser, String requestMessage, URI telegramUri) {
        resourceBundle = getResourceBundleUsingTelegramUser(telegramUser);
        uri = telegramUri;
        try {
            chatIdCreatedByManager = Long.parseLong(requestMessage.substring(7));
            Optional<TelegramGroup> telegramGroup = telegramGroupService.findByChatId(chatIdCreatedByManager);
            if (telegramGroup.isEmpty()) {
                return sendMessageIfNotExistsBotGroup(telegramUser);
            }

            Optional<CustomerTelegram> customerTelegramOptional = customerTelegramRepository.findByTelegramId(telegramUser.getId());
            if (customerTelegramOptional.isPresent()) startCommand(telegramUser, customerTelegramOptional.get());
            else startCommand(telegramUser);
        } catch (NumberFormatException numberFormatException) {
            log.error("Error parsing chatId to Long when bot started");
            return sendMessage(telegramUser.getId(), "❌ " + resourceBundle.getString("bot.message.invalid.chat.number"));
        }
        return new SendMessage();
    }

    private SendMessage executeCommandStepByStep(User telegramUser, String requestMessage, Update update) {
        Optional<CustomerTelegram> customerTelegramOptional = customerTelegramRepository.findByTelegramId(telegramUser.getId());
        if (customerTelegramOptional.isPresent()) {
            CustomerTelegram customerTelegram = customerTelegramOptional.get();
            customerTelegram.setIsActive(true);
            resourceBundle = getResourceBundleUsingCustomerTelegram(customerTelegram);

            Integer step = customerTelegram.getStep();
            if (requestMessage.startsWith("/start ")) step = 1;

            return switch (step) {
                case 1 -> registerCustomerClientAndShowCustomerMenu(requestMessage, telegramUser, customerTelegram);
                case 2 -> mainCommand(requestMessage, telegramUser, customerTelegram, update.getMessage());
                case 3 -> payRequestForService(requestMessage, telegramUser, customerTelegram);
                case 4 -> changeEmail(requestMessage, telegramUser, customerTelegram);
                case 5 -> changePhoneNumber(requestMessage, telegramUser, customerTelegram);
                case 6 -> replenishBalance(requestMessage, telegramUser, customerTelegram);
                case 7 -> changeFullName(requestMessage, telegramUser, customerTelegram);
                default -> sendMessage(telegramUser.getId(), "❌ " + resourceBundle.getString("bot.message.unknown.command"));
            };
        }

        if (!getLanguages().containsKey(requestMessage)){
            resourceBundle = getResourceBundleUsingTelegramUser(telegramUser);
            String responseString = resourceBundle.getString("bot.message.choice.language") + " 👇";
            ReplyKeyboardMarkup replyKeyboardMarkup = KeyboardUtil.language();

            log.info("Message send successfully! User id: {} | Message text: {} | Update: {}",
                telegramUser.getId(), responseString, update);
            return sendMessage(telegramUser.getId(), responseString, replyKeyboardMarkup);
        }

        CustomerTelegram customerTelegram = createCustomerTelegramToSaveDatabase(telegramUser);
        customerTelegram.setLanguageCode(getLanguages().get(requestMessage));

        entityManager.detach(customerTelegram);
        customerTelegram.setChatId(chatIdCreatedByManager);
        Optional<TelegramGroup> telegramGroupOptional = telegramGroupService.findByChatId(chatIdCreatedByManager);

        if (telegramGroupOptional.isPresent()) {
            TelegramGroup telegramGroup = new TelegramGroup();
            telegramGroup.setId(telegramGroupOptional.get().getId());
            customerTelegram.setTelegramGroups(Set.of(telegramGroup));
        }

        customerTelegramRepository.save(customerTelegram);

        return registerCustomerClientAndShowCustomerMenu(requestMessage, telegramUser, customerTelegram);
    }

    @Override
    public SendMessage unknownCommand(User telegramUser, URI telegramURI) {
        resourceBundle = getResourceBundleUsingTelegramUser(telegramUser);
        uri = telegramURI;
        return sendMessage(telegramUser.getId(), "❌ " + resourceBundle.getString("bot.message.unknown.command"));
    }

    @Override
    public CustomerTelegramDTO findByTelegramId(Long telegramId) {
        if (telegramId == null) {
            return null;
        }

        Optional<CustomerTelegram> customerTelegramOptional =
            customerTelegramRepository.findByTelegramId(telegramId);

        return customerTelegramOptional.map(customerTelegramMapper::toDto).orElse(null);
    }

    @Override
    public CustomerTelegram findEntityByTelegramId(Long telegramId) {
        if (telegramId == null) {
            return null;
        }
        Optional<CustomerTelegram> customerTelegramOptional =
            customerTelegramRepository.findByTelegramId(telegramId);

        return customerTelegramOptional.orElse(null);
    }

    @Override
    public List<CustomerTelegramDTO> findByTelegramGroupTelegramId(Long telegramId) {
        List<CustomerTelegram> customerTelegramList = customerTelegramRepository.findAllByTelegramGroupsChatId(telegramId);
        if (customerTelegramList == null)
            return null;

        return customerTelegramList.stream()
            .map(customerTelegramMapper::toDto)
            .toList();
    }

    @Override
    public List<CustomerTelegramDTO> findAllByIsActiveTrue() {
        log.info("request to get all active telegram customers ");
        List<CustomerTelegram> customerTelegramList = customerTelegramRepository.findAllByIsActiveTrue();
        if (customerTelegramList == null) {
            return null;
        }
        return customerTelegramList.stream()
            .map(customerTelegramMapper::toDto)
            .toList();
    }

    @Override
    public List<CustomerTelegramDTO> findAll() {
        log.info("request to get all telegram customers");
        List<CustomerTelegram> customerTelegramList = customerTelegramRepository.findAll();
        if (customerTelegramList.size() == 0) {
            return null;
        }
        return customerTelegramList.stream()
            .map(customerTelegramMapper::toDto)
            .toList();
    }

    @Override
    public void setFalseToTelegramCustomerProfile(List<Long> ids) {
        log.info("request to set false to telegram customers profile");
        customerTelegramRepository.setFalseTelegramCustomersProfile(ids);
    }

    @Override
    public void deleteAllTelegramCustomersIsActiveFalse(List<Long> ids) {
        log.info("request to delete all telegram customers if isActive is false");
        customerTelegramRepository.deleteAllByIdInAndIsActiveFalse(ids);
    }

    @Override
    public ResponseDTO<CustomerTelegramDTO> getCustomerByTelegramId(Long telegramId) {
        return null;
    }

    @Override
    public ResponseDTO<CustomerTelegramDTO> update(CustomerTelegramDTO dto) {
        return null;
    }

    @Override
    public ResponseDTO<CustomerTelegramDTO> findByBotTgId(Long botId) {
        return null;
    }

    @Override
    public SendMessage commandWithCallbackQuery(CallbackQuery callbackQuery, URI telegramURI) {
        uri = telegramURI;
        User telegramUser = callbackQuery.getFrom();
        Optional<CustomerTelegram> optionalCustomerTelegram = customerTelegramRepository.findByTelegramId(telegramUser.getId());
        if (optionalCustomerTelegram.isEmpty())
            return sendCustomerDataNotFoundMessage(telegramUser);

        CustomerTelegram customerTelegram = optionalCustomerTelegram.get();
        customerTelegram.setIsActive(true);
        resourceBundle = getResourceBundleUsingCustomerTelegram(customerTelegram);

        String inlineButtonData = callbackQuery.getData().split(" ")[0];
        switch (inlineButtonData) {
            case "left":
            case "right":
                customerFeign.editMessageText(uri, whenPressingLeftOrRightToGetCustomerPayment(telegramUser, callbackQuery, customerTelegram));
                return new SendMessage();
            case "back":
                customerFeign.editMessageReplyMarkup(
                    uri,
                    new EditMessageDTO(String.valueOf(telegramUser.getId()), callbackQuery.getMessage().getMessageId(), callbackQuery.getInlineMessageId(), new InlineKeyboardMarkup())
                );
                return sendCustomerMenu(telegramUser, customerTelegram);
            case "show":
                customerFeign.editMessageText(uri, showCurrentCustomerPayment(telegramUser, customerTelegram, callbackQuery));
                return new SendMessage();
            case "payment":
                customerFeign.editMessageText(uri, sendRequestPaymentSum(customerTelegram, telegramUser, callbackQuery));
                return new SendMessage();
            case "change":
                return changeCustomerProfile(telegramUser, customerTelegram, callbackQuery.getData());
            default:
                return sendMessage(telegramUser.getId(), "❌ " + resourceBundle.getString("bot.message.unknown.command"));
        }
    }

    private SendMessage changeCustomerProfile(User telegramUser, CustomerTelegram customerTelegram, String dataWithChange) {
        resourceBundle = getResourceBundleUsingCustomerTelegram(customerTelegram);
        customerTelegram.setStep(2);
        customerTelegramRepository.save(customerTelegram);

        ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove();
        replyKeyboardRemove.setRemoveKeyboard(true);

        String data = dataWithChange.substring(7);
        List<String> dataList = List.of("email", "phone number", "the balance", "name");
        int step = 4;
        customerTelegram.setStep(step + dataList.indexOf(data));
        customerTelegramRepository.save(customerTelegram);

        return switch (data) {
            case "email" ->
                sendMessage(telegramUser.getId(), resourceBundle.getString("bot.message.request.change.email") + " \uD83D\uDC47", replyKeyboardRemove);
            case "phone number" ->
                sendMessage(telegramUser.getId(), resourceBundle.getString("bot.message.request.change.phone.number") + " \uD83D\uDC47", replyKeyboardRemove);
            case "the balance" ->
                sendMessage(telegramUser.getId(), resourceBundle.getString("bot.message.request.change.balance") + " \uD83D\uDC47", replyKeyboardRemove);
            // change fullName
            default ->
                sendMessage(telegramUser.getId(), resourceBundle.getString("bot.message.request.change.name") + " \uD83D\uDC47", replyKeyboardRemove);
        };
    }

    private SendMessage changeEmail(String email, User telegramUser, CustomerTelegram customerTelegram) {
        resourceBundle = getResourceBundleUsingCustomerTelegram(customerTelegram);
        if (!email.endsWith("@gmail.com")) {
            return sendMessage(telegramUser.getId(), resourceBundle.getString("bot.message.invalid.email") + " \uD83D\uDC47");
        }

        Optional<uz.devops.intern.domain.User> userOptional = userService.findByEmail(email);
        if (userOptional.isPresent())
            return sendMessage(telegramUser.getId(), resourceBundle.getString("bot.message.existing.email") + " \uD83D\uDC47");

        Customers customer = customerTelegram.getCustomer();
        uz.devops.intern.domain.User jhi_user = customer.getUser();

        addCustomerTelegramToSecurityContextHolder(customerTelegram);
        userService.updateUser(jhi_user.getFirstName(), jhi_user.getLastName(), email, jhi_user.getLangKey(), jhi_user.getImageUrl());
        customerTelegram.setStep(2);
        customerTelegramRepository.save(customerTelegram);

        return sendMessage(telegramUser.getId(), resourceBundle.getString("bot.message.email.changed") + " ✅", backToMenuInlineButton(customerTelegram));
    }

    private InlineKeyboardMarkup backToMenuInlineButton(CustomerTelegram customerTelegram) {
        resourceBundle = getResourceBundleUsingCustomerTelegram(customerTelegram);
        backHomeMenuButton = resourceBundle.getString("bot.message.back.home.menu.button");
        InlineKeyboardButton keyboardButton = new InlineKeyboardButton(backHomeMenuButton);
        keyboardButton.setCallbackData(DATA_BACK_TO_HOME);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(List.of(List.of(keyboardButton)));

        return inlineKeyboardMarkup;
    }

    private SendMessage changePhoneNumber(String phoneNumber, User telegramUser, CustomerTelegram customerTelegram) {
        resourceBundle = getResourceBundleUsingCustomerTelegram(customerTelegram);
        int count = 0;
        for (char ch : phoneNumber.toCharArray()) {
            if (Character.isLetter(ch))
                return sendMessage(telegramUser.getId(), resourceBundle.getString("bot.message.invalid.phone.number") + " \uD83D\uDC47");
            count++;
        }
        if (count != 13) return sendMessage(telegramUser.getId(), resourceBundle.getString("bot.message.invalid.phone.number") + " \uD83D\uDC47");
        if (!phoneNumber.startsWith("+998")) return sendMessage(telegramUser.getId(), resourceBundle.getString("bot.message.invalid.phone.number") + " \uD83D\uDC47");

        Optional<Customers> customerOptional = customersService.findByPhoneNumber(phoneNumber);
        if (customerOptional.isPresent())
            return sendMessage(telegramUser.getId(), "❌ " + resourceBundle.getString("bot.message.existing.phone.number"));

        Customers customer = customerTelegram.getCustomer();
        customer.setPhoneNumber(phoneNumber);
        customerTelegram.setStep(2);
        customerTelegram.setPhoneNumber(phoneNumber);
        customerTelegramRepository.save(customerTelegram);

        return sendMessage(telegramUser.getId(), resourceBundle.getString("bot.message.change.phone.number") + " ✅", backToMenuInlineButton(customerTelegram));
    }

    private SendMessage replenishBalance(String summa, User telegramUser, CustomerTelegram customerTelegram) {
        resourceBundle = getResourceBundleUsingCustomerTelegram(customerTelegram);
        try {
            Double money = Double.parseDouble(summa);
            if (money <= 0)
                return sendMessage(telegramUser.getId(),  "❌ " + resourceBundle.getString("bot.message.send.money.greater.zero"));

            Customers customer = customerTelegram.getCustomer();
            customersService.replenishCustomerBalance(money, customer.getId());
            customerTelegram.setStep(2);
            customerTelegramRepository.save(customerTelegram);

            return sendMessage(telegramUser.getId(), resourceBundle.getString("bot.message.replenished.balance") +" ✅", backToMenuInlineButton(customerTelegram));
        } catch (NumberFormatException e) {
            return sendMessage(telegramUser.getId(), "❌ " + resourceBundle.getString("bot.message.wrong.entered.money"));
        }
    }

    private SendMessage changeFullName(String fullName, User telegramUser, CustomerTelegram customerTelegram) {
        resourceBundle = getResourceBundleUsingCustomerTelegram(customerTelegram);
        String[] newFullName = fullName.split(" ");
        if (newFullName.length < 2)
            return sendMessage(telegramUser.getId(), "❌ " + resourceBundle.getString("bot.message.not.entered.full.name"));

        Optional<uz.devops.intern.domain.User> userOptional = userService.findByFirstName(newFullName[0]);
        if (userOptional.isPresent())
            return sendMessage(telegramUser.getId(), "❌ " + resourceBundle.getString("bot.message.existing.name") + " \uD83D\uDC47");

        Customers customer = customerTelegram.getCustomer();
        uz.devops.intern.domain.User jhi_user = customer.getUser();
        addCustomerTelegramToSecurityContextHolder(customerTelegram);
        userService.updateUser(newFullName[0], newFullName[1], jhi_user.getEmail(), jhi_user.getLangKey(), jhi_user.getImageUrl());
        customerTelegram.setStep(2);
        customerTelegramRepository.save(customerTelegram);

        return sendMessage(telegramUser.getId(), resourceBundle.getString("bot.message.changed.name") + " ✅",
            backToMenuInlineButton(customerTelegram));
    }

    private EditMessageTextDTO showCurrentCustomerPayment(User telegramUser, CustomerTelegram customerTelegram, CallbackQuery callbackQuery) {
        String[] data = callbackQuery.getData().split(" ");
        Long idPaymentHistory = Long.parseLong(data[3]);

        Optional<PaymentHistoryDTO> optionalPaymentHistoryDTO = paymentHistoryService.findOne(idPaymentHistory);

        PaymentHistory paymentHistory = optionalPaymentHistoryDTO
            .map(paymentHistoryMapper::toEntity).get();

        StringBuilder builder = new StringBuilder();
        buildPaymentHistoryMessage(paymentHistory, builder);

        EditMessageTextDTO editMessageTextDTO = createMessageTextDTO(builder.toString(), callbackQuery, telegramUser);
        editMessageTextDTO.setReplyMarkup(backToMenuInlineButton(customerTelegram));
        return editMessageTextDTO;
    }

    private EditMessageTextDTO createMessageTextDTO(String text, CallbackQuery callbackQuery, User telegramUser) {
        EditMessageTextDTO editMessageTextDTO = new EditMessageTextDTO();
        editMessageTextDTO.setChatId(String.valueOf(telegramUser.getId()));
        editMessageTextDTO.setText(text);
        editMessageTextDTO.setParseMode("HTML");
        editMessageTextDTO.setMessageId(callbackQuery.getMessage().getMessageId());
        editMessageTextDTO.setInlineMessageId(callbackQuery.getInlineMessageId());
        return editMessageTextDTO;
    }

    private EditMessageTextDTO sendRequestPaymentSum(CustomerTelegram customerTelegram, User telegramUser, CallbackQuery callbackQuery) {
        resourceBundle = getResourceBundleUsingCustomerTelegram(customerTelegram);
        customerTelegram.setStep(3);
        customerTelegramRepository.save(customerTelegram);

        ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove();
        replyKeyboardRemove.setRemoveKeyboard(true);

        CallbackDataRedis callbackDataRedis = new CallbackDataRedis(telegramUser.getId(), callbackQuery.getData());
        callbackRedisRepository.save(callbackDataRedis);

        String stringMessage = resourceBundle.getString("bot.message.request.money.to.payment") + " \uD83D\uDC47";


        EditMessageTextDTO editMessageTextDTO = createMessageTextDTO(stringMessage, callbackQuery, telegramUser);
        editMessageTextDTO.setReplyMarkup(new InlineKeyboardMarkup());
        return editMessageTextDTO;
    }

    private SendMessage payRequestForService(String paymentSum, User telegramUser, CustomerTelegram customerTelegram) {
        resourceBundle = getResourceBundleUsingCustomerTelegram(customerTelegram);
        try {
            double requestPaymentSum = Double.parseDouble(paymentSum);
            if (requestPaymentSum < 1000)
                return sendMessage(telegramUser.getId(), "❌ " + resourceBundle.getString("bot.message.sum.less.than.enough"));
            if (requestPaymentSum > 100_000_000)
                return sendMessage(telegramUser.getId(), "❌ " + resourceBundle.getString("bot.message.sum.greater.than.enough"));

            Optional<CallbackDataRedis> callbackDataRedisOptional = callbackRedisRepository.findById(telegramUser.getId());
            if (callbackDataRedisOptional.isEmpty()) {
                SendMessage sendMessage = sendMessage(telegramUser.getId(), "⚠️" + resourceBundle.getString("bot.message.not.found.from.redis"));

                customerFeign.sendMessage(uri, sendMessage);
                return sendCustomerMenu(telegramUser, customerTelegram);
            }

            CallbackDataRedis redis = callbackDataRedisOptional.get();
            String stringId = redis.getCallbackDate().substring(8);
            Long id = Long.parseLong(stringId);

            Optional<PaymentDTO> paymentOptional = paymentService.findOne(id);
            if (paymentOptional.isEmpty()) {
                customerFeign.sendMessage(
                    uri, sendMessage(telegramUser.getId(), "⚠️ " + resourceBundle.getString("bot.message.not.found.payment"))
                );
                return sendCustomerMenu(telegramUser, customerTelegram);
            }

            PaymentDTO paymentDTO = paymentOptional.get();
            paymentDTO.setPaidMoney(requestPaymentSum);

            addCustomerTelegramToSecurityContextHolder(customerTelegram);
            ResponseDTO<PaymentHistoryDTO> responsePayment = paymentService.payForService(paymentDTO);
            backHomeMenuButton = resourceBundle.getString("bot.message.back.home.menu.button");
            if (responsePayment.getSuccess() && responsePayment.getCode().equals(0) && responsePayment.getResponseData() != null) {
                customerTelegram.setStep(2);
                customerTelegramRepository.save(customerTelegram);

                PaymentHistoryDTO paymentHistoryDTO = responsePayment.getResponseData();
                String successMessage = resourceBundle.getString("bot.message.paid.for.payment") + " ✅";

                InlineKeyboardButton showCurrentPaymentButton = new InlineKeyboardButton();
                showCurrentPaymentButton.setCallbackData("show current payment " + paymentHistoryDTO.getId());

                InlineKeyboardButton backHomeMenuInlineButton = new InlineKeyboardButton(backHomeMenuButton);
                backHomeMenuInlineButton.setCallbackData(DATA_BACK_TO_HOME);

                InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
                inlineKeyboardMarkup.setKeyboard(List.of(List.of(showCurrentPaymentButton, backHomeMenuInlineButton)));
                customerTelegram.setStep(2);
                customerTelegramRepository.save(customerTelegram);

                return sendMessage(telegramUser.getId(), successMessage, inlineKeyboardMarkup);
            }
            if (responsePayment.getCode().equals(-5)) {
                return sendMessage(telegramUser.getId(), "\uD83D\uDEAB " + resourceBundle.getString("bot.message.not.enough.money"));
            }
            KeyboardRow keyboardRow = new KeyboardRow();
            keyboardRow.add(new KeyboardButton(backHomeMenuButton));

            ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
            markup.setResizeKeyboard(true);
            markup.setKeyboard(List.of(keyboardRow));

            customerTelegram.setStep(2);
            customerTelegramRepository.save(customerTelegram);

            return sendMessage(telegramUser.getId(), resourceBundle.getString("bot.message.not.found.message"), markup);
        } catch (NumberFormatException numberFormatException) {
            return sendMessage(telegramUser.getId(), "❌ " + resourceBundle.getString("bot.message.wrong.entered.money"));
        }
    }

    private void addCustomerTelegramToSecurityContextHolder(CustomerTelegram customerTelegram) {
        Customers customer = customerTelegram.getCustomer();
        uz.devops.intern.domain.User authenticatedUser = customer.getUser();

        Iterator<Authority> authorityIterator = authenticatedUser.getAuthorities().iterator();
        Set<SimpleGrantedAuthority> simpleGrantedAuthoritySet = new HashSet<>();
        while (authorityIterator.hasNext()) {
            Authority authority = authorityIterator.next();
            SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority(authority.getName());
            simpleGrantedAuthoritySet.add(simpleGrantedAuthority);
        }

        org.springframework.security.core.userdetails.User securityUser = new org.springframework.security.core.userdetails.User(
            authenticatedUser.getLogin(), authenticatedUser.getPassword(), simpleGrantedAuthoritySet
        );

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(securityUser, "", simpleGrantedAuthoritySet);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    public void setTextToButtons(ResourceBundle resourceBundle){
        paymentHistoryReplyButton = resourceBundle.getString("bot.message.payment.history.button");
        paymentReplyButton = resourceBundle.getString("bot.message.payment.button");
        payReplyButton = resourceBundle.getString("bot.message.debts.button");
        myProfileReplyButton =  resourceBundle.getString("bot.message.my.profile.button");
        groupReplyButton = resourceBundle.getString("bot.message.group.button");
        backHomeMenuButton = resourceBundle.getString("bot.message.back.home.menu.button");

        inlineButtonShowCurrentPayment = resourceBundle.getString("bot.message.current.payment.button");
    }
    public SendMessage mainCommand(String buttonMessage, User telegramUser, CustomerTelegram customerTelegram, Message message) {
        resourceBundle = getResourceBundleUsingCustomerTelegram(customerTelegram);
        setTextToButtons(resourceBundle);

        if(groupReplyButton.equals(buttonMessage)) return sendCustomerGroups(telegramUser, customerTelegram);
        if(payReplyButton.equals(buttonMessage)) return sendCustomerPayments(telegramUser, customerTelegram, message);
        if(paymentReplyButton.equals(buttonMessage)) return  sendAllCustomerPayments(telegramUser, customerTelegram);
        if(paymentHistoryReplyButton.equals(buttonMessage)) return sendCustomerPaymentsHistory(telegramUser, customerTelegram);
        if(myProfileReplyButton.equals(buttonMessage)) return showCustomerProfile(telegramUser, customerTelegram);
        if(backHomeMenuButton.equals(buttonMessage)) return sendCustomerMenu(telegramUser, customerTelegram);

        return sendMessage(telegramUser.getId(), "❌ " + resourceBundle.getString("bot.message.unknown.command"));
    }

    private SendMessage showCustomerProfile(User telegramUser, CustomerTelegram customerTelegram) {
        resourceBundle = getResourceBundleUsingCustomerTelegram(customerTelegram);
        StringBuilder customerProfileBuilder = new StringBuilder();

        uz.devops.intern.domain.User jhi_user = customerTelegram.getCustomer().getUser();
        Customers customer = customerTelegram.getCustomer();

        customerProfileBuilder.append("<b>Mening profilim: </b>\n\n");
        customerProfileBuilder.append(String.format("<b>Ism: </b> %s\n", jhi_user.getFirstName()));
        customerProfileBuilder.append(String.format("<b>Familiya: </b>%s\n", jhi_user.getLastName()));
        customerProfileBuilder.append(String.format("<b>Email: </b> %s\n", jhi_user.getEmail()));
        customerProfileBuilder.append(String.format("<b>Tel raqam: </b> %s\n", customerTelegram.getPhoneNumber()));
        customerProfileBuilder.append(String.format("<b>Balans: </b> %.2f", customer.getBalance()));

        BotTokenDTO botTokenDTO = botTokenService.findByChatId(customerTelegram.getChatId());
        Optional<TelegramGroup> telegramGroupOptional = telegramGroupService.findByChatId(customerTelegram.getChatId());
        if (botTokenDTO != null && telegramGroupOptional.isPresent()) {
            BotToken botToken = botTokenMapper.toEntity(botTokenDTO);
            TelegramGroup telegramGroup = telegramGroupOptional.get();
            uz.devops.intern.domain.User managerUser = botToken.getCreatedBy();

            customerProfileBuilder.append("\n<b>Telegram guruhi: </b>\n");
            customerProfileBuilder.append(String.format("<b>Nomi: </b> %s\n", telegramGroup.getName()));
            customerProfileBuilder.append(String.format("<b>Guruh rahbari: </b> %s %s\n", managerUser.getFirstName(), managerUser.getLastName()));
            customerProfileBuilder.append(String.format("<b>Guruh linki: </b> %s\n", botToken.getUsername()));
        }

        InlineKeyboardButton changeFullNameButton = new InlineKeyboardButton("bot.message.change.name.button");
        changeFullNameButton.setCallbackData(DATA_INLINE_CHANGE_NAME_BUTTON);

        InlineKeyboardButton changeEmailButton = new InlineKeyboardButton("bot.message.change.email.button");
        changeEmailButton.setCallbackData(DATA_INLINE_CHANGE_EMAIL_BUTTON);

        InlineKeyboardButton changePhoneNumberButton = new InlineKeyboardButton("bot.message.change.phone.number.button");
        changePhoneNumberButton.setCallbackData(DATA_INLINE_CHANGE_PHONE_NUMBER_BUTTON);

        InlineKeyboardButton replenishBalanceButton = new InlineKeyboardButton("bot.message.change.balance.button");
        replenishBalanceButton.setCallbackData(DATA_INLINE_REPLENISH_BALANCE_BUTTON);

        backHomeMenuButton = resourceBundle.getString("bot.message.back.home.menu.button");
        InlineKeyboardButton backHomeButton = new InlineKeyboardButton(backHomeMenuButton);
        backHomeButton.setCallbackData(DATA_BACK_TO_HOME);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(
            List.of(
                List.of(changeFullNameButton),
                List.of(changePhoneNumberButton),
                List.of(changeEmailButton, replenishBalanceButton),
                List.of(backHomeButton))
        );

        ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove();
        replyKeyboardRemove.setRemoveKeyboard(true);

        customerFeign.sendMessage(uri, sendMessage(telegramUser.getId(), "\uD83D\uDE4B\u200D♂️", replyKeyboardRemove));
        return sendMessage(telegramUser.getId(), customerProfileBuilder.toString(), markup);
    }

    private SendMessage sendAllCustomerPayments(User telegramUser, CustomerTelegram customerTelegram) {
        resourceBundle = getResourceBundleUsingCustomerTelegram(customerTelegram);
        Customers customer = customerTelegram.getCustomer();
        if (customer == null || customer.getGroups() == null)
            return sendCustomerDataNotFoundMessage(telegramUser);

        addCustomerTelegramToSecurityContextHolder(customerTelegram);
        ResponseDTO<List<PaymentDTO>> listResponseDTO = paymentService.getAllCustomerPayments();
        if (listResponseDTO.getResponseData() == null || listResponseDTO.getResponseData().size() == 0)
            return sendCustomerDataNotFoundMessage(telegramUser);

        List<PaymentDTO> paymentDTOList = listResponseDTO.getResponseData();
        for (PaymentDTO payment : paymentDTOList) {
            StringBuilder buildCustomerPayments = new StringBuilder();
            buildCustomerPayments(payment, buildCustomerPayments, customerTelegram);
            String status;
            if (payment.getIsPaid()) status = resourceBundle.getString("bot.message.full.paid");
            else if(payment.getPaidMoney().equals(0D)) status = resourceBundle.getString("bot.message.not.paid");
            else status = resourceBundle.getString("bot.message.partially.paid");
            buildCustomerPayments.append(String.format("<b>%s: </b> %s", resourceBundle.getString("bot.message.status"), status));

            customerFeign.sendMessage(uri, sendMessage(telegramUser.getId(), buildCustomerPayments.toString()));
        }

        return new SendMessage();
    }

    private SendMessage sendMessageIfPhoneNumberIsNull(User telegramUser) {
        resourceBundle = getResourceBundleUsingTelegramUser(telegramUser);
        String sendStringMessage = resourceBundle.getString("bot.message.request.phone.number");

        SendMessage sendMessage = sendMessage(telegramUser.getId(), sendStringMessage, sendMarkup(telegramUser));
        log.info("Message send successfully! User id: {} | Message text: {}", telegramUser, sendMessage.getText());
        return sendMessage;
    }

    private SendMessage registerCustomerClientAndShowCustomerMenu(String requestMessage, User telegramUser, CustomerTelegram customerTelegram) {
        SendMessage sendMessage = new SendMessage();
        Optional<CustomerTelegramRedis> redisOptional = customerTelegramRedisRepository.findById(telegramUser.getId());

        if (!requestMessage.startsWith("+998") && customerTelegram.getCustomer() == null)
            return sendMessageIfPhoneNumberIsNull(telegramUser);

        // when customer entered from another telegram bot
        if (customerTelegram.getPhoneNumber() != null && chatIdCreatedByManager != null) {
            resourceBundle = getResourceBundleUsingCustomerTelegram(customerTelegram);
            Set<TelegramGroup> customerTelegramGroups = customerTelegram.getTelegramGroups();
            entityManager.detach(customerTelegram);

            Optional<TelegramGroup> telegramGroupOptional = telegramGroupService.findByChatId(chatIdCreatedByManager);

            if (telegramGroupOptional.isPresent()) {
                Set<TelegramGroup> newCustomerTelegramGroups = new HashSet<>(customerTelegramGroups);
                TelegramGroup telegramGroup = new TelegramGroup();

                telegramGroup.setId(telegramGroupOptional.get().getId());
                newCustomerTelegramGroups.add(telegramGroup);
                customerTelegram.setTelegramGroups(newCustomerTelegramGroups);
                customerTelegramRepository.save(customerTelegram);
            }
            return sendCustomerMenu(telegramUser, customerTelegram);
        }

        Customers customer = checkCustomerPhoneNumber(requestMessage);
        Authority customerAuthority = new Authority();
        customerAuthority.setName("ROLE_CUSTOMER");
        if (customer == null) {
            String sendStringMessage = resourceBundle.getString("bot.message.customer.not.registered");
            sendMessage = sendMessage(telegramUser.getId(), sendStringMessage, sendMarkup(telegramUser));
            log.info("Message send successfully! User id: {} | Message text: {}", telegramUser, sendMessage);
            return sendMessage;
        }
        if (customer.getUser() == null || !customer.getUser().getAuthorities().contains(customerAuthority)) {
            String sendStringMessage = "\uD83D\uDEAB " + resourceBundle.getString("bot.message.authority.not.exits");

            sendMessage = sendMessage(telegramUser.getId(), sendStringMessage, sendMarkup(telegramUser));
            log.info("Message send successfully! User id: {} | Message text: {}", telegramUser, sendMessage);
            return sendMessage;
        }
        customerTelegram.customer(customer);
        customerTelegramRepository.save(customerTelegram);

        customerTelegram.setPhoneNumber(requestMessage);
        customerTelegram.setStep(2);
        customerTelegramRepository.save(customerTelegram);
        log.info("Phone number successfully set to existing user! telegram user : {} | phoneNumber: {} ", customerTelegram, requestMessage);

        sendMessage.setChatId(telegramUser.getId());
        sendMessage.setText(resourceBundle.getString("bot.message.dear.customer") + " " + telegramUser.getFirstName() +
             resourceBundle.getString("bot.message.successfully.registration") + " ✅");

        customerFeign.sendMessage(uri, sendMessage);

        if (redisOptional.isEmpty()) {
            CustomerTelegramRedis customerTelegramRedis = new CustomerTelegramRedis(telegramUser.getId(), telegramUser);
            customerTelegramRedisRepository.save(customerTelegramRedis);
            log.info("New telegram user successfully saved to redis! UserRedis : {}", customerTelegramRedis);
        }

        return sendCustomerMenu(telegramUser, customerTelegram);
    }

    // send Customer Payments where paid is false
    public SendMessage sendCustomerPayments(User telegramUser, CustomerTelegram customerTelegram, Message message) {
        Customers customer = customerTelegram.getCustomer();
        if (customer == null || customer.getGroups() == null)
            return sendCustomerDataNotFoundMessage(telegramUser);

        List<PaymentDTO> paymentDTOList = paymentService.getAllCustomerPaymentsPayedIsFalse(customer);
        if (paymentDTOList == null)
            return sendCustomerDataNotFoundMessage(telegramUser);

        StringBuilder buildCustomerPayments = new StringBuilder();
        indexOfCustomerPayment = 0;

        List<CustomerPaymentRedis> customerPaymentRedisList = new ArrayList<>();
        for (PaymentDTO paymentDTO : paymentDTOList) {
            customerPaymentRedisList.add(new CustomerPaymentRedis(indexOfCustomerPayment++, paymentDTO));
        }
        customerPaymentRepository.saveAll(customerPaymentRedisList);
        indexOfCustomerPayment = 0;
        buildCustomerPayments(paymentDTOList.get(0), buildCustomerPayments, customerTelegram);

        ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove();
        replyKeyboardRemove.setRemoveKeyboard(true);

        customerFeign.sendMessage(uri, sendMessage(telegramUser.getId(), "💸", replyKeyboardRemove));
        InlineKeyboardMarkup markup = paymentOfCustomerInlineMarkup(paymentDTOList.get(0), customerTelegram);
        return sendMessage(telegramUser.getId(), buildCustomerPayments.toString(), markup);
    }

    private InlineKeyboardMarkup paymentOfCustomerInlineMarkup(PaymentDTO paymentDTO, CustomerTelegram customerTelegram) {
        resourceBundle = getResourceBundleUsingCustomerTelegram(customerTelegram);
        InlineKeyboardButton payButton = new InlineKeyboardButton(resourceBundle.getString("bot.message.pay.button"));
        payButton.setCallbackData("payment " + paymentDTO.getId());

        InlineKeyboardButton leftButton = new InlineKeyboardButton("⬅️");
        leftButton.setCallbackData("left");

        InlineKeyboardButton rightButton = new InlineKeyboardButton("➡️");
        rightButton.setCallbackData("right");

        backHomeMenuButton = resourceBundle.getString("bot.message.back.home.menu.button");
        InlineKeyboardButton backButton = new InlineKeyboardButton(backHomeMenuButton);
        backButton.setCallbackData(DATA_BACK_TO_HOME);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(List.of(List.of(leftButton, payButton, rightButton), List.of(backButton)));
        return markup;
    }

    private EditMessageTextDTO whenPressingLeftOrRightToGetCustomerPayment(User telegramUser, CallbackQuery callbackQuery, CustomerTelegram customerTelegram) {
        List<CustomerPaymentRedis> customerPaymentRedisList = new ArrayList<>();
        Iterable<CustomerPaymentRedis> customerPaymentRedisIterable = customerPaymentRepository.findAll();
        customerPaymentRedisIterable.forEach(customerPaymentRedisList::add);
        int sizeCustomerPayment = customerPaymentRedisList.size();

        String callBackData = callbackQuery.getData();
        if (callBackData.equals("left")) {
            --indexOfCustomerPayment;
        } else {
            ++indexOfCustomerPayment;
        }

        if (indexOfCustomerPayment < 0) {
            indexOfCustomerPayment = sizeCustomerPayment - 1;
        } else if (indexOfCustomerPayment == sizeCustomerPayment) {
            indexOfCustomerPayment = 0;
        }

        PaymentDTO paymentDTO = customerPaymentRedisList.get(indexOfCustomerPayment).getPaymentDTO();
        StringBuilder builder = new StringBuilder();
        buildCustomerPayments(paymentDTO, builder, customerTelegram);

        InlineKeyboardMarkup inlineKeyboardMarkup = paymentOfCustomerInlineMarkup(paymentDTO, customerTelegram);
        EditMessageTextDTO messageTextDTO = new EditMessageTextDTO();
        messageTextDTO.setChatId(String.valueOf(telegramUser.getId()));
        messageTextDTO.setText(builder.toString());
        messageTextDTO.setMessageId(callbackQuery.getMessage().getMessageId());
        messageTextDTO.setInlineMessageId(callbackQuery.getInlineMessageId());
        messageTextDTO.setParseMode("HTML");
        messageTextDTO.setReplyMarkup(inlineKeyboardMarkup);

        return messageTextDTO;
    }

    private void buildCustomerPayments(PaymentDTO payment, StringBuilder buildCustomerPayments, CustomerTelegram customerTelegram) {
        resourceBundle = getResourceBundleUsingCustomerTelegram(customerTelegram);

        buildCustomerPayments.append(String.format("<b>%s: </b> %d\n", resourceBundle.getString("bot.message.number.debts"),payment.getId()));
        buildCustomerPayments.append(String.format("<b>%s: </b> %s\n", resourceBundle.getString("bot.message.type.service"), payment.getService().getName()));
        buildCustomerPayments.append(String.format("<b>%s: </b> %s\n", resourceBundle.getString("bot.message.organization.name"), payment.getGroup().getOrganization().getName()));
        buildCustomerPayments.append(String.format("<b>%s: </b> %s\n", resourceBundle.getString("bot.message.group.name"), payment.getGroup().getName()));
        buildCustomerPayments.append(String.format("<b>%s: </b>%.2f sum\n", resourceBundle.getString("bot.message.service.price"), payment.getPaymentForPeriod()));
        buildCustomerPayments.append(String.format("<b>%s: </b>%.2f sum\n", resourceBundle.getString("bot.message.paid.money"), payment.getPaidMoney()));
        buildCustomerPayments.append(String.format("""
                <b>%s:
                %s: </b> %s
                <b>%s: </b> %s
                """,
            resourceBundle.getString("bot.message.time.payment"),
            resourceBundle.getString("bot.message.started.time.payment"),
            DateUtils.parseToStringFromLocalDate(payment.getStartedPeriod()),
            resourceBundle.getString("bot.message.finished.time.payment"),
            DateUtils.parseToStringFromLocalDate(payment.getFinishedPeriod())));
    }

    // Customer can be entered to this menu after registration
    public SendMessage sendCustomerMenu(User userTelegram, CustomerTelegram customerTelegram) {
        customerTelegram.setStep(2);
        customerTelegramRepository.save(customerTelegram);

        return sendMessage(userTelegram.getId(), "\uD83C\uDFE0 Menu", menu(customerTelegram));
    }

    public ReplyKeyboardMarkup menu(CustomerTelegram customerTelegram) {
        resourceBundle = getResourceBundleUsingCustomerTelegram(customerTelegram);
        setTextToButtons(resourceBundle);

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton(groupReplyButton));
        row1.add(new KeyboardButton(paymentHistoryReplyButton));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton(paymentReplyButton));
        row2.add(new KeyboardButton(payReplyButton));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton(myProfileReplyButton));

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        markup.setKeyboard(List.of(row1, row2, row3));
        return markup;
    }

    private SendMessage sendCustomerDataNotFoundMessage(User telegramUser) {
        resourceBundle = getResourceBundleUsingTelegramUser(telegramUser);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(telegramUser.getId());
        sendMessage.setText(resourceBundle.getString("bot.message.not.found.message"));
        return sendMessage;
    }

    public SendMessage sendCustomerGroups(User telegramUser, CustomerTelegram customerTelegram) {
        resourceBundle = getResourceBundleUsingCustomerTelegram(customerTelegram);
        SendMessage sendMessage;
        Customers customer = customerTelegram.getCustomer();
        if (customer == null || customer.getGroups() == null)
            return sendCustomerDataNotFoundMessage(telegramUser);

        StringBuilder buildCustomerGroups = new StringBuilder();
        for (Groups group : customer.getGroups()) {
            buildCustomerGroups.append(String.format("<b>%s: </b> %s\n", resourceBundle.getString("bot.message.group.name"), group.getName()));
            buildCustomerGroups.append(String.format("<b>%s: </b> %s\n\n", resourceBundle.getString("bot.message.organization.name"), group.getOrganization().getName()));
            buildCustomerGroups.append("------------------------------------\n");
            buildCustomerGroups.append(String.format("          <b>%s</b>\n\n", resourceBundle.getString("bot.message.group.people")));

            for (Customers groupCustomer : group.getCustomers()) {
                buildCustomerGroups.append(String.format("<b>%s: </b> %s\n", resourceBundle.getString("bot.message.people.name"), groupCustomer.getUsername()));
                buildCustomerGroups.append(String.format("<b>%s: </b> %s\n\n",resourceBundle.getString("bot.message.people.phone.number"), groupCustomer.getPhoneNumber()));
            }
            sendMessage = sendMessage(telegramUser.getId(), buildCustomerGroups.toString());
            sendMessage.enableHtml(true);
            customerFeign.sendMessage(uri, sendMessage);
            buildCustomerGroups = new StringBuilder();
        }
        return new SendMessage();
    }

    private void buildPaymentHistoryMessage(PaymentHistory paymentHistory, StringBuilder buildCustomerPaymentHistories) {
        buildCustomerPaymentHistories.append(String.format("<b>To'lov raqami: </b> %d\n", paymentHistory.getId()));
        buildCustomerPaymentHistories.append(String.format("<b>Tashkilot nomi: </b> %s\n", paymentHistory.getOrganizationName()));
        buildCustomerPaymentHistories.append(String.format("<b>Guruh nomi: </b> %s\n", paymentHistory.getGroupName()));
        buildCustomerPaymentHistories.append(String.format("<b>Xizmat turi: </b> %s\n", paymentHistory.getServiceName()));
        buildCustomerPaymentHistories.append(String.format("<b>To'lov miqdori: </b> %.2f sum\n", paymentHistory.getSum()));
        buildCustomerPaymentHistories.append(String.format("<b>To'langan sana: </b> %s", DateUtils.parseToStringFromLocalDate(paymentHistory.getCreatedAt())));
    }

    public SendMessage sendCustomerPaymentsHistory(User telegramUser, CustomerTelegram customerTelegram) {
        Customers customer = customerTelegram.getCustomer();
        if (customer == null || customer.getGroups() == null) return sendCustomerDataNotFoundMessage(telegramUser);

        List<PaymentHistory> paymentHistoryList = paymentHistoryService.getTelegramCustomerPaymentHistories(customer);
        if (paymentHistoryList.size() == 0) return sendCustomerDataNotFoundMessage(telegramUser);
        StringBuilder buildCustomerPaymentHistories = new StringBuilder();

        for (PaymentHistory paymentHistory : paymentHistoryList) {
            buildPaymentHistoryMessage(paymentHistory, buildCustomerPaymentHistories);
            SendMessage sendMessage = sendMessage(telegramUser.getId(), buildCustomerPaymentHistories.toString());
            customerFeign.sendMessage(uri, sendMessage);
            buildCustomerPaymentHistories = new StringBuilder();
        }
        return new SendMessage();
    }

    private Customers checkCustomerPhoneNumber(String phoneNumber) {
        log.info("Checking customer phoneNumber : {}", phoneNumber);
        Optional<Customers> customerOptional = customersService.findByPhoneNumber(phoneNumber);
        if (customerOptional.isEmpty()) {
            return null;
        }
        return customerOptional.get();
    }

    public void startCommand(User user, SendMessage sendMessage) {
        String sendStringMessage = "Assalomu alaykum " + user.getUserName() +
            ", CPM(nom qo'yiladi) to'lov tizimiga xush kelibsiz! \n";
        sendMessage = new SendMessage();
        sendMessage.setText(sendStringMessage);
        sendMessage.setChatId(user.getId());

        customerFeign.sendMessage(uri, sendMessage);
    }

    @Override
    public SendMessage helpCommand(Update update, Message message) {
        String newMessage = "Bu yerda platformadan qanday foydalanish kerakligi yozilgan bo'ladi.\n" +
            "\n" +
            "Komandalar nima qilishi: \n" +
            "1. \n" +
            "2. \n" +
            "3. \n" +
            "\n" +
            "Platformadan qanday foydalanish instruksiyasi: \n" +
            "1. \n" +
            "2. \n" +
            "3. \n" +
            "\n" +
            "Bosing: /start";

        return sendMessage(message.getFrom().getId(), newMessage);
    }
}
