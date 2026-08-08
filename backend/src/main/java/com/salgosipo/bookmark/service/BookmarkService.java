package com.salgosipo.bookmark.service;

import com.salgosipo.bookmark.dto.BookmarkResponseDto;
import com.salgosipo.bookmark.mapper.BookmarkMapper;
import com.salgosipo.user.mapper.UserMapper;
import com.salgosipo.user.domain.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkMapper bookmarkMapper;

    private final UserMapper userMapper;

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "propertyList", allEntries = true),
        @CacheEvict(value = "propertyDetail", allEntries = true)
    })
    public void addBookmark(String loginId, Long propertyId){
        Long userId = getUserId(loginId);
        if(bookmarkMapper.countByUserIdAndPropertyId(userId, propertyId)>0){
            throw new IllegalArgumentException("이미 찜한 매물입니다.");
        }
        bookmarkMapper.addBookmark(userId,propertyId);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "propertyList", allEntries = true),
        @CacheEvict(value = "propertyDetail", allEntries = true)
    })
    public void removeBookmark(String loginId, Long propertyId){
        Long userId = getUserId(loginId);
        bookmarkMapper.removeBookmark(userId,propertyId);
    }

    public List<BookmarkResponseDto> getBookmarks(String loginId){
        Long userId = getUserId(loginId);
        return bookmarkMapper.findBookmarksByUserId(userId);
    }

    private Long getUserId(String loginId) {
        UserVO vo = userMapper.findByLoginId(loginId);
        if (vo == null) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }
        return vo.getUserId();
    }
}