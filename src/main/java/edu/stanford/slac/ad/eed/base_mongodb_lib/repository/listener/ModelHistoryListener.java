package edu.stanford.slac.ad.eed.base_mongodb_lib.repository.listener;

import edu.stanford.slac.ad.eed.base_mongodb_lib.repository.ModelHistoryRepository;
import edu.stanford.slac.ad.eed.baselib.model.CaptureChanges;
import edu.stanford.slac.ad.eed.baselib.model.ModelChange;
import edu.stanford.slac.ad.eed.baselib.model.ModelChangesHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class ModelHistoryListener {
    private Object beforeSaveObject = null;
    private final MongoTemplate mongoTemplate;
    private final ModelHistoryRepository modelHistoryRepository;

    public void handleBeforeSaveEvent(BeforeSaveEvent<Object> event) {
        Object updatedModel = event.getSource();
        if(updatedModel.getClass().isAssignableFrom(ModelChangesHistory.class)) {
            return;
        }
        String modelId = getModelId(updatedModel);
        if (modelId != null) {
            beforeSaveObject = fetchExistingModel(updatedModel, modelId);
        }
    }

    public void handleAfterSaveEvent(AfterSaveEvent<Object> event) {
        Object updatedModel = event.getSource();
        if(updatedModel.getClass().isAssignableFrom(ModelChangesHistory.class)) {
            return;
        }
        String modelId = getModelId(updatedModel);
        if (modelId != null) {
            if (beforeSaveObject != null) {
                List<ModelChange> historyList = new ArrayList<>();
                Field[] fields = updatedModel.getClass().getDeclaredFields();

                for (Field field : fields) {
                    field.setAccessible(true);
                    try {
                        if(field.getAnnotationsByType(CaptureChanges.class).length == 0) {
                            continue;
                        }
                        Object oldValue = field.get(beforeSaveObject);
                        Object newValue = field.get(updatedModel);

                        if (oldValue != null && !oldValue.equals(newValue)) {
                            historyList.add
                                    (
                                            ModelChange.builder()
                                                    .fieldName(field.getName())
                                                    .newValue(newValue != null ? newValue.toString() : null)
                                                    .oldValue(oldValue != null ? oldValue.toString() : null)
                                                    .build()
                                    );
                        }
                    } catch (IllegalAccessException e) {
                        // Handle exception
                    }
                }

                if (!historyList.isEmpty()) {
                    modelHistoryRepository.save(
                            ModelChangesHistory
                                    .builder()
                                    .modelId(modelId)
                                    .changes(historyList)
                                    .build()
                    );
                }
            } else {
                List<ModelChange> historyList = new ArrayList<>();
                Field[] fields = updatedModel.getClass().getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    try {
                        if(field.getAnnotationsByType(CaptureChanges.class).length == 0) {
                            continue;
                        }
                        Object newValue = field.get(updatedModel);
                        historyList.add
                                (
                                        ModelChange.builder()
                                                .fieldName(field.getName())
                                                .newValue(newValue != null ? newValue.toString() : null)
                                                .build()
                                );
                    } catch (IllegalAccessException e) {
                        // Handle exception
                    }
                }

                if (!historyList.isEmpty()) {
                    modelHistoryRepository.save(
                            ModelChangesHistory
                                    .builder()
                                    .modelId(modelId)
                                    .changes(historyList)
                                    .build()
                    );
                }
            }
        }
    }

    /**
     * Fetch the existing model from the database
     *
     * @param model The model to be updated
     * @return The existing model
     */
    private Object fetchExistingModel(Object model, String modelId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("id").is(modelId));
        return mongoTemplate.findOne(query, model.getClass());
    }

    /**
     * Get the identifier of the model
     *
     * @param model The model
     * @return The identifier of the model
     */
    private String getModelId(Object model) {
        try {
            Field idField = model.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            var idFieldValue = idField.get(model);
            return idFieldValue != null ? idFieldValue.toString() : null;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Handle exception
            return null;
        }
    }
}