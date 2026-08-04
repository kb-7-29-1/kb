package com.salgosipo.loan.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MinfundLoanItem {
    @JacksonXmlProperty(localName = "finprdnm")
    private String productName;

    @JacksonXmlProperty(localName = "lnlmt")
    private String loanLimit;

    @JacksonXmlProperty(localName = "irtCtg")
    private String rateCategory;

    @JacksonXmlProperty(localName = "irt")
    private String rate;

    @JacksonXmlProperty(localName = "usge")
    private String usage;

    @JacksonXmlProperty(localName = "trgt")
    private String target;

    @JacksonXmlProperty(localName = "ofrinstnm")
    private String institution;

    @JacksonXmlProperty(localName = "suprtgtdtlcond")
    private String detailCondition;

    @JacksonXmlProperty(localName = "jnmthd")
    private String applyMethod;

    @JacksonXmlProperty(localName = "rltsite")
    private String site;
}