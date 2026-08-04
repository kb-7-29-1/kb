package com.salgosipo.loan.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.util.List;

@Data
@JacksonXmlRootElement(localName = "response")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MinfundLoanApiResponse {

    @JacksonXmlProperty(localName = "body")
    private Body body;

    @Data
    public static class Body {
        @JacksonXmlProperty(localName = "items")
        private Items items;

        @JacksonXmlProperty(localName = "totalCount")
        private int totalCount;
    }

    @Data
    public static class Items {
        @JacksonXmlProperty(localName = "item")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<MinfundLoanItem> item;
    }
}
