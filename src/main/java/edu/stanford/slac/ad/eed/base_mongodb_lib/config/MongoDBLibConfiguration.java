package edu.stanford.slac.ad.eed.base_mongodb_lib.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MongoDBProperties.class)
public class MongoDBLibConfiguration {
}
