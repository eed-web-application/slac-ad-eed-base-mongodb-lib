package edu.stanford.slac.ad.eed.base_mongodb_lib.repository.listener;

import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ModelHistoryListenerFactory {
    private final ListenerHolder listenerHolder;
    private final ApplicationContext applicationContext;

    public ModelHistoryListener getListener() {
        var listener =  listenerHolder.getListener();
        if (listener == null) {
            listener = applicationContext.getBean(ModelHistoryListener.class);
            listenerHolder.setListener(listener);
        }
        return listener;
    }

    public void clear() {
        listenerHolder.clear();
    }
}