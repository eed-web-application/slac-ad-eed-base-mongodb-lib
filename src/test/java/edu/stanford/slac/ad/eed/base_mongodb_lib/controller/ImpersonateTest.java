package edu.stanford.slac.ad.eed.base_mongodb_lib.controller;

import edu.stanford.slac.ad.eed.base_mongodb_lib.repository.AuthenticationTokenRepository;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthenticationTokenDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.NewAuthenticationTokenDTO;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.NewLocalGroupDTO;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.exception.AuthenticationTokenMalformed;
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
public class ImpersonateTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AppProperties appProperties;
    @Autowired
    private AuthService authService;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    AuthenticationTokenRepository authenticationTokenRepository;
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
        appProperties.getRootUserList().add("user2@slac.stanford.edu");
        authService.updateRootUser();
    }
    @Test
    public void checkImpersonatingOnCreateBy() throws Exception {
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

        assertThat(authToken).isNotNull();

        var foundAuthenticationTokenModel = authenticationTokenRepository.findById(authToken.getPayload().id());
        assertThat(foundAuthenticationTokenModel).isPresent();
        assertThat(foundAuthenticationTokenModel.get().getCreatedBy()).isEqualTo("user1@slac.stanford.edu");

        // now create another token impersonating user2
        authToken = assertDoesNotThrow(
                () -> testControllerHelperService.createNewAuthenticationToken(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.of("user2@slac.stanford.edu"),
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-b")
                                .expiration(LocalDate.of(2023,12,31))
                                .build()
                )
        );

        assertThat(authToken).isNotNull();
        foundAuthenticationTokenModel = authenticationTokenRepository.findById(authToken.getPayload().id());
        assertThat(foundAuthenticationTokenModel).isPresent();
        assertThat(foundAuthenticationTokenModel.get().getCreatedBy()).isEqualTo("user2@slac.stanford.edu");

        // now impersonating user3 the operation fails wth not authorized
        var notAuthorizedException = assertThrows(
                NotAuthorized.class,
                () -> testControllerHelperService.createNewAuthenticationToken(
                        mockMvc,
                        status().is4xxClientError(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.of("user3@slac.stanford.edu"),
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-b")
                                .expiration(LocalDate.of(2023,12,31))
                                .build()
                )
        );

        assertThat(notAuthorizedException).isNotNull();
    }
}
