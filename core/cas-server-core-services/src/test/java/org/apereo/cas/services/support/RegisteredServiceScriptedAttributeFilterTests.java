package org.apereo.cas.services.support;

import org.apereo.cas.util.serialization.JacksonObjectMapperFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This is {@link RegisteredServiceScriptedAttributeFilterTests}.
 *
 * @author Misagh Moayyed
 * @since 5.3.0
 */
@Tag("GroovyServices")
class RegisteredServiceScriptedAttributeFilterTests {
    private static final ObjectMapper MAPPER = JacksonObjectMapperFactory.builder()
        .defaultTypingEnabled(true).build().toObjectMapper();

    private Map<String, List<Object>> givenAttributesMap;

    @BeforeEach
    void initialize() {
        this.givenAttributesMap = new HashMap<>();
        this.givenAttributesMap.put("employeeId", List.of("E1234"));
        this.givenAttributesMap.put("memberOf", Arrays.asList("math101", "science", "chemistry", "marathon101"));
    }

    @Test
    void verifyScriptedAttributeFilter() throws Throwable {
        val f = Files.createTempFile("attr", ".groovy").toFile();
        val stream = new ClassPathResource("groovy-attr-filter.groovy").getInputStream();
        FileUtils.copyInputStreamToFile(stream, f);
        val filter = new RegisteredServiceScriptedAttributeFilter(0, "file:" + f.getCanonicalPath());
        val results = filter.filter(this.givenAttributesMap);
        assertEquals(3, results.size());
        val file = new File(FileUtils.getTempDirectoryPath(), "verifyScriptedAttributeFilter.json");
        MAPPER.writeValue(file, filter);
        val read = MAPPER.readValue(file, RegisteredServiceScriptedAttributeFilter.class);
        assertEquals(filter, read);
    }

    @Test
    void verifyScriptedAttributeFilterInlined() throws Throwable {
        val filter = new RegisteredServiceScriptedAttributeFilter(0, "groovy {logger.debug('exec'); return attributes;}");
        val results = filter.filter(this.givenAttributesMap);
        assertEquals(2, results.size());

        val file = new File(FileUtils.getTempDirectoryPath(), "verifyScriptedAttributeFilterInlined.json");
        MAPPER.writeValue(file, filter);
        val read = MAPPER.readValue(file, RegisteredServiceScriptedAttributeFilter.class);
        assertEquals(filter, read);
    }
}
