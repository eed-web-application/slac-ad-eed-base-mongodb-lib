package edu.stanford.slac.ad.eed.base_mongodb_lib.repository;

import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AuthorizationRepository extends MongoRepository<Authorization, String> {
    Optional<Authorization> findByOwnerIsAndResourceIsAndAuthorizationTypeIs(String owner, String resource, Integer authorizationType);
    @Query("{ 'owner' : '?0', $or: [{'resource' : '?1', 'authorizationType' : { '$gte' : ?2}},{'authorizationType' : 2, resource:'*'}]}")
    Optional<Authorization> findByOwnerIsAndResourceIsAndAuthorizationTypeIsGreaterThanEqual(String owner, String resource, Integer authorizationType);
    List<Authorization> findByResourceIs(String resource);
    List<Authorization> findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(String resource, Integer authorizationType);
    //@Query("{ 'owner' : '?0', $or: [{'authorizationType' : { '$gte' : '?1'}, 'resource' : { $regex : \"?2\"}}, {'authorizationType' : 2, resource:'*'}]}")
    List<Authorization> findByOwnerAndOwnerTypeAndAuthorizationTypeIsGreaterThanEqualAndResourceStartingWith(String owner, Authorization.OType ownerType, Integer authorizationType, String resource);
    void deleteByOwnerIsAndResourceIsAndAuthorizationTypeIs(String owner, String resource, Integer authorizationType);
    void deleteAllByResourceStartingWith(String resourcePrefix);
    void deleteAllByResourceIs(String resource);
    void deleteAllByOwnerIs(String owner);
}
