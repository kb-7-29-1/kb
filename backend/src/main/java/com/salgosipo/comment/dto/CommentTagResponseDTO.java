package com.salgosipo.comment.dto;

import lombok.Data;

@Data
public class CommentTagResponseDTO {

    private Integer tagType;
    private String tagName;
    private String type;
    private Integer count;
}
