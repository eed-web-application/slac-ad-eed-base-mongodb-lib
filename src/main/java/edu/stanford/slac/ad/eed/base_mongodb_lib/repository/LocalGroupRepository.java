package edu.stanford.slac.ad.eed.base_mongodb_lib.repository;

import edu.stanford.slac.ad.eed.baselib.model.LocalGroup;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LocalGroupRepository extends MongoRepository<LocalGroup, String>, LocalGroupRepositoryCustom {
}
