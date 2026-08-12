package com.salgosipo.comment.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentResponseDTO {

    private Long commentId;

    private Long propertyId;

    private String propertyAddress;

    private Long userId;

    private String nickname;

    private String profileImage;

    private String content;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Boolean isMine;
}
