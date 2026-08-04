package com.salgosipo.comment.controller;

import com.salgosipo.comment.dto.CommentRequestDTO;
import com.salgosipo.comment.dto.CommentResponseDTO;
import com.salgosipo.comment.service.CommentService;
import com.salgosipo.global.security.account.domain.CustomUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/properties/{propertyId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

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

    @PostMapping
    public ResponseEntity<Void> createComment(
            @PathVariable Long propertyId,
            @AuthenticationPrincipal CustomUser customUser,
            @RequestBody CommentRequestDTO request
    ) {
        commentService.createComment(propertyId, customUser.getUsername(), request);
        return ResponseEntity.ok().build();
    }

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
