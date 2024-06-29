package edu.stanford.slac.ad.eed.base_mongodb_lib.service;

import edu.stanford.slac.ad.eed.base_mongodb_lib.repository.ModelHistoryRepository;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ModelChangesHistoryDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.mapper.ModelChangeMapper;
import edu.stanford.slac.ad.eed.baselib.service.ModelHistoryService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@Service
@AllArgsConstructor
public class ModelHistoryServiceImpl extends ModelHistoryService {
    private final ModelChangeMapper modelChangeMapper;
    private final ModelHistoryRepository modelHistoryRepository;

    @Override
    public List<ModelChangesHistoryDTO> findChangesByModelId(String modelId) {
        return wrapCatch
                (
                        () -> modelHistoryRepository.findAllByModelId(modelId),
                        -1
                )
                .stream()
                .map(modelChangeMapper::toDTO)
                .toList();
    }
}
