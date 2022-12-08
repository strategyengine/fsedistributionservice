package com.strategyengine.xrpl.fsedistributionservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@EqualsAndHashCode
@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class XrplDataNftDto {


    @JsonProperty("NFTokenID")
    private String nfTokenID;
    @JsonProperty("Issuer")
    private String issuer;
    @JsonProperty("Owner")
    private String owner;
    @JsonProperty("Taxon")
    private Long taxon;
    @JsonProperty("TransferFee")
    private Long transferFee;
    @JsonProperty("Flags")
    private Long flags;
    @JsonProperty("Sequence")
    private Long sequence;
    @JsonProperty("URI")
    private String uri;



}
