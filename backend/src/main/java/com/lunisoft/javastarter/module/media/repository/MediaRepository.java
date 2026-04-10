package com.lunisoft.javastarter.module.media.repository;

import com.lunisoft.javastarter.module.media.entity.Media;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<Media, UUID> {}
