package edu.stanford.slac.ad.eed.base_mongodb_lib.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@TestConfiguration
@EnableMongoRepositories(basePackages = "edu.stanford.slac.ad.eed.base_mongodb_lib.model")
@ComponentScan(basePackages = "edu.stanford.slac.ad.eed")
public class TestConfig {
}