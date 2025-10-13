/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.ninja.action.mining.generator.object;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import static com.evolveum.midpoint.ninja.action.mining.generator.context.RbacGeneratorUtils.createOrgAssignment;

/**
 * This interface represents an org generator used for initial org object generation.
 * <p>
 * Part of RBAC Testing Data, which provides testing data for role mining and other RBAC-related processes.
 */
public interface InitialOrg {
    String getName();
    String getOidValue();
    String parentOid();

    default OrgType generateOrgObject() {
        OrgType org = new OrgType();
        org.setName(PolyStringType.fromOrig(getName()));
        org.setOid(getOidValue());
        if (parentOid() != null) {
            AssignmentType orgAssignment = createOrgAssignment(parentOid());
            org.getAssignment().add(orgAssignment);
        }
        return org;
    }

}
