package com.lunisoft.javastarter.module.media.mapper;

import com.lunisoft.javastarter.module.media.entity.Media;
import java.util.UUID;

/**
 * Lightweight projection of a {@link Media} used when embedding media in other resources.
 */
public record MediaSummaryView(UUID id, String fileName, String key, String mimeType, String previewUrl) {}
