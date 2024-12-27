package org.apereo.cas.services;

import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.RandomUtils;
import org.apereo.cas.util.serialization.JacksonObjectMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This is {@link AttributeBasedRegisteredServiceAccessStrategyActivationCriteriaTests}.
 *
 * @author Misagh Moayyed
 * @since 7.0.0
 */
@Tag("RegisteredService")
class AttributeBasedRegisteredServiceAccessStrategyActivationCriteriaTests {
    private static final ObjectMapper MAPPER = JacksonObjectMapperFactory.builder()
        .defaultTypingEnabled(true).build().toObjectMapper();


    @Test
    void verifySerializeToJson() throws Throwable {
        val criteria = new AttributeBasedRegisteredServiceAccessStrategyActivationCriteria()
            .setOperator(LogicalOperatorTypes.OR)
            .setAllowIfInactive(true)
            .setRequiredAttributes(Map.of("common-name", List.of("n@m3"), "cn", List.of("***")));
        val jsonFile = Files.createTempFile(RandomUtils.randomAlphabetic(8), ".json").toFile();
        MAPPER.writeValue(jsonFile, criteria);
        val policyRead = MAPPER.readValue(jsonFile, RegisteredServiceAccessStrategyActivationCriteria.class);
        assertEquals(criteria, policyRead);
    }

    @Test
    void verifyRequiredPlain() {
        val request = RegisteredServiceAccessStrategyRequest.builder()
            .principalId("casuser")
            .attributes(CollectionUtils.wrap("cn", List.of("name1")))
            .build();
        val criteria = new AttributeBasedRegisteredServiceAccessStrategyActivationCriteria()
            .setOperator(LogicalOperatorTypes.AND)
            .setAllowIfInactive(true)
            .setRequiredAttributes(Map.of("cn", List.of("name1", "name2")));
        assertTrue(criteria.isAllowIfInactive());
        assertTrue(criteria.shouldActivate(request));
    }

    @Test
    void verifyRequiredRegex() {
        val request = RegisteredServiceAccessStrategyRequest.builder()
            .principalId("casuser")
            .attributes(CollectionUtils.wrap("cn", List.of("***")))
            .build();
        val criteria = new AttributeBasedRegisteredServiceAccessStrategyActivationCriteria()
            .setOperator(LogicalOperatorTypes.OR)
            .setAllowIfInactive(true)
            .setRequiredAttributes(Map.of("common-name", List.of("n@m3"), "cn", List.of("***")));
        assertTrue(criteria.shouldActivate(request));
    }
}
