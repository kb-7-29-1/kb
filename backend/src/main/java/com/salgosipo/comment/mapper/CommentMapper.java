package com.salgosipo.comment.mapper;

import com.salgosipo.comment.domain.PropertyCommentVO;
import com.salgosipo.comment.domain.PropertyTagVO;
import com.salgosipo.comment.dto.CommentResponseDTO;
import com.salgosipo.comment.dto.CommentTagResponseDTO;
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

    // 회원 탈퇴 시 댓글 삭제
    int softDeleteByUserId(Long userId);

    // 태그 분석용 댓글 조회
    List<String> findActiveContentsByPropertyId(Long propertyId);

    // 태그 조회
    List<CommentTagResponseDTO> findTagsByPropertyId(Long propertyId);

    // 기존 태그 삭제
    int deleteTagsByPropertyId(Long propertyId);

    // 태그 저장
    int insertPropertyTag(PropertyTagVO tag);
}
