package com.salgosipo.comment.domain;

import lombok.Data;

@Data
public class PropertyTagVO {

    private Long propertyId;
    private Integer tagType;
    private Integer tagCount;
}
