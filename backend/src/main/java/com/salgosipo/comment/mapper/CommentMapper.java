package com.salgosipo.comment.mapper;

import com.salgosipo.comment.domain.PropertyCommentVO;
import com.salgosipo.comment.dto.CommentResponseDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CommentMapper {

    // 댓글 목록 조회
    List<CommentResponseDTO> findByPropertyId(PropertyCommentVO comment);

    // 댓글 등록
    int insertComment(PropertyCommentVO comment);

    // 댓글 수정
    int updateComment(PropertyCommentVO comment);

    // 댓글 삭제
    int deleteComment(PropertyCommentVO comment);

    // 회원 탈퇴 시 작성 댓글 soft delete
    int softDeleteByUserId(Long userId);
}
