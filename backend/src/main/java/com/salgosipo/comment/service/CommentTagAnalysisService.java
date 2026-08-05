package com.salgosipo.comment.service;

import com.salgosipo.comment.domain.PropertyTagVO;
import com.salgosipo.comment.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentTagAnalysisService {

    private static final Map<Integer, List<String>> TAG_KEYWORDS = new LinkedHashMap<>();

    static {
        TAG_KEYWORDS.put(1, List.of("현관보안", "공동현관", "도어락", "출입문 잠금", "보안 좋"));
        TAG_KEYWORDS.put(2, List.of("귀갓길 밝", "밤길 밝", "가로등", "길이 밝"));
        TAG_KEYWORDS.put(3, List.of("유동인구", "사람 많", "번화", "상권 좋"));
        TAG_KEYWORDS.put(4, List.of("여성전용", "여자 전용", "여성 전용"));
        TAG_KEYWORDS.put(5, List.of("관리인 상주", "관리인", "경비 상주", "경비원 상주"));
        TAG_KEYWORDS.put(6, List.of("외부인 출입", "외부인 많", "출입 잦", "낯선 사람", "보안 취약"));
        TAG_KEYWORDS.put(7, List.of("귀갓길 어둡", "밤길 어둡", "가로등 없", "길이 어둡"));
        TAG_KEYWORDS.put(8, List.of("인적 드물", "인적 없", "사람 없", "외진"));
    }

    private final CommentMapper commentMapper;

    @Transactional
    public void refreshPropertyTags(Long propertyId) {
        List<String> comments = commentMapper.findActiveContentsByPropertyId(propertyId);
        Map<Integer, Integer> counts = new LinkedHashMap<>();

        for (String comment : comments) {
            String normalized = comment == null ? "" : comment.toLowerCase(Locale.ROOT);
            TAG_KEYWORDS.forEach((tagType, keywords) -> {
                if (keywords.stream().anyMatch(normalized::contains)) {
                    counts.merge(tagType, 1, Integer::sum);
                }
            });
        }

        commentMapper.deleteTagsByPropertyId(propertyId);
        counts.forEach((tagType, tagCount) -> {
            PropertyTagVO tag = new PropertyTagVO();
            tag.setPropertyId(propertyId);
            tag.setTagType(tagType);
            tag.setTagCount(tagCount);
            commentMapper.insertPropertyTag(tag);
        });
    }

}
