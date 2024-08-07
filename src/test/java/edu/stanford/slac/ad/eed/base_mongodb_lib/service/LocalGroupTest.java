package edu.stanford.slac.ad.eed.base_mongodb_lib.service;

import edu.stanford.slac.ad.eed.base_mongodb_lib.repository.AuthenticationTokenRepository;
import edu.stanford.slac.ad.eed.base_mongodb_lib.repository.AuthorizationRepository;
import edu.stanford.slac.ad.eed.base_mongodb_lib.utility.InitAuthorizationIndex;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationOwnerTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.NewAuthorizationDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.PersonDTO;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.*;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.ad.eed.baselib.exception.GroupNotFound;
import edu.stanford.slac.ad.eed.baselib.exception.PersonNotFound;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.model.LocalGroup;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AutoConfigureMockMvc
@SpringBootTest(properties = {})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class LocalGroupTest {
    @Autowired
    private AuthService authService;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private AuthorizationRepository authorizationRepository;
    @SpyBean
    @Autowired
    private AuthenticationTokenRepository authenticationTokenRepository;

    @BeforeEach
    public void preTest() {
        Mockito.reset(authenticationTokenRepository);
        mongoTemplate.remove(new Query(), Authorization.class);
        mongoTemplate.remove(new Query(), LocalGroup.class);
        InitAuthorizationIndex initAuthorizationIndex = new InitAuthorizationIndex(mongoTemplate);
        initAuthorizationIndex.updateIndex();
    }

    @Test
    public void testAssignUserToManageGroup() {
        assertDoesNotThrow(
                () -> authService.authorizeUserIdToManageGroup("user1@slac.stanford.edu")
        );

        var iAuthorized = assertDoesNotThrow(
                () -> authService.canManageGroup(new UsernamePasswordAuthenticationToken("user1@slac.stanford.edu", ""))
        );
        assertThat(iAuthorized).isTrue();

        assertDoesNotThrow(
                () -> authService.removeAuthorizationToUserIdToManageGroup("user1@slac.stanford.edu")
        );

        iAuthorized = assertDoesNotThrow(
                () -> authService.canManageGroup(new UsernamePasswordAuthenticationToken("user1@slac.stanford.edu", ""))
        );
        assertThat(iAuthorized).isFalse();
    }

    @Test
    public void testAssignUserToManageGroupWithGeneralAPI() {
        // add user
        assertDoesNotThrow(
                () -> authService.manageAuthorizationOnGroup(
                        AuthorizationGroupManagementDTO
                                .builder()
                                .addUsers(List.of("user1@slac.stanford.edu"))
                                .removeUsers(List.of())
                                .build()
                )
        );

        var authorizationList1 = assertDoesNotThrow(
                () -> authService.getGroupManagementAuthorization(List.of("user1@slac.stanford.edu"))
        );
        assertThat(authorizationList1).isNotEmpty();
        assertThat(authorizationList1)
                .extracting(UserGroupManagementAuthorizationLevel::user)
                .extracting(PersonDTO::mail)
                .contains("user1@slac.stanford.edu");

        // add new user and remove old
        assertDoesNotThrow(
                () -> authService.manageAuthorizationOnGroup(
                        AuthorizationGroupManagementDTO
                                .builder()
                                .addUsers(List.of("user2@slac.stanford.edu"))
                                .removeUsers(List.of("user1@slac.stanford.edu"))
                                .build()
                )
        );
        var authorizationList2 = assertDoesNotThrow(
                () -> authService.getGroupManagementAuthorization(List.of("user1@slac.stanford.edu", "user2@slac.stanford.edu"))
        );
        assertThat(authorizationList2).isNotEmpty();
        assertThat(authorizationList2)
                .extracting(UserGroupManagementAuthorizationLevel::user)
                .extracting(PersonDTO::mail)
                .contains("user1@slac.stanford.edu", "user2@slac.stanford.edu");
        assertThat(authorizationList2)
                .extracting(UserGroupManagementAuthorizationLevel::user)
                .extracting(PersonDTO::mail)
                .contains("user1@slac.stanford.edu", "user2@slac.stanford.edu");
    }

    @Test
    public void createGroup() {
        String groupId = assertDoesNotThrow(
                () -> authService.createLocalGroup(
                        NewLocalGroupDTO
                                .builder()
                                .name("test")
                                .description("test")
                                .members(List.of("user1@slac.stanford.edu", "user2@slac.stanford.edu"))
                                .build()
                )
        );
        assertThat(groupId).isNotNull();

        var groupFound = assertDoesNotThrow(
                () -> authService.findLocalGroup(
                        LocalGroupQueryParameterDTO
                                .builder()
                                .limit(10)
                                .build()
                )
        );
        assertThat(groupFound).isNotNull();
        assertThat(groupFound.size()).isEqualTo(1);
        assertThat(groupFound.get(0).id()).isEqualTo(groupId);
        assertThat(groupFound.get(0).description()).isEqualTo("test");
        assertThat(groupFound.get(0).members().size()).isEqualTo(2);
        assertThat(groupFound.get(0).members()).extracting("mail").contains("user1@slac.stanford.edu", "user2@slac.stanford.edu");
    }

    @Test
    public void updateGroup() {
        String groupId = assertDoesNotThrow(
                () -> authService.createLocalGroup(
                        NewLocalGroupDTO
                                .builder()
                                .name("test")
                                .description("test")
                                .members(List.of("user1@slac.stanford.edu", "user2@slac.stanford.edu"))
                                .build()
                )
        );
        assertThat(groupId).isNotNull();

        // update to change field value
        assertDoesNotThrow(
                () -> authService.updateLocalGroup(
                        groupId,
                        UpdateLocalGroupDTO
                                .builder()
                                .name("name updated")
                                .description("description updated")
                                .members(List.of("user2@slac.stanford.edu", "user2@slac.stanford.edu"))
                                .build()
                )
        );

        var groupFound = assertDoesNotThrow(
                () -> authService.findLocalGroup(
                        LocalGroupQueryParameterDTO
                                .builder()
                                .limit(10)
                                .build()
                )
        );
        assertThat(groupFound).isNotNull();
        assertThat(groupFound.size()).isEqualTo(1);
        assertThat(groupFound.get(0).id()).isEqualTo(groupId);
        assertThat(groupFound.get(0).name()).isEqualTo("name updated");
        assertThat(groupFound.get(0).description()).isEqualTo("description updated");
        assertThat(groupFound.get(0).members().size()).isEqualTo(2);
        assertThat(groupFound.get(0).members()).extracting("mail").contains("user2@slac.stanford.edu", "user2@slac.stanford.edu");
    }

    @Test
    public void testFullTextSearch() {
        // generate 100 random local group
        for (int i = 0; i < 100; i++) {
            int finalI = i;
            assertDoesNotThrow(
                    () -> authService.createLocalGroup(
                            NewLocalGroupDTO
                                    .builder()
                                    .name("test-%d".formatted(finalI))
                                    .description("test-%d description-%d".formatted(finalI, finalI))
                                    .members(List.of("user2@slac.stanford.edu", "user2@slac.stanford.edu"))
                                    .build()
                    )
            );
        }

        // found with text search
        var groupFound = assertDoesNotThrow(
                () -> authService.findLocalGroup(
                        LocalGroupQueryParameterDTO
                                .builder()
                                .search("\"test-10\"")
                                .limit(10)
                                .build()
                )
        );
        assertThat(groupFound).isNotNull();
        assertThat(groupFound.size()).isEqualTo(1);
        assertThat(groupFound.get(0).name()).isEqualTo("test-10");
    }

    @Test
    public void groupNotFoundReturn404() {
        var groupFound = assertThrows(
                GroupNotFound.class,
                () -> authService.findLocalGroupById("wrong-id")
        );
        assertThat(groupFound).isNotNull();
    }

    @Test
    public void createGroupFailWithBadUserId() {
        var groupFound = assertThrows(
                PersonNotFound.class,
                () -> authService.createLocalGroup(
                        NewLocalGroupDTO
                                .builder()
                                .name("test")
                                .description("test")
                                .members(List.of("wrong-id"))
                                .build()
                )
        );
        assertThat(groupFound).isNotNull();
    }

    @Test
    public void modifyGroupFailWithBadUserId() {
        String groupId = assertDoesNotThrow(
                () -> authService.createLocalGroup(
                        NewLocalGroupDTO
                                .builder()
                                .name("test")
                                .description("test")
                                .members(List.of("user2@slac.stanford.edu", "user2@slac.stanford.edu"))
                                .build()
                )
        );
        assertThat(groupId).isNotNull();

        var groupFound = assertThrows(
                PersonNotFound.class,
                () -> authService.updateLocalGroup(
                        groupId,
                        UpdateLocalGroupDTO
                                .builder()
                                .name("test")
                                .description("test")
                                .members(List.of("wrong-id"))
                                .build()
                )
        );
        assertThat(groupFound).isNotNull();
    }
}
