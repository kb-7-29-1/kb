package com.salgosipo.bookmark.service;

import com.salgosipo.bookmark.dto.BookmarkResponseDto;
import com.salgosipo.bookmark.mapper.BookmarkMapper;
import com.salgosipo.user.mapper.UserMapper;
import com.salgosipo.user.domain.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
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
        try {
            bookmarkMapper.addBookmark(userId,propertyId);
        } catch (DataIntegrityViolationException e) {
            // 연속 클릭 등으로 두 요청이 거의 동시에 들어와 중복 체크를 함께 통과한 경우,
            // bookmarks 테이블의 PK(user_id, property_id) 제약으로 두 번째 INSERT가 여기서 걸림
            throw new IllegalArgumentException("이미 찜한 매물입니다.");
        }
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