package edu.stanford.slac.ad.eed.base_mongodb_lib.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.*;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.NewLocalGroupDTO;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.exception.AuthenticationTokenMalformed;
import edu.stanford.slac.ad.eed.baselib.exception.AuthenticationTokenNotFound;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.ad.eed.baselib.exception.NotAuthorized;
import edu.stanford.slac.ad.eed.baselib.model.AuthenticationToken;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.model.LocalGroup;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import org.assertj.core.api.AssertionsForClassTypes;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static edu.stanford.slac.ad.eed.baselib.utility.StringUtilities.normalizeStringWithReplace;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AuthControllerTest {
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
    private String group1Id = null;
    private String group2Id = null;

    @BeforeEach
    public void preTest() {
        //reset authorizations
        mongoTemplate.remove(new Query(), LocalGroup.class);
        mongoTemplate.remove(new Query(), AuthenticationToken.class);
        mongoTemplate.remove(new Query(), Authorization.class);

        group1Id = authService.createLocalGroup(
                NewLocalGroupDTO
                        .builder()
                        .name("group-1")
                        .description("group-1")
                        .members(List.of("user1@slac.stanford.edu"))
                        .build()
        );
        group2Id = authService.createLocalGroup(
                NewLocalGroupDTO
                        .builder()
                        .name("group-2")
                        .description("group-2")
                        .members(List.of("user1@slac.stanford.edu", "user2@slac.stanford.edu"))
                        .build()
        );
        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("user1@slac.stanford.edu");
        authService.updateRootUser();
    }

    @Test
    public void getMe() {
        // add some other authorization for user
        assertDoesNotThrow(
                ()->authService.addNewAuthorization(
                        NewAuthorizationDTO
                                .builder()
                                .authorizationType(AuthorizationTypeDTO.Read)
                                .ownerType(AuthorizationOwnerTypeDTO.User)
                                .owner("user1@slac.stanford.edu")
                                .resource("read::resource1")
                                .build()
                )
        );
        assertDoesNotThrow(
                ()->authService.addNewAuthorization(
                        NewAuthorizationDTO
                                .builder()
                                .authorizationType(AuthorizationTypeDTO.Write)
                                .ownerType(AuthorizationOwnerTypeDTO.User)
                                .owner("user1@slac.stanford.edu")
                                .resource("write::resource2")
                                .build()
                )
        );
        assertDoesNotThrow(
                ()->authService.addNewAuthorization(
                        NewAuthorizationDTO
                                .builder()
                                .authorizationType(AuthorizationTypeDTO.Read)
                                .ownerType(AuthorizationOwnerTypeDTO.Group)
                                .owner(group1Id)
                                .resource("read::resource3")
                                .build()
                )
        );
        //add authorization for user 2 to mix the things
        assertDoesNotThrow(
                ()->authService.addNewAuthorization(
                        NewAuthorizationDTO
                                .builder()
                                .authorizationType(AuthorizationTypeDTO.Read)
                                .ownerType(AuthorizationOwnerTypeDTO.User)
                                .owner("user2@slac.stanford.edu")
                                .resource("read::resource1")
                                .build()
                )
        );

        ApiResultResponse<PersonDetailsDTO> meResult = assertDoesNotThrow(
                () -> testControllerHelperService.getMe(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty()
                )
        );

        assertThat(meResult).isNotNull();
        assertThat(meResult.getErrorCode()).isEqualTo(0);
        assertThat(meResult.getPayload().person().uid()).isEqualTo("user1");
        assertThat(meResult.getPayload().authorizations()).hasSize(4);
        assertThat(meResult.getPayload().authorizations())
                .extracting(
                        AuthorizationDTO::resource
                )
                .contains(
                        "*",
                        "read::resource1",
                        "write::resource2",
                        "read::resource3"
                );
    }

    @Test
    public void getMeFailUnauthorized() {
        NotAuthorized userNotFoundException = assertThrows(
                NotAuthorized.class,
                () -> testControllerHelperService.getMe(
                        mockMvc,
                        status().isUnauthorized(),
                        Optional.empty(),
                        Optional.empty()
                )
        );

        AssertionsForClassTypes.assertThat(userNotFoundException).isNotNull();
        AssertionsForClassTypes.assertThat(userNotFoundException.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void findUsersOK() {
        ApiResultResponse<List<PersonDTO>> meResult = assertDoesNotThrow(
                () -> testControllerHelperService.findUsers(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        Optional.of("name")
                )
        );

        AssertionsForClassTypes.assertThat(meResult).isNotNull();
        AssertionsForClassTypes.assertThat(meResult.getErrorCode()).isEqualTo(0);
        assertThat(meResult.getPayload()).hasSize(3);
    }

    @Test
    public void findUsersByNameOK() {
        ApiResultResponse<List<PersonDTO>> meResult = assertDoesNotThrow(
                () -> testControllerHelperService.findUsers(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        Optional.of("Name1")
                )
        );

        AssertionsForClassTypes.assertThat(meResult).isNotNull();
        AssertionsForClassTypes.assertThat(meResult.getErrorCode()).isEqualTo(0);
        assertThat(meResult.getPayload()).hasSize(1);
        AssertionsForClassTypes.assertThat(meResult.getPayload().get(0).gecos()).isEqualTo("Name1 Surname1");
    }

    @Test
    public void findUsersBySurnameOK() {
        ApiResultResponse<List<PersonDTO>> meResult = assertDoesNotThrow(
                () -> testControllerHelperService.findUsers(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        Optional.of("Surname1")
                )
        );

        AssertionsForClassTypes.assertThat(meResult).isNotNull();
        AssertionsForClassTypes.assertThat(meResult.getErrorCode()).isEqualTo(0);
        assertThat(meResult.getPayload()).hasSize(1);
        AssertionsForClassTypes.assertThat(meResult.getPayload().get(0).gecos()).isEqualTo("Name1 Surname1");
    }

    @Test
    public void findUsersFailUnauthorized() {
        NotAuthorized userNotFoundException = assertThrows(
                NotAuthorized.class,
                () -> testControllerHelperService.findUsers(
                        mockMvc,
                        status().isUnauthorized(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("name")
                )
        );

        AssertionsForClassTypes.assertThat(userNotFoundException).isNotNull();
        AssertionsForClassTypes.assertThat(userNotFoundException.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void findGroupsFailUnauthorized() {
        NotAuthorized userNotFoundException = assertThrows(
                NotAuthorized.class,
                () -> testControllerHelperService.findGroups(
                        mockMvc,
                        status().isUnauthorized(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("group")
                )
        );

        AssertionsForClassTypes.assertThat(userNotFoundException).isNotNull();
        AssertionsForClassTypes.assertThat(userNotFoundException.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void createAuthenticationTokenFailsOnMalformed() {
        AuthenticationTokenMalformed userNotFoundException = assertThrows(
                AuthenticationTokenMalformed.class,
                () -> testControllerHelperService.createNewAuthenticationToken(
                        mockMvc,
                        status().isBadRequest(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        NewAuthenticationTokenDTO
                                .builder()
                                .build()
                )
        );

        AssertionsForClassTypes.assertThat(userNotFoundException.getErrorCode()).isEqualTo(-1);

        userNotFoundException = assertThrows(
                AuthenticationTokenMalformed.class,
                () -> testControllerHelperService.createNewAuthenticationToken(
                        mockMvc,
                        status().isBadRequest(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("tok-name")
                                .build()
                )
        );

        AssertionsForClassTypes.assertThat(userNotFoundException.getErrorCode()).isEqualTo(-1);

        userNotFoundException = assertThrows(
                AuthenticationTokenMalformed.class,
                () -> testControllerHelperService.createNewAuthenticationToken(
                        mockMvc,
                        status().isBadRequest(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        NewAuthenticationTokenDTO
                                .builder()
                                .expiration(LocalDate.of(2023,12,31))
                                .build()
                )
        );

        AssertionsForClassTypes.assertThat(userNotFoundException.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void createAuthenticationTokenOK() {
        ApiResultResponse<AuthenticationTokenDTO> authToken = assertDoesNotThrow(
                () -> testControllerHelperService.createNewAuthenticationToken(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-a")
                                .expiration(LocalDate.of(2023,12,31))
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(authToken.getErrorCode()).isEqualTo(0);
    }

    @Test
    public void createAuthenticationTokenFailOnSameAppManagedToken() {
        appProperties.getRootAuthenticationTokenList().clear();
        appProperties.getRootAuthenticationTokenList().add(
                NewAuthenticationTokenDTO
                        .builder()
                        .name("token-root-a")
                        .expiration(LocalDate.of(3000, 12, 31))
                        .build()
        );

        assertDoesNotThrow(
                () -> authService.updateAutoManagedRootToken()
        );

        ControllerLogicException exceptionForSaveAlreadyExistingToken = assertThrows(
                ControllerLogicException.class,
                () -> testControllerHelperService.createNewAuthenticationToken(
                        mockMvc,
                        status().is5xxServerError(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-root-a")
                                .expiration(LocalDate.of(2023,12,31))
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(exceptionForSaveAlreadyExistingToken.getErrorCode()).isEqualTo(-3);
    }

    @Test
    public void createRootAuthorizationFailOnSameAppManagedToken() {
        appProperties.getRootAuthenticationTokenList().clear();
        appProperties.getRootAuthenticationTokenList().add(
                NewAuthenticationTokenDTO
                        .builder()
                        .name("token-root-a")
                        .expiration(LocalDate.of(3000, 12, 31))
                        .build()
        );

        assertDoesNotThrow(
                () -> authService.updateAutoManagedRootToken()
        );
        Optional<AuthenticationTokenDTO> token = authService.getAuthenticationTokenByName("token-root-a");
        assertThat(token).isPresent();
        ControllerLogicException notAuthorizedOnAppManagedToken = assertThrows(
                ControllerLogicException.class,
                ()->testControllerHelperService.createNewRootUser(
                        mockMvc,
                        status().isInternalServerError(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        token.get().id()
                )
        );
        AssertionsForClassTypes.assertThat(notAuthorizedOnAppManagedToken.getErrorCode()).isEqualTo(-3);
    }

    @Test
    public void deleteRootAuthorizationFailOnSameAppManagedToken() {
        appProperties.getRootAuthenticationTokenList().clear();
        appProperties.getRootAuthenticationTokenList().add(
                NewAuthenticationTokenDTO
                        .builder()
                        .name("token-root-a")
                        .expiration(LocalDate.of(3000, 12, 31))
                        .build()
        );

        assertDoesNotThrow(
                () -> authService.updateAutoManagedRootToken()
        );
        Optional<AuthenticationTokenDTO> token = authService.getAuthenticationTokenByName("token-root-a");
        assertThat(token).isPresent();
        ControllerLogicException notAuthorizedOnAppManagedToken = assertThrows(
                ControllerLogicException.class,
                ()->testControllerHelperService.deleteRootUser(
                        mockMvc,
                        status().isInternalServerError(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        token.get().id()
                )
        );
        AssertionsForClassTypes.assertThat(notAuthorizedOnAppManagedToken.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void getAllAuthenticationTokenOK() {
        ApiResultResponse<AuthenticationTokenDTO> authToken1 = assertDoesNotThrow(
                () -> testControllerHelperService.createNewAuthenticationToken(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-a")
                                .expiration(LocalDate.of(2023,12,31))
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(authToken1.getErrorCode()).isEqualTo(0);

        ApiResultResponse<AuthenticationTokenDTO> authToken2 = assertDoesNotThrow(
                () -> testControllerHelperService.createNewAuthenticationToken(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-b")
                                .expiration(LocalDate.of(2023,12,31))
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(authToken2.getErrorCode()).isEqualTo(0);

        ApiResultResponse<List<AuthenticationTokenDTO>> allToken =  assertDoesNotThrow(
                () -> testControllerHelperService.getAllAuthenticationToken(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty()
                )
        );
        AssertionsForClassTypes.assertThat(allToken.getErrorCode()).isEqualTo(0);
        assertThat(allToken.getPayload())
                .hasSize(2)
                .extracting(
                        AuthenticationTokenDTO::name
                )
                .contains(
                        "token-a",
                        "token-b"
                );
    }

    @Test
    public void deleteAllAuthenticationTokenOK() {
        ApiResultResponse<AuthenticationTokenDTO> authToken1 = assertDoesNotThrow(
                () -> testControllerHelperService.createNewAuthenticationToken(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-a")
                                .expiration(LocalDate.of(2023,12,31))
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(authToken1.getErrorCode()).isEqualTo(0);

        ApiResultResponse<AuthenticationTokenDTO> authToken2 = assertDoesNotThrow(
                () -> testControllerHelperService.createNewAuthenticationToken(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-b")
                                .expiration(LocalDate.of(2023,12,31))
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(authToken2.getErrorCode()).isEqualTo(0);

        ApiResultResponse<List<AuthenticationTokenDTO>> allToken =  assertDoesNotThrow(
                () -> testControllerHelperService.getAllAuthenticationToken(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty()
                )
        );
        AssertionsForClassTypes.assertThat(allToken.getErrorCode()).isEqualTo(0);
        assertThat(allToken.getPayload())
                .hasSize(2)
                .extracting(
                        AuthenticationTokenDTO::name
                )
                .contains(
                        "token-a",
                        "token-b"
                );

        ApiResultResponse<Boolean> deleteOpResult = assertDoesNotThrow(
                () -> testControllerHelperService.deleteAuthenticationToken(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        authToken2.getPayload().id()
                )
        );
        AssertionsForClassTypes.assertThat(deleteOpResult.getErrorCode()).isEqualTo(0);

        // now we will have only one token
        allToken =  assertDoesNotThrow(
                () -> testControllerHelperService.getAllAuthenticationToken(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty()
                )
        );
        AssertionsForClassTypes.assertThat(allToken.getErrorCode()).isEqualTo(0);
        assertThat(allToken.getPayload())
                .hasSize(1)
                .extracting(
                        AuthenticationTokenDTO::name
                )
                .contains(
                        "token-a"
                );
    }

    @Test
    public void createAuthTokenAndMakItRootFailOnNotExistingPerson() {
        ControllerLogicException personNotFoundException = assertThrows(
                ControllerLogicException.class,
                ()->testControllerHelperService.createNewRootUser(
                        mockMvc,
                        status().isNotFound(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        "wrong@user.email"
                )
        );

        AssertionsForClassTypes.assertThat(personNotFoundException.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void createRootForGlobalToken() {
        var authenticationCreationResponse = assertDoesNotThrow(
                ()->testControllerHelperService.createNewAuthenticationToken(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-a")
                                .expiration(LocalDate.of(2023,12,31))
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(authenticationCreationResponse.getErrorCode()).isEqualTo(0);
        // create root user for token
        var rootUseCreatedResponse = assertDoesNotThrow(
                ()->testControllerHelperService.createNewRootUser(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        authenticationCreationResponse.getPayload().id()
                )
        );

        AssertionsForClassTypes.assertThat(rootUseCreatedResponse.getErrorCode()).isEqualTo(0);
        // check if token has been created
        var allRootUseResponse = assertDoesNotThrow(
                ()->testControllerHelperService.findAllRootUser(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty()
                )
        );
        AssertionsForClassTypes.assertThat(allRootUseResponse.getErrorCode()).isEqualTo(0);
        assertThat(allRootUseResponse.getPayload())
                .extracting(AuthorizationDTO::owner)
                .hasSize(2)
                .contains(authenticationCreationResponse.getPayload().id());

        var deleteRootUserResponse = assertDoesNotThrow(
                ()->testControllerHelperService.deleteRootUser(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        authenticationCreationResponse.getPayload().id()
                )
        );
        AssertionsForClassTypes.assertThat(deleteRootUserResponse.getErrorCode()).isEqualTo(0);

        // check if token has been deleted (only the default one should be present)
        allRootUseResponse = assertDoesNotThrow(
                ()->testControllerHelperService.findAllRootUser(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty()
                )
        );
        AssertionsForClassTypes.assertThat(allRootUseResponse.getErrorCode()).isEqualTo(0);
        assertThat(allRootUseResponse.getPayload())
                .extracting(AuthorizationDTO::owner)
                .hasSize(1)
                .doesNotContain(authenticationCreationResponse.getPayload().id());
    }

    @Test
    public void checkIfRootUserCanCreateOtherRootUser() {
        var authenticationTokenCreationResponse = assertDoesNotThrow(
                ()->testControllerHelperService.createNewAuthenticationToken(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-a")
                                .expiration(LocalDate.of(2023,12,31))
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(authenticationTokenCreationResponse.getErrorCode()).isEqualTo(0);
        // create root user for token
        var rootUseCreatedResponse = assertDoesNotThrow(
                ()->testControllerHelperService.createNewRootUser(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty(),
                        authenticationTokenCreationResponse.getPayload().id()
                )
        );

        AssertionsForClassTypes.assertThat(rootUseCreatedResponse.getErrorCode()).isEqualTo(0);

        // create new token and root auth with this new token
        var anotherAuthenticationTokenCreationResponse = assertDoesNotThrow(
                ()->testControllerHelperService.createNewAuthenticationToken(
                        mockMvc,
                        status().isCreated(),
                        Optional.of(authenticationTokenCreationResponse.getPayload().id()),
                        Optional.empty(),
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-b-made-by-a")
                                .expiration(LocalDate.of(2023,12,31))
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(anotherAuthenticationTokenCreationResponse.getErrorCode()).isEqualTo(0);

        var anotherRootAuthorizationCreatedResponse = assertDoesNotThrow(
                ()->testControllerHelperService.createNewRootUser(
                        mockMvc,
                        status().isCreated(),
                        Optional.of(authenticationTokenCreationResponse.getPayload().id()),
                        Optional.empty(),
                        anotherAuthenticationTokenCreationResponse.getPayload().id()
                )
        );

        AssertionsForClassTypes.assertThat(anotherRootAuthorizationCreatedResponse.getErrorCode()).isEqualTo(0);
    }
}
