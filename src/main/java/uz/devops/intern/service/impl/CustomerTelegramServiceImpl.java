package uz.devops.intern.service.impl;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import uz.devops.intern.domain.*;
import uz.devops.intern.feign.CustomerFeign;
import uz.devops.intern.redis.CustomerTelegramRedis;
import uz.devops.intern.redis.CustomerTelegramRedisRepository;
import uz.devops.intern.repository.CustomerTelegramRepository;
import uz.devops.intern.service.*;
import uz.devops.intern.service.dto.PaymentDTO;
import uz.devops.intern.service.utils.DateUtils;
import uz.devops.intern.telegram.bot.utils.KeyboardUtil;


import javax.persistence.EntityManager;

import static uz.devops.intern.telegram.bot.utils.KeyboardUtil.sendMarkup;
import static uz.devops.intern.telegram.bot.utils.TelegramsUtil.*;

/**
 * Service Implementation for managing {@link CustomerTelegram}.
 */
@Service
@Transactional
public class CustomerTelegramServiceImpl implements CustomerTelegramService {
    @Autowired
    private EntityManager entityManager;

    private static final String groupButtonMessage = "\uD83D\uDCAC Guruhlarim";
    private static final String paymentHistoryButtonMessage = "\uD83D\uDCC3 To'lovlar tarixi";
    private static final String paymentButtonMessage = "\uD83D\uDCB0 Qarzdorligim";
    private static final String payButtonMessage = "\uD83D\uDCB8 To'lov qilish";
    private static final String changeButtonMessage = "✏️ Profilni o'zgartirish";
    private static final String backButtonMessage = "⬅️ Ortga";
    private static final String inlineButtonPayForService = "💸 To'lov qilish";
    private static Long chatIdCreatedByManager;
    private final Logger log = LoggerFactory.getLogger(CustomerTelegramServiceImpl.class);
    private final CustomerTelegramRepository customerTelegramRepository;
    private final CustomerTelegramRedisRepository customerTelegramRedisRepository;
    private static Customers authenticatedCustomer;
    private final CustomersService customersService;
    private final CustomerFeign customerFeign;
    private final PaymentService paymentService;
    private final TelegramGroupService telegramGroupService;
    private final PaymentHistoryService paymentHistoryService;
    public CustomerTelegramServiceImpl(CustomerTelegramRepository customerTelegramRepository, CustomerTelegramRedisRepository customerTelegramRedisRepository, CustomersService customersService, CustomerFeign customerFeign, PaymentService paymentService, TelegramGroupService telegramGroupService, PaymentHistoryService paymentHistoryService) {
        this.customerTelegramRepository = customerTelegramRepository;
        this.customerTelegramRedisRepository = customerTelegramRedisRepository;
        this.customersService = customersService;
        this.customerFeign = customerFeign;
        this.paymentService = paymentService;
        this.telegramGroupService = telegramGroupService;
        this.paymentHistoryService = paymentHistoryService;
    }

    public SendMessage checkBotToken(User telegramUser, Long chatId){
        Optional<TelegramGroup> telegramGroup = telegramGroupService.findByChatId(chatId);
        if (telegramGroup.isEmpty()){
            String sendStringMessage = "\uD83D\uDEAB Kechirasiz, sizdagi mavjud telegram guruhi tizimdan " +
                "foydalanish uchun ro'yxatdan o'tmagan. Guruh rahbaringiz telegram guruhni tizimdan ro'yxatdan o'tkazishi zarur!";

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(telegramUser.getId());
            sendMessage.setText(sendStringMessage);

            return sendMessage;
        }
        return null;
    }

    @Override
    public SendMessage botCommands(Update update) {
        if (!update.hasMessage()){
            if (!update.hasCallbackQuery()){
                return null;
            }

            SendMessage sendMessage = whenPressingInlineButton(update.getCallbackQuery());
            if (sendMessage != null) return sendMessage;
        }else {
            Message message = update.getMessage();
            User telegramUser = message.getFrom();

            SendMessage sendMessage;
            String requestMessage = message.getText();

            if (message.getText() == null) {
                requestMessage = message.getContact().getPhoneNumber();
            }

            switch (requestMessage) {
                case "/start":
                    return sendMessage(telegramUser.getId(), "\uD83D\uDEAB Botga guruhga tashlangan link orqali kiring!");
                case "/help":
                    return helpCommand(message);
            }
            boolean isEnteredStartCommand = false;
            if (requestMessage.startsWith("/start ")) {
                try {
                    chatIdCreatedByManager = Long.parseLong(requestMessage.substring(7));
                    sendMessage = checkBotToken(telegramUser, chatIdCreatedByManager);
                    if (sendMessage != null) {
                        return sendMessage;
                    }

                    startCommand(telegramUser, sendMessage);
                    isEnteredStartCommand = true;
                }catch (NumberFormatException numberFormatException){
                    log.error("Error parsing chatId to Long when bot started");
                    return sendMessage(telegramUser.getId(), "❌ Chat raqami xato kiritilgan!");
                }
            }

            Optional<CustomerTelegram> customerTelegramOptional = customerTelegramRepository.findByTelegramId(telegramUser.getId());
            if (customerTelegramOptional.isEmpty()) {
                CustomerTelegram customerTelegram = createCustomerTelegramToSaveDatabase(telegramUser);

                if (chatIdCreatedByManager != null) {
                    entityManager.detach(customerTelegram);
                    Optional<TelegramGroup> telegramGroupOptional = telegramGroupService.findByChatId(chatIdCreatedByManager);
                    if (telegramGroupOptional.isPresent()){
                        TelegramGroup telegramGroup = new TelegramGroup();
                        telegramGroup.setId(telegramGroupOptional.get().getId());
                        customerTelegram.setTelegramGroups(Set.of(telegramGroup));
                    }
                }
                customerTelegramRepository.save(customerTelegram);

                String responseString = "Quyidagi tillardan birini tanlang \uD83D\uDC47";
                ReplyKeyboardMarkup replyKeyboardMarkup = KeyboardUtil.language();
                log.info("Message send successfully! User id: {} | Message text: {} | Update: {}",
                    telegramUser.getId(), responseString, update);
                return sendMessage(telegramUser.getId(), responseString, replyKeyboardMarkup);
            } else {
                CustomerTelegram customerTelegram = customerTelegramOptional.get();
                Integer step = customerTelegram.getStep();

                if (step != 1 && isEnteredStartCommand){
                    step = 1;
                }

                switch (step) {
                    case 1:
                        return registerCustomerClientAndShowCustomerMenu(requestMessage, telegramUser, customerTelegram);
                    case 2:
                        sendMessage = mainCommand(requestMessage, telegramUser, customerTelegram);
                        if (sendMessage != null) return sendMessage;
//                    case 3:
                }
            }
        }

        return null;
    }

    private SendMessage whenPressingInlineButton(CallbackQuery callbackQuery) {


        String stringMessage = """
                To'lamoqchi bo'lgan summangizni kiriting

                <i>Qo'shimcha: </i> Kiritiladigan summa faqat butun sonlardan tashkil topgan bo'lishi va manfiy bo'lmasligi zarur \uD83D\uDC47""";
//
//        return switch (inlineButtonText){
//            case inlineButtonPayForService -> sendMessage(telegramUser.getId(), stringMessage);
//
//        };

        return null;
    }

    private SendMessage payRequestForService(User telegramUser, CustomerTelegram customerTelegram, String paymentData){
//        InlineKeyboardMarkup markup = update.getCallbackQuery().getMessage().getReplyMarkup();
//        List<InlineKeyboardButton> buttonList = markup.getKeyboard().get(0);


        //        Optional<CustomerTelegram> optionalCustomerTelegram = customerTelegramRepository.findByTelegramId(telegramUser.getId());
//        if (authenticatedCustomer == null || optionalCustomerTelegram.isEmpty())
//            return sendCustomerDataNotFoundMessage(telegramUser);
//
//        CustomerTelegram customerTelegram = optionalCustomerTelegram.get();

        String stringId = paymentData.substring(9);
        Long id = Long.parseLong(stringId);

        Optional<PaymentDTO> paymentOptional = paymentService.findOne(id);
        if (paymentOptional.isEmpty())
            return sendMessage(telegramUser.getId(), "\uD83D\uDEAB Kechirasiz bu qarzdorlik turi ma'lumotlar omboridan topilmadi. " +
                "Noqulaylik uchun uzr so'raymiz");

        PaymentDTO paymentDTO = paymentOptional.get();

//        ResponseDTO responsePayment = paymentService.payForService();
        return null;
    }

    public SendMessage mainCommand(String buttonMessage, User telegramUser, CustomerTelegram customerTelegram){
        SendMessage sendMessage = new SendMessage();

        return switch (buttonMessage) {
            case groupButtonMessage -> sendCustomerGroups(telegramUser, customerTelegram);
            case paymentButtonMessage, payButtonMessage -> sendCustomerPayments(telegramUser, customerTelegram);
            case paymentHistoryButtonMessage -> sendCustomerPaymentsHistory(telegramUser, customerTelegram);
            default -> sendMessage;
        };
    }

    private SendMessage registerCustomerClientAndShowCustomerMenu(String requestMessage, User telegramUser, CustomerTelegram customerTelegram) {
        SendMessage sendMessage = new SendMessage();
        Optional<CustomerTelegramRedis> redisOptional = customerTelegramRedisRepository.findById(telegramUser.getId());

        if (!requestMessage.startsWith("+998")) {
            sendMessage = checkPhoneNumberIsNull(customerTelegram, telegramUser);
            if (sendMessage != null) {
                return sendMessage;
            }
        } else {

            // when customer entered from another telegram bot
            if (customerTelegram.getPhoneNumber() != null){
                if (chatIdCreatedByManager != null) {
                    TelegramGroup telegramGroup = new TelegramGroup();
                    telegramGroup.setChatId(chatIdCreatedByManager);

                    Set<TelegramGroup> customerTelegramGroups = customerTelegram.getTelegramGroups();

                    if (customerTelegramGroups != null){
                        customerTelegramGroups.add(telegramGroup);
                    }
                }
                return sendCustomerMenu(telegramUser);

            }else {
                Customers customer = checkCustomerPhoneNumber(requestMessage);
                Authority customerAuthority = new Authority();
                customerAuthority.setName("ROLE_CUSTOMER");
                if (customer == null) {
                    String sendStringMessage = "\uD83D\uDEAB Kechirasiz, bu raqam ma'lumotlar omboridan topilmadi\n" +
                        "Tizim web-sahifasi orqali ro'yxatdan o'tishingizni so'raymiz!";

                    sendMessage = sendMessage(telegramUser.getId(), sendStringMessage, sendMarkup());
                    log.info("Message send successfully! User id: {} | Message text: {}", telegramUser, sendMessage);
                    return sendMessage;
                } else if (customer.getUser() == null || !customer.getUser().getAuthorities().contains(customerAuthority)) {
                    String sendStringMessage = "\uD83D\uDEAB Kechirasiz, bu raqamga 'Foydalanuvchi' huquqi berilmagan\n" +
                        "Boshqa raqam kiritishingizni so'raymiz!";

                    sendMessage = sendMessage(telegramUser.getId(), sendStringMessage, sendMarkup());
                    log.info("Message send successfully! User id: {} | Message text: {}", telegramUser, sendMessage);
                    return sendMessage;
                } else {
                    customerTelegram.customer(customer);
                    customerTelegramRepository.save(customerTelegram);

                    authenticatedCustomer = customer;
                }

                customerTelegram.setPhoneNumber(requestMessage);
                customerTelegram.setStep(2);
                customerTelegramRepository.save(customerTelegram);
                log.info("Phone number successfully set to existing user! telegram user : {} | phoneNumber: {} ", customerTelegram, requestMessage);

                sendMessage.setChatId(telegramUser.getId());
                sendMessage.setText("Hurmatli foydalanuvchi " + telegramUser.getFirstName() +
                    ", tizimdan foydalanish uchun muvaffaqiyatli ro'yxatdan o'tdingiz ✅");

                customerFeign.sendMessage(sendMessage);

                if (redisOptional.isEmpty()) {
                    CustomerTelegramRedis customerTelegramRedis = new CustomerTelegramRedis(telegramUser.getId(), telegramUser);
                    customerTelegramRedisRepository.save(customerTelegramRedis);
                    log.info("New telegram user successfully saved to redis! UserRedis : {}", customerTelegramRedis);
                }
            }
        }

        return sendCustomerMenu(telegramUser);
    }

    // Customer can be entered to this menu after registration
    public SendMessage sendCustomerMenu(User userTelegram){
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton(groupButtonMessage));
        row1.add(new KeyboardButton(paymentHistoryButtonMessage));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton(paymentButtonMessage));
        row2.add(new KeyboardButton(payButtonMessage));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton(changeButtonMessage));
        row3.add(new KeyboardButton(backButtonMessage));

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        markup.setKeyboard(List.of(row1, row2, row3));

        return sendMessage(userTelegram.getId(), "\uD83C\uDFE0 Menu", markup);
    }

    private SendMessage sendCustomerDataNotFoundMessage(User telegramUser){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(telegramUser.getId());
        sendMessage.setText("❗️Kechirasiz, ma'lumotlar omboridan sizning ma'lumotlaringiz topilmadi!");
        return sendMessage;
    }

    public SendMessage sendCustomerPayments(User telegramUser, CustomerTelegram customerTelegram){
        Customers customer = customerTelegram.getCustomer();
        if (customer == null || customer.getGroups() == null)
            return sendCustomerDataNotFoundMessage(telegramUser);

        List<Payment> paymentList = paymentService.getAllCustomerPaymentsPayedIsFalse(customer);
        if (paymentList.size() == 0)
            return sendCustomerDataNotFoundMessage(telegramUser);

        customerTelegram.setStep(3);
        StringBuilder buildCustomerPayments = new StringBuilder();
        for (Payment payment: paymentList){
            buildCustomerPayments.append(String.format("<b>Qarzdorlik raqami: </b> %d\n", payment.getId()));
            buildCustomerPayments.append(String.format("<b>Xizmat turi: </b> %s\n", payment.getService().getName()));
            buildCustomerPayments.append(String.format("<b>Qaysi tashkilot uchun: </b> %s\n", payment.getGroup().getOrganization().getName()));
            buildCustomerPayments.append(String.format("<b>Qaysi guruh uchun: </b> %s\n", payment.getGroup().getName()));
            buildCustomerPayments.append(String.format("<b>Xizmat narxi: </b>%.2f sum\n", payment.getPaymentForPeriod()));
            buildCustomerPayments.append(String.format("<b>To'langan summa: </b>%.2f sum\n", payment.getPaidMoney()));
            buildCustomerPayments.append(String.format("""
                    <b>To'lov muddati:
                    Boshlanish vaqti: </b> %s\t<b>Tugash vaqti: </b> %s
                    """,
                DateUtils.parseToStringFromLocalDate(payment.getStartedPeriod()), DateUtils.parseToStringFromLocalDate(payment.getFinishedPeriod())));

            InlineKeyboardButton payButton = new InlineKeyboardButton(inlineButtonPayForService);
            payButton.setCallbackData("payment: " + payment.getId());

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            markup.setKeyboard(List.of(List.of(payButton)));
            SendMessage sendMessage = sendMessage(telegramUser.getId(), buildCustomerPayments.toString(), markup);

            customerFeign.sendMessage(sendMessage);
            buildCustomerPayments = new StringBuilder();
        }
        return null;
    }

    public SendMessage sendCustomerGroups(User telegramUser, CustomerTelegram customerTelegram){
        SendMessage sendMessage;
        Customers customer = customerTelegram.getCustomer();
        if (customer == null || customer.getGroups() == null)
            return sendCustomerDataNotFoundMessage(telegramUser);

        StringBuilder buildCustomerGroups = new StringBuilder();

        for (Groups group : customer.getGroups()) {
            buildCustomerGroups.append(String.format("<b>Guruh nomi: </b> %s\n", group.getName()));
            buildCustomerGroups.append(String.format("<b>Tashkilot nomi:</b> %s\n\n", group.getOrganization().getName()));
            buildCustomerGroups.append("          <b>Guruhdagi bolalar</b>\n\n");

            for (Customers groupCustomer : group.getCustomers()) {
                buildCustomerGroups.append(String.format("<b>Ismi: </b> %s\n", groupCustomer.getUsername()));
                buildCustomerGroups.append(String.format("<b>Tel raqami: </b> %s\n\n", groupCustomer.getPhoneNumber()));
            }
            sendMessage = sendMessage(telegramUser.getId(), buildCustomerGroups.toString());
            sendMessage.enableHtml(true);
            customerFeign.sendMessage(sendMessage);
            buildCustomerGroups = new StringBuilder();
        }
        return null;
    }

    public SendMessage sendCustomerPaymentsHistory(User telegramUser, CustomerTelegram customerTelegram){
        Customers customer = customerTelegram.getCustomer();
        if (customer == null || customer.getGroups() == null) return sendCustomerDataNotFoundMessage(telegramUser);

        List<PaymentHistory> paymentHistoryList = paymentHistoryService.getTelegramCustomerPaymentHistories(customer);
        if (paymentHistoryList.size() == 0) return sendCustomerDataNotFoundMessage(telegramUser);
        StringBuilder buildCustomerPaymentHistories = new StringBuilder();

        for (PaymentHistory paymentHistory: paymentHistoryList){
            buildCustomerPaymentHistories.append(String.format("<b>To'lov raqami: </b> %d\n", paymentHistory.getId()));
            buildCustomerPaymentHistories.append(String.format("<b>Tashkilot nomi: </b> %s\n", paymentHistory.getOrganizationName()));
            buildCustomerPaymentHistories.append(String.format("<b>Guruh nomi: </b> %s\n", paymentHistory.getGroupName()));
            buildCustomerPaymentHistories.append(String.format("<b>Xizmat turi: </b> %s\n", paymentHistory.getServiceName()));
            buildCustomerPaymentHistories.append(String.format("<b>To'lov miqdori: </b> %.2f sum\n", paymentHistory.getSum()));
            buildCustomerPaymentHistories.append(String.format("<b>To'langan sana: </b> %s", DateUtils.parseToStringFromLocalDate(paymentHistory.getCreatedAt())));

            SendMessage sendMessage = sendMessage(telegramUser.getId(), buildCustomerPaymentHistories.toString());
            customerFeign.sendMessage(sendMessage);
            buildCustomerPaymentHistories = new StringBuilder();
        }
        return null;
    }

    private Customers checkCustomerPhoneNumber(String phoneNumber) {
        log.info("Checking customer phoneNumber : {}", phoneNumber);
        Optional<Customers> customerOptional = customersService.findByPhoneNumber(phoneNumber);
        if (customerOptional.isEmpty()){
            return null;
        }
        return customerOptional.get();
    }

    public void startCommand(User user, SendMessage sendMessage){
        String sendStringMessage = "Assalomu alaykum " + user.getUserName() +
            ", CPM(nom qo'yiladi) to'lov tizimiga xush kelibsiz! \n";
        sendMessage.setText(sendStringMessage);
        sendMessage.setChatId(user.getId());

        customerFeign.sendMessage(sendMessage);
    }

    public SendMessage helpCommand(Message message){
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
