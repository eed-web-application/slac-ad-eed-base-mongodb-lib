package edu.stanford.slac.ad.eed.base_mongodb_lib.repository;

import edu.stanford.slac.ad.eed.baselib.model.ModelChangesHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ModelHistoryRepository extends MongoRepository<ModelChangesHistory, String> {
    @Query(sort = "{createdDate: -1}")
    List<ModelChangesHistory> findAllByModelId(String modelId);
}
