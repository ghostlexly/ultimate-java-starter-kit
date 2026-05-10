package com.lunisoft.javastarter.module.appconfig.repository;

import com.lunisoft.javastarter.module.appconfig.entity.AppConfig;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppConfigRepository extends JpaRepository<AppConfig, UUID> {
  Optional<AppConfig> findByKey(String key);
}
