package edu.stanford.slac.ad.eed.base_mongodb_lib.service;

import edu.stanford.slac.ad.eed.base_mongodb_lib.model.*;
import edu.stanford.slac.ad.eed.base_mongodb_lib.repository.AuthorizationRepository;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ModelChangeDTO;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.model.ModelChangesHistory;
import edu.stanford.slac.ad.eed.baselib.service.ModelHistoryService;
import org.assertj.core.api.Condition;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.ldap.repository.config.EnableLdapRepositories;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
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
        mongoTemplate.getDb().getCollection("jv_snapshots").deleteMany(new Document());
        mongoTemplate.getDb().getCollection("jv_head_id").deleteMany(new Document());
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
        var listOfChanges = assertDoesNotThrow(() -> modelHistoryService.findChangesByModelId(TestChangeModelPrimitive.class, savedTestModel.getId()));
        assertThat(listOfChanges).hasSize(1);
        assertThat(listOfChanges.get(0).changes()).isNotEmpty();
        assertThat(listOfChanges.get(0).changes())
                .extracting(ModelChangeDTO::newValue)
                .contains("true", "string1");

        var modelStateChanges = assertDoesNotThrow(() -> modelHistoryService.findModelChangesByModelId(TestChangeModelPrimitive.class, savedTestModel.getId()));
        assertThat(modelStateChanges).isNotEmpty().hasSize(1);
        assertThat(modelStateChanges.get(0).getBoolField1()).isTrue();
        assertThat(modelStateChanges.get(0).getStringField1()).isEqualTo("string1");
    }

    @Test
    public void testChangesOnUpdatedModel() {
        // save test model
        var savedTestModel = assertDoesNotThrow(() -> testChangeModel1Repository.save(
                        TestChangeModelPrimitive
                                .builder()
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
        var listOfChanges = assertDoesNotThrow(() -> modelHistoryService.findChangesByModelId(TestChangeModelPrimitive.class, savedTestModel.getId()));
        assertThat(listOfChanges).isNotEmpty().hasSize(3);
        assertThat(listOfChanges.get(0).changes()).hasSize(1);
        assertThat(listOfChanges.get(0).changes())
                .extracting(ModelChangeDTO::newValue)
                .contains("true");
        assertThat(listOfChanges.get(1).changes()).hasSize(2);
        assertThat(listOfChanges.get(1).changes())
                .extracting(ModelChangeDTO::newValue)
                .contains("false", "string2");

        var modelStateChanges = assertDoesNotThrow(() -> modelHistoryService.findModelChangesByModelId(TestChangeModelPrimitive.class, savedTestModel.getId()));
        assertThat(modelStateChanges).isNotEmpty().hasSize(3);
        assertThat(modelStateChanges.get(0).getBoolField1()).isTrue();
        assertThat(modelStateChanges.get(0).getStringField1()).isEqualTo("string2");
        assertThat(modelStateChanges.get(1).getBoolField1()).isFalse();
        assertThat(modelStateChanges.get(1).getStringField1()).isEqualTo("string2");
        assertThat(modelStateChanges.get(2).getBoolField1()).isNull();
        assertThat(modelStateChanges.get(2).getStringField1()).isNull();

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

                        var listOfChanges = assertDoesNotThrow(() -> modelHistoryService.findChangesByModelId(TestChangeModelPrimitive.class, savedTestModel.getId()));
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
        var listOfChanges = assertDoesNotThrow(() -> modelHistoryService.findChangesByModelId(TestChangeModelPrimitive.class, savedTestModel.getId()));
        assertThat(listOfChanges).isNotEmpty().hasSize(2);
        assertThat(listOfChanges.get(0).changes()).hasSize(8);
        assertThat(listOfChanges.get(0).changes())
                .extracting(ModelChangeDTO::fieldName)
                .contains("boolField1", "stringField1", "intField1", "doubleField1", "longField1", "floatField1", "dateField1", "dateTimeField1");
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
        var listOfChanges = assertDoesNotThrow(() -> modelHistoryService.findChangesByModelId(TestChangeModelArray.class, savedTestModel.getId()));
        assertThat(listOfChanges).isNotEmpty().hasSize(2);
        assertThat(listOfChanges.get(0).changes()).hasSize(2);
        assertThat(listOfChanges.get(0).changes())
                .extracting(ModelChangeDTO::fieldName)
                .contains("stringField1", "boolField1");
        assertThat(listOfChanges.get(0).changes())
                .areAtLeastOne(new ArrayCondition(new Object[]{false, true}, true))
                .areAtLeastOne(new ArrayCondition(new Object[]{"string2", "string3"}, true));
        assertThat(listOfChanges.get(1).changes())
                .areAtLeastOne(new ArrayCondition(new Object[]{true, false}, true))
                .areAtLeastOne(new ArrayCondition(new Object[]{"string1", "string2"}, true));
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
        var listOfChanges = assertDoesNotThrow(() -> modelHistoryService.findChangesByModelId(TestChangeModelList.class, savedTestModel.getId()));
        assertThat(listOfChanges).isNotEmpty().hasSize(2);
        assertThat(listOfChanges.get(0).changes()).hasSize(2);
        assertThat(listOfChanges.get(0).changes())
                .extracting(ModelChangeDTO::fieldName)
                .contains("stringField1", "boolField1");
        assertThat(listOfChanges.get(0).changes())
                .areAtLeastOne(new ListCondition(List.of(false, true), true))
                .areAtLeastOne(new ListCondition(List.of("string2", "string3"), true));
        assertThat(listOfChanges.get(1).changes())
                .areAtLeastOne(new ListCondition(List.of(true, false), true))
                .areAtLeastOne(new ListCondition(List.of("string1", "string2"), true));
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
                                .listBoolField1(java.util.List.of(true, true))
                                .listStringField(java.util.List.of("string1", "string2"))
                                .build()
                )
        );
        // verify all fields
        var listOfChanges = assertDoesNotThrow(() -> modelHistoryService.findChangesByModelId(TestChangeModelMixed.class, savedTestModel.getId()));
        assertThat(listOfChanges).isNotEmpty().hasSize(2);
        assertThat(listOfChanges.get(0).changes()).hasSize(3);
        assertThat(listOfChanges.get(0).changes())
                .extracting(ModelChangeDTO::fieldName)
                .contains("stringField1", "listBoolField1", "listStringField");
        assertThat(listOfChanges.get(0).changes())
                .extracting(ModelChangeDTO::newValue)
                .contains("string1");
        assertThat(listOfChanges.get(0).changes())
                .areAtLeastOne(new ListCondition(List.of(true, true), true))
                .areAtLeastOne(new ListCondition(List.of("string1", "string2"), true));
        // another update
        var anotherUpdatedTestModel = assertDoesNotThrow(() -> testChangeModelMixedRepository.save(
                        savedTestModel.toBuilder()
                                .stringField1("string2")
                                .listBoolField1(java.util.List.of(true))
                                .listStringField(java.util.List.of("string5", "new-string1", "new-string3"))
                                .build()
                )
        );
        // verify all fields
        listOfChanges = assertDoesNotThrow(() -> modelHistoryService.findChangesByModelId(TestChangeModelMixed.class, savedTestModel.getId()));
        assertThat(listOfChanges).isNotEmpty().hasSize(3);
        assertThat(listOfChanges.get(0).changes()).hasSize(3);
        assertThat(listOfChanges.get(0).changes())
                .extracting(ModelChangeDTO::fieldName)
                .contains("stringField1", "listBoolField1", "listStringField");
        assertThat(listOfChanges.get(0).changes())
                .extracting(ModelChangeDTO::newValue)
                .contains("string2");
        assertThat(listOfChanges.get(0).changes())
                .areAtLeastOne(new ListCondition(List.of(true), true))
                .areAtLeastOne(new ListCondition(List.of("string5", "new-string1", "new-string3"), true));
        assertThat(listOfChanges.get(1).changes()).hasSize(3);
        assertThat(listOfChanges.get(1).changes())
                .extracting(ModelChangeDTO::fieldName)
                .contains("stringField1", "listBoolField1", "listStringField");
        assertThat(listOfChanges.get(1).changes())
                .extracting(ModelChangeDTO::newValue)
                .contains("string1");
        assertThat(listOfChanges.get(1).changes())
                .areAtLeastOne(new ListCondition(List.of(true, true), true))
                .areAtLeastOne(new ListCondition(List.of("string1", "string2"), true));

    }

    public class ListCondition extends Condition<ModelChangeDTO> {
        private final List<Object> expectedValues;
        private final boolean onNewValue;

        public ListCondition(List<Object> expectedValues, boolean onNewValue) {
            this.expectedValues = expectedValues;
            this.onNewValue = onNewValue;
        }

        @Override
        public boolean matches(ModelChangeDTO modelChangeDTO) {
            if (onNewValue) {
                if (modelChangeDTO.newValue() instanceof List) {
                    return expectedValues.containsAll((Collection<?>) modelChangeDTO.newValue());
                } else {
                    return false;
                }
            } else {
                if (modelChangeDTO.oldValue() instanceof List) {
                    return expectedValues.containsAll((Collection<?>) modelChangeDTO.oldValue());
                } else {
                    return false;
                }
            }
        }
    }

    public class ArrayCondition extends Condition<ModelChangeDTO> {
        private final Object[] expectedValues;
        private final boolean onNewValue;

        public ArrayCondition(Object[] expectedValues, boolean onNewValue) {
            this.expectedValues = expectedValues;
            this.onNewValue = onNewValue;
        }

        @Override
        public boolean matches(ModelChangeDTO modelChangeDTO) {
            Object[] arrTest = null;
            if (onNewValue) {
                if (isArray(modelChangeDTO.newValue())) {
                    arrTest = convertToObjectArray(modelChangeDTO.newValue());
                }
            } else {
                if (isArray(modelChangeDTO.oldValue())) {
                    arrTest = convertToObjectArray(modelChangeDTO.oldValue());
                }
            }
            if (arrTest == null) {
                return false;
            } else {
                return Arrays.equals(arrTest, expectedValues);
            }
        }

        public boolean isArray(Object obj) {
            return obj != null && obj.getClass().isArray();
        }

        public Object[] convertToObjectArray(Object array) {
            if (!isArray(array)) {
                throw new IllegalArgumentException("The provided object is not an array");
            }

            int length = java.lang.reflect.Array.getLength(array);
            Object[] outputArray = new Object[length];
            for (int i = 0; i < length; i++) {
                outputArray[i] = java.lang.reflect.Array.get(array, i);
            }
            return outputArray;
        }
    }
}
