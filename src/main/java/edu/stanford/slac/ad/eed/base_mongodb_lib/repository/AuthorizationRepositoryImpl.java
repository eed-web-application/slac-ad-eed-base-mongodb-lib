package edu.stanford.slac.ad.eed.base_mongodb_lib.repository;

import com.mongodb.DuplicateKeyException;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import static edu.stanford.slac.ad.eed.baselib.utility.StringUtilities.normalizeStringWithReplace;

@Repository
@AllArgsConstructor
public class AuthorizationRepositoryImpl implements AuthorizationRepositoryCustom {
    MongoTemplate mongoTemplate;

    public String getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getPrincipal().toString();
    }

    @Override
    public String ensureAuthorization(Authorization authorization) {
        Authorization authorizationCreated = null;
        Query query = new Query(
                Criteria.where("owner").is(authorization.getOwner())
                        .and("resource").is(authorization.getResource())
                        .and("ownerType").is(authorization.getOwnerType())
                        .and("authorizationType").is(authorization.getAuthorizationType())
        );
        Update update = new Update()
                .setOnInsert("owner", authorization.getOwner())
                .setOnInsert("resource", authorization.getResource())
                .setOnInsert("ownerType", authorization.getOwnerType())
                .setOnInsert("authorizationType", authorization.getAuthorizationType())
                .setOnInsert("createdBy", getCurrentAuditor())
                .setOnInsert("lastModifiedBy", getCurrentAuditor())
                .setOnInsert("createdDate", LocalDateTime.now())
                .setOnInsert("lastModifiedDate", LocalDateTime.now());

        FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true).upsert(true);
        try {
            authorizationCreated = mongoTemplate.findAndModify(
                    query,
                    update,
                    options,
                    Authorization.class
            );
        } catch (DuplicateKeyException e) {
            // The insert failed because the document already exists, so fetch and return it
            authorizationCreated = mongoTemplate.findOne(query, Authorization.class);
        }
        return Objects.requireNonNull(authorizationCreated).getId();
    }
}
