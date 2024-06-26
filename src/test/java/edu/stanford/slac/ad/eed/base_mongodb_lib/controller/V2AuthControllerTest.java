package edu.stanford.slac.ad.eed.base_mongodb_lib.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.PersonDTO;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.AuthorizationGroupManagementDTO;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.UserGroupManagementAuthorizationLevel;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.model.AuthenticationToken;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
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
}
