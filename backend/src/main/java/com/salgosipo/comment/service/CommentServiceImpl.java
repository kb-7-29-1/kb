package com.salgosipo.comment.service;

import com.salgosipo.comment.domain.PropertyCommentVO;
import com.salgosipo.comment.dto.CommentResponseDTO;
import com.salgosipo.comment.dto.CommentRequestDTO;
import com.salgosipo.comment.dto.CommentTagResponseDTO;
import com.salgosipo.comment.exception.CommentNotFoundException;
import com.salgosipo.comment.mapper.CommentMapper;
import com.salgosipo.user.domain.UserVO;
import com.salgosipo.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final CommentTagAnalysisService commentTagAnalysisService;

    @Override
    public List<CommentResponseDTO> getCommentsByPropertyId(Long propertyId, String loginId) {
        return commentMapper.findByPropertyId(createCommentVO(propertyId, null, loginId, null));
    }

    @Override
    @Transactional
    public void createComment(Long propertyId, String loginId, CommentRequestDTO request) {
        commentMapper.insertComment(createCommentVO(propertyId, null, loginId, getContent(request)));
        commentTagAnalysisService.refreshPropertyTags(propertyId);
    }

    @Override
    @Transactional
    public void updateComment(Long propertyId, Long commentId, String loginId, CommentRequestDTO request) {
        if (commentMapper.updateComment(
                createCommentVO(propertyId, commentId, loginId, getContent(request))
        ) == 0) {
            throw new CommentNotFoundException("수정할 댓글이 없습니다.");
        }
        commentTagAnalysisService.refreshPropertyTags(propertyId);
    }

    @Override
    @Transactional
    public void deleteComment(Long propertyId, Long commentId, String loginId) {
        if (commentMapper.deleteComment(createCommentVO(propertyId, commentId, loginId, null)) == 0) {
            throw new IllegalArgumentException("삭제할 댓글이 없습니다.");
        }
        commentTagAnalysisService.refreshPropertyTags(propertyId);
    }

    @Override
    public List<CommentTagResponseDTO> getTagsByPropertyId(Long propertyId) {
        // 기존에 작성된 댓글도 첫 조회부터 분석 결과에 포함한다.
        commentTagAnalysisService.refreshPropertyTags(propertyId);
        return commentMapper.findTagsByPropertyId(propertyId);
    }

    private Long getUserId(String loginId) {
        UserVO user = userMapper.findByLoginId(loginId);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
        return user.getUserId();
    }

    private PropertyCommentVO createCommentVO(
            Long propertyId,
            Long commentId,
            String loginId,
            String content
    ) {
        PropertyCommentVO comment = new PropertyCommentVO();
        comment.setPropertyId(propertyId);
        comment.setCommentId(commentId);
        if (loginId != null) {
            comment.setUserId(getUserId(loginId));
        }
        comment.setContent(content);
        return comment;
    }

    private String getContent(CommentRequestDTO request) {
        String content = request == null ? null : request.getContent();
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("댓글 내용을 입력해주세요.");
        }
        if (content.length() > 255) {
            throw new IllegalArgumentException("댓글은 255자 이하로 입력해주세요.");
        }
        return content.trim();
    }
}
