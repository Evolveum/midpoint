/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.sql.data.common.container.RCaseWorkItem;
import com.evolveum.midpoint.repo.sql.data.common.container.RCaseWorkItemReference;
import com.evolveum.midpoint.repo.sql.data.common.other.RCaseWorkItemReferenceOwner;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class CaseWorkItemReferenceMapper extends ReferenceMapper<RCaseWorkItemReference> {

    @Override
    public RCaseWorkItemReference map(Referencable input, MapperContext context) {
        RCaseWorkItem owner = (RCaseWorkItem) context.getOwner();

        ObjectReferenceType objectRef = buildReference(input);

        RCaseWorkItemReferenceOwner type;
        ItemName name = context.getDelta().getPath().lastName();
        if (QNameUtil.match(name, CaseWorkItemType.F_ASSIGNEE_REF)) {
            type = RCaseWorkItemReferenceOwner.ASSIGNEE;
        } else if (QNameUtil.match(name, CaseWorkItemType.F_CANDIDATE_REF)) {
            type = RCaseWorkItemReferenceOwner.CANDIDATE;
        } else {
            throw new IllegalStateException("Unknown case work item reference owner: " + name + "(delta = " + context.getDelta() + ")");
        }

        return RCaseWorkItemReference.jaxbRefToRepo(objectRef, owner, context.getRelationRegistry(), type);
    }
}
