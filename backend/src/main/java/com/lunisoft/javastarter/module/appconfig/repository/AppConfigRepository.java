package com.lunisoft.javastarter.module.appconfig.repository;

import com.lunisoft.javastarter.module.appconfig.entity.AppConfigEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppConfigRepository extends JpaRepository<AppConfigEntity, UUID> {
  Optional<AppConfigEntity> findByKey(String key);
}
