/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

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
