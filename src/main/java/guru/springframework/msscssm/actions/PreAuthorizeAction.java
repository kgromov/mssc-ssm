package guru.springframework.msscssm.actions;

import guru.springframework.msscssm.domain.PaymentEvent;
import guru.springframework.msscssm.domain.PaymentState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.Random;

import static guru.springframework.msscssm.domain.PaymentEvent.PRE_AUTH_APPROVED;
import static guru.springframework.msscssm.domain.PaymentEvent.PRE_AUTH_DECLINED;
import static guru.springframework.msscssm.services.PaymentServiceImpl.PAYMENT_ID_HEADER;

@Slf4j
@Component
public class PreAuthorizeAction implements Action<PaymentState, PaymentEvent> {
    @Override
    public void execute(StateContext<PaymentState, PaymentEvent> context) {
        log.info("PreAuth was called!!!");

        if (new Random().nextInt(10) < 8) {
            log.info("Approved");
            sendEvent(context, PRE_AUTH_APPROVED);

        } else {
            log.info("Declined! No Credit!!!!!!");
            sendEvent(context, PRE_AUTH_DECLINED);
        }
    }

    private void sendEvent(StateContext<PaymentState, PaymentEvent> context, PaymentEvent paymentEvent) {
        context.getStateMachine().sendEvent(MessageBuilder.withPayload(paymentEvent)
                .setHeader(PAYMENT_ID_HEADER, context.getMessageHeader(PAYMENT_ID_HEADER))
                .build());
    }
}
