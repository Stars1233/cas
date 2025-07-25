package org.apereo.cas.metadata;

import org.apereo.cas.configuration.support.DurationCapable;
import org.apereo.cas.configuration.support.ExpressionLanguageCapable;
import org.apereo.cas.configuration.support.PropertyOwner;
import org.apereo.cas.configuration.support.RegularExpressionCapable;
import org.apereo.cas.configuration.support.RequiredProperty;
import org.apereo.cas.configuration.support.RequiresModule;
import org.apereo.cas.util.function.FunctionUtils;
import org.apereo.cas.util.serialization.JacksonObjectMapperFactory;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.lambda.Unchecked;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.util.ReflectionUtils;
import java.io.File;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * This is {@link CasConfigurationMetadataCatalog}.
 *
 * @author Misagh Moayyed
 * @since 6.3.0
 */
@UtilityClass
public class CasConfigurationMetadataCatalog {

    private static final ObjectMapper MAPPER = JacksonObjectMapperFactory.builder()
        .defaultTypingEnabled(false).build().toObjectMapper();

    public static ObjectMapper getObjectMapper() {
        return MAPPER;
    }

    /**
     * Export.
     *
     * @param destination the destination
     * @param data        the data
     */
    public static void export(final File destination, final Object data) {
        FunctionUtils.doUnchecked(__ -> {
            val mapper = new ObjectMapper(new YAMLFactory())
                .setSerializationInclusion(JsonInclude.Include.NON_DEFAULT)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
                .configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.writeValue(destination, data);
        });
    }

    /**
     * Catalog cas properties container.
     *
     * @param query the query
     * @return the cas properties container
     */
    public static CasPropertiesContainer query(final ConfigurationMetadataCatalogQuery query) {
        return query(query, new CasConfigurationMetadataRepository());
    }

    /**
     * Query cas properties.
     *
     * @param query the query
     * @param repo  the repo
     * @return the cas properties container
     */
    public static CasPropertiesContainer query(final ConfigurationMetadataCatalogQuery query,
                                               final CasConfigurationMetadataRepository repo) {
        val allProperties = repo.getRepository()
            .getAllProperties()
            .entrySet()
            .stream()
            .filter(entry -> {
                if (query.getQueryType() == ConfigurationMetadataCatalogQuery.QueryTypes.CAS) {
                    return CasConfigurationMetadataRepository.isCasProperty(entry.getValue());
                }
                if (query.getQueryType() == ConfigurationMetadataCatalogQuery.QueryTypes.THIRD_PARTY) {
                    return !CasConfigurationMetadataRepository.isCasProperty(entry.getValue());
                }
                return true;
            })
            .filter(entry -> query.getQueryFilter().test(entry.getValue())).toList();

        val properties = allProperties
            .stream()
            .filter(entry -> doesPropertyBelongToModule(entry.getValue(), query))
            .map(Map.Entry::getValue)
            .map(property -> collectReferenceProperty(property, repo.getRepository()))
            .filter(Objects::nonNull)
            .filter(property -> query.getRequiredPropertiesOnly() == null || query.getRequiredPropertiesOnly().equals(property.isRequired()))
            .sorted(Comparator.comparing(CasReferenceProperty::getName))
            .collect(Collectors.toCollection(TreeSet::new));
        return new CasPropertiesContainer(properties);
    }

    private static boolean doesPropertyBelongToModule(final ConfigurationMetadataProperty property,
                                                      final ConfigurationMetadataCatalogQuery query) {
        if (query.getModules().isEmpty()) {
            return true;
        }

        val valueHints = property.getHints().getValueHints();
        return valueHints
            .stream()
            .filter(hint -> StringUtils.isNotBlank(hint.getDescription()))
            .filter(hint -> hint.getDescription().equals(RequiresModule.class.getName()))
            .anyMatch(hint -> {
                val results = reasonJsonValueAsMap(hint.getValue().toString());
                val module = results.get("module").toString();
                return query.getModules().contains(module);
            });
    }

    private static Map reasonJsonValueAsMap(final String value) {
        return FunctionUtils.doUnchecked(() -> MAPPER.readValue(value, Map.class));
    }

    private static CasReferenceProperty collectReferenceProperty(final ConfigurationMetadataProperty property,
                                                                 final ConfigurationMetadataRepository repository) {
        if (repository.getAllGroups().containsKey(property.getId())) {
            return null;
        }

        val builder = CasReferenceProperty.builder();
        builder.owner(determinePropertySourceType(property));

        property.getHints().getValueHints().forEach(Unchecked.consumer(hint -> {
            val description = hint.getDescription();
            if (StringUtils.isNotBlank(description)) {
                if (description.equals(RequiredProperty.class.getName())) {
                    builder.required(true);
                }
                if (description.equals(RequiresModule.class.getName())) {
                    val results = MAPPER.readValue(hint.getValue().toString(), Map.class);
                    builder.module(results.get("module").toString());
                }
                if (description.equals(PropertyOwner.class.getName())) {
                    val results = MAPPER.readValue(hint.getValue().toString(), Map.class);
                    builder.owner(results.get("owner").toString());
                }
                if (description.equals(DurationCapable.class.getName())) {
                    builder.duration(true);
                }
                if (description.equals(ExpressionLanguageCapable.class.getName())) {
                    builder.expressionLanguage(true);
                }
                if (description.equals(RegularExpressionCapable.class.getName())) {
                    builder.regexPattern(true);
                }
            }
        }));
        builder.type(property.getType());
        val description = StringUtils.defaultString(
            StringUtils.defaultIfBlank(property.getDescription(), property.getShortDescription()));
        builder.description(description);
        builder.shortDescription(property.getShortDescription());
        builder.name(property.getId());
        builder.defaultValue(ObjectUtils.getIfNull(property.getDefaultValue(), StringUtils.EMPTY));
        if (property.isDeprecated()) {
            val deprecation = property.getDeprecation();
            builder.deprecationLevel(deprecation.getLevel().toString());
            if (deprecation.getShortReason() != null) {
                builder.deprecationReason(deprecation.getShortReason());
            }
            if (deprecation.getReplacement() != null) {
                builder.deprecationReplacement(deprecation.getReplacement());
            }
        }
        return builder.build();
    }

    private static String determinePropertySourceType(final ConfigurationMetadataProperty property) {
        return FunctionUtils.doUnchecked(() -> {
            val method = ReflectionUtils.findMethod(property.getClass(), "getSourceType");
            if (method == null) {
                return null;
            }
            method.setAccessible(true);
            return (String) method.invoke(property);
        });
    }

    /**
     * The type Cas properties container.
     */
    public record CasPropertiesContainer(TreeSet<CasReferenceProperty> properties) {
    }
}
