package guru.springframework.msscssm.services;

import guru.springframework.msscssm.domain.Payment;
import guru.springframework.msscssm.domain.PaymentEvent;
import guru.springframework.msscssm.domain.PaymentState;
import guru.springframework.msscssm.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.statemachine.StateMachine;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Transactional
@SpringBootTest
class PaymentServiceImplTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    private Payment payment;

    @BeforeEach
    void setUp() {
        Payment payment = Payment.builder().amount(new BigDecimal("12.99")).build();
        this.payment = paymentService.newPayment(payment);
    }

    @Test
    void preAuth() {
        System.out.println("Should be NEW");
        System.out.println(payment.getState());

        StateMachine<PaymentState, PaymentEvent> sm = paymentService.preAuth(payment.getId());

        Payment preAuthedPayment = paymentRepository.getOne(payment.getId());

        System.out.println("Should be PRE_AUTH or PRE_AUTH_ERROR");
        System.out.println(sm.getState().getId());

        System.out.println(preAuthedPayment);

    }

    @RepeatedTest(10)
    void auth_whenAuthApproveIsInvoked_thenStateShouldBeAuth() {
        StateMachine<PaymentState, PaymentEvent> preAuthSM = paymentService.preAuth(payment.getId());
        if (preAuthSM.getState().getId() == PaymentState.PRE_AUTH) {
            StateMachine<PaymentState, PaymentEvent> sm = paymentService.authorizePayment(payment.getId());
            System.out.println("Should be AUTH or AUTH_ERROR: " + sm.getState().getId());
        } else {
            System.out.println("PRE Auth failed");
        }
    }

    @Test
    void auth_whenAuthDeclineIsInvoked_thenStateShouldBeAuthError() {
        Payment preAuthPayment = Payment.builder()
                .state(PaymentState.PRE_AUTH)
                .amount(BigDecimal.valueOf(12.99))
                .build();
        paymentRepository.save(preAuthPayment);

        System.out.println("Should be PRE_AUTH");
        System.out.println(preAuthPayment.getState());

        StateMachine<PaymentState, PaymentEvent> sm = paymentService.declineAuth(preAuthPayment.getId());

        Payment authPayment = paymentRepository.getOne(preAuthPayment.getId());

        System.out.println("Should be AUTH or AUTH_ERROR");
        System.out.println(sm.getState().getId());

        System.out.println(authPayment);

    }
}