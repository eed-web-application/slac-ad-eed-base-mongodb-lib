package edu.stanford.slac.ad.eed.base_mongodb_lib.model;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.springframework.data.mongodb.repository.MongoRepository;

@JaversSpringDataAuditable
public interface TestChangeModelPrimitiveRepository extends MongoRepository<TestChangeModelPrimitive, String> {
}
