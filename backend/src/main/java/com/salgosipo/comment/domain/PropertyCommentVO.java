package com.salgosipo.comment.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PropertyCommentVO {

    private Long commentId;
    private Long propertyId;
    private Long userId;
    private String content;
    private String delYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
