package com.lunisoft.javastarter.module.media.mapper;

import com.lunisoft.javastarter.core.storage.S3Service;
import com.lunisoft.javastarter.module.media.entity.Media;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MediaSummaryViewMapper {

    private final S3Service s3Service;

    public MediaSummaryView toView(Media media) {
        String previewUrl = s3Service.generatePresignedGetUrl(media.getKey());

        return new MediaSummaryView(
                media.getId(), media.getFileName(), media.getKey(), media.getMimeType(), previewUrl);
    }

    public @Nullable MediaSummaryView toViewOrNull(@Nullable Media media) {
        if (media == null) {
            return null;
        }

        return toView(media);
    }
}
