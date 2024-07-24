package edu.stanford.slac.ad.eed.base_mongodb_lib.service;

import edu.stanford.slac.ad.eed.base_mongodb_lib.repository.AuthenticationTokenRepository;
import edu.stanford.slac.ad.eed.base_mongodb_lib.repository.AuthorizationRepository;
import edu.stanford.slac.ad.eed.base_mongodb_lib.utility.InitAuthorizationIndex;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthenticationTokenDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthenticationTokenQueryParameterDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.NewAuthenticationTokenDTO;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.exception.AuthenticationTokenMalformed;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.ad.eed.baselib.model.AuthenticationToken;
import edu.stanford.slac.ad.eed.baselib.model.AuthenticationTokenQueryParameter;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.model.AuthorizationOwnerType;
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
import java.util.Optional;

import static edu.stanford.slac.ad.eed.baselib.model.Authorization.Type.Read;
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
public class AuthServiceTokenTest {
    @Autowired
    AppProperties appProperties;
    @Autowired
    private AuthorizationRepository authorizationRepository;
    @SpyBean
    @Autowired
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
        InitAuthorizationIndex initAuthorizationIndex = new InitAuthorizationIndex(mongoTemplate);
        initAuthorizationIndex.updateIndex();
    }

    @Test
    public void createGlobalToken(){
        AuthenticationTokenDTO newAuthToken = assertDoesNotThrow(
                () -> authService.addNewAuthenticationToken(
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-a")
                                .expiration(LocalDate.of(2023,12,31))
                                .build(),
                        true
                )
        );

        assertThat(newAuthToken).isNotNull();
        assertThat(newAuthToken.id()).isNotNull().isNotEmpty();
        assertThat(newAuthToken.email()).isEqualTo("token-a@%s".formatted(appProperties.getAuthenticationTokenDomain()));
    }

    @Test
    public void createApplicationToken(){
        AuthenticationTokenDTO newAuthToken = assertDoesNotThrow(
                () -> authService.addNewApplicationAuthenticationToken(
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-a")
                                .expiration(LocalDate.of(2023,12,31))
                                .build(),
                        true
                )
        );

        assertThat(newAuthToken).isNotNull();
        assertThat(newAuthToken.id()).isNotNull().isNotEmpty();
        assertThat(newAuthToken.email()).isEqualTo("token-a@%s".formatted(appProperties.getAppEmailPostfix()));
    }

    @Test
    public void createTokenFailsOnMalformed() {
        AuthenticationTokenMalformed malformedException = assertThrows(
                AuthenticationTokenMalformed.class,
                () -> authService.addNewAuthenticationToken(
                        NewAuthenticationTokenDTO
                                .builder()
                                .build(),
                        false
                )
        );

        AssertionsForClassTypes.assertThat(malformedException.getErrorCode()).isEqualTo(-1);

        malformedException = assertThrows(
                AuthenticationTokenMalformed.class,
                () -> authService.addNewAuthenticationToken(
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("is the name")
                                .build(),
                        false
                )
        );
        AssertionsForClassTypes.assertThat(malformedException.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void createTokenOk() {
        AuthenticationTokenDTO newAuthToken = assertDoesNotThrow(
                () -> authService.addNewAuthenticationToken(
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-a")
                                .expiration(LocalDate.of(2023,12,31))
                                .build(),
                        false
                )
        );

        AssertionsForClassTypes.assertThat(newAuthToken).isNotNull();
        AssertionsForClassTypes.assertThat(newAuthToken.id()).isNotNull().isNotEmpty();
    }

    @Test
    public void createTokenGetOkByIDAndName() {
        AuthenticationTokenDTO newAuthToken = assertDoesNotThrow(
                () -> authService.addNewAuthenticationToken(
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-a")
                                .expiration(LocalDate.of(2023,12,31))
                                .build(),
                        false
                )
        );

        AssertionsForClassTypes.assertThat(newAuthToken).isNotNull();
        AssertionsForClassTypes.assertThat(newAuthToken.id()).isNotNull().isNotEmpty();

        Optional<AuthenticationTokenDTO> tokenByID = assertDoesNotThrow(
                () -> authService.getAuthenticationTokenById(
                        newAuthToken.id()
                )
        );
        assertThat(tokenByID.isPresent()).isTrue();
        AssertionsForClassTypes.assertThat(tokenByID.get().id()).isEqualTo(newAuthToken.id());

        Optional<AuthenticationTokenDTO> tokenByName = assertDoesNotThrow(
                () -> authService.getAuthenticationTokenByName(
                        "token-a"
                )
        );
        assertThat(tokenByName.isPresent()).isTrue();
        AssertionsForClassTypes.assertThat(tokenByName.get().id()).isEqualTo(newAuthToken.id());
    }

    @Test
    public void createTokenGetOkGetAllOk() {
        AuthenticationTokenDTO newAuthToken1 = assertDoesNotThrow(
                () -> authService.addNewAuthenticationToken(
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-a")
                                .expiration(LocalDate.of(2023,12,31))
                                .build(),
                        false
                )
        );
        AuthenticationTokenDTO newAuthToken2 = assertDoesNotThrow(
                () -> authService.addNewAuthenticationToken(
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-b")
                                .expiration(LocalDate.of(2023,12,31))
                                .build(),
                        false
                )
        );
        List<AuthenticationTokenDTO> allTokens = assertDoesNotThrow(
                () -> authService.getAllAuthenticationToken()
        );
        assertThat(allTokens)
                .hasSize(2)
                .extracting(AuthenticationTokenDTO::name)
                .containsExactly(
                        "token-a",
                        "token-b"
                );
    }

    /**
     * The deletion of the token delete also the authorization
     */
    @Test
    public void deleteTokenDeleteAlsoAuthorization() {
        AuthenticationTokenDTO newAuthToken1 = assertDoesNotThrow(
                () -> authService.addNewAuthenticationToken(
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-a")
                                .expiration(LocalDate.of(2023,12,31))
                                .build(),
                        false
                )
        );
        // add authorization
        Authorization newAuth = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization
                                .builder()
                                .authorizationType(Read.getValue())
                                .resource("r1")
                                .owner(newAuthToken1.id())
                                .ownerType(AuthorizationOwnerType.Token)
                                .build()
                )
        );
        //delete token
        assertDoesNotThrow(
                () -> authService.deleteToken(
                        newAuthToken1.id()
                )
        );

        // now the authorization should be gone away
        var exists = assertDoesNotThrow(()->authorizationRepository.existsById(newAuth.getId()));
        AssertionsForClassTypes.assertThat(exists).isFalse();
    }

    /**
     * Here is simulated the exception during to delete of the token and in this way the
     * authorization, that are deleted before it will not be erased due to transaction abortion
     */
    @Test
    public void testExceptionOnDeletingTokenNotDeleteAuth(){
        // throw exception during the delete operation of the authentication token
        AuthenticationTokenDTO newAuthToken1 = assertDoesNotThrow(
                () -> authService.addNewAuthenticationToken(
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-a")
                                .expiration(LocalDate.of(2023,12,31))
                                .build(),
                        false
                )
        );
        // add authorization
        Authorization newAuth = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization
                                .builder()
                                .authorizationType(Read.getValue())
                                .resource("r1")
                                .owner(newAuthToken1.email())
                                .ownerType(AuthorizationOwnerType.Token)
                                .build()
                )
        );
        Mockito.doThrow(new RuntimeException()).when(authenticationTokenRepository).deleteById(
                Mockito.any(String.class)
        );
        //delete token
        ControllerLogicException deleteException = assertThrows(
                ControllerLogicException.class,
                () -> authService.deleteToken(
                        newAuthToken1.id()
                )
        );
        AssertionsForClassTypes.assertThat(deleteException.getErrorCode()).isEqualTo(-3);
        // now the authorization should be gone away
        var exists = assertDoesNotThrow(()->authorizationRepository.existsById(newAuth.getId()));
        AssertionsForClassTypes.assertThat(exists).isTrue();
    }

    @Test
    public void testAuthenticationTokenPagination() {
        // create some authentication token to test the search
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            AuthenticationTokenDTO newAuthToken = assertDoesNotThrow(
                    () -> authService.addNewAuthenticationToken(
                            NewAuthenticationTokenDTO
                                    .builder()
                                    .name("token-%02d".formatted(finalI))
                                    .expiration(LocalDate.of(3000,1,1))
                                    .build(),
                            false
                    )
            );
            assertThat(newAuthToken).isNotNull();
        }

        // find first page
        List<AuthenticationTokenDTO> firstPage = assertDoesNotThrow(
                () -> authService.findAllAuthenticationToken(
                        AuthenticationTokenQueryParameterDTO
                                .builder()
                                .limit(5)
                                .build()
                )
        );
        assertThat(firstPage).hasSize(5);
        assertThat(firstPage.get(0).name()).isEqualTo("token-00");
        assertThat(firstPage.get(1).name()).isEqualTo("token-01");
        assertThat(firstPage.get(2).name()).isEqualTo("token-02");
        assertThat(firstPage.get(3).name()).isEqualTo("token-03");
        assertThat(firstPage.get(4).name()).isEqualTo("token-04");

        // find second page
        List<AuthenticationTokenDTO> secondPage = assertDoesNotThrow(
                () -> authService.findAllAuthenticationToken(
                        AuthenticationTokenQueryParameterDTO
                                .builder()
                                .anchor(firstPage.get(4).id())
                                .limit(5)
                                .build()
                )
        );
        assertThat(secondPage).hasSize(5);
        assertThat(secondPage.get(0).name()).isEqualTo("token-05");
        assertThat(secondPage.get(1).name()).isEqualTo("token-06");
        assertThat(secondPage.get(2).name()).isEqualTo("token-07");
        assertThat(secondPage.get(3).name()).isEqualTo("token-08");
        assertThat(secondPage.get(4).name()).isEqualTo("token-09");

        // get previous context
        List<AuthenticationTokenDTO> thirdPage = assertDoesNotThrow(
                () -> authService.findAllAuthenticationToken(
                        AuthenticationTokenQueryParameterDTO
                                .builder()
                                .anchor(secondPage.get(0).id())
                                .context(5)
                                .build()
                )
        );
        assertThat(thirdPage).hasSize(5);
        assertThat(thirdPage.get(0).name()).isEqualTo("token-01");
        assertThat(thirdPage.get(1).name()).isEqualTo("token-02");
        assertThat(thirdPage.get(2).name()).isEqualTo("token-03");
        assertThat(thirdPage.get(3).name()).isEqualTo("token-04");
        assertThat(thirdPage.get(4).name()).isEqualTo("token-05");
    }

    @Test
    public void testAuthenticationTokenPaginationTextSearch() {
        // create some authentication token to test the search
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            AuthenticationTokenDTO newAuthToken = assertDoesNotThrow(
                    () -> authService.addNewAuthenticationToken(
                            NewAuthenticationTokenDTO
                                    .builder()
                                    .name("token-%02d".formatted(finalI))
                                    .expiration(LocalDate.of(3000,1,1))
                                    .build(),
                            false
                    )
            );
            assertThat(newAuthToken).isNotNull();
        }

        List<AuthenticationTokenDTO> thirdPage = assertDoesNotThrow(
                () -> authService.findAllAuthenticationToken(
                        AuthenticationTokenQueryParameterDTO
                                .builder()
                                .searchFilter("01")
                                .limit(5)
                                .build()
                )
        );
        assertThat(thirdPage).hasSize(1);
        assertThat(thirdPage.get(0).name()).isEqualTo("token-01");
    }
}
