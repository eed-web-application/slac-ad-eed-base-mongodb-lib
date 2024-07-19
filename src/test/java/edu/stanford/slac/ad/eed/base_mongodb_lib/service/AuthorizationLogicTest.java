package edu.stanford.slac.ad.eed.base_mongodb_lib.service;

import edu.stanford.slac.ad.eed.base_mongodb_lib.repository.AuthorizationRepository;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationOwnerTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.NewLocalGroupDTO;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.model.LocalGroup;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.*;
import static edu.stanford.slac.ad.eed.baselib.model.AuthorizationOwnerType.Group;
import static edu.stanford.slac.ad.eed.baselib.model.AuthorizationOwnerType.User;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AutoConfigureMockMvc
@SpringBootTest(properties = {})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AuthorizationLogicTest {
    @Autowired
    private AppProperties appProperties;
    @Autowired
    private AuthorizationRepository authorizationRepository;
    @Autowired
    private AuthService authService;
    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), Authorization.class);
        mongoTemplate.remove(new Query(), LocalGroup.class);
    }


    @Test
    public void authorizationEnumTest() {
        Authorization.Type type = Authorization.Type.valueOf("Read");
        assertThat(type).isEqualTo(Authorization.Type.Read);
    }

    @Test
    public void authorizationEnumIntegerTest() {
        Integer type = Authorization.Type.valueOf("Read").getValue();
        assertThat(type).isEqualTo(Authorization.Type.Read.getValue());
    }

    @Test
    public void findUserAuthorizationsInheritedByGroups() {
        //read ->r1
        Authorization newAuthWriteUser1 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Read.getValue())
                                .owner("user1@slac.stanford.edu")
                                .ownerType(User)
                                .resource("/r1")
                                .build()
                )
        );
        // write->r1
        Authorization newAuthWriteGroups1_1 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Write.getValue())
                                .owner("group-1")
                                .ownerType(Group)
                                .resource("/r1")
                                .build()
                )
        );
        // write -> r2
        Authorization newAuthWriteGroups1_2 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Write.getValue())
                                .owner("group-1")
                                .ownerType(Group)
                                .resource("/r2")
                                .build()
                )
        );
        // read -> r3
        Authorization newAuthWriteGroups1_3 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Read.getValue())
                                .owner("group-1")
                                .ownerType(Group)
                                .resource("/r3")
                                .build()
                )
        );
        // admin -> r3
        Authorization newAuthWriteGroups2_1 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Admin.getValue())
                                .owner("group-2")
                                .ownerType(Group)
                                .resource("/r3")
                                .build()
                )
        );

        List<AuthorizationDTO> allReadAuthorization = authService.getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
                "user1@slac.stanford.edu",
                Read,
                "/r",
                Optional.empty()
        );

        // check auth on r1 read|write
        assertThat(allReadAuthorization)
                .filteredOn(auth -> auth.resource().compareToIgnoreCase("/r1") == 0)
                .filteredOn(
                        auth -> auth.authorizationType() == Write
                )
                .extracting(AuthorizationDTO::owner)
                .contains("group-1");

        // check auth on r2 write
        assertThat(allReadAuthorization)
                .filteredOn(auth -> auth.resource().compareToIgnoreCase("/r2") == 0)
                .filteredOn(
                        auth -> auth.authorizationType() == Write
                )
                .extracting(AuthorizationDTO::owner)
                .contains("group-1");

        // check auth on r3 read|admin
        assertThat(allReadAuthorization)
                .filteredOn(auth -> auth.resource().compareToIgnoreCase("/r3") == 0)
                .filteredOn(
                        auth ->
                                auth.authorizationType() == Read ||
                                        auth.authorizationType() == Admin
                )
                .extracting(AuthorizationDTO::owner)
                .contains("group-1", "group-2");

        // remove all the authorization from the same resource and keep all the higher one
        List<AuthorizationDTO> allHigherAuthorization = authService.getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
                "user1@slac.stanford.edu",
                Read,
                "/r",
                Optional.of(true)
        );

        // check auth on r1 write
        assertThat(allReadAuthorization)
                .filteredOn(auth -> auth.resource().compareToIgnoreCase("/r1") == 0)
                .filteredOn(
                        auth -> auth.authorizationType() == Write
                )
                .extracting(AuthorizationDTO::owner)
                .contains("group-1");

        // check auth on r2 write
        assertThat(allReadAuthorization)
                .filteredOn(auth -> auth.resource().compareToIgnoreCase("/r2") == 0)
                .filteredOn(
                        auth -> auth.authorizationType() == Write
                )
                .extracting(AuthorizationDTO::owner)
                .contains("group-1");

        // check auth on r3 admin
        assertThat(allReadAuthorization)
                .filteredOn(auth -> auth.resource().compareToIgnoreCase("/r3") == 0)
                .filteredOn(
                        auth -> auth.authorizationType() == Admin
                )
                .extracting(AuthorizationDTO::owner)
                .contains("group-2");
    }

    @Test
    public void checkNonInheritanceFromGroup() {
        //read ->r1
        Authorization newAuthWriteUser1 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Read.getValue())
                                .owner("user1@slac.stanford.edu")
                                .ownerType(User)
                                .resource("/r1")
                                .build()
                )
        );
        // write->r1
        Authorization newAuthWriteGroups1_1 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Write.getValue())
                                .owner("group-1")
                                .ownerType(Group)
                                .resource("/r1")
                                .build()
                )
        );


        List<AuthorizationDTO> allUserAuthorization = authService.getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
                "user1@slac.stanford.edu",
                Read,
                "/r",
                Optional.empty(),
                Optional.of(false)
        );

        // check auth on r1 read|write
        assertThat(allUserAuthorization)
                .filteredOn(auth -> auth.resource().compareToIgnoreCase("/r1") == 0)
                .hasSize(1);

        allUserAuthorization = authService.getAllAuthenticationForOwner(
                "user1@slac.stanford.edu",
                AuthorizationOwnerTypeDTO.User,
                Optional.of(false),
                Optional.of(false)
        );

        // check auth on r1 read|write
        assertThat(allUserAuthorization)
                .filteredOn(auth -> auth.resource().compareToIgnoreCase("/r1") == 0)
                .hasSize(1);

        List<AuthorizationDTO> allUserAuthorizationWithGroupInheritance = authService.getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
                "user1@slac.stanford.edu",
                Read,
                "/r",
                Optional.empty(),
                Optional.of(true)
        );

        // check auth on r1 read|write
        assertThat(allUserAuthorizationWithGroupInheritance)
                .filteredOn(auth -> auth.resource().compareToIgnoreCase("/r1") == 0)
                .hasSize(2);

        allUserAuthorizationWithGroupInheritance = authService.getAllAuthenticationForOwner(
                "user1@slac.stanford.edu",
                AuthorizationOwnerTypeDTO.User,
                Optional.of(false),
                Optional.of(true)
        );
        assertThat(allUserAuthorizationWithGroupInheritance)
                .filteredOn(auth -> auth.resource().compareToIgnoreCase("/r1") == 0)
                .hasSize(2);
    }

    @Test
    public void findAuthorizationByLevel() {
        appProperties.getRootUserList().clear();
        Authorization newAuthReadUser2 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Read.getValue())
                                .owner("user2@slac.stanford.edu")
                                .resource("/r1")
                                .build()
                )
        );

        Authorization newAuthWriteUser3 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Write.getValue())
                                .owner("user3@slac.stanford.edu")
                                .resource("/r1")
                                .build()
                )
        );

        Authorization newAuthAdminUser4 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Admin.getValue())
                                .owner("user4@slac.stanford.edu")
                                .resource("/r1")
                                .build()
                )
        );

        //get all the reader
        List<Authorization> readerShouldBeAllUser = assertDoesNotThrow(
                () -> authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                        "/r1",
                        Authorization.Type.Read.getValue()
                )
        );

        assertThat(readerShouldBeAllUser).hasSize(3);
        assertThat(readerShouldBeAllUser)
                .extracting(Authorization::getOwner)
                .contains(
                        "user2@slac.stanford.edu",
                        "user3@slac.stanford.edu",
                        "user4@slac.stanford.edu"
                );

        //get all the writer
        List<Authorization> writerShouldBeUser3And4 = assertDoesNotThrow(
                () -> authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                        "/r1",
                        Authorization.Type.Write.getValue()
                )
        );

        assertThat(writerShouldBeUser3And4).hasSize(2);
        assertThat(writerShouldBeUser3And4)
                .extracting(Authorization::getOwner)
                .contains(
                        "user3@slac.stanford.edu",
                        "user4@slac.stanford.edu"
                );

        //get all the writer
        List<Authorization> adminShouldBeUser4 = assertDoesNotThrow(
                () -> authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                        "/r1",
                        Authorization.Type.Admin.getValue()
                )
        );

        assertThat(adminShouldBeUser4).hasSize(1);
        assertThat(adminShouldBeUser4)
                .extracting(Authorization::getOwner)
                .contains(
                        "user4@slac.stanford.edu"
                );
    }

    @Test
    public void testEnsureAuthorizationMultipleThread() {
        int numberOfThreads = 20; // Number of concurrent threads
        List<Future<String>> futures;
        try (ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads)) {
            List<Callable<String>> tasks = new ArrayList<>();
            AuthorizationDTO authorizationDTO = AuthorizationDTO
                    .builder()
                    .owner("owner")
                    .authorizationType(Read)
                    .resource("/r1")
                    .ownerType(AuthorizationOwnerTypeDTO.User)
                    .build();

            for (int i = 0; i < numberOfThreads * 10; i++) {
                tasks.add(() -> authService.ensureAuthorization(authorizationDTO));
            }

            futures = assertDoesNotThrow(() -> executorService.invokeAll(tasks));

            // Shut down the executor service
            executorService.shutdown();
        }

        // Assert that all threads received the same ID
        String expectedId = null;
        for (Future<String> future : futures) {
            String id = assertDoesNotThrow(() -> future.get());
            if (expectedId == null) {
                expectedId = id; // Set the expected ID from the first thread
            }
            assertThat(id).isEqualTo(expectedId);
        }
    }

    static private class AuthorizationIs extends Condition<String> {

        private final String expectedValue;

        private AuthorizationIs(String expectedValue) {
            this.expectedValue = expectedValue;
        }

        public static AuthorizationIs ofType(String type) {
            return new AuthorizationIs(type);
        }

        @Override
        public boolean matches(String actualValue) {
            return actualValue != null && actualValue.compareToIgnoreCase(expectedValue) == 0;
        }
    }

    @Test
    public void deleteAuthorizationForASpecificUserAndResource() {
        appProperties.getRootUserList().clear();
        Authorization newAuthUser1 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Read.getValue())
                                .owner("user1@slac.stanford.edu")
                                .ownerType(User)
                                .resource("/r1")
                                .build()
                )
        );
        Authorization newAuthUser2 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Read.getValue())
                                .owner("user2@slac.stanford.edu")
                                .ownerType(User)
                                .resource("/r1")
                                .build()
                )
        );

        Authorization newAuthGroup2 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Read.getValue())
                                .owner("group-name")
                                .ownerType(Group)
                                .resource("/r1")
                                .build()
                )
        );

        assertDoesNotThrow(
                () -> authService.deleteAuthorizationForResourcePrefix("/r1", "user2@slac.stanford.edu", AuthorizationOwnerTypeDTO.User)
        );

        var authorization = assertDoesNotThrow(
                () -> authService.findByResourceIs("/r1")
        );

        assertThat(authorization).hasSize(2);
        assertThat(authorization)
                .extracting(AuthorizationDTO::owner)
                .contains("user1@slac.stanford.edu", "group-name");
    }

    @Test
    public void deleteAuthorizationForASpecificGroupAndResource() {
        appProperties.getRootUserList().clear();
        Authorization newAuthUser1 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Read.getValue())
                                .owner("user1@slac.stanford.edu")
                                .ownerType(User)
                                .resource("/r1")
                                .build()
                )
        );
        Authorization newAuthUser2 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Read.getValue())
                                .owner("user2@slac.stanford.edu")
                                .ownerType(User)
                                .resource("/r1")
                                .build()
                )
        );

        Authorization newAuthGroup2 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Read.getValue())
                                .owner("group-name")
                                .ownerType(Group)
                                .resource("/r1")
                                .build()
                )
        );

        assertDoesNotThrow(
                () -> authService.deleteAuthorizationForResourcePrefix("/r1", "group-name", AuthorizationOwnerTypeDTO.Group)
        );

        var authorization = assertDoesNotThrow(
                () -> authService.findByResourceIs("/r1")
        );

        assertThat(authorization).hasSize(2);
        assertThat(authorization)
                .extracting(AuthorizationDTO::owner)
                .contains("user1@slac.stanford.edu", "user2@slac.stanford.edu");
    }

    @Test
    public void deleteAuthorizationForASpecificResourceAndOwnerType() {
        appProperties.getRootUserList().clear();
        Authorization newAuthUser1 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Read.getValue())
                                .owner("user1@slac.stanford.edu")
                                .ownerType(User)
                                .resource("/r1")
                                .build()
                )
        );
        Authorization newAuthUser2 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Read.getValue())
                                .owner("user2@slac.stanford.edu")
                                .ownerType(User)
                                .resource("/r1")
                                .build()
                )
        );

        Authorization newAuthGroup1 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Read.getValue())
                                .owner("group-name-1")
                                .ownerType(Group)
                                .resource("/r1")
                                .build()
                )
        );

        assertDoesNotThrow(
                () -> authService.deleteAuthorizationForResourcePrefix("/r1", AuthorizationOwnerTypeDTO.Group)
        );

        var authorization = assertDoesNotThrow(
                () -> authService.findByResourceIs("/r1")
        );

        assertThat(authorization).hasSize(2);
        assertThat(authorization)
                .extracting(AuthorizationDTO::owner)
                .contains("user1@slac.stanford.edu", "user2@slac.stanford.edu");

        assertDoesNotThrow(
                () -> authService.deleteAuthorizationForResourcePrefix("/r1", AuthorizationOwnerTypeDTO.User)
        );

        authorization = assertDoesNotThrow(
                () -> authService.findByResourceIs("/r1")
        );

        assertThat(authorization).hasSize(0);
    }

    @Test
    public void testFindAuthorizationForSpecificOwnerOTypeAndResource() {
        appProperties.getRootUserList().clear();
        Authorization newAuthUser1 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Read.getValue())
                                .owner("user1@slac.stanford.edu")
                                .ownerType(User)
                                .resource("/r1")
                                .build()
                )
        );
        Authorization newAuthUser21 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Write.getValue())
                                .owner("user2@slac.stanford.edu")
                                .ownerType(User)
                                .resource("/r1")
                                .build()
                )
        );
        Authorization newAuthUser22 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Read.getValue())
                                .owner("user2@slac.stanford.edu")
                                .ownerType(User)
                                .resource("/r2")
                                .build()
                )
        );
        Authorization newAuthGroup11 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Admin.getValue())
                                .owner("group-1")
                                .ownerType(Group)
                                .resource("/r1")
                                .build()
                )
        );
        Authorization newAuthGroup12 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Read.getValue())
                                .owner("group-1")
                                .ownerType(Group)
                                .resource("/r2")
                                .build()
                )
        );
        Authorization newAuthGroup2 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Admin.getValue())
                                .owner("group-2")
                                .ownerType(Group)
                                .resource("/r1")
                                .build()
                )
        );

        var userAuthorization = assertDoesNotThrow(
                () -> authService.getAllAuthenticationForOwner(
                        "user1@slac.stanford.edu",
                        AuthorizationOwnerTypeDTO.User,
                        "/r1",
                        Optional.of(true)
                )
        );
        assertThat(userAuthorization)
                .hasSize(1)
                .anySatisfy(auth -> AssertionsForClassTypes.assertThat(auth).is(AuthorizationDTOIs.of("/r1", Admin)));

        userAuthorization = assertDoesNotThrow(
                () -> authService.getAllAuthenticationForOwner(
                        "user1@slac.stanford.edu",
                        AuthorizationOwnerTypeDTO.User,
                        "/r1",
                        Optional.empty()
                )
        );
        assertThat(userAuthorization)
                .hasSize(3)
                .anySatisfy(auth -> AssertionsForClassTypes.assertThat(auth).is(AuthorizationDTOIs.of("/r1", Admin)))
                .anySatisfy(auth -> AssertionsForClassTypes.assertThat(auth).is(AuthorizationDTOIs.of("/r1", Read)));
    }

    @Test
    public void testFindAuthorizationForSpecificOwnerOType() {
        appProperties.getRootUserList().clear();
        Authorization newAuthUser1 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Read.getValue())
                                .owner("user1@slac.stanford.edu")
                                .ownerType(User)
                                .resource("/r1")
                                .build()
                )
        );
        Authorization newAuthUser21 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Write.getValue())
                                .owner("user2@slac.stanford.edu")
                                .ownerType(User)
                                .resource("/r1")
                                .build()
                )
        );
        Authorization newAuthUser22 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Read.getValue())
                                .owner("user2@slac.stanford.edu")
                                .ownerType(User)
                                .resource("/r2")
                                .build()
                )
        );
        Authorization newAuthGroup11 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Admin.getValue())
                                .owner("group-1")
                                .ownerType(Group)
                                .resource("/r1")
                                .build()
                )
        );
        Authorization newAuthGroup12 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Read.getValue())
                                .owner("group-1")
                                .ownerType(Group)
                                .resource("/r2")
                                .build()
                )
        );
        Authorization newAuthGroup2 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Admin.getValue())
                                .owner("group-2")
                                .ownerType(Group)
                                .resource("/r1")
                                .build()
                )
        );

        var userAuthorization = assertDoesNotThrow(
                () -> authService.getAllAuthenticationForOwner(
                        "user1@slac.stanford.edu",
                        AuthorizationOwnerTypeDTO.User,
                        Optional.of(true)
                )
        );
        assertThat(userAuthorization)
                .hasSize(2)
                .anySatisfy(auth -> assertThat(auth).is(AuthorizationDTOIs.of("/r1", Admin)))
                .anySatisfy(auth -> assertThat(auth).is(AuthorizationDTOIs.of("/r2", Read)));

        userAuthorization = assertDoesNotThrow(
                () -> authService.getAllAuthenticationForOwner(
                        "user1@slac.stanford.edu",
                        AuthorizationOwnerTypeDTO.User,
                        Optional.empty()
                )
        );
        assertThat(userAuthorization)
                .hasSize(4)
                .anySatisfy(auth -> assertThat(auth).is(AuthorizationDTOIs.of("/r1", Admin)))
                .anySatisfy(auth -> assertThat(auth).is(AuthorizationDTOIs.of("/r1", Read)))
                .anySatisfy(auth -> assertThat(auth).is(AuthorizationDTOIs.of("/r2", Read)));
    }

    @Test
    public void checkAuthorizationUsingLocalGroup() {
        appProperties.getRootUserList().clear();


        authService.createLocalGroup(
                NewLocalGroupDTO
                        .builder()
                        .name("local-group-1")
                        .description("local-group-1")
                        .members(List.of("user3@slac.stanford.edu"))
                        .build()
        );
        appProperties.getRootUserList().clear();
        Authorization newAuthUser1 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Read.getValue())
                                .owner("user1@slac.stanford.edu")
                                .ownerType(User)
                                .resource("/r1")
                                .build()
                )
        );
        Authorization newAuthUser21 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Write.getValue())
                                .owner("user2@slac.stanford.edu")
                                .ownerType(User)
                                .resource("/r1")
                                .build()
                )
        );
        Authorization newAuthUser22 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Read.getValue())
                                .owner("user2@slac.stanford.edu")
                                .ownerType(User)
                                .resource("/r2")
                                .build()
                )
        );
        Authorization newAuthGroup11 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Admin.getValue())
                                .owner("local-group-1")
                                .ownerType(Group)
                                .resource("/r1")
                                .build()
                )
        );
        Authorization newAuthGroup12 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Read.getValue())
                                .owner("local-group-1")
                                .ownerType(Group)
                                .resource("/r2")
                                .build()
                )
        );


        var userAuthorization = assertDoesNotThrow(
                () -> authService.getAllAuthenticationForOwner(
                        "user1@slac.stanford.edu",
                        AuthorizationOwnerTypeDTO.User,
                        Optional.of(true)
                )
        );

        userAuthorization = assertDoesNotThrow(
                () -> authService.getAllAuthenticationForOwner(
                        "user3@slac.stanford.edu",
                        AuthorizationOwnerTypeDTO.User,
                        Optional.empty()
                )
        );
        assertThat(userAuthorization)
                .hasSize(2)
                .anySatisfy(auth -> assertThat(auth).is(AuthorizationDTOIs.of("/r1", Admin)))
                .anySatisfy(auth -> assertThat(auth).is(AuthorizationDTOIs.of("/r2", Read)));

    }

    /**
     * Test to ensure that the authorization is not duplicated when the same authorization is added multiple times
     */
    static private class AuthorizationDTOIs extends Condition<AuthorizationDTO> {
        private final String resource;
        private final AuthorizationTypeDTO authorizationTypeDTO;

        private AuthorizationDTOIs(String resource, AuthorizationTypeDTO authorizationTypeDTO) {
            this.resource = resource;
            this.authorizationTypeDTO = authorizationTypeDTO;
        }

        public static AuthorizationDTOIs of(String resource, AuthorizationTypeDTO authorizationTypeDTO) {
            return new AuthorizationDTOIs(resource, authorizationTypeDTO);
        }

        @Override
        public boolean matches(AuthorizationDTO authorizationDTO) {
            return authorizationDTO != null
                    && authorizationDTO.resource().equalsIgnoreCase(resource)
                    && authorizationDTO.authorizationType().equals(authorizationTypeDTO);
        }
    }
}
