package com.lunisoft.ultimatejavastarterkit.repository;

import com.lunisoft.ultimatejavastarterkit.entity.AppConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppConfigRepository extends JpaRepository<AppConfigEntity, UUID> {
  Optional<AppConfigEntity> findByKey(String key);
}
