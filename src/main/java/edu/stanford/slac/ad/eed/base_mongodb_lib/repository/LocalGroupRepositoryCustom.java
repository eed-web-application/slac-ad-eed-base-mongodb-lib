package edu.stanford.slac.ad.eed.base_mongodb_lib.repository;

import edu.stanford.slac.ad.eed.baselib.model.LocalGroup;
import edu.stanford.slac.ad.eed.baselib.model.LocalGroupQueryParameter;

import java.util.List;

public interface LocalGroupRepositoryCustom {
    List<LocalGroup> findAll(LocalGroupQueryParameter queryLocalGroup);
}
