package com.lunisoft.javastarter.module.appconfig.repository;

import com.lunisoft.javastarter.module.appconfig.entity.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppConfigRepository extends JpaRepository<AppConfig, UUID> {
    Optional<AppConfig> findByKey(String key);
}
