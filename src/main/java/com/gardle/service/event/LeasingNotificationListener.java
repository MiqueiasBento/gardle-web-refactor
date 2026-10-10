package com.gardle.service.event;

import com.gardle.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LeasingNotificationListener {
    private final MessageService messageService;

    @EventListener
    public void onLeasingChanged(LeasingChangedEvent event) {
        messageService.createLeasingNotification(event.getLeasing());
    }
}
