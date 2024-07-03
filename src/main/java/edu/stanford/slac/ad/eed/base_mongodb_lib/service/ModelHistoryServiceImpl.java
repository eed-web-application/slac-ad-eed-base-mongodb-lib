package edu.stanford.slac.ad.eed.base_mongodb_lib.service;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ModelChangeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ModelChangesHistoryDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.mapper.ModelChangeMapper;
import edu.stanford.slac.ad.eed.baselib.service.ModelHistoryService;
import lombok.AllArgsConstructor;
import org.javers.core.Changes;
import org.javers.core.Javers;
import org.javers.core.commit.CommitId;
import org.javers.core.diff.changetype.PropertyChange;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.repository.jql.QueryBuilder;
import org.javers.shadow.Shadow;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@Service
@AllArgsConstructor
@ConditionalOnProperty(name = "edu.stanford.slac.ad.eed.baselib.enable-model-change-history", havingValue = "true", matchIfMissing = false)
public class ModelHistoryServiceImpl extends ModelHistoryService {
    private final Javers javers;
    private final ModelChangeMapper modelChangeMapper;

    @Override
    public <T> List<ModelChangesHistoryDTO> findChangesByModelId(Class<T> modelClazz, String modelId) {
        List<CdoSnapshot> snapshots = getSnapshotById(modelClazz, modelId);
        Changes changes = getChangesById(modelClazz, modelId);
        return snapshots.stream()
                .map(snapshot ->
                        ModelChangesHistoryDTO.builder()
                                .modelId(snapshot.getCommitMetadata().getId().toString())
                                .modelId(modelId)
                                .changes(extractChanges(changes, snapshot.getCommitId()))
                                .createdDate(snapshot.getCommitMetadata().getCommitDate())
                                .createdBy(snapshot.getCommitMetadata().getAuthor())
                                .build()
                )
                .toList();
    }

    @Override
    public <T> List<T> findModelChangesByModelId(Class<T> modelClazz, String modelId) {
        List<Shadow<T>> snapshotList = getShadowsById(modelClazz, modelId);
        var result = snapshotList
                .stream()
                .sorted
                        (
                                Comparator.comparing
                                        (
                                                shadow -> shadow.getCommitMetadata().getCommitDate(),
                                                Comparator.reverseOrder()
                                        )
                        )
                .map(
                        shadow -> shadow.get()
                )
                .toList();
        return result;
    }

    /**
     * Get all snapshots of a model by its id
     *
     * @param modelClazz model class
     * @param modelId    model id
     * @param <T>        model type
     * @return list of snapshots
     */
    private <T> List<CdoSnapshot> getSnapshotById(Class<T> modelClazz, String modelId) {
        return javers.findSnapshots(QueryBuilder.byInstanceId(modelId, modelClazz).build());
    }

    private <T> List<Shadow<T>> getShadowsById(Class<T> modelClazz, String modelId) {
        return javers.findShadows(
                QueryBuilder
                        .byInstanceId(modelId, modelClazz)
                        .build()
        );
    }

    /**
     * Get all changes of a model by its id
     *
     * @param modelClazz model class
     * @param modelId    model id
     * @param <T>        model type
     * @return list of changes
     */
    private <T> Changes getChangesById(Class<T> modelClazz, String modelId) {
        return javers.findChanges(QueryBuilder.byInstanceId(modelId, modelClazz).build());
    }

    /**
     * Get all changes of a model by its id
     */
    private <T> List<ModelChangeDTO> extractChanges(Changes changes, CommitId commitId) {
        var result = changes.stream()
                .filter(
                        change -> {
                            boolean selected = change instanceof PropertyChange;
                            selected = selected && change.getCommitMetadata().isPresent();
                            selected = selected && change.getCommitMetadata().get().getId().equals(commitId);
                            return selected;
                        }
                )
                .map(change -> {
                    PropertyChange propertyChange = (PropertyChange) change;
                    return ModelChangeDTO.builder()
                            .fieldName(propertyChange.getPropertyName())
                            .oldValue(propertyChange.getLeft() != null ? propertyChange.getLeft() : null)
                            .newValue(propertyChange.getRight() != null ? propertyChange.getRight(): null)
                            .build();

                })
                .toList();
        result = new LinkedList<>(result); // reverse the order of the list
        Collections.reverse(result);
        return result;
    }
}
