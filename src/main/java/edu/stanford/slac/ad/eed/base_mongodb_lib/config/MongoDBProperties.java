package edu.stanford.slac.ad.eed.base_mongodb_lib.config;


import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Log4j2
@Getter
@Setter
@ConfigurationProperties(prefix = "edu.stanford.slac.mongodb")
public class MongoDBProperties {
    // the administrator uri for setup the mongodb user and database for the applicaition
    private String dbAdminUri;
}
