package com.lunisoft.ultimatejavastarterkit.module.media.repository;

import com.lunisoft.ultimatejavastarterkit.module.media.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MediaRepository extends JpaRepository<Media, UUID> {}
