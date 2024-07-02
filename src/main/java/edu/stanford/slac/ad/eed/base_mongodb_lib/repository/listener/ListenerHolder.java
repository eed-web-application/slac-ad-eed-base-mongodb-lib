package edu.stanford.slac.ad.eed.base_mongodb_lib.repository.listener;

import org.springframework.stereotype.Component;

@Component
public class ListenerHolder {
    private static final ThreadLocal<ModelHistoryListener> listenerHolder = new ThreadLocal<>();
    public void setListener(ModelHistoryListener listener) {
        listenerHolder.set(listener);
    }
    public ModelHistoryListener getListener() {
        return listenerHolder.get();
    }
    public void clear() {
        listenerHolder.remove();
    }
}