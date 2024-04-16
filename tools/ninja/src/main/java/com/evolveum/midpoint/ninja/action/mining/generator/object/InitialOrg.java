/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.mining.generator.object;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * This interface represents an org generator used for initial org object generation.
 * <p>
 * Part of RBAC Testing Data, which provides testing data for role mining and other RBAC-related processes.
 */
public interface InitialOrg {
    String getName();
    String getOidValue();

    default OrgType generateOrgObject() {
        OrgType org = new OrgType();
        org.setName(PolyStringType.fromOrig(getName()));
        org.setOid(getOidValue());
        return org;
    }

}
