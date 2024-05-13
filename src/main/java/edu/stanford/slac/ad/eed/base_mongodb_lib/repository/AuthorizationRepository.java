package edu.stanford.slac.ad.eed.base_mongodb_lib.repository;

import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.model.AuthorizationOwnerType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AuthorizationRepository extends MongoRepository<Authorization, String>, AuthorizationRepositoryCustom {
    /**
     * Find all authorizations for a given owner
     * @param owner owner
     * @return list of authorizations
     */
    Optional<Authorization> findByOwnerIsAndResourceIsAndAuthorizationTypeIs(String owner, String resource, Integer authorizationType);
    /**
     * Find all authorizations for a given owner
     * @param owner owner
     * @return list of authorizations
     */
    @Query("{ 'owner' : '?0', $or: [{'resource' : '?1', 'authorizationType' : { '$gte' : ?2}},{'authorizationType' : 2, resource:'*'}]}")
    Optional<Authorization> findByOwnerIsAndResourceIsAndAuthorizationTypeIsGreaterThanEqual(String owner, String resource, Integer authorizationType);
    /**
     * Find all authorizations for a given resource
     * @param resource owner
     * @return list of authorizations
     */
    List<Authorization> findByResourceIs(String resource);
    /**
     * Find all authorizations for a given resource and authorization type
     * @param resource owner
     * @param authorizationType authorization type
     * @return list of authorizations
     */
    List<Authorization> findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(String resource, Integer authorizationType);
    /**
     * Find all authorizations for a given resource and authorization type
     * @param resource owner
     * @param authorizationType authorization type
     * @return list of authorizations
     */
    List<Authorization> findByResourceIsAndAuthorizationTypeIsGreaterThanEqualAndOwnerTypeIs(String resource, Integer authorizationType, AuthorizationOwnerType ownerType);
    /**
     * Find all authorizations for a given resource and authorization type
     * @param resource owner
     * @param authorizationType authorization type
     * @return list of authorizations
     */
    List<Authorization> findByOwnerAndOwnerTypeAndAuthorizationTypeIsGreaterThanEqualAndResourceStartingWith(String owner, AuthorizationOwnerType ownerType, Integer authorizationType, String resource);

    /**
     * Find all authorizations for a given owner
     * @param owner owner
     * @return list of authorizations
     */
    List<Authorization> findByOwnerAndOwnerTypeIs(String owner, AuthorizationOwnerType ownerType);

    /**
     * Find all authorizations for a given owner and resource
     * @param owner owner
     * @param resource resource
     * @return list of authorizations
     */
    List<Authorization> findByOwnerAndOwnerTypeIsAndResourceStartingWith(String owner, AuthorizationOwnerType ownerType, String resourcePrefix);

    /**
     * Delete all authorizations for a given owner, resource and authorization type
     * @param owner owner
     * @param resource authorization type
     * @param authorizationType owner type
     */
    void deleteByOwnerIsAndResourceIsAndAuthorizationTypeIs(String owner, String resource, Integer authorizationType);
    /**
     * Delete all authorizations for a given resource prefix
     * @param resourcePrefix resource prefix
     */
    void deleteAllByResourceStartingWith(String resourcePrefix);
    /**
     * Delete all authorizations for a given resource
     * @param resource resource
     */
    void deleteAllByResourceIs(String resource);
    /**
     * Delete all authorizations for a given owner
     * @param owner owner
     */
    void deleteAllByOwnerIs(String owner);
    /**
     * Delete all authorizations for a given owner and owner type
     * @param owner owner
     * @param ownerType owner type
     */
    void deleteAllByResourceStartingWithAndOwnerIsAndOwnerTypeIs(String resourcePrefix, String owner, AuthorizationOwnerType ownerType);

    /**
     * Delete all authorizations for a given resource prefix and owner type
     * @param ownerType owner type
     */
    void deleteAllByResourceStartingWithAndOwnerTypeIs(String resourcePrefix, AuthorizationOwnerType ownerType);
}
