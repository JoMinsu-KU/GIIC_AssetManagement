package org.example.giic_asset.event;

import lombok.RequiredArgsConstructor;
import org.example.giic_asset.service.AccountMailService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SignupMailEventListener {

    private final AccountMailService accountMailService;

    @Async("mailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSignupRequested(SignupRequestedEvent event) {
        accountMailService.sendSignupReceived(event);
        accountMailService.sendSignupRequestToSuperAdmins(event);
    }

    @Async("mailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSignupApproved(SignupApprovedEvent event) {
        accountMailService.sendSignupApproved(event);
    }

    @Async("mailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSignupRejected(SignupRejectedEvent event) {
        accountMailService.sendSignupRejected(event);
    }
}
