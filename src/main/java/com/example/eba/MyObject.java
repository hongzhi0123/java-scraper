package com.example.eba;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Data;

@Data
public class MyObject {
    @JsonProperty("CA_OwnerID")
    private String caOwnerId;

    @JsonProperty("EntityCode")
    private String entityCode;

    @JsonProperty("EntityType")
    private String entityType;

    @JsonProperty("Properties")
    private List<Property> properties;

    @JsonProperty("Services")
    private List<Service> services = new ArrayList<>();

    @JsonProperty("__EBA_EntityVersion")
    private String entityVersion;
}

@Data
class Property {
    @JsonProperty("ENT_AUT")
    private List<String> autDate;

    @JsonProperty("ENT_NAT_REF_COD")
    private String natRefCode;

    @JsonProperty("ENT_NAM")
    @JsonDeserialize(using = SingleOrListDeserializer.class)
    private List<String> name;

    @JsonProperty("ENT_NAM_COM")
    @JsonDeserialize(using = SingleOrListDeserializer.class)
    private List<String> nameCom;

    @JsonProperty("ENT_ADD")
    @JsonDeserialize(using = SingleOrListDeserializer.class)
    private List<String> address;

    @JsonDeserialize(using = SingleOrListDeserializer.class)
    @JsonProperty("ENT_TOW_CIT_RES")
    private List<String> city;

    @JsonProperty("ENT_COU_RES")
    private String country;

    @JsonProperty("ENT_POS_COD")
    @JsonDeserialize(using = SingleOrListDeserializer.class)
    private List<String> postalCode;

    @JsonProperty("ENT_EXC")
    @JsonDeserialize(using = SingleOrListDeserializer.class)
    private List<String> excl;

    @JsonProperty("ENT_DES_ACT_EXC_SCP")
    @JsonDeserialize(using = SingleOrListDeserializer.class)
    private List<String> excDes;

    @JsonProperty("ENT_TYP_PAR_ENT")
    private String parentType;

    @JsonProperty("ENT_COD_PAR_ENT")
    private String parentCode;

    @JsonProperty("DER_CHI_ENT_AUT")
    private String status;
    
}

@Data
class Service {
    private String country;
    private List<String> serviceCodes;

    @JsonAnySetter
    public void setService(String key, Object value) {
        this.country = key;
        this.serviceCodes = new ArrayList<>();

        if (value instanceof List) {
            // Handle array like ["PS_070", "PS_080"]
            this.serviceCodes.addAll((List<String>) value);
        } else if (value instanceof String) {
            // Handle single value like "PS_060"
            this.serviceCodes.add((String) value);
        }
    }

}
