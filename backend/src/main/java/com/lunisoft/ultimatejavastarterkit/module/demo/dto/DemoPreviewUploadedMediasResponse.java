package com.lunisoft.ultimatejavastarterkit.module.demo.dto;

import java.util.UUID;

public record DemoPreviewUploadedMediasResponse(
    UUID id, String fileName, String key, String mimeType, String previewUrl) {}
