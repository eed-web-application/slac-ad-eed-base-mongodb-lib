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
import java.util.List;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class ModelHistoryListener {
    private Object beforeSaveObject = null;
    private final MongoTemplate mongoTemplate;
    private final ModelHistoryRepository modelHistoryRepository;

    /**
     * Handle the before save event
     *
     * @param event The before save event
     */
    public void handleBeforeSaveEvent(BeforeSaveEvent<Object> event) {
        String modelId = getModelId(event.getSource());
        if (modelId != null) {
            beforeSaveObject = fetchExistingModel(event.getSource(), modelId);
        }
    }

    /**
     * Handle the after save event
     *
     * @param event The after save event
     */
    public void handleAfterSaveEvent(AfterSaveEvent<Object> event) {
        // we are going to manage only the models that have
        // difference from a previously model save
        // for the first creation nothing is recorded
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

    /**
     * Fetch the changes for an array field
     *
     * @param field        The field
     * @param updatedModel The updated model
     * @return The list of changes
     * @throws IllegalAccessException If the field is not accessible
     */
    private List<ModelChange> fetchChangesForArrayField(Field field, Object updatedModel, Object oldModel) throws IllegalAccessException {
        List<ModelChange> changes = new ArrayList<>();
        Object[] newArray = (Object[]) field.get(updatedModel);
        Object[] oldArray = oldModel != null ? (Object[]) field.get(oldModel) : null;
        if (oldArray != null && newArray != null) {
            for (int i = 0; i < Math.min(oldArray.length, newArray.length); i++) {
                if (!oldArray[i].equals(newArray[i])) {
                    changes.add(ModelChange.builder()
                            .fieldName(field.getName() + "[" + i + "]")
                            .oldValue(oldArray[i].toString())
                            .newValue(newArray[i].toString())
                            .build());
                }
            }
        } else {
            if (oldArray != null) {
                for (int i = 0; i < oldArray.length; i++) {
                    changes.add(ModelChange.builder()
                            .fieldName(field.getName() + "[" + i + "]")
                            .oldValue(oldArray[i].toString())
                            .newValue(null)
                            .build());
                }
            } else if (newArray != null) {
                for (int i = 0; i < newArray.length; i++) {
                    changes.add(ModelChange.builder()
                            .fieldName(field.getName() + "[" + i + "]")
                            .oldValue(null)
                            .newValue(newArray[i].toString())
                            .build());
                }
            }
        }
        return changes;
    }

    /**
     * Fetch the changes for a list field
     *
     * @param field        The field
     * @param updatedModel The updated model
     * @return The list of changes
     * @throws IllegalAccessException If the field is not accessible
     */
    private List<ModelChange> fetchChangesForListField(Field field, Object updatedModel, Object oldModel) throws IllegalAccessException {
        List<ModelChange> changes = new ArrayList<>();
        List<Object> newList = (List<Object>) field.get(updatedModel);
        List<Object> oldList = oldModel != null ? (List<Object>) field.get(oldModel) : null;
        if (oldList != null && newList != null) {
            for (int i = 0; i < Math.min(oldList.size(), newList.size()); i++) {
                if (!oldList.get(i).equals(newList.get(i))) {
                    changes.add(ModelChange.builder()
                            .fieldName(field.getName() + "[" + i + "]")
                            .oldValue(oldList.get(i).toString())
                            .newValue(newList.get(i).toString())
                            .build());
                }
            }
        } else {
            if (oldList != null) {
                for (int i = 0; i < oldList.size(); i++) {
                    changes.add(ModelChange.builder()
                            .fieldName(field.getName() + "[" + i + "]")
                            .oldValue(oldList.get(i).toString())
                            .newValue(null)
                            .build());
                }
            } else if (newList != null) {
                for (int i = 0; i < newList.size(); i++) {
                    changes.add(ModelChange.builder()
                            .fieldName(field.getName() + "[" + i + "]")
                            .oldValue(null)
                            .newValue(newList.get(i).toString())
                            .build());
                }
            }
        }
        return changes;
    }

    /**
     * Fetch the changes for a regular field
     *
     * @param field        The field
     * @param updatedModel The updated model
     * @return The list of changes
     * @throws IllegalAccessException If the field is not accessible
     */
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
                // It's a primitive type or a wrapper, compare directly

                changes.add(ModelChange.builder()
                        .fieldName(filedCompositeName)
                        .oldValue(oldValue != null ? oldValue.toString() : null)
                        .newValue(newValue != null ? newValue.toString() : null)
                        .build());

            } else {
                // It's a custom class, recursively check for changes
                Field[] innerFields = field.getType().getDeclaredFields();
                for (Field innerField : innerFields) {
                    if (!innerField.isAnnotationPresent(CaptureChanges.class)) {
                        continue;
                    }
                    innerField.setAccessible(true);
                    // Pass the value of the field (which is an object of the custom class type)
                    // when calling fetchChangesForRegularField recursively
                    changes.addAll(fetchChangesForRegularField(innerField, newValue, oldValue, field.getName()));
                }
            }
        }
        return changes;
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