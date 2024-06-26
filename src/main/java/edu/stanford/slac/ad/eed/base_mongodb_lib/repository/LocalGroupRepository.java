package edu.stanford.slac.ad.eed.base_mongodb_lib.repository;

import edu.stanford.slac.ad.eed.baselib.model.LocalGroup;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LocalGroupRepository extends MongoRepository<LocalGroup, String>, LocalGroupRepositoryCustom {

    List<LocalGroup> findAllByMembersContains(String userId);
}
