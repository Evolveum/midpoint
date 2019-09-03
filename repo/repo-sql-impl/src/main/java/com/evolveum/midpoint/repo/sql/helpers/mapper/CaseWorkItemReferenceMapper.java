/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
