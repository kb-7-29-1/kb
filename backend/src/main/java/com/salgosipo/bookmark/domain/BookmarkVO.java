package com.salgosipo.bookmark.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookmarkVO {
    private Long bookmarkId;
    private Long userId;
    private Long propertyId;
    private Date createAt;
}