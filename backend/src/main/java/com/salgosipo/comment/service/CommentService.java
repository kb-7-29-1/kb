package com.salgosipo.comment.service;

import com.salgosipo.comment.dto.CommentResponseDTO;
import com.salgosipo.comment.dto.CommentRequestDTO;
import com.salgosipo.comment.dto.CommentTagResponseDTO;

import java.util.List;

public interface CommentService {

    List<CommentResponseDTO> getCommentsByPropertyId(Long propertyId, String loginId);

    void createComment(Long propertyId, String loginId, CommentRequestDTO request);

    void updateComment(Long propertyId, Long commentId, String loginId, CommentRequestDTO request);

    void deleteComment(Long propertyId, Long commentId, String loginId);

    List<CommentTagResponseDTO> getTagsByPropertyId(Long propertyId);
}
