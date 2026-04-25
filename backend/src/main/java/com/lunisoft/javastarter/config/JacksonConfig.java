package com.lunisoft.javastarter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.deser.jdk.StringDeserializer;
import tools.jackson.databind.module.SimpleModule;

/**
 * Configures Jackson to automatically trim all incoming String values during JSON deserialization.
 * Any {@link JacksonModule} bean is automatically registered with the auto-configured JsonMapper.
 */
@Configuration
public class JacksonConfig {

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
