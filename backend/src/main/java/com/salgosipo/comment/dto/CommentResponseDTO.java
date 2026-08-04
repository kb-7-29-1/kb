package com.salgosipo.comment.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentResponseDTO {

    private Long commentId;

    private Long propertyId;

    private Long userId;

    private String nickname;

    private String content;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Boolean isMine;
}
