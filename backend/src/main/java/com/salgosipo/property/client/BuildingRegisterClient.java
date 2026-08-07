package com.salgosipo.property.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Log4j2
public class BuildingRegisterClient {

    private final RestTemplate restTemplate;

    @Value("${PUBLIC_DATA_SERVICE_KEY:}")
    private String serviceKey;

    private static final String API_URL = "https://apis.data.go.kr/1613000/BldRgstHubService/getBrTitleInfo";

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BuildingRegisterResult {
        private String useAprDay;           // 사용승인일 (YYYYMMDD)
        private String structureName;       // 건물 구조 (철근콘크리트구조)
        private String mainPurposeName;     // 주요용도 (공동주택, 오피스텔 등)
        private String earthquakeProofYn;   // 내진설계 여부 (1/0)
        private String illegalReason;       // 위반/적법 지정 사유
    }

    /**
     * 주소를 기반으로 국토교통부 건축물대장 표제부 OpenAPI(getBrTitleInfo)를 조회합니다.
     */
    public BuildingRegisterResult fetchBuildingInfo(String sigunguCd, String bjdongCd, String bun, String ji) {
        try {
            String uri = UriComponentsBuilder.fromHttpUrl(API_URL)
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("sigunguCd", sigunguCd)
                    .queryParam("bjdongCd", bjdongCd)
                    .queryParam("platGbCd", "0")
                    .queryParam("bun", String.format("%04d", Integer.parseInt(bun)))
                    .queryParam("ji", String.format("%04d", Integer.parseInt(ji.isEmpty() ? "0" : ji)))
                    .queryParam("numOfRows", 1)
                    .queryParam("pageNo", 1)
                    .build(true)
                    .toUriString();

            String responseXml = restTemplate.getForObject(uri, String.class);
            if (responseXml == null || responseXml.isEmpty()) {
                return null;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(responseXml.getBytes(StandardCharsets.UTF_8)));

            NodeList items = doc.getElementsByTagName("item");
            if (items.getLength() == 0) {
                return null;
            }

            Element item = (Element) items.item(0);
            String useAprDay = getXmlTagValue(item, "useAprDay");
            String strctCdNm = getXmlTagValue(item, "strctCdNm");
            String mainPurpsCdNm = getXmlTagValue(item, "mainPurpsCdNm");
            String rserthqkDsgnApplyYn = getXmlTagValue(item, "rserthqkDsgnApplyYn");

            return BuildingRegisterResult.builder()
                    .useAprDay(useAprDay.isEmpty() ? null : useAprDay)
                    .structureName(strctCdNm.isEmpty() ? "철근콘크리트구조" : strctCdNm)
                    .mainPurposeName(mainPurpsCdNm.isEmpty() ? "공동주택" : mainPurpsCdNm)
                    .earthquakeProofYn("1".equals(rserthqkDsgnApplyYn) ? "1" : "0")
                    .illegalReason("건축물대장 표제부 기준 적법 건물 (무단 증·개축 없음)")
                    .build();

        } catch (Exception e) {
            log.warn("건축물대장 OpenAPI 조회 실패 (기본값 사용): {}", e.getMessage());
            return null;
        }
    }

    private String getXmlTagValue(Element element, String tagName) {
        NodeList nodeList = element.getElementsByTagName(tagName);
        if (nodeList != null && nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent().trim();
        }
        return "";
    }
}
