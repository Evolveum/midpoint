/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.configuration.dto;

import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.io.Serializable;
import java.util.List;

public class PrincipalDto implements Serializable {

    private UserType userType;
    private List<String> nodeIdentidier;

    public PrincipalDto(UserType userType, List<String> nodeIdentidier) {
        this.userType = userType;
        this.nodeIdentidier = nodeIdentidier;
    }

    public List<String> getNodeIdentidier() {
        return nodeIdentidier;
    }

    public void setNodeIdentidier(List<String> nodeIdentidier) {
        this.nodeIdentidier = nodeIdentidier;
    }
}
