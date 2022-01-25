package guru.springframework.msscssm.config;

import guru.springframework.msscssm.actions.PreAuthorizeAction;
import guru.springframework.msscssm.actions.PaymentIdGuard;
import guru.springframework.msscssm.domain.PaymentEvent;
import guru.springframework.msscssm.domain.PaymentState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

import java.util.EnumSet;
import java.util.Random;

import static guru.springframework.msscssm.domain.PaymentEvent.*;
import static guru.springframework.msscssm.services.PaymentServiceImpl.PAYMENT_ID_HEADER;

/**
 * Created by jt on 2019-07-23.
 */
@Slf4j
@EnableStateMachineFactory
@Configuration
@RequiredArgsConstructor
public class StateMachineConfig extends StateMachineConfigurerAdapter<PaymentState, PaymentEvent> {
    private final PaymentIdGuard paymentIdGuard;
    private final PreAuthorizeAction preAuthorizeAction;

    @Override
    public void configure(StateMachineStateConfigurer<PaymentState, PaymentEvent> states) throws Exception {
        states.withStates()
                .initial(PaymentState.NEW)
                .states(EnumSet.allOf(PaymentState.class))
                .end(PaymentState.AUTH)
                .end(PaymentState.PRE_AUTH_ERROR)
                .end(PaymentState.AUTH_ERROR);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<PaymentState, PaymentEvent> transitions) throws Exception {
        // preauth
        transitions.withExternal().source(PaymentState.NEW).target(PaymentState.NEW).event(PRE_AUTHORIZE)
//                .action(preAuthAction()).guard(paymentIdGuard())
                .action(preAuthorizeAction).guard(paymentIdGuard)
                .and()
                .withExternal().source(PaymentState.NEW).target(PaymentState.PRE_AUTH).event(PRE_AUTH_APPROVED)
                .action(preAuthApproved())
                .and()
                .withExternal().source(PaymentState.NEW).target(PaymentState.PRE_AUTH_ERROR).event(PRE_AUTH_DECLINED)
                .action(preAuthDeclined())
                // auth
                .and()
                .withExternal().source(PaymentState.PRE_AUTH).target(PaymentState.PRE_AUTH).event(AUTHORIZE)
                .action(authAction())
                .and()
                .withExternal().source(PaymentState.PRE_AUTH).target(PaymentState.AUTH).event(AUTH_APPROVED)
                .action(authApproved())
                .and()
                .withExternal().source(PaymentState.PRE_AUTH).target(PaymentState.AUTH_ERROR).event(AUTH_DECLINED)
                .action(authDeclined())
        ;
    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<PaymentState, PaymentEvent> config) throws Exception {
        StateMachineListenerAdapter<PaymentState, PaymentEvent> adapter = new StateMachineListenerAdapter<>() {
            @Override
            public void stateChanged(State<PaymentState, PaymentEvent> from, State<PaymentState, PaymentEvent> to) {
                log.info(String.format("stateChanged(from: %s, to: %s)", from, to));
            }
        };

        config.withConfiguration()
                .listener(adapter);
    }

//    @Bean
    public Action<PaymentState, PaymentEvent> preAuthAction() {
        return context -> {
            log.info("PreAuth was called!!!");

            if (new Random().nextInt(10) < 8) {
                log.info("Approved");
                sendEvent(context, PRE_AUTH_APPROVED);

            } else {
                log.info("Declined! No Credit!!!!!!");
                sendEvent(context, PRE_AUTH_DECLINED);
            }
        };
    }

    @Bean
    public Action<PaymentState, PaymentEvent> authAction() {
        return context -> {
            log.info("Auth was called!!!");

            if (new Random().nextInt(10) < 6) {
                log.info("Approved");
                sendEvent(context, AUTH_APPROVED);

            } else {
                log.info("Declined! No Credit!!!!!!");
                sendEvent(context, AUTH_DECLINED);
            }
        };
    }

    @Bean
    public Action<PaymentState, PaymentEvent> preAuthApproved() {
        return context -> System.out.println("Pre Auth Approved");
    }

    @Bean
    public Action<PaymentState, PaymentEvent> preAuthDeclined() {
        return context -> System.out.println("Pre Auth Declined");
    }

    @Bean
    public Action<PaymentState, PaymentEvent> authApproved() {
        return context -> System.out.println("Auth Approved");
    }

    @Bean
    public Action<PaymentState, PaymentEvent> authDeclined() {
        return context -> System.out.println("Auth Declined");
    }

    public Guard<PaymentState, PaymentEvent> paymentIdGuard(){
        return context -> context.getMessageHeader(PAYMENT_ID_HEADER) != null;
    }

    private void sendEvent(StateContext<PaymentState, PaymentEvent> context, PaymentEvent paymentEvent) {
        context.getStateMachine().sendEvent(MessageBuilder.withPayload(paymentEvent)
                .setHeader(PAYMENT_ID_HEADER, context.getMessageHeader(PAYMENT_ID_HEADER))
                .build());
    }
}
