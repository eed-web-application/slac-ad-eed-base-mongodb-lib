package edu.stanford.slac.ad.eed.base_mongodb_lib.repository;

import edu.stanford.slac.ad.eed.base_mongodb_lib.utility.InitAuthorizationIndex;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.model.LocalGroup;
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
public class LocalGroupRepositoryTest {
    @Autowired
    private LocalGroupRepository localGroupRepository;
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
    public void testFindAllUserGroup() {
        assertDoesNotThrow(
                () -> localGroupRepository.save(
                        LocalGroup
                                .builder()
                                .name("test-1")
                                .description("test-1")
                                .members(List.of("user1@slac.stanford.edu"))
                                .build()
                )
        );
        assertDoesNotThrow(
                () -> localGroupRepository.save(
                        LocalGroup
                                .builder()
                                .name("test-2")
                                .description("test-2")
                                .members(List.of("user1@slac.stanford.edu", "user2@slac.stanford.edu"))
                                .build()
                )
        );
        assertDoesNotThrow(
                () -> localGroupRepository.save(
                        LocalGroup
                                .builder()
                                .name("test-3")
                                .description("test-3")
                                .members(List.of("user1@slac.stanford.edu", "user2@slac.stanford.edu", "user3@slac.stanford.edu"))
                                .build()
                )
        );

        // check for "user1@slac.stanford.edu"
        var user1Result = assertDoesNotThrow(
                () -> localGroupRepository.findAllByMembersContains("user1@slac.stanford.edu")
        );
        assertThat(user1Result.size()).isEqualTo(3);
        assertThat(user1Result).extracting(LocalGroup::getName).contains("test-1", "test-2", "test-3");

        // check for "user2@slac.stanford.edu"
        var user2Result = assertDoesNotThrow(
                () -> localGroupRepository.findAllByMembersContains("user2@slac.stanford.edu")
        );
        assertThat(user2Result.size()).isEqualTo(2);
        assertThat(user2Result).extracting(LocalGroup::getName).contains("test-2", "test-3");

        // check for "user3@slac.stanford.edu"
        var user3Result = assertDoesNotThrow(
                () -> localGroupRepository.findAllByMembersContains("user3@slac.stanford.edu")
        );
        assertThat(user3Result.size()).isEqualTo(1);
        assertThat(user3Result).extracting(LocalGroup::getName).contains("test-3");
    }
}
