package com.lunisoft.ultimatejavastarterkit.module.appconfig.repository;

import com.lunisoft.ultimatejavastarterkit.module.appconfig.entity.AppConfigEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppConfigRepository extends JpaRepository<AppConfigEntity, UUID> {
  Optional<AppConfigEntity> findByKey(String key);
}
