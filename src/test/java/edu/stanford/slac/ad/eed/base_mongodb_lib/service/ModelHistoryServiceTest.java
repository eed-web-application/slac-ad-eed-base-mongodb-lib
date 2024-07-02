package edu.stanford.slac.ad.eed.base_mongodb_lib.service;

import edu.stanford.slac.ad.eed.base_mongodb_lib.model.*;
import edu.stanford.slac.ad.eed.base_mongodb_lib.repository.AuthorizationRepository;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ModelChangeDTO;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.model.ModelChangesHistory;
import edu.stanford.slac.ad.eed.baselib.service.ModelHistoryService;
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

import java.util.concurrent.*;

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
    private TestChangeModelPrimitiveRepository testChangeModel1Repository;
    @Autowired
    private TestChangeModelArrayRepository testChangeModelArrayRepository;
    @Autowired
    private TestChangeModelListRepository testChangeModelListRepository;
    @Autowired
    private TestChangeModelMixedRepository testChangeModelMixedRepository;
    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), ModelChangesHistory.class);
        mongoTemplate.remove(new Query(), TestChangeModelPrimitive.class);
    }

    @Test
    public void testChangesOnNewModel() {
        // save test model
        var savedTestModel = assertDoesNotThrow(() -> testChangeModel1Repository.save(
                        TestChangeModelPrimitive
                                .builder()
                                .boolField1(true)
                                .stringField1("string1")
                                .build()
                )
        );
        var listOfChanges = assertDoesNotThrow(() -> modelHistoryService.findChangesByModelId(savedTestModel.getId()));
        assertThat(listOfChanges).isEmpty();
    }

    @Test
    public void testChangesOnUpdatedModel() {
        // save test model
        var savedTestModel = assertDoesNotThrow(() -> testChangeModel1Repository.save(
                        TestChangeModelPrimitive
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
        var listOfChanges = assertDoesNotThrow(() -> modelHistoryService.findChangesByModelId(savedTestModel.getId()));
        assertThat(listOfChanges).isNotEmpty().hasSize(2);
        assertThat(listOfChanges.get(0).changes()).hasSize(1);
        assertThat(listOfChanges.get(0).changes())
                .extracting(ModelChangeDTO::newValue)
                .contains("true");
        assertThat(listOfChanges.get(1).changes()).hasSize(2);
        assertThat(listOfChanges.get(1).changes())
                .extracting(ModelChangeDTO::newValue)
                .contains("false", "string2");

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
                                        TestChangeModelPrimitive
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
                        assertThat(listOfChanges).isNotEmpty().hasSize(1);
                        assertThat(listOfChanges.get(0).changes()).hasSize(1);
                        assertThat(listOfChanges.get(0).changes())
                                .extracting(ModelChangeDTO::newValue)
                                .contains("false", "string-2-%d".formatted(finalIdx));
                        ;
                    }
            );
        }
        assertDoesNotThrow(() -> executor.shutdown());
    }

    @Test
    public void testAllField() {
        var testLocalDate = java.time.LocalDate.now();
        var testLocalDateTime = java.time.LocalDateTime.now();
        var savedTestModel = assertDoesNotThrow(() -> testChangeModel1Repository.save(
                        TestChangeModelPrimitive
                                .builder()
                                .build()
                )
        );
        var updatedTestModel = assertDoesNotThrow(() -> testChangeModel1Repository.save(
                        savedTestModel.toBuilder()
                                .boolField1(true)
                                .stringField1("string1")
                                .intField1(1)
                                .doubleField1(1.0)
                                .longField1(1L)
                                .floatField1(1.0f)
                                .dateField1(testLocalDate)
                                .dateTimeField1(testLocalDateTime)
                                .build()
                )
        );
        // verify all fields
        var listOfChanges = assertDoesNotThrow(() -> modelHistoryService.findChangesByModelId(savedTestModel.getId()));
        assertThat(listOfChanges).isNotEmpty().hasSize(1);
        assertThat(listOfChanges.get(0).changes()).hasSize(8);
        assertThat(listOfChanges.get(0).changes())
                .extracting(ModelChangeDTO::fieldName)
                .contains("boolField1", "stringField1", "intField1", "doubleField1", "longField1", "floatField1", "dateField1", "dateTimeField1");
        assertThat(listOfChanges.get(0).changes())
                .extracting(ModelChangeDTO::newValue)
                .contains("true", "string1", "1", "1.0", "1", "1.0", testLocalDate.toString(), testLocalDateTime.toString());
    }

    @Test
    public void testChangesArrayField() {
        var savedTestModel = assertDoesNotThrow(() -> testChangeModelArrayRepository.save(
                        TestChangeModelArray
                                .builder()
                                .stringField1(new String[]{"string1", "string2"})
                                .boolField1(new Boolean[]{true, false})
                                .build()
                )
        );
        var updatedTestModel = assertDoesNotThrow(() -> testChangeModelArrayRepository.save(
                        savedTestModel.toBuilder()
                                .stringField1(new String[]{"string2", "string3"})
                                .boolField1(new Boolean[]{false, true})
                                .build()
                )
        );
        // verify all fields
        var listOfChanges = assertDoesNotThrow(() -> modelHistoryService.findChangesByModelId(savedTestModel.getId()));
        assertThat(listOfChanges).isNotEmpty().hasSize(1);
        assertThat(listOfChanges.get(0).changes()).hasSize(4);
        assertThat(listOfChanges.get(0).changes())
                .extracting(ModelChangeDTO::fieldName)
                .contains("stringField1[0]", "stringField1[1]", "boolField1[0]", "boolField1[1]");
        assertThat(listOfChanges.get(0).changes())
                .extracting(ModelChangeDTO::newValue)
                .contains("string2", "string3", "false", "true");
        assertThat(listOfChanges.get(0).changes())
                .extracting(ModelChangeDTO::oldValue)
                .contains("string1", "string2", "true", "false");
    }

    @Test
    public void testChangesListField() {
        var savedTestModel = assertDoesNotThrow(() -> testChangeModelListRepository.save(
                        TestChangeModelList
                                .builder()
                                .stringField1(java.util.List.of("string1", "string2"))
                                .boolField1(java.util.List.of(true, false))
                                .build()
                )
        );
        var updatedTestModel = assertDoesNotThrow(() -> testChangeModelListRepository.save(
                        savedTestModel.toBuilder()
                                .stringField1(java.util.List.of("string2", "string3"))
                                .boolField1(java.util.List.of(false, true))
                                .build()
                )
        );
        // verify all fields
        var listOfChanges = assertDoesNotThrow(() -> modelHistoryService.findChangesByModelId(savedTestModel.getId()));
        assertThat(listOfChanges).isNotEmpty().hasSize(1);
        assertThat(listOfChanges.get(0).changes()).hasSize(4);
        assertThat(listOfChanges.get(0).changes())
                .extracting(ModelChangeDTO::fieldName)
                .contains("stringField1[0]", "stringField1[1]", "boolField1[0]", "boolField1[1]");
        assertThat(listOfChanges.get(0).changes())
                .extracting(ModelChangeDTO::newValue)
                .contains("string2", "string3", "false", "true");
        assertThat(listOfChanges.get(0).changes())
                .extracting(ModelChangeDTO::oldValue)
                .contains("string1", "string2", "true", "false");
    }

    @Test
    public void testChangesMixedClass() {
        var savedTestModel = assertDoesNotThrow(() -> testChangeModelMixedRepository.save(
                        TestChangeModelMixed
                                .builder()
                                .build()
                )
        );
        var updatedTestModel = assertDoesNotThrow(() -> testChangeModelMixedRepository.save(
                        savedTestModel.toBuilder()
                                .stringField1("string1")
                                .boolField1(java.util.List.of(true, true))
                                .classPrimitiveField(
                                        TestChangeModelPrimitive
                                                .builder()
                                                .boolField1(true)
                                                .stringField1("string1")
                                                .build()
                                )
                                .build()
                )
        );
        // verify all fields
        var listOfChanges = assertDoesNotThrow(() -> modelHistoryService.findChangesByModelId(savedTestModel.getId()));
        assertThat(listOfChanges).isNotEmpty().hasSize(1);
        assertThat(listOfChanges.get(0).changes()).hasSize(5);
        assertThat(listOfChanges.get(0).changes())
                .extracting(ModelChangeDTO::fieldName)
                .contains("stringField1", "boolField1[0]", "boolField1[1]", "classPrimitiveField.boolField1", "classPrimitiveField.stringField1");
        assertThat(listOfChanges.get(0).changes())
                .extracting(ModelChangeDTO::newValue)
                .contains("string1", "true", "true", "true", "string1");

        //second update
        var secondUpdatedTestModel = assertDoesNotThrow(() -> testChangeModelMixedRepository.save(
                        savedTestModel.toBuilder()
                                .stringField1("string2")
                                .boolField1(java.util.List.of(false, false))
                                .classPrimitiveField(
                                        TestChangeModelPrimitive
                                                .builder()
                                                .stringField1("string2")
                                                .build()
                                )
                                .build()
                )
        );
        // verify all fields
        listOfChanges = assertDoesNotThrow(() -> modelHistoryService.findChangesByModelId(savedTestModel.getId()));
        assertThat(listOfChanges).isNotEmpty().hasSize(2);
        assertThat(listOfChanges.get(0).changes()).hasSize(5);
        assertThat(listOfChanges.get(0).changes())
                .extracting(ModelChangeDTO::fieldName)
                .contains("stringField1", "boolField1[0]", "boolField1[1]", "classPrimitiveField.boolField1", "classPrimitiveField.stringField1");
        assertThat(listOfChanges.get(0).changes())
                .extracting(ModelChangeDTO::newValue)
                .contains("string2", "false", "false", null, "string2");
        assertThat(listOfChanges.get(1).changes()).hasSize(5);
        assertThat(listOfChanges.get(1).changes())
                .extracting(ModelChangeDTO::fieldName)
                .contains("stringField1", "boolField1[0]", "boolField1[1]", "classPrimitiveField.boolField1", "classPrimitiveField.stringField1");
        assertThat(listOfChanges.get(1).changes())
                .extracting(ModelChangeDTO::newValue)
                .contains("string1", "true", "true", "true", "string1");
    }

    @Test
    public void testChangesMixedClassComplexList() {
        var savedTestModel = assertDoesNotThrow(() -> testChangeModelMixedRepository.save(
                        TestChangeModelMixed
                                .builder()
                                .build()
                )
        );
        var updatedTestModel = assertDoesNotThrow(() -> testChangeModelMixedRepository.save(
                        savedTestModel.toBuilder()
                                .stringField1("string1")
                                .boolField1(java.util.List.of(true, true))
                                .classListArrayField(java.util.List.of(
                                        TestChangeModelArray
                                                .builder()
                                                .stringField1(new String[]{"string1", "string2"})
                                                .boolField1(new Boolean[]{true, false})
                                                .build(),
                                        TestChangeModelArray
                                                .builder()
                                                .stringField1(new String[]{"string3", "string4"})
                                                .boolField1(new Boolean[]{false, true})
                                                .build()
                                ))
                                .classListListArrayField(java.util.List.of(
                                        java.util.List.of(
                                                TestChangeModelArray
                                                        .builder()
                                                        .stringField1(new String[]{"string1", "string2"})
                                                        .boolField1(new Boolean[]{true, false})
                                                        .build(),
                                                TestChangeModelArray
                                                        .builder()
                                                        .stringField1(new String[]{"string3", "string4"})
                                                        .boolField1(new Boolean[]{false, true})
                                                        .build()
                                        ),
                                        java.util.List.of(
                                                TestChangeModelArray
                                                        .builder()
                                                        .stringField1(new String[]{"string5", "string6"})
                                                        .boolField1(new Boolean[]{true, false})
                                                        .build(),
                                                TestChangeModelArray
                                                        .builder()
                                                        .stringField1(new String[]{"string7", "string8"})
                                                        .boolField1(new Boolean[]{false, true})
                                                        .build()
                                        )
                                ))
                                .classPrimitiveListField(java.util.List.of(
                                        TestChangeModelPrimitive
                                                .builder()
                                                .stringField1("string1")
                                                .boolField1(true)
                                                .build(),
                                        TestChangeModelPrimitive
                                                .builder()
                                                .stringField1("string2")
                                                .boolField1(false)
                                                .build()
                                ))
                                .build()
                )
        );
        // verify all fields
        var listOfChanges = assertDoesNotThrow(() -> modelHistoryService.findChangesByModelId(savedTestModel.getId()));
        assertThat(listOfChanges).isNotEmpty().hasSize(1);
        assertThat(listOfChanges.get(0).changes()).hasSize(15);
        assertThat(listOfChanges.get(0).changes())
                .extracting(ModelChangeDTO::fieldName)
                .contains(
                        "stringField1",
                        "boolField1[0]",
                        "boolField1[1]",
                        "classListArrayField[0].stringField1[0]",
                        "classListArrayField[0].stringField1[1]",
                        "classListArrayField[0].boolField1[0]",
                        "classListArrayField[0].boolField1[1]",
                        "classListArrayField[1].stringField1[0]",
                        "classListArrayField[1].stringField1[1]",
                        "classListArrayField[1].boolField1[0]",
                        "classListArrayField[1].boolField1[1]",
                        "classListListArrayField[0][0].stringField1[0]",
                        "classListListArrayField[0][0].stringField1[1]",
                        "classListListArrayField[0][0].boolField1[0]",
                        "classListListArrayField[0][0].boolField1[1]"
                );

    }
}
