package com.example.eba;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private Property property = new Property();

    // Jackson calls this for the JSON array
    @JsonProperty("Properties")
    public void setProperties(List<Map<String, Object>> propertiesList) {
        for (Map<String, Object> entry : propertiesList) {
            for (Map.Entry<String, Object> keyValue : entry.entrySet()) {
                property.setValue(keyValue.getKey(), keyValue.getValue());
            }
        }
    }

    private Map<String, List<String>> services = new HashMap<>();

    // Jackson will call this for the JSON array
    @JsonProperty("Services")
    public void setServices(List<Map<String, Object>> servicesList) {
        for (Map<String, Object> entry : servicesList) {
            for (Map.Entry<String, Object> keyValue : entry.entrySet()) {
                var value = keyValue.getValue();
                if (value instanceof List) {
                    services.put(keyValue.getKey(), (List<String>) value);
                } else if (value instanceof String) {
                    services.put(keyValue.getKey(), Collections.singletonList((String) value)); 
                }
            }
        }
    }

    @JsonProperty("__EBA_EntityVersion")
    private String entityVersion;
}

@Data
class Property {
    private List<String> autDate;
    private String natRefCode;
    private List<String> name;
    private List<String> nameCom;
    private List<String> address;
    private List<String> city;
    private String country;
    private List<String> postalCode;
    private List<String> excl;
    private List<String> excDes;
    private String parentType;
    private String parentCode;
    private String status;

    // Called by MyObject to flatten the array
    public void setValue(String key, Object value) {
        switch (key) {
            case "ENT_AUT":
                this.autDate = normalizeList(value);
                break;
            case "ENT_NAT_REF_COD":
                this.natRefCode = (String) value;
                break;
            case "ENT_NAM":
                this.name = normalizeList(value);
                break;
            case "ENT_NAM_COM":
                this.nameCom = normalizeList(value);
                break;
            case "ENT_ADD":
                this.address = normalizeList(value); // Handles string or array
                break;
            case "ENT_TOW_CIT_RES":
                this.city = normalizeList(value); // Handles string or array
                break;
            case "ENT_COU_RES":
                this.country = (String) value;
                break;
            case "ENT_POS_COD":
                this.postalCode = normalizeList(value);
                break;
            case "ENT_EXC":
                this.excl = normalizeList(value);
                break;
            case "ENT_DES_ACT_EXC_SCP":
                this.excDes = normalizeList(value);
                break;
            case "ENT_TYP_PAR_ENT":
                this.parentType = (String) value;
                break;
            case "ENT_COD_PAR_ENT":
                this.parentCode = (String) value;
                break;
            case "DER_CHI_ENT_AUT":
                this.status = (String) value;
                break;
        }
    }

    // Normalizes both "value" and ["value"] to List<String>
    private List<String> normalizeList(Object value) {
        List<String> list = new ArrayList<>();
        if (value == null)
            return list;

        if (value instanceof String str) {
            list.add(str);
        } else if (value instanceof List<?> lst) {
            for (Object item : lst) {
                if (item != null) {
                    list.add(item.toString());
                }
            }
        }
        return list;
    }
}


