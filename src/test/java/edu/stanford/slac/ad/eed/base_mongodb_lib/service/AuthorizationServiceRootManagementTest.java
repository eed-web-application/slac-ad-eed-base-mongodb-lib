package edu.stanford.slac.ad.eed.base_mongodb_lib.service;

import edu.stanford.slac.ad.eed.base_mongodb_lib.repository.AuthenticationTokenRepository;
import edu.stanford.slac.ad.eed.base_mongodb_lib.repository.AuthorizationRepository;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.NewAuthenticationTokenDTO;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.ad.eed.baselib.model.AuthenticationToken;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import org.assertj.core.api.AssertionsForClassTypes;
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
import org.springframework.data.ldap.repository.config.EnableLdapRepositories;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AutoConfigureMockMvc
@SpringBootTest(properties = {})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
//@EnableLdapRepositories(basePackages = "edu.stanford.slac.ad.eed.baselib.repository")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AuthorizationServiceRootManagementTest {
    @Autowired
    private AppProperties appProperties;
    @Autowired
    private AuthorizationRepository authorizationRepository;
    @Autowired
    @SpyBean
    private AuthenticationTokenRepository authenticationTokenRepository;
    @Autowired
    private AuthService authService;
    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    public void preTest() {
        Mockito.reset(authenticationTokenRepository);
        mongoTemplate.remove(new Query(), Authorization.class);
        mongoTemplate.remove(new Query(), AuthenticationToken.class);
    }

    @Test
    public void getCreateRootUser() {
        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("user1@slac.stanford.edu");
        authService.updateRootUser();
        List<Authorization> rootAuth = authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                "*",
                Authorization.Type.Admin.getValue()
        );

        assertThat(rootAuth).hasSize(1);
        assertThat(rootAuth).extracting(Authorization::getOwner).contains("user1@slac.stanford.edu");
    }

    @Test
    public void getCreateRootAuthToken() {
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

        // check created authorization
        List<Authorization> rootAuth = assertDoesNotThrow(
                () -> authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                        "*",
                        Authorization.Type.Admin.getValue()
                )
        );

        assertThat(rootAuth).hasSize(1);
        assertThat(rootAuth)
                .extracting(Authorization::getOwner)
                .contains(
                        "token-root-a@%s".formatted(
                                appProperties.getAuthenticationTokenDomain()
                        )
                );
        //check created authentication token
        List<AuthenticationToken> allTokenFound = assertDoesNotThrow(
                () -> authenticationTokenRepository.findAllByApplicationManagedIsTrue()
        );
        assertThat(allTokenFound).hasSize(1);
        assertThat(allTokenFound)
                .extracting(AuthenticationToken::getEmail)
                .contains(
                        "token-root-a@%s".formatted(
                                appProperties.getAuthenticationTokenDomain()
                        )
                );
    }

    @Test
    public void getUpdateRootUser() {
        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("user1@slac.stanford.edu");
        authService.updateRootUser();
        List<Authorization> rootAuth = authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                "*",
                Authorization.Type.Admin.getValue()
        );

        assertThat(rootAuth).hasSize(1);
        assertThat(rootAuth).extracting(Authorization::getOwner).contains("user1@slac.stanford.edu");

        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("user2@slac.stanford.edu");

        authService.updateRootUser();
        rootAuth = authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                "*",
                Authorization.Type.Admin.getValue()
        );

        assertThat(rootAuth).hasSize(1);
        assertThat(rootAuth).extracting(Authorization::getOwner).contains("user2@slac.stanford.edu");
    }

    @Test
    public void updateAndDeleteRootAuthToken() {
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
        appProperties.getRootAuthenticationTokenList().clear();
        appProperties.getRootAuthenticationTokenList().add(
                NewAuthenticationTokenDTO
                        .builder()
                        .name("token-root-b")
                        .expiration(LocalDate.of(3000, 12, 31))
                        .build()
        );
        assertDoesNotThrow(
                () -> authService.updateAutoManagedRootToken()
        );
        // check created authorization
        List<Authorization> rootAuth = assertDoesNotThrow(
                () -> authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                        "*",
                        Authorization.Type.Admin.getValue()
                )
        );

        assertThat(rootAuth).hasSize(1);
        assertThat(rootAuth)
                .extracting(Authorization::getOwner)
                .contains(
                        "token-root-b@%s".formatted(
                                appProperties.getAuthenticationTokenDomain()
                        )
                );
        //check created authentication token
        List<AuthenticationToken> allTokenFound = assertDoesNotThrow(
                () -> authenticationTokenRepository.findAllByApplicationManagedIsTrue()
        );
        assertThat(allTokenFound).hasSize(1);
        assertThat(allTokenFound)
                .extracting(AuthenticationToken::getEmail)
                .contains(
                        "token-root-b@%s".formatted(
                                appProperties.getAuthenticationTokenDomain()
                        )
                );
    }

    @Test
    public void getUpdateWithRemoveRootUser() {
        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().addAll(List.of("user1@slac.stanford.edu", "user3@slac.stanford.edu"));
        authService.updateRootUser();
        List<Authorization> rootAuth = authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                "*",
                Authorization.Type.Admin.getValue()
        );

        assertThat(rootAuth).hasSize(2);
        assertThat(rootAuth).extracting(Authorization::getOwner).contains("user1@slac.stanford.edu", "user3@slac.stanford.edu");

        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().addAll(List.of("user1@slac.stanford.edu", "user2@slac.stanford.edu"));

        authService.updateRootUser();
        rootAuth = authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                "*",
                Authorization.Type.Admin.getValue()
        );

        assertThat(rootAuth).hasSize(2);
        assertThat(rootAuth).extracting(Authorization::getOwner).contains("user1@slac.stanford.edu", "user2@slac.stanford.edu");
    }

    @Test
    public void deleteAllRootAuthToken() {
        appProperties.getRootAuthenticationTokenList().clear();
        appProperties.getRootAuthenticationTokenList().add(
                NewAuthenticationTokenDTO
                        .builder()
                        .name("token-root-a")
                        .expiration(LocalDate.of(3000, 12, 31))
                        .build()
        );
        appProperties.getRootAuthenticationTokenList().add(
                NewAuthenticationTokenDTO
                        .builder()
                        .name("token-root-b")
                        .expiration(LocalDate.of(3000, 12, 31))
                        .build()
        );
        assertDoesNotThrow(
                () -> authService.updateAutoManagedRootToken()
        );

        // check created authorization
        List<Authorization> rootAuth = assertDoesNotThrow(
                () -> authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                        "*",
                        Authorization.Type.Admin.getValue()
                )
        );

        assertThat(rootAuth).hasSize(2);
        assertThat(rootAuth)
                .extracting(Authorization::getOwner)
                .contains(
                        "token-root-a@%s".formatted(
                                appProperties.getAuthenticationTokenDomain()
                        ),
                        "token-root-b@%s".formatted(
                                appProperties.getAuthenticationTokenDomain()
                        )
                );
        //check created authentication token
        List<AuthenticationToken> allTokenFound = assertDoesNotThrow(
                () -> authenticationTokenRepository.findAllByApplicationManagedIsTrue()
        );
        assertThat(allTokenFound).hasSize(2);
        assertThat(allTokenFound)
                .extracting(AuthenticationToken::getEmail)
                .contains(
                        "token-root-b@%s".formatted(
                                appProperties.getAuthenticationTokenDomain()
                        ),
                        "token-root-b@%s".formatted(
                                appProperties.getAuthenticationTokenDomain()
                        )
                );

        appProperties.getRootAuthenticationTokenList().clear();
        assertDoesNotThrow(
                () -> authService.updateAutoManagedRootToken()
        );
        rootAuth = assertDoesNotThrow(
                () -> authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                        "*",
                        Authorization.Type.Admin.getValue()
                )
        );

        assertThat(rootAuth).hasSize(0);

        //check created authentication token
        allTokenFound = assertDoesNotThrow(
                () -> authenticationTokenRepository.findAllByApplicationManagedIsTrue()
        );
        assertThat(allTokenFound).hasSize(0);
    }

    @Test
    public void testExceptionDuringDeleteThatLeaveAllUnthouched() {
        appProperties.getRootAuthenticationTokenList().clear();
        appProperties.getRootAuthenticationTokenList().add(
                NewAuthenticationTokenDTO
                        .builder()
                        .name("token-root-a")
                        .expiration(LocalDate.of(3000, 12, 31))
                        .build()
        );
        appProperties.getRootAuthenticationTokenList().add(
                NewAuthenticationTokenDTO
                        .builder()
                        .name("token-root-b")
                        .expiration(LocalDate.of(3000, 12, 31))
                        .build()
        );
        assertDoesNotThrow(
                () -> authService.updateAutoManagedRootToken()
        );

        // check created authorization
        List<Authorization> rootAuth = assertDoesNotThrow(
                () -> authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                        "*",
                        Authorization.Type.Admin.getValue()
                )
        );

        assertThat(rootAuth).hasSize(2);
        assertThat(rootAuth)
                .extracting(Authorization::getOwner)
                .contains(
                        "token-root-a@%s".formatted(
                                appProperties.getAuthenticationTokenDomain()
                        ),
                        "token-root-b@%s".formatted(
                                appProperties.getAuthenticationTokenDomain()
                        )
                );
        //check created authentication token
        List<AuthenticationToken> allTokenFound = assertDoesNotThrow(
                () -> authenticationTokenRepository.findAllByApplicationManagedIsTrue()
        );
        assertThat(allTokenFound).hasSize(2);
        assertThat(allTokenFound)
                .extracting(AuthenticationToken::getEmail)
                .contains(
                        "token-root-b@%s".formatted(
                                appProperties.getAuthenticationTokenDomain()
                        ),
                        "token-root-b@%s".formatted(
                                appProperties.getAuthenticationTokenDomain()
                        )
                );

        appProperties.getRootAuthenticationTokenList().clear();

        Mockito.doThrow(new RuntimeException()).when(authenticationTokenRepository).deleteAllByApplicationManagedIsTrue();
        ControllerLogicException removeAllException = assertThrows(
                ControllerLogicException.class,
                () -> authService.updateAutoManagedRootToken()
        );
        AssertionsForClassTypes.assertThat(removeAllException.getErrorCode()).isEqualTo(-4);

        rootAuth = assertDoesNotThrow(
                () -> authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                        "*",
                        Authorization.Type.Admin.getValue()
                )
        );

        assertThat(rootAuth).hasSize(2);

        //check created authentication token
        allTokenFound = assertDoesNotThrow(
                () -> authenticationTokenRepository.findAllByApplicationManagedIsTrue()
        );
        assertThat(allTokenFound).hasSize(2);
    }

    @Test
    public void manageUserAndToken(){
        appProperties.getRootAuthenticationTokenList().clear();
        appProperties.getRootAuthenticationTokenList().add(
                NewAuthenticationTokenDTO
                        .builder()
                        .name("token-root-a")
                        .expiration(LocalDate.of(3000, 12, 31))
                        .build()
        );
        appProperties.getRootAuthenticationTokenList().add(
                NewAuthenticationTokenDTO
                        .builder()
                        .name("token-root-b")
                        .expiration(LocalDate.of(3000, 12, 31))
                        .build()
        );

        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().addAll(List.of("user1@slac.stanford.edu"));

        assertDoesNotThrow(()->authService.updateRootUser());
        assertDoesNotThrow(() -> authService.updateAutoManagedRootToken());

        // check token authorization
        List<Authorization> rootAuth = assertDoesNotThrow(
                () -> authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                        "*",
                        Authorization.Type.Admin.getValue()
                )
        );

        assertThat(rootAuth).hasSize(3);
        assertThat(rootAuth)
                .extracting(Authorization::getOwner)
                .contains(
                        "user1@slac.stanford.edu",
                        "token-root-a@%s".formatted(
                                appProperties.getAuthenticationTokenDomain()
                        ),
                        "token-root-b@%s".formatted(
                                appProperties.getAuthenticationTokenDomain()
                        )
                );

        // re-execute update and all need to be the same
        assertDoesNotThrow(()->authService.updateRootUser());
        assertDoesNotThrow(() -> authService.updateAutoManagedRootToken());
        // check token authorization
        rootAuth = assertDoesNotThrow(
                () -> authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                        "*",
                        Authorization.Type.Admin.getValue()
                )
        );

        assertThat(rootAuth).hasSize(3);
        assertThat(rootAuth)
                .extracting(Authorization::getOwner)
                .contains(
                        "user1@slac.stanford.edu",
                        "token-root-a@%s".formatted(
                                appProperties.getAuthenticationTokenDomain()
                        ),
                        "token-root-b@%s".formatted(
                                appProperties.getAuthenticationTokenDomain()
                        )
                );

        //add user and token
        appProperties.getRootUserList().add("user2@slac.stanford.edu");
        appProperties.getRootAuthenticationTokenList().add(
                NewAuthenticationTokenDTO
                        .builder()
                        .name("token-root-c")
                        .expiration(LocalDate.of(3000, 12, 31))
                        .build()
        );
        // re-execute update and all need to be the same
        assertDoesNotThrow(()->authService.updateRootUser());
        assertDoesNotThrow(() -> authService.updateAutoManagedRootToken());
        // check token authorization
        rootAuth = assertDoesNotThrow(
                () -> authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                        "*",
                        Authorization.Type.Admin.getValue()
                )
        );

        assertThat(rootAuth).hasSize(5);
        assertThat(rootAuth)
                .extracting(Authorization::getOwner)
                .contains(
                        "user1@slac.stanford.edu",
                        "user2@slac.stanford.edu",
                        "token-root-a@%s".formatted(
                                appProperties.getAuthenticationTokenDomain()
                        ),
                        "token-root-b@%s".formatted(
                                appProperties.getAuthenticationTokenDomain()
                        ),
                        "token-root-c@%s".formatted(
                                appProperties.getAuthenticationTokenDomain()
                        )
                );
        // remove user and token
        appProperties.getRootUserList().removeFirst();
        appProperties.getRootAuthenticationTokenList().removeFirst();
        // re-execute update and all need to be the same
        assertDoesNotThrow(()->authService.updateRootUser());
        assertDoesNotThrow(() -> authService.updateAutoManagedRootToken());
        // check token authorization
        rootAuth = assertDoesNotThrow(
                () -> authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                        "*",
                        Authorization.Type.Admin.getValue()
                )
        );

        assertThat(rootAuth).hasSize(3);
        assertThat(rootAuth)
                .extracting(Authorization::getOwner)
                .contains(
                        "user2@slac.stanford.edu",
                        "token-root-b@%s".formatted(
                                appProperties.getAuthenticationTokenDomain()
                        ),
                        "token-root-c@%s".formatted(
                                appProperties.getAuthenticationTokenDomain()
                        )
                );
    }

    @Test
    public void manageInternalAndNormalAuthenticationToken() {
        appProperties.getRootAuthenticationTokenList().clear();
        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().addAll(List.of("user1@slac.stanford.edu", "service@internal.app-1.slac.app$"));
        appProperties.getRootAuthenticationTokenList().add(
                NewAuthenticationTokenDTO
                        .builder()
                        .name("token-root-a")
                        .expiration(LocalDate.of(3000, 12, 31))
                        .build()
        );
        appProperties.getRootAuthenticationTokenList().add(
                NewAuthenticationTokenDTO
                        .builder()
                        .name("token-root-b")
                        .expiration(LocalDate.of(3000, 12, 31))
                        .build()
        );
        assertDoesNotThrow(() -> authService.updateRootUser());
        assertDoesNotThrow(() -> authService.updateAutoManagedRootToken());

        // check token authorization
        List<Authorization> rootAuth = assertDoesNotThrow(
                () -> authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                        "*",
                        Authorization.Type.Admin.getValue()
                )
        );

        assertThat(rootAuth).hasSize(4);
        assertThat(rootAuth)
                .extracting(Authorization::getOwner)
                .contains(
                        "user1@slac.stanford.edu",
                        "service@internal.app-1.slac.app$",
                        "token-root-a@%s".formatted(
                                appProperties.getAuthenticationTokenDomain()
                        ),
                        "token-root-b@%s".formatted(
                                appProperties.getAuthenticationTokenDomain()
                        )
                );

        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().addAll(List.of("user1@slac.stanford.edu"));

        assertDoesNotThrow(() -> authService.updateRootUser());
        assertDoesNotThrow(() -> authService.updateAutoManagedRootToken());
        rootAuth = assertDoesNotThrow(
                () -> authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                        "*",
                        Authorization.Type.Admin.getValue()
                )
        );
        assertThat(rootAuth).hasSize(3);
        assertThat(rootAuth)
                .extracting(Authorization::getOwner)
                .contains(
                        "user1@slac.stanford.edu",
                        "token-root-a@%s".formatted(
                                appProperties.getAuthenticationTokenDomain()
                        ),
                        "token-root-b@%s".formatted(
                                appProperties.getAuthenticationTokenDomain()
                        )
                );
    }
}
