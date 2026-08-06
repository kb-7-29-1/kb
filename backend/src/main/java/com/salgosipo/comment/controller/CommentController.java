package com.salgosipo.comment.controller;

import com.salgosipo.comment.dto.CommentRequestDTO;
import com.salgosipo.comment.dto.CommentResponseDTO;
import com.salgosipo.comment.dto.CommentTagResponseDTO;
import com.salgosipo.comment.service.CommentService;
import com.salgosipo.global.security.account.domain.CustomUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/properties/{propertyId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // 댓글 조회
    @GetMapping
    public ResponseEntity<List<CommentResponseDTO>> getComments(
            @PathVariable Long propertyId,
            @AuthenticationPrincipal CustomUser customUser
    ) {
        return ResponseEntity.ok(
                commentService.getCommentsByPropertyId(
                        propertyId,
                        customUser == null ? null : customUser.getUsername()
                )
        );
    }

    // 태그 조회
    @GetMapping("/tags")
    public ResponseEntity<List<CommentTagResponseDTO>> getTags(@PathVariable Long propertyId) {
        return ResponseEntity.ok(commentService.getTagsByPropertyId(propertyId));
    }

    // 댓글 작성
    @PostMapping
    public ResponseEntity<Void> createComment(
            @PathVariable Long propertyId,
            @AuthenticationPrincipal CustomUser customUser,
            @RequestBody CommentRequestDTO request
    ) {
        commentService.createComment(propertyId, customUser.getUsername(), request);
        return ResponseEntity.ok().build();
    }

    // 댓글 수정
    @PatchMapping("/{commentId}")
    public ResponseEntity<Void> updateComment(
            @PathVariable Long propertyId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUser customUser,
            @RequestBody CommentRequestDTO request
    ) {
        commentService.updateComment(propertyId, commentId, customUser.getUsername(), request);
        return ResponseEntity.ok().build();
    }

    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long propertyId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUser customUser
    ) {
        commentService.deleteComment(propertyId, commentId, customUser.getUsername());
        return ResponseEntity.noContent().build();
    }
}
