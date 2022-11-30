package uz.devops.intern.web.rest;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;
import uz.devops.intern.repository.PaymentRepository;
import uz.devops.intern.service.PaymentService;
import uz.devops.intern.service.dto.PaymentDTO;
import uz.devops.intern.service.dto.ResponseDTO;
import uz.devops.intern.web.rest.errors.BadRequestAlertException;

/**
 * REST controller for managing {@link uz.devops.intern.domain.Payment}.
 */
@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_MANAGER','ROLE_ADMIN')")
public class PaymentResource {
    private final Logger log = LoggerFactory.getLogger(PaymentResource.class);
    private static final String ENTITY_NAME = "payment";
    @Value("${jhipster.clientApp.name}")
    private String applicationName;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    public PaymentResource(PaymentService paymentService, PaymentRepository paymentRepository) {
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
    }
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER')")
    @PutMapping("/payment/pay")
    public ResponseEntity<ResponseDTO> payForService(@Valid @RequestBody PaymentDTO paymentDTO){
        return ResponseEntity.ok(paymentService.payForService(paymentDTO));
    }

    /**
     * {@code GET  /payments} : get all the payments.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of payments in body.
     */
    @GetMapping("/manager-payments")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER')")
    public ResponseEntity<List<PaymentDTO>> getAllPaymentsCreatedByManager() {
        log.debug("REST request to get PaymentList");
        List<PaymentDTO> paymentDTOList = paymentService.getAllPaymentsCreatedByGroupManager();
       return ResponseEntity.ok(paymentDTOList);
    }

    /**
     * {@code GET  /payments} : get all the payments.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of payments in body.
     */
    @GetMapping("/customer-payments")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER')")
    public ResponseDTO<List<PaymentDTO>> getAllCustomerPayments() {
        log.debug("REST request to get PaymentList");
        return paymentService.getAllCustomerPayments();
    }

    /**
     * {@code PUT  /payments/:id} : Updates an existing payment.
     *
     * @param id the id of the paymentDTO to save.
     * @param paymentDTO the paymentDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated paymentDTO,
     * or with status {@code 400 (Bad Request)} if the paymentDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the paymentDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/payments/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER')")
    public ResponseEntity<PaymentDTO> updatePayment(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody PaymentDTO paymentDTO
    ) throws URISyntaxException {
        log.debug("REST request to update PaymentDTO : {}, {}", id, paymentDTO);
        if (!Objects.equals(id, paymentDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        PaymentDTO result = paymentService.update(paymentDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, paymentDTO.getId().toString()))
            .body(result);
    }

    /**
     * {@code PATCH  /payments/:id} : Partial updates given fields of an existing payment, field will ignore if it is null
     *
     * @param id the id of the paymentDTO to save.
     * @param paymentDTO the paymentDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated paymentDTO,
     * or with status {@code 400 (Bad Request)} if the paymentDTO is not valid,
     * or with status {@code 404 (Not Found)} if the paymentDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the paymentDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER')")
    @PatchMapping(value = "/payments/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<PaymentDTO> partialUpdatePayment(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody PaymentDTO paymentDTO
    ) throws URISyntaxException {
        log.debug("REST request to partial update PaymentDTO partially : {}, {}", id, paymentDTO);
        if (paymentDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, paymentDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!paymentRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<PaymentDTO> result = paymentService.partialUpdate(paymentDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, paymentDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /payments} : get all the payments.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of payments in body.
     */
    @GetMapping("/payments")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<PaymentDTO>> getAllPayments(@org.springdoc.api.annotations.ParameterObject Pageable pageable) {
        log.debug("REST request to get a page of Payments");
        Page<PaymentDTO> page = paymentService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /payments/:id} : get the "id" payment.
     *
     * @param id the id of the paymentDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the paymentDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/payments/{id}")
    public ResponseEntity<PaymentDTO> getPayment(@PathVariable Long id) {
        log.debug("REST request to get PaymentDTO : {}", id);
        Optional<PaymentDTO> paymentDTO = paymentService.findOne(id);
        return ResponseUtil.wrapOrNotFound(paymentDTO);
    }

    /**
     * {@code DELETE  /payments/:id} : delete the "id" payment.
     *
     * @param id the id of the paymentDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/payments/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable Long id) {
        log.debug("REST request to delete PaymentDTO : {}", id);
        paymentService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
