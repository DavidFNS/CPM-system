package uz.devops.intern.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.devops.intern.domain.Customers;
import uz.devops.intern.domain.Groups;
import uz.devops.intern.domain.Payment;
import uz.devops.intern.domain.Services;
import uz.devops.intern.repository.ServicesRepository;
import uz.devops.intern.schedule.TimerTaskToSendMessage;
import uz.devops.intern.service.CustomersService;
import uz.devops.intern.service.GroupsService;
import uz.devops.intern.service.PaymentService;
import uz.devops.intern.service.ServicesService;
import uz.devops.intern.constants.ResponseCodeConstants;
import uz.devops.intern.service.dto.ResponseDTO;
import uz.devops.intern.constants.ResponseMessageConstants;
import uz.devops.intern.service.dto.ServicesDTO;
import uz.devops.intern.service.mapper.ServiceMapper;
import uz.devops.intern.service.mapper.ServicesMapper;
import uz.devops.intern.service.utils.AuthenticatedUserUtil;
import uz.devops.intern.service.utils.ContextHolderUtil;
import uz.devops.intern.web.rest.errors.BadRequestAlertException;

import java.time.LocalDate;
import java.util.*;

import static uz.devops.intern.constants.ResponseMessageConstants.*;
import static uz.devops.intern.constants.ResponseCodeConstants.OK;

/**
 * Service Implementation for managing {@link Services}.
 */
@Service
public class ServicesServiceImpl implements ServicesService {
    private final Logger log = LoggerFactory.getLogger(ServicesServiceImpl.class);
    private final ServicesRepository servicesRepository;
    private final ServicesMapper servicesMapper;
    private final PaymentService paymentService;
    private final TimerTaskToSendMessage taskToSendMessage;
    private final GroupsService groupService;
    private static final String ENTITY_NAME = "services";
    private final CustomersService customerService;
    private final AuthenticatedUserUtil authenticatedUserUtil;
    public ServicesServiceImpl(ServicesRepository servicesRepository, ServicesMapper servicesMapper, PaymentService paymentService, TimerTaskToSendMessage taskToSendMessage, GroupsService groupService, CustomersService customerService, AuthenticatedUserUtil authenticatedUserUtil) {
        this.servicesRepository = servicesRepository;
        this.servicesMapper = servicesMapper;
        this.paymentService = paymentService;
        this.taskToSendMessage = taskToSendMessage;
        this.groupService = groupService;
        this.customerService = customerService;
        this.authenticatedUserUtil = authenticatedUserUtil;
    }

    @Override
    public ResponseDTO<ServicesDTO> save(ServicesDTO servicesDTO) {
        log.debug("Request to save Services : {}", servicesDTO);
        if (servicesDTO.getGroups().size() == 0){
            return new ResponseDTO<ServicesDTO>(ResponseCodeConstants.NOT_FOUND, "group not found", false, null);
        }

        Services services = ServiceMapper.toEntity(servicesDTO);
        services = servicesRepository.save(services);

        ResponseDTO responseDTO = createPaymentEachCustomers(services);
        if (!responseDTO.getSuccess())
            return responseDTO;

        ServicesDTO servicesDTOResponse = ServiceMapper.toDtoForSaveServiceMethod(services);
        return new ResponseDTO<ServicesDTO>(OK, SAVED, true, servicesDTOResponse);
    }

    private ResponseDTO createPaymentEachCustomers(Services service) {
        Payment paymentToSetPeriod = new Payment();
        LocalDate startedPeriod = service.getStartedPeriod();
        paymentToSetPeriod.setStartedPeriod(startedPeriod);
        PaymentServiceImpl.setNextFinishedPeriodToPayment(paymentToSetPeriod, service);
        LocalDate endPeriod = paymentToSetPeriod.getFinishedPeriod();

        Set<Groups> groupsForService = service.getGroups();
        List<Payment> paymentList = new ArrayList<>();
        List<Long> ids = new ArrayList<>();

        for (Groups groupIncludeToPayment: groupsForService){
            ids.add(groupIncludeToPayment.getId());
        }
        List<Groups> groupsIncludeToPayment = groupService.getGroupsIncludeToPayment(ids);
        if (groupsIncludeToPayment.size() == 0)
            return new ResponseDTO<ServicesDTO>(ResponseCodeConstants.NOT_FOUND, "group not fond", false, null);

        for (Groups group: groupsIncludeToPayment) {
            if (group.getCustomers() != null) {
                for (Customers customerForService : group.getCustomers()) {
                    Payment newPaymentForService = new Payment();
                    newPaymentForService.setService(service);
                    newPaymentForService.setGroup(group);
                    newPaymentForService.setCustomer(customerForService);
                    newPaymentForService.setStartedPeriod(startedPeriod);
                    newPaymentForService.setFinishedPeriod(endPeriod);
                    newPaymentForService.setIsPaid(false);
                    newPaymentForService.setPaidMoney(0D);
                    newPaymentForService.setPaymentForPeriod(service.getPrice());
                    paymentList.add(newPaymentForService);
                    taskToSendMessage.sendNotificationIfCustomerNotPaidForService(
                        customerForService, group, service, startedPeriod, endPeriod
                    );
                }
            }
        }
        paymentService.saveAll(paymentList);

        return new ResponseDTO<ServicesDTO>(OK, ResponseMessageConstants.OK, true, null);
    }

    @Override
    public ServicesDTO update(ServicesDTO servicesDTO) {
        log.debug("Request to update Services : {}", servicesDTO);
        if (servicesDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!servicesRepository.existsById(servicesDTO.getId())) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Services services = ServiceMapper.toEntity(servicesDTO);

        services = servicesRepository.save(services);
        return ServiceMapper.toDtoForSaveServiceMethod(services);
    }

    @Override
    public Optional<ServicesDTO> partialUpdate(ServicesDTO servicesDTO) {
        log.debug("Request to partially update Services : {}", servicesDTO);

        return servicesRepository
            .findById(servicesDTO.getId())
            .map(existingServices -> {
                servicesMapper.partialUpdate(existingServices, servicesDTO);

                return existingServices;
            })
            .map(servicesRepository::save)
            .map(ServiceMapper::toDtoForGetting);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServicesDTO> findAll() {
        log.debug("Request to get all Services");
        return servicesRepository.findAll().stream()
            .map(ServiceMapper::toDtoForGetting)
            .toList();
    }

    @Override
    public ResponseDTO<List<ServicesDTO>> getAllManagerServices() {
        log.debug("Request to get all Manager services");
        String username = ContextHolderUtil.getUsernameFromContextHolder();

        List<ServicesDTO> servicesDTOList = servicesRepository.findAll()
            .stream()
            .map(ServiceMapper::toDtoForGetting)
            .toList();
        return new ResponseDTO<>(OK, ResponseMessageConstants.OK, true, servicesDTOList);
    }

    @Override
    public Page<ServicesDTO> findAllWithEagerRelationships(Pageable pageable) {
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ServicesDTO> findOne(Long id) {
        log.debug("Request to get Services : {}", id);
        return servicesRepository.findById(id).map(ServiceMapper::toDtoForGetting);
    }

    @Override
    public void delete(Long id) {
        log.debug("Request to delete Services : {}", id);
        servicesRepository.deleteById(id);
    }
}
