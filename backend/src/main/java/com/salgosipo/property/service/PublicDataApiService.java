package com.salgosipo.property.service;

import com.salgosipo.property.dto.PropertyListDTO;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@Log4j2
public class PublicDataApiService {

    @Value("${PUBLIC_DATA_SERVICE_KEY:}")
    private String apiKey;

    /**
     * 광진구(LAWD_CD=11215) 실제 공공데이터 실거래 월세 매물 5개 리스트 조회
     */
    public List<PropertyListDTO> getGwangjinMonthlyProperties() {
        List<PropertyListDTO> realList = fetchRealDataFromPublicApi();
        if (realList != null && !realList.isEmpty()) {
            log.info("Successfully fetched {} real property items from Public Data API.", realList.size());
            return realList.subList(0, Math.min(5, realList.size()));
        }

        log.warn("Public Data API returned empty or failed. Using fallback Gwangjin properties.");
        return getFallbackProperties();
    }

    private List<PropertyListDTO> fetchRealDataFromPublicApi() {
        List<PropertyListDTO> list = new ArrayList<>();
        try {
            // 국토교통부 오피스텔/아파트 전월세 실거래가 API (광진구 11215)
            String serviceUrl = "http://apis.data.go.kr/1613000/RTMSDataSvcOffiRent/getRTMSDataSvcOffiRent";
            String dealYmd = "202403"; // 최근 거래월 기준

            StringBuilder urlBuilder = new StringBuilder(serviceUrl);
            urlBuilder.append("?").append(URLEncoder.encode("serviceKey", StandardCharsets.UTF_8)).append("=")
                    .append(apiKey);
            urlBuilder.append("&").append(URLEncoder.encode("LAWD_CD", StandardCharsets.UTF_8)).append("=")
                    .append(URLEncoder.encode("11215", StandardCharsets.UTF_8));
            urlBuilder.append("&").append(URLEncoder.encode("DEAL_YMD", StandardCharsets.UTF_8)).append("=")
                    .append(URLEncoder.encode(dealYmd, StandardCharsets.UTF_8));
            urlBuilder.append("&").append(URLEncoder.encode("numOfRows", StandardCharsets.UTF_8)).append("=")
                    .append(URLEncoder.encode("10", StandardCharsets.UTF_8));

            URL url = new URL(urlBuilder.toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-type", "application/xml");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                try (InputStream is = conn.getInputStream()) {
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(is);
                    doc.getDocumentElement().normalize();

                    NodeList nList = doc.getElementsByTagName("item");
                    long idCounter = 201L;

                    String[] thumbnails = {
                            "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=600&q=80",
                            "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=600&q=80",
                            "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=600&q=80",
                            "https://images.unsplash.com/photo-1554995207-c18c203602cb?auto=format&fit=crop&w=600&q=80",
                            "https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=600&q=80"
                    };

                    for (int temp = 0; temp < nList.getLength(); temp++) {
                        Element element = (Element) nList.item(temp);

                        String depositStr = getTagValue("보증금액", element).replace(",", "").trim();
                        String rentStr = getTagValue("월세금액", element).replace(",", "").trim();
                        String name = getTagValue("단지명", element);
                        if (name.isEmpty())
                            name = getTagValue("오피스텔", element);
                        if (name.isEmpty())
                            name = "광진구 실거래 매물";

                        String dong = getTagValue("법정동", element).trim();
                        String jibun = getTagValue("지번", element).trim();
                        String areaStr = getTagValue("전용면적", element);
                        String floorStr = getTagValue("층", element);

                        int deposit = depositStr.isEmpty() ? 1000 : Integer.parseInt(depositStr);
                        int monthlyRent = rentStr.isEmpty() ? 60 : Integer.parseInt(rentStr);
                        double area = areaStr.isEmpty() ? 23.5 : Double.parseDouble(areaStr);
                        int floor = floorStr.isEmpty() ? 3 : Integer.parseInt(floorStr);

                        // 법정동별 실제 중심 좌표
                        double lat = 37.5485;
                        double lng = 127.0720;
                        if (dong.contains("군자")) {
                            lat = 37.5528;
                            lng = 127.0745;
                        } else if (dong.contains("자양")) {
                            lat = 37.5385;
                            lng = 127.0660;
                        } else if (dong.contains("구의")) {
                            lat = 37.5450;
                            lng = 127.0850;
                        }

                        String address = "서울특별시 광진구 " + dong + " " + jibun;

                        PropertyListDTO dto = PropertyListDTO.builder()
                                .propertyId(idCounter++)
                                .title(dong + " " + name + " (실거래가)")
                                .buildingType(3)
                                .roomType(1)
                                .deposit(deposit)
                                .monthlyRent(monthlyRent)
                                .area(area)
                                .floor(floor)
                                .address(address)
                                .latitude(lat + (temp * 0.0012)) // 마커 겹침 방지 오프셋
                                .longitude(lng + (temp * 0.0015))
                                .thumbnailUrl(thumbnails[temp % thumbnails.length])
                                .safetyScore(85 + (temp % 10))
                                .safetyGrade("SAFE")
                                .isBookmarked(temp % 2 == 0)
                                .tags(List.of("공공데이터실거래", dong, "실거래가검증"))
                                .build();

                        list.add(dto);
                        if (list.size() >= 5)
                            break;
                    }
                }
            }
            conn.disconnect();
        } catch (Exception e) {
            log.error("Error fetching real data from Public Data API: {}", e.getMessage(), e);
        }
        return list;
    }

    private String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList != null && nodeList.getLength() > 0) {
            NodeList childList = nodeList.item(0).getChildNodes();
            if (childList != null && childList.getLength() > 0) {
                return childList.item(0).getNodeValue();
            }
        }
        return "";
    }

    private List<PropertyListDTO> getFallbackProperties() {
        List<PropertyListDTO> list = new ArrayList<>();

        list.add(PropertyListDTO.builder()
                .propertyId(101L)
                .title("세종대 화양동 프리미엄 오피스텔")
                .buildingType(3)
                .roomType(1)
                .deposit(1000)
                .monthlyRent(65)
                .area(24.5)
                .floor(5)
                .address("서울특별시 광진구 화양동 111-23")
                .latitude(37.5485)
                .longitude(127.0720)
                .thumbnailUrl(
                        "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=600&q=80")
                .safetyScore(92)
                .safetyGrade("SAFE")
                .isBookmarked(true)
                .tags(List.of("풀옵션", "역세권", "CCTV가득"))
                .build());

        list.add(PropertyListDTO.builder()
                .propertyId(102L)
                .title("어린이대공원역 역세권 신축 원룸")
                .buildingType(1)
                .roomType(1)
                .deposit(500)
                .monthlyRent(55)
                .area(22.0)
                .floor(3)
                .address("서울특별시 광진구 군자동 361-15")
                .latitude(37.5528)
                .longitude(127.0745)
                .thumbnailUrl(
                        "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=600&q=80")
                .safetyScore(88)
                .safetyGrade("SAFE")
                .isBookmarked(false)
                .tags(List.of("초역세권", "안심길", "보호구역"))
                .build());

        list.add(PropertyListDTO.builder()
                .propertyId(103L)
                .title("건대입구역 가성비 밝은 원룸")
                .buildingType(2)
                .roomType(1)
                .deposit(2000)
                .monthlyRent(60)
                .area(26.8)
                .floor(2)
                .address("서울특별시 광진구 화양동 48-12")
                .latitude(37.5442)
                .longitude(127.0685)
                .thumbnailUrl(
                        "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=600&q=80")
                .safetyScore(78)
                .safetyGrade("WARNING")
                .isBookmarked(false)
                .tags(List.of("가성비", "남향", "번화가가까움"))
                .build());

        list.add(PropertyListDTO.builder()
                .propertyId(104L)
                .title("세종대 후문 풀옵션 다가구 원룸")
                .buildingType(2)
                .roomType(1)
                .deposit(1000)
                .monthlyRent(50)
                .area(21.0)
                .floor(4)
                .address("서울특별시 광진구 군자동 102-4")
                .latitude(37.5545)
                .longitude(127.0782)
                .thumbnailUrl(
                        "https://images.unsplash.com/photo-1554995207-c18c203602cb?auto=format&fit=crop&w=600&q=80")
                .safetyScore(95)
                .safetyGrade("SAFE")
                .isBookmarked(false)
                .tags(List.of("세종대도보3분", "최고안전점수", "조용한주택가"))
                .build());

        list.add(PropertyListDTO.builder()
                .propertyId(105L)
                .title("자양동 신양초 인근 안심 투룸")
                .buildingType(1)
                .roomType(2)
                .deposit(3000)
                .monthlyRent(80)
                .area(45.2)
                .floor(3)
                .address("서울특별시 광진구 자양동 224-8")
                .latitude(37.5385)
                .longitude(127.0660)
                .thumbnailUrl(
                        "https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=600&q=80")
                .safetyScore(82)
                .safetyGrade("SAFE")
                .isBookmarked(true)
                .tags(List.of("투룸", "넓은면적", "경찰서인근"))
                .build());

        return list;
    }
}
