package edu.stanford.slac.ad.eed.base_mongodb_lib.utility;

import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;


@Log4j2
@Builder
@AllArgsConstructor
public class RootAuthorizationManagement extends MongoDDLOps {
    private final AuthService authService;

    public void updateRootAuthorization() {
        try {
            log.info("Start managing root user");
            authService.updateRootUser();
        } catch (RuntimeException ex) {
            log.error("Error during root user management: {}", ex.toString());
        }
        try {
            log.info("Start managing root authentication token");
            authService.updateAutoManagedRootToken();
        } catch (RuntimeException ex) {
            log.error("Error during root token management: {}", ex.toString());
        }
    }
}
