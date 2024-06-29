package edu.stanford.slac.ad.eed.base_mongodb_lib.service;

import edu.stanford.slac.ad.eed.base_mongodb_lib.model.TestChangeModel1;
import edu.stanford.slac.ad.eed.base_mongodb_lib.model.TestChangeModel1Repository;
import edu.stanford.slac.ad.eed.base_mongodb_lib.repository.AuthorizationRepository;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationOwnerTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ModelChangeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.NewLocalGroupDTO;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.model.LocalGroup;
import edu.stanford.slac.ad.eed.baselib.model.ModelChangesHistory;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.ad.eed.baselib.service.ModelHistoryService;
import org.assertj.core.api.AssertionsForClassTypes;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.*;
import static edu.stanford.slac.ad.eed.baselib.model.AuthorizationOwnerType.Group;
import static edu.stanford.slac.ad.eed.baselib.model.AuthorizationOwnerType.User;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@AutoConfigureMockMvc
@SpringBootTest(properties = {})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ModelHistoryServiceTest {
    @Autowired
    private AppProperties appProperties;
    @Autowired
    private AuthorizationRepository authorizationRepository;
    @Autowired
    private ModelHistoryService modelHistoryService;
    @Autowired
    private TestChangeModel1Repository testChangeModel1Repository;
    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), ModelChangesHistory.class);
        mongoTemplate.remove(new Query(), TestChangeModel1.class);
    }

    @Test
    public void testChangesOnNewModel() {
        // save test model
        var savedTestModel = assertDoesNotThrow(()->testChangeModel1Repository.save(
                TestChangeModel1
                        .builder()
                        .boolField1(true)
                        .stringField1("string1")
                        .build()
        )
        );
        var listOfChanges = assertDoesNotThrow(()->modelHistoryService.findChangesByModelId(savedTestModel.getId()));
        assertThat(listOfChanges).isNotEmpty().hasSize(1);
        assertThat(listOfChanges.get(0).changes()).hasSize(2);
        assertThat(listOfChanges.get(0).changes())
                .extracting(ModelChangeDTO::fieldName)
                .contains("boolField1", "stringField1");
        assertThat(listOfChanges.get(0).changes())
                .extracting(ModelChangeDTO::newValue)
                .contains("true", "string1");

    }

    @Test
    public void testChangesOnUpdatedModel() {
        // save test model
        var savedTestModel = assertDoesNotThrow(() -> testChangeModel1Repository.save(
                        TestChangeModel1
                                .builder()
                                .boolField1(true)
                                .stringField1("string1")
                                .build()
                )
        );

        var updatedTestModel = assertDoesNotThrow(() -> testChangeModel1Repository.save(
                        savedTestModel.toBuilder()
                                .boolField1(false)
                                .stringField1("string2")
                                .build()
                )
        );
        var anotherUpdatedTestModel = assertDoesNotThrow(() -> testChangeModel1Repository.save(
                        updatedTestModel.toBuilder()
                                .boolField1(true)
                                .build()
                )
        );
        var listOfChanges = assertDoesNotThrow(()->modelHistoryService.findChangesByModelId(savedTestModel.getId()));
        assertThat(listOfChanges).isNotEmpty().hasSize(3);
        assertThat(listOfChanges.get(0).changes()).hasSize(1);
        assertThat(listOfChanges.get(0).changes())
                .extracting(ModelChangeDTO::newValue)
                .contains("true");
        assertThat(listOfChanges.get(1).changes()).hasSize(2);
        assertThat(listOfChanges.get(1).changes())
                .extracting(ModelChangeDTO::newValue)
                .contains("false", "string2");
        assertThat(listOfChanges.get(2).changes()).hasSize(2);
        assertThat(listOfChanges.get(2).changes())
                .extracting(ModelChangeDTO::newValue)
                .contains("true", "string1");

    }

    @Test
    public void testChangesOnMultithreadingUpdatedModel() {
        // execute update in multithreading
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        for (int idx = 0; idx < 10; idx++) {
            int finalIdx = idx;
            executor.execute(
                    () -> {
                        var savedTestModel = assertDoesNotThrow(() -> testChangeModel1Repository.save(
                                        TestChangeModel1
                                                .builder()
                                                .boolField1(true)
                                                .stringField1("string-1-%d".formatted(finalIdx))
                                                .build()
                                )
                        );
                        var updatedTestModel = assertDoesNotThrow(() -> testChangeModel1Repository.save(
                                        savedTestModel.toBuilder()
                                                .boolField1(false)
                                                .stringField1("string-2-%d".formatted(finalIdx))
                                                .build()
                                )
                        );

                        var listOfChanges = assertDoesNotThrow(() -> modelHistoryService.findChangesByModelId(savedTestModel.getId()));
                        assertThat(listOfChanges).isNotEmpty().hasSize(2);
                        assertThat(listOfChanges.get(0).changes()).hasSize(1);
                        assertThat(listOfChanges.get(0).changes())
                                .extracting(ModelChangeDTO::newValue)
                                .contains("false", "string-2-%d".formatted(finalIdx));
                        assertThat(listOfChanges.get(1).changes()).hasSize(2);
                        assertThat(listOfChanges.get(1).changes())
                                .extracting(ModelChangeDTO::newValue)
                                .contains("true", "string-1-%d".formatted(finalIdx));
                    }
            );
        }
        assertDoesNotThrow(() -> executor.shutdown());
    }
}
