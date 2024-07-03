package edu.stanford.slac.ad.eed.base_mongodb_lib.repository.listener;

import edu.stanford.slac.ad.eed.base_mongodb_lib.repository.ModelHistoryRepository;
import edu.stanford.slac.ad.eed.baselib.model.ModelChange;
import edu.stanford.slac.ad.eed.baselib.model.ModelChangesHistory;
import lombok.RequiredArgsConstructor;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.javers.core.diff.changetype.*;
import org.javers.core.diff.changetype.container.ArrayChange;
import org.javers.core.diff.changetype.container.ListChange;
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
    private final Javers javers = JaversBuilder.javers().build();

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
        List<ModelChange> historyList = new ArrayList<>();
        Diff diff = javers.compare(beforeSaveObject, updatedModel);

        diff.groupByObject().forEach(object -> {
            object.get().forEach(change -> {
                if (change.getClass().isAssignableFrom(ValueChange.class)) {
                    historyList.add(
                            ModelChange.builder()
                                    .fieldName(((ValueChange) change).getPropertyName())
                                    .oldValue(((ValueChange) change).getLeft())
                                    .newValue(((ValueChange) change).getRight())
                                    .build()
                    );
                } else if(change.getClass().isAssignableFrom(InitialValueChange.class)) {
                    // Handle other types of changes
                    historyList.add(
                            ModelChange.builder()
                                    .fieldName(((InitialValueChange) change).getPropertyName())
                                    .newValue(((InitialValueChange) change).getRight())
                                    .build()
                    );
                } else if(change.getClass().isAssignableFrom(ArrayChange.class)) {
                    // Handle other types of changes
                    historyList.add(
                            ModelChange.builder()
                                    .fieldName(((ArrayChange) change).getPropertyName())
                                    .oldValue(((ArrayChange) change).getLeft())
                                    .newValue(((ArrayChange) change).getRight())
                                    .build()
                    );
                } else if(change.getClass().isAssignableFrom(ListChange.class)) {
                    // Handle other types of changes
                    historyList.add(
                            ModelChange.builder()
                                    .fieldName(((ListChange) change).getPropertyName())
                                    .oldValue(((ListChange) change).getLeft())
                                    .newValue(((ListChange) change).getRight())
                                    .build()
                    );
                }else if(change.getClass().isAssignableFrom(NewObject.class)) {
                    // Handle other types of changes
                    System.out.println("New object added");
                } else if(change.getClass().isAssignableFrom(ReferenceChange.class)) {
                    // Handle other types of changes
                    System.out.println("Refefrence changed object added");
                }
            });
        });

        if (!historyList.isEmpty()) {
            modelHistoryRepository.save(
                    ModelChangesHistory.builder()
                            .modelId(modelId)
                            .changes(historyList)
                            .build()
            );
        }
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