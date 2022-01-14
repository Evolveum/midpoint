package com.evolveum.midpoint.model.impl.correlator.idmatch.data.structure;

import java.util.List;

public class UserDataStructure {


    String sorLabel;
    String sorId;
    List<UserSpecParameter> object;


    public UserDataStructure(String sorLabel, String sorId, List<UserSpecParameter> object) {
        this.sorLabel = sorLabel;
        this.sorId = sorId;
        this.object = object;
    }


    public String getSorLabel() {
        return sorLabel;
    }


    public String getSorId() {
        return sorId;
    }

    public List<UserSpecParameter> getObject() {
        return object;
    }

}