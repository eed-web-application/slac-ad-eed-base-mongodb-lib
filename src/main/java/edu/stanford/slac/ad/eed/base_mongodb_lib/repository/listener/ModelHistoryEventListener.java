package edu.stanford.slac.ad.eed.base_mongodb_lib.repository.listener;

import lombok.AllArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ModelHistoryEventListener {
    private final ModelHistoryListenerFactory modelHistoryListenerFactory;

    @EventListener
    public void handleBeforeSaveEvent(BeforeSaveEvent<Object> event) {
        ModelHistoryListener listener = modelHistoryListenerFactory.getListener();
        listener.handleBeforeSaveEvent(event);
    }

    @EventListener
    public void handleAfterSaveEvent(AfterSaveEvent<Object> event) {
        try {
            ModelHistoryListener listener = modelHistoryListenerFactory.getListener();
            if (listener != null) {
                listener.handleAfterSaveEvent(event);
            }
        } finally {
            modelHistoryListenerFactory.clear();
        }
    }
}
