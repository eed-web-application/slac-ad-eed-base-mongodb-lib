package edu.stanford.slac.ad.eed.base_mongodb_lib.controller;

import edu.stanford.slac.ad.eed.base_mongodb_lib.utility.InitAuthorizationIndex;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.PersonDTO;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.AuthorizationGroupManagementDTO;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.NewLocalGroupDTO;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.UpdateLocalGroupDTO;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.UserGroupManagementAuthorizationLevel;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.model.AuthenticationToken;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.model.LocalGroup;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class V2AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AppProperties appProperties;
    @Autowired
    private AuthService authService;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    TestControllerHelperService testControllerHelperService;

    @BeforeEach
    public void preTest() {
        //reset authorizations
        mongoTemplate.remove(new Query(), AuthenticationToken.class);
        mongoTemplate.remove(new Query(), Authorization.class);
        mongoTemplate.remove(new Query(), LocalGroup.class);

        InitAuthorizationIndex initAuthorizationIndex = new InitAuthorizationIndex(mongoTemplate);
        initAuthorizationIndex.updateIndex();

        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("user1@slac.stanford.edu");
        authService.updateRootUser();
    }

    @Test
    public void authorizeUserToManageGroup() {
        var resultUpdateOps = assertDoesNotThrow(
                () -> testControllerHelperService.v2AuthorizationControllerManageGroupManagementAuthorization(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        AuthorizationGroupManagementDTO
                                .builder()
                                .addUsers(List.of("user2@slac.stanford.edu"))
                                .build()
                )
        );
        assertThat(resultUpdateOps).isNotNull();
        assertThat(resultUpdateOps.getPayload()).isTrue();

        var authorizationLists = assertDoesNotThrow(
                () -> testControllerHelperService.v2AuthorizationControllerGetGroupManagementAuthorization(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        List.of("user1@slac.stanford.edu", "user2@slac.stanford.edu")
                )
        );
        assertThat(authorizationLists).isNotNull();
        assertThat(authorizationLists.getPayload()).hasSize(2);
        assertThat(authorizationLists.getPayload())
                .extracting(UserGroupManagementAuthorizationLevel::user)
                .extracting(PersonDTO::mail)
                .contains("user1@slac.stanford.edu","user2@slac.stanford.edu");
        assertThat(authorizationLists.getPayload())
                .extracting(UserGroupManagementAuthorizationLevel::canManageGroup)
                .contains(true, true);
    }

    @Test
    public void createGroupWithRootUser() {
        var newGroupResult = assertDoesNotThrow(
                () -> testControllerHelperService.v2AuthorizationControllerCreateNewLocalGroup(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        NewLocalGroupDTO
                                .builder()
                                .name("group1")
                                .description("group1 description")
                                .members(List.of("user2@slac.stanford.edu"))
                                .build()
                )
        );
        assertThat(newGroupResult)
                .isNotNull();
        assertThat(newGroupResult.getPayload()).isNotEmpty();
    }

    @Test
    public void createGroupWithAuthorizedUser() {
        // authorize user 2 to manage group
        var resultUpdateOps = assertDoesNotThrow(
                () -> testControllerHelperService.v2AuthorizationControllerManageGroupManagementAuthorization(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        AuthorizationGroupManagementDTO
                                .builder()
                                .addUsers(List.of("user2@slac.stanford.edu"))
                                .build()
                )
        );
        assertThat(resultUpdateOps).isNotNull();
        assertThat(resultUpdateOps.getPayload()).isTrue();

        // create new group with user 2
        var newGroupResult = assertDoesNotThrow(
                () -> testControllerHelperService.v2AuthorizationControllerCreateNewLocalGroup(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user2@slac.stanford.edu"),
                        Optional.empty(),
                        NewLocalGroupDTO
                                .builder()
                                .name("group1")
                                .description("group1 description")
                                .members(List.of("user2@slac.stanford.edu"))
                                .build()
                )
        );
        assertThat(newGroupResult)
                .isNotNull();
        assertThat(newGroupResult.getPayload()).isNotEmpty();
    }

    @Test
    public void updateLocalGroup() {
        var newGroupResult = assertDoesNotThrow(
                () -> testControllerHelperService.v2AuthorizationControllerCreateNewLocalGroup(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        NewLocalGroupDTO
                                .builder()
                                .name("group1")
                                .description("group1 description")
                                .members(List.of("user2@slac.stanford.edu"))
                                .build()
                )
        );
        assertThat(newGroupResult)
                .isNotNull();
        assertThat(newGroupResult.getPayload()).isNotEmpty();

        // update the group
        var updateGroupResult = assertDoesNotThrow(
                () -> testControllerHelperService.v2AuthorizationControllerUpdateLocalGroup(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        newGroupResult.getPayload(),
                        UpdateLocalGroupDTO
                                .builder()
                                .name("group1 updated")
                                .description("group1 description updated")
                                .members(List.of("user3@slac.stanford.edu"))
                                .build()
                )
        );
        assertThat(updateGroupResult)
                .isNotNull();
        assertThat(updateGroupResult.getPayload()).isTrue();

        // fetch local group to check the update
        var groupFoundResult = assertDoesNotThrow(
                () -> testControllerHelperService.v2AuthorizationControllerFindLocalGroupById(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        newGroupResult.getPayload()
                )
        );
        assertThat(groupFoundResult).isNotNull();
        assertThat(groupFoundResult.getPayload()).isNotNull();
        assertThat(groupFoundResult.getPayload().name()).isEqualTo("group1 updated");
        assertThat(groupFoundResult.getPayload().description()).isEqualTo("group1 description updated");
        assertThat(groupFoundResult.getPayload().members()).hasSize(1);
        assertThat(groupFoundResult.getPayload().members())
                .extracting(PersonDTO::mail)
                .contains("user3@slac.stanford.edu");
    }

    @Test
    public void testFindAll() {
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            var newGroupResult = assertDoesNotThrow(
                    () -> testControllerHelperService.v2AuthorizationControllerCreateNewLocalGroup(
                            mockMvc,
                            status().isCreated(),
                            Optional.of("user1@slac.stanford.edu"),
                            Optional.empty(),
                            NewLocalGroupDTO
                                    .builder()
                                    .name("group-%d".formatted(finalI))
                                    .description("group-%d description".formatted(finalI))
                                    .members(List.of("user2@slac.stanford.edu"))
                                    .build()
                    )
            );
            assertThat(newGroupResult)
                    .isNotNull();
            assertThat(newGroupResult.getPayload()).isNotEmpty();
        }

        var groupFoundResult = assertDoesNotThrow(
                () -> testControllerHelperService.v2AuthorizationControllerFindLocalGroup(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(10),
                        Optional.empty()
                )
        );
        assertThat(groupFoundResult).isNotNull();
        assertThat(groupFoundResult.getPayload()).hasSize(10);
        for (int i = 0; i < 10; i++) {
            assertThat(groupFoundResult.getPayload().get(i).name()).isEqualTo("group-%d" .formatted(i));
            assertThat(groupFoundResult.getPayload().get(i).description()).isEqualTo("group-%d description" .formatted(i));
            assertThat(groupFoundResult.getPayload().get(i).members()).hasSize(1);
        }
    }
}
