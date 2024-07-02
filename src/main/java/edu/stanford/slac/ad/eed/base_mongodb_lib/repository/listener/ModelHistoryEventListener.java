package edu.stanford.slac.ad.eed.base_mongodb_lib.repository.listener;

import edu.stanford.slac.ad.eed.baselib.model.CaptureChanges;
import edu.stanford.slac.ad.eed.baselib.model.ModelChangesHistory;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "edu.stanford.slac.ad.eed.baselib.enable-model-change-history", havingValue = "true", matchIfMissing = false)
@AllArgsConstructor
public class ModelHistoryEventListener {
    private final ModelHistoryListenerFactory modelHistoryListenerFactory;

    @EventListener
    public void handleBeforeSaveEvent(BeforeSaveEvent<Object> event) {
        Object updatedModel = event.getSource();
        if(notManageable(updatedModel)){
            return;
        }
        ModelHistoryListener listener = modelHistoryListenerFactory.getListener();
        listener.handleBeforeSaveEvent(event);
    }

    @EventListener
    public void handleAfterSaveEvent(AfterSaveEvent<Object> event) {
        Object updatedModel = event.getSource();
        if(notManageable(updatedModel)){
            return;
        }
        try {
            ModelHistoryListener listener = modelHistoryListenerFactory.getListener();
            if (listener != null) {
                listener.handleAfterSaveEvent(event);
            }
        } finally {
            modelHistoryListenerFactory.clear();
        }
    }

    /**
     * Check if the model is not manageable
     * @param updatedModel
     * @return
     */
    private static boolean notManageable(Object updatedModel) {
        if(updatedModel.getClass().isAssignableFrom(ModelChangesHistory.class)) {
            // Do not capture changes for ModelChangesHistory
            return true;
        }

        // Capture changes only for models annotated with @CaptureChanges
        if(!updatedModel.getClass().isAnnotationPresent(CaptureChanges.class)) {
            return true;
        }
        return false;
    }
}
