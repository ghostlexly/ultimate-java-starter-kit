package com.lunisoft.javastarter.module.media.usecase;

import java.io.InputStream;

/**
 * Generic input for {@link UploadMediaUseCase}. Decouples the use case from {@code MultipartFile}
 * so any module can upload a file (e.g. an in-memory byte array wrapped in a {@code
 * ByteArrayInputStream}, an S3 download stream, etc.).
 *
 * <p>The caller is responsible for closing the {@code inputStream} and for performing any business
 * validation (mime type, size limits, ...). {@code size} must be the exact byte length of the
 * stream — S3 requires it for streamed uploads.
 */
public record UploadMediaInput(
    InputStream inputStream, String fileName, String mimeType, long size) {}
