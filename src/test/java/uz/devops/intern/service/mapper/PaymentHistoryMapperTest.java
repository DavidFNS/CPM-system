package uz.devops.intern.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentHistoryMapperTest {

    private PaymentHistoryMapper paymentHistoryMapper;

    @BeforeEach
    public void setUp() {
        paymentHistoryMapper = new PaymentHistoryMapperImpl();
    }
}
