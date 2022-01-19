/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

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
