package uz.devops.intern.service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import javax.validation.constraints.*;

/**
 * A DTO for the {@link uz.devops.intern.domain.Payment} entity.
 */

@SuppressWarnings("common-java:DuplicatedBlocks")
public class PaymentDTO implements Serializable {
    private Long id;
    @NotNull
    @DecimalMin(value = "0")
    private Double paidMoney;
    @DecimalMin(value = "10000")
    private Double paymentForPeriod;
    private Boolean isPaid;
    @NotNull
    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate startedPeriod;
    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate finishedPeriod;
    private CustomersDTO customer;
    private ServicesDTO service;
    private GroupsDTO group;
    public GroupsDTO getGroup() {
        return group;
    }
    public void setGroup(GroupsDTO group) {
        this.group = group;
    }

    public Boolean getPaid() {
        return isPaid;
    }

    public void setPaid(Boolean paid) {
        isPaid = paid;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getPaidMoney() {
        return paidMoney;
    }

    public void setPaidMoney(Double paidMoney) {
        this.paidMoney = paidMoney;
    }

    public Double getPaymentForPeriod() {
        return paymentForPeriod;
    }

    public void setPaymentForPeriod(Double paymentForPeriod) {
        this.paymentForPeriod = paymentForPeriod;
    }

    public Boolean getIsPaid() {
        return isPaid;
    }

    public void setIsPaid(Boolean isPaid) {
        this.isPaid = isPaid;
    }

    public LocalDate getStartedPeriod() {
        return startedPeriod;
    }

    public void setStartedPeriod(LocalDate startedPeriod) {
        this.startedPeriod = startedPeriod;
    }

    public LocalDate getFinishedPeriod() {
        return finishedPeriod;
    }

    public void setFinishedPeriod(LocalDate finishedPeriod) {
        this.finishedPeriod = finishedPeriod;
    }

    public CustomersDTO getCustomer() {
        return customer;
    }

    public void setCustomer(CustomersDTO customer) {
        this.customer = customer;
    }

    public ServicesDTO getService() {
        return service;
    }

    public void setService(ServicesDTO service) {
        this.service = service;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PaymentDTO)) {
            return false;
        }

        PaymentDTO paymentDTO = (PaymentDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, paymentDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PaymentDTO{" +
            "id=" + id +
            ", paidMoney=" + paidMoney +
            ", paymentForPeriod=" + paymentForPeriod +
            ", isPaid=" + isPaid +
            ", startedPeriod=" + startedPeriod +
            ", finishedPeriod=" + finishedPeriod +
            ", customer=" + customer +
            ", service=" + service +
            ", group=" + group +
            '}';
    }
}
