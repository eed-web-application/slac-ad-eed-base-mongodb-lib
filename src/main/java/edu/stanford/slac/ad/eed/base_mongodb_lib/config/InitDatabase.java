package edu.stanford.slac.ad.eed.base_mongodb_lib.config;


import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.List;
import java.util.Objects;

@Log4j2
@AllArgsConstructor
@Configuration
@EnableMongoAuditing
@EnableTransactionManagement
@EnableMongoRepositories(basePackages = "edu.stanford.slac.ad.eed.base_mongodb_lib.repository")
public class InitDatabase {
    private final AppProperties appProperties;
    private MongoProperties mongoProperties;
    private final MongoDBProperties mongoDBProperties;

    @Bean
    @Primary
    MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }

    @Bean
    public MongoClient mongoAdmin() {
        ConnectionString adminConnectionString = new ConnectionString(mongoDBProperties.getDbAdminUri());
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(adminConnectionString)
                .applicationName(appProperties.getAppName())
                .build();
        return MongoClients.create(mongoClientSettings);
    }

    @Bean
    public MongoDatabaseFactory mongoDbFactory() {
        ConnectionString connectionString = new ConnectionString(mongoProperties.getUri());

        // ensure database and user
        createApplicationUser(mongoAdmin(), connectionString);

        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();

        return new SimpleMongoClientDatabaseFactory(
                MongoClients.create(mongoClientSettings),
                Objects.requireNonNull(connectionString.getDatabase())
        );
    }

    private void createApplicationUser(MongoClient mongoClient, ConnectionString connectionString) {
        log.info("Start user creation");
        // Connect to the admin database
        MongoDatabase applicationDb = mongoClient.getDatabase(Objects.requireNonNull(connectionString.getDatabase()));

        // Retrieve the list of users
        @SuppressWarnings("unchecked")
        List<Document> users = applicationDb.runCommand(new Document("usersInfo", 1)).get("users", List.class);
        // Check if the desired user exists
        for (Document user : users) {
            if (Objects.equals(connectionString.getUsername(), user.getString("user"))) {
                return;
            }
        }
        // Create user command
        Document createUserCommand = new Document("createUser", Objects.requireNonNull(connectionString.getCredential()).getUserName())
                .append("pwd", new String(Objects.requireNonNull(connectionString.getCredential().getPassword())))
                .append("roles", List.of(
                                new Document("role", "readWrite").append("db", connectionString.getDatabase())
                        )
                );

        // Execute the createUser command
        Document result = applicationDb.runCommand(createUserCommand);
        log.info("User creation result: {}", result);
    }
}