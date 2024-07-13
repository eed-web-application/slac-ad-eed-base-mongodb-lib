package edu.stanford.slac.ad.eed.base_mongodb_lib.utility;

import edu.stanford.slac.ad.eed.baselib.model.AuthenticationToken;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.model.LocalGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
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

        // create index on local group
        MongoDDLOps.createIndex(
                LocalGroup.class,
                mongoTemplate,
                new Index()
                        .on(
                                "name",
                                Sort.Direction.ASC
                        )
                        .unique()
                        .named("uniqueNameIndex")
        );
        MongoDDLOps.createIndex(
                LocalGroup.class,
                mongoTemplate,
                new Index()
                        .on(
                                "members",
                                Sort.Direction.ASC
                        )
                        .named("membersIndex")
        );
        MongoDDLOps.createIndex(
                LocalGroup.class,
                mongoTemplate,
                new Index()
                        .on(
                                "createdDate",
                                Sort.Direction.ASC
                        )
                        .named("createdDateIndex")
        );
        MongoDDLOps.createIndex(
                LocalGroup.class,
                mongoTemplate,
                new TextIndexDefinition.TextIndexDefinitionBuilder()
                        .onField("name")
                        .onField("description")
                        .named("fullTextIndex")
                        .build()
        );

        // create index on authentication token
        MongoDDLOps.createIndex(
                AuthenticationToken.class,
                mongoTemplate,
                new Index()
                        .on(
                                "createdDate",
                                Sort.Direction.ASC
                        )
                        .named("createDateIndex")
        );
        MongoDDLOps.createIndex(
                AuthenticationToken.class,
                mongoTemplate,
                new TextIndexDefinition.TextIndexDefinitionBuilder()
                        .onField("name")
                        .named("fullTextIndex")
                        .build()
        );
    }
}
