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

//    static {
//        TAG_KEYWORDS.put(1, List.of("현관보안", "공동현관", "도어락", "출입문 잠금", "보안 좋"));
//        TAG_KEYWORDS.put(2, List.of("귀갓길 밝", "밤길 밝", "가로등", "길이 밝"));
//        TAG_KEYWORDS.put(3, List.of("유동인구", "사람 많", "번화", "상권 좋"));
//        TAG_KEYWORDS.put(4, List.of("여성전용", "여자 전용", "여성 전용"));
//        TAG_KEYWORDS.put(5, List.of("관리인 상주", "관리인", "경비 상주", "경비원 상주"));
//        TAG_KEYWORDS.put(6, List.of("외부인 출입", "외부인 많", "출입 잦", "낯선 사람", "보안 취약"));
//        TAG_KEYWORDS.put(7, List.of("귀갓길 어둡", "밤길 어둡", "가로등 없", "길이 어둡"));
//        TAG_KEYWORDS.put(8, List.of("인적 드물", "인적 없", "사람 없", "외진"));
//    }

    static {
        // 1. 현관 보안 좋음
        TAG_KEYWORDS.put(1, List.of(
                "현관보안", "공동현관", "도어락", "출입문 잠금", "보안이 좋", "보안 좋", "보안 훌륭", "지문인식", "카드키"
        ));

        // 2. 귀갓길 밝음
        TAG_KEYWORDS.put(2, List.of(
                "귀갓길이 밝", "귀가길이 밝", "밤길이 밝", "가로등", "길이 밝", "환하", "밝아서", "밝은 편", "밝은편"
        ));

        // 3. 근처 유동인구 많음
        TAG_KEYWORDS.put(3, List.of(
                "유동인구", "사람 많", "사람이 많", "번화", "상권 좋", "상권이 좋", "위치가 좋", "대로변"
        ));

        // 4. 여성 전용 원룸
        TAG_KEYWORDS.put(4, List.of(
                "여성전용", "여성 전용", "여자전용", "여자 전용"
        ));

        // 5. 관리인 상주
        TAG_KEYWORDS.put(5, List.of(
                "관리인이 상주", "경비 상주", "경비원이 상주", "관리실", "경비실", "관리인 계", "경비원 계", "관리 아저씨", "경비 아저씨"
        ));

        // 6. 외부인 출입 잦음
        TAG_KEYWORDS.put(6, List.of(
                "외부인 출입", "외부인 많", "외부인 들어", "출입이 잦", "출입 잦", "낯선 사람", "보안 취약", "보안 허술", "보안 안 좋", "보안이 안"
        ));

        // 7. 귀갓길 어두움
        TAG_KEYWORDS.put(7, List.of(
                "귀갓길이 어둡", "귀가길이 어둡", "밤길이 어둡", "가로등이 없", "길이 어둡", "어두워", "어두운", "어둡고", "깜깜"
        ));

        // 8. 근처 인적 드묾
        TAG_KEYWORDS.put(8, List.of(
                "인적이 드물", "인적이 드묾", "인적이 없", "사람 없", "사람이 없", "외진", "골목길"
        ));
    }

    private final CommentMapper commentMapper;

    @Transactional
    public void refreshPropertyTags(Long propertyId) {
        List<String> comments = commentMapper.findActiveContentsByPropertyId(propertyId);
        Map<Integer, Integer> counts = new LinkedHashMap<>();

        for (String comment : comments) {
            String normalized = comment == null ? "" : comment.toLowerCase(Locale.ROOT);
            String noSpaceComment = normalized.replaceAll("\\s+", "");

            TAG_KEYWORDS.forEach((tagType, keywords) -> {
                boolean isMatched = keywords.stream().anyMatch(keyword -> {
                    String noSpaceKeyword = keyword.replaceAll("\\s+", "");

                    if (!noSpaceComment.contains(noSpaceKeyword)) {
                        return false;
                    }

                    boolean isFalsePositive = false;

                    // 예: "도어락 고장", "보안 안 좋음" 등
                    if (tagType == 1 && (noSpaceComment.contains(noSpaceKeyword + "고장") ||
                            noSpaceComment.contains(noSpaceKeyword + "안돼") ||
                            noSpaceComment.contains("보안안좋"))) {
                        isFalsePositive = true;
                    // 예: "가로등 없음", "가로등 고장나서" 등
                    } else if (tagType == 2 && (noSpaceComment.contains(noSpaceKeyword + "없") ||
                            noSpaceComment.contains(noSpaceKeyword + "고장"))) {
                        isFalsePositive = true;
                    // 예: "관리실 없음", "경비원 안계심" 등
                    } else if (tagType == 5 && (noSpaceComment.contains(noSpaceKeyword + "없") ||
                            noSpaceComment.contains(noSpaceKeyword + "안계"))) {
                        isFalsePositive = true;
                    }

                    return !isFalsePositive;
                });

                if (isMatched) {
                    counts.merge(tagType, 1, Integer::sum);
                }
            });
        }

        commentMapper.deleteTagsByPropertyId(propertyId);
        counts.forEach((tagType, tagCount) ->

        {
            PropertyTagVO tag = new PropertyTagVO();
            tag.setPropertyId(propertyId);
            tag.setTagType(tagType);
            tag.setTagCount(tagCount);
            commentMapper.insertPropertyTag(tag);
        });
    }

}
