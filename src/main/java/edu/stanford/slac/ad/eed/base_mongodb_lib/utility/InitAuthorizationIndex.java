package edu.stanford.slac.ad.eed.base_mongodb_lib.utility;

import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@Builder
@AllArgsConstructor
public class InitAuthorizationIndex extends MongoDDLOps {
    private final MongoTemplate mongoTemplate;

    public void updateIndex() {
        MongoDDLOps.createIndex(
                Authorization.class,
                mongoTemplate,
                new Index()
                        .on(
                                "resource",
                                Sort.Direction.ASC
                        )
                        .named("resource")
        );
        MongoDDLOps.createIndex(
                Authorization.class,
                mongoTemplate,
                new Index()
                        .on(
                                "authorizationType",
                                Sort.Direction.ASC
                        )
                        .named("authorizationType")
        );
        MongoDDLOps.createIndex(
                Authorization.class,
                mongoTemplate,
                new Index()
                        .on(
                                "owner",
                                Sort.Direction.ASC
                        )
                        .named("owner")
        );
        MongoDDLOps.createIndex(
                Authorization.class,
                mongoTemplate,
                new Index()
                        .on(
                                "owner",
                                Sort.Direction.ASC
                        )
                        .on(
                                "resource",
                                Sort.Direction.ASC
                        )
                        .unique()
                        .sparse()
                        .named("ownerAuthResourceUnique")
        );
    }
}
