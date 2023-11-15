package edu.stanford.slac.ad.eed.base_mongodb_lib.repository;

import edu.stanford.slac.ad.eed.baselib.model.AuthenticationToken;
import edu.stanford.slac.ad.eed.baselib.repository.AuthenticationTokenRepository;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongodbAuthenticationTokenRepository extends AuthenticationTokenRepository, MongoRepository<AuthenticationToken, String> {
}
