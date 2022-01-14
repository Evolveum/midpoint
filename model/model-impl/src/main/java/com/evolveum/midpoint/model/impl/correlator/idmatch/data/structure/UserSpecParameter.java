package com.evolveum.midpoint.model.impl.correlator.idmatch.data.structure;

public class UserSpecParameter {


    String groupName;
    String objectName;
    String objectType;
    String object;


    public UserSpecParameter(String groupName, String objectName, String objectType, String object) {
        this.groupName = groupName;
        this.objectName = objectName;
        this.objectType = objectType;
        this.object = object;
    }


    public String getGroupName() {
        return groupName;
    }


    public String getObjectName() {
        return objectName;
    }


    public String getObjectType() {
        return objectType;
    }


    public String getObject() {
        return object;
    }

}
