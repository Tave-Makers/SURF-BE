package com.tavemakers.surf.domain.post.mapper;

import com.tavemakers.surf.domain.post.dto.PostViewUpdateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class PostMapper {

    private static final String KEY_DELIMITER = ":";
    private static final int POST_ID_INDEX = 1;

    public PostViewUpdateDto toUpdateDto(String key, String value) {
        try {
            Long postId = extractPostId(key);
            int viewCountDelta = Integer.parseInt(value);
            return new PostViewUpdateDto(postId, viewCountDelta);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            log.warn("유효하지 않은 Key, Value 형식입니다. key={}, value={}", key, value);
            return null;
        }
    }

    private Long extractPostId(String key) {
        return Long.parseLong(key.split(KEY_DELIMITER)[POST_ID_INDEX]);
    }

}
