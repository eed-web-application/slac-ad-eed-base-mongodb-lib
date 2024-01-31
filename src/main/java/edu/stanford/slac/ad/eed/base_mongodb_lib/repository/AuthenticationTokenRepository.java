package edu.stanford.slac.ad.eed.base_mongodb_lib.repository;


import edu.stanford.slac.ad.eed.baselib.model.AuthenticationToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AuthenticationTokenRepository extends MongoRepository<AuthenticationToken, String> {
    /**
     * Find all authentication tokens that are managed by the application.
     *
     * @return List of authentication tokens.
     */
    List<AuthenticationToken> findAllByApplicationManagedIsTrue();

    /**
     * Delete all authentication tokens that are managed by the application.
     */
    void deleteAllByApplicationManagedIsTrue();
    /**
     * Find all authentication tokens that are not managed by the application.
     *
     * @return List of authentication tokens.
     */
    Optional<AuthenticationToken> findByName(String name);
    /**
     * Find all authentication tokens that are not managed by the application.
     *
     * @return List of authentication tokens.
     */
    boolean existsByName(String name);
    /**
     * Find all authentication tokens that are not managed by the application.
     *
     * @return List of authentication tokens.
     */
    Optional<AuthenticationToken> findByEmailIs(String email);
    /**
     * Find all authentication tokens that are not managed by the application.
     *
     * @return List of authentication tokens.
     */
    Optional<AuthenticationToken> findByNameIsAndEmailEndsWith(String name, String emailPostfix);
    /**
     * Find all authentication tokens that are not managed by the application.
     *
     * @return List of authentication tokens.
     */
    List<AuthenticationToken> findAllByEmailEndsWith(String emailPostfix);
    /**
     * Find all authentication tokens that are not managed by the application.
     *
     * @return List of authentication tokens.
     */
    boolean existsByEmail(String email);
    /**
     * Find all authentication tokens that are not managed by the application.
     *
     * @return List of authentication tokens.
     */
    void deleteAllByEmailEndsWith(String emailPostfix);
}
