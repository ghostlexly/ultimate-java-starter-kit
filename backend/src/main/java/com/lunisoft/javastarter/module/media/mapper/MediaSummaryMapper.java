package com.lunisoft.javastarter.module.media.mapper;

import com.lunisoft.javastarter.core.storage.S3Service;
import com.lunisoft.javastarter.module.media.entity.Media;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class MediaSummaryMapper {

    private final S3Service s3Service;

    public MediaSummaryView toView(Media media) {
        String previewUrl = s3Service.generatePresignedGetUrl(media.getKey(), Duration.ofHours(1));

        return new MediaSummaryView(
                media.getId(), media.getFileName(), media.getKey(), media.getMimeType(), previewUrl);
    }
}
