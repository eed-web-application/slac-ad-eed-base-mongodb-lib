package edu.stanford.slac.ad.eed.base_mongodb_lib.repository.listener;

import edu.stanford.slac.ad.eed.base_mongodb_lib.repository.ModelHistoryRepository;
import edu.stanford.slac.ad.eed.baselib.model.CaptureChanges;
import edu.stanford.slac.ad.eed.baselib.model.ModelChange;
import edu.stanford.slac.ad.eed.baselib.model.ModelChangesHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class ModelHistoryListener {
    private Object beforeSaveObject = null;
    private final MongoTemplate mongoTemplate;
    private final ModelHistoryRepository modelHistoryRepository;

    public void handleBeforeSaveEvent(BeforeSaveEvent<Object> event) {
        String modelId = getModelId(event.getSource());
        if (modelId != null) {
            beforeSaveObject = fetchExistingModel(event.getSource(), modelId);
        }
    }

    public void handleAfterSaveEvent(AfterSaveEvent<Object> event) {
        if (beforeSaveObject == null) {
            return;
        }
        Object updatedModel = event.getSource();
        String modelId = getModelId(updatedModel);
        if (modelId != null) {
            List<ModelChange> historyList = new ArrayList<>();
            Field[] fields = updatedModel.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    if (!field.isAnnotationPresent(CaptureChanges.class)) {
                        continue;
                    }
                    if (field.getType().isArray()) {
                        historyList.addAll(fetchChangesForArrayField(field, updatedModel, beforeSaveObject));
                    } else if (List.class.isAssignableFrom(field.getType())) {
                        historyList.addAll(fetchChangesForListField(field, updatedModel, beforeSaveObject));
                    } else {
                        historyList.addAll(fetchChangesForRegularField(field, updatedModel, beforeSaveObject, null));
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
        }
    }

    private List<ModelChange> fetchChangesForArrayField(Field field, Object updatedModel, Object oldModel) throws IllegalAccessException {
        List<ModelChange> changes = new ArrayList<>();
        Object[] newArray = (Object[]) field.get(updatedModel);
        Object[] oldArray = oldModel != null ? (Object[]) field.get(oldModel) : null;
        if ((oldArray != null && newArray == null) || (oldArray == null && newArray != null) || (oldArray != null && !Arrays.equals(oldArray, newArray))) {
            changes.add(ModelChange.builder()
                    .fieldName(field.getName())
                    .oldValue(oldArray != null ? Arrays.toString(oldArray) : null)
                    .newValue(newArray != null ? Arrays.toString(newArray) : null)
                    .build());
        }
        return changes;
    }

    private List<ModelChange> fetchChangesForListField(Field field, Object updatedModel, Object oldModel) throws IllegalAccessException {
        List<ModelChange> changes = new ArrayList<>();
        List<Object> newList = (List<Object>) field.get(updatedModel);
        List<Object> oldList = oldModel != null ? (List<Object>) field.get(oldModel) : null;
        if ((oldList != null && newList == null) || (oldList == null && newList != null) || (oldList != null && !oldList.equals(newList))) {
            changes.add(ModelChange.builder()
                    .fieldName(field.getName())
                    .oldValue(oldList != null ? oldList.toString() : null)
                    .newValue(newList != null ? newList.toString() : null)
                    .build());
        }
        return changes;
    }

    private List<ModelChange> fetchChangesForRegularField(Field field, Object updatedModel, Object oldModel, String parentFieldName) throws IllegalAccessException {
        List<ModelChange> changes = new ArrayList<>();
        Object oldValue = oldModel != null ? field.get(oldModel) : null;
        Object newValue = field.get(updatedModel);
        if (newValue != null || oldValue != null) {
            if (
                    field.getType().isPrimitive() ||
                            field.getType().getName().startsWith("java.lang") ||
                            field.getType().getName().startsWith("java.time")
            ) {
                String filedCompositeName = parentFieldName != null ? parentFieldName + "." + field.getName() : field.getName();
                changes.add(ModelChange.builder()
                        .fieldName(filedCompositeName)
                        .oldValue(oldValue != null ? oldValue.toString() : null)
                        .newValue(newValue != null ? newValue.toString() : null)
                        .build());
            } else {
                Field[] innerFields = field.getType().getDeclaredFields();
                for (Field innerField : innerFields) {
                    if (!innerField.isAnnotationPresent(CaptureChanges.class)) {
                        continue;
                    }
                    innerField.setAccessible(true);
                    changes.addAll(fetchChangesForRegularField(innerField, newValue, oldValue, field.getName()));
                }
            }
        }
        return changes;
    }

    private Object fetchExistingModel(Object model, String modelId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("id").is(modelId));
        return mongoTemplate.findOne(query, model.getClass());
    }

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