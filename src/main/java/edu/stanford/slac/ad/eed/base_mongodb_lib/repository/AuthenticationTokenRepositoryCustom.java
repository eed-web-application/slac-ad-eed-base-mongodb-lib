package edu.stanford.slac.ad.eed.base_mongodb_lib.repository;

import edu.stanford.slac.ad.eed.baselib.model.AuthenticationToken;
import edu.stanford.slac.ad.eed.baselib.model.AuthenticationTokenQueryParameter;

import java.util.List;

public interface AuthenticationTokenRepositoryCustom {
    List<AuthenticationToken> findAll(AuthenticationTokenQueryParameter queryParameterDTO);
}
