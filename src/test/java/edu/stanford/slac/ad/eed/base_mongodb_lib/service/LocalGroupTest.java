package edu.stanford.slac.ad.eed.base_mongodb_lib.service;

import edu.stanford.slac.ad.eed.base_mongodb_lib.repository.AuthenticationTokenRepository;
import edu.stanford.slac.ad.eed.base_mongodb_lib.repository.AuthorizationRepository;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.LocalGroupQueryParameterDTO;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.NewLocalGroupDTO;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.model.LocalGroup;
import edu.stanford.slac.ad.eed.baselib.model.LocalGroupQueryParameter;
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
import org.springframework.security.core.Authentication;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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
    }

    @Test
    public void testAssignUserToManageGroup() {
        assertDoesNotThrow(
                ()->authService.authorizeUserIdToManageGroup("user1@slac.stanford.edu")
        );

        var iAuthorized = assertDoesNotThrow(
                ()->authService.canManageGroup(new UsernamePasswordAuthenticationToken("user1@slac.stanford.edu","") )
        );
        assertThat(iAuthorized).isTrue();

        assertDoesNotThrow(
                ()->authService.removeAuthorizationToUserIdToManageGroup("user1@slac.stanford.edu")
        );

        iAuthorized = assertDoesNotThrow(
                ()->authService.canManageGroup(new UsernamePasswordAuthenticationToken("user1@slac.stanford.edu","") )
        );
        assertThat(iAuthorized).isFalse();
    }

    @Test
    public void createGroup(){
        String groupId = assertDoesNotThrow(
                ()->authService.createLocalGroup(
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
                ()->authService.findLocalGroup(
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
}