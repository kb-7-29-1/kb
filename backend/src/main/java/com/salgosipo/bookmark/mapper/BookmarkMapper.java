package com.salgosipo.bookmark.mapper;

import com.salgosipo.bookmark.dto.BookmarkResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BookmarkMapper {
    void addBookmark(@Param("userId") Long userId, @Param("propertyId") Long propertyId);

    void removeBookmark(@Param("userId") Long userId, @Param("propertyId") Long propertyId);

    int countByUserIdAndPropertyId(@Param("userId") Long userId,@Param("propertyId") Long propertyId);

    List<BookmarkResponseDto> findBookmarksByUserId(Long userId);
}
