package com.salgosipo.comment.controller;

import com.salgosipo.comment.dto.CommentResponseDTO;
import com.salgosipo.comment.service.CommentService;
import com.salgosipo.global.security.account.domain.CustomUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class MyCommentController {

    private final CommentService commentService;

    @GetMapping("/me")
    public ResponseEntity<List<CommentResponseDTO>> getMyComments(
            @AuthenticationPrincipal CustomUser customUser
    ) {
        return ResponseEntity.ok(commentService.getMyComments(customUser.getUsername()));
    }
}
