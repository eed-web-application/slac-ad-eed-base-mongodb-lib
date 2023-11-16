package edu.stanford.slac.ad.eed.base_mongodb_lib.utility;

import edu.stanford.slac.ad.eed.baselib.model.AuthenticationToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
@Builder
@AllArgsConstructor
public class InitAuthenticationTokenIndex extends MongoDDLOps {
    private final MongoTemplate mongoTemplate;

    public void changeSet() {
        MongoDDLOps.createIndex(
                AuthenticationToken.class,
                mongoTemplate,
                new Index()
                        .on(
                                "email",
                                Sort.Direction.ASC
                        )
                        .unique()
                        .named("email")
        );
    }
}
