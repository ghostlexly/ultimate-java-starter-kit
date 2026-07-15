package com.lunisoft.javastarter.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.deser.jdk.StringDeserializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.type.LogicalType;

/**
 * Configures Jackson to automatically trim all incoming String values during JSON deserialization.
 * Any {@link JacksonModule} bean is automatically registered with the auto-configured JsonMapper.
 */
@Configuration
public class JacksonConfig {

    /**
     * Coerces blank JSON strings (e.g. an unselected form field sending "") to null for every enum
     * field, so the field-level Bean Validation message (e.g. {@code @NotNull}) fires instead of a
     * generic Jackson deserialization error. Unknown non-blank values still fail deserialization.
     */
    @Bean
    public JsonMapperBuilderCustomizer enumBlankStringAsNullCustomizer() {
        return builder -> builder.withCoercionConfig(
                LogicalType.Enum,
                config -> config.setAcceptBlankAsEmpty(true)
                        .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull));
    }

    @Bean
    public JacksonModule trimStringModule() {
        SimpleModule module = new SimpleModule("TrimStringModule");
        module.addDeserializer(String.class, new TrimStringDeserializer());

        return module;
    }

    /** Custom String deserializer that trims whitespace and converts blank strings to null. */
    private static class TrimStringDeserializer extends StringDeserializer {
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) {
            String value = p.getString();

            return value == null ? null : value.trim();
        }
    }
}
