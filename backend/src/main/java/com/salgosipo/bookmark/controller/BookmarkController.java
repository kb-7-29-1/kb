package com.salgosipo.bookmark.controller;

import com.salgosipo.bookmark.dto.BookmarkRequestDto;
import com.salgosipo.bookmark.dto.BookmarkResponseDto;
import com.salgosipo.bookmark.service.BookmarkService;
import com.salgosipo.global.security.account.domain.CustomUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookmark")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @PostMapping
    public ResponseEntity<?> addBookmark(@AuthenticationPrincipal CustomUser customUser,
                                         @RequestBody BookmarkRequestDto dto){
        bookmarkService.addBookmark(customUser.getUsername(),dto.getPropertyId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{propertyId}")
    public ResponseEntity<?> removeBookmark(@AuthenticationPrincipal CustomUser customUser,
                                            @PathVariable Long propertyId){
        bookmarkService.removeBookmark(customUser.getUsername(), propertyId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<?> getBookmarks(@AuthenticationPrincipal CustomUser customUser){
        List<BookmarkResponseDto> bookmarks = bookmarkService.getBookmarks(customUser.getUsername());
        return ResponseEntity.ok(bookmarks);
    }
}