/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.web.model.impl;

import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.model.RoleManager;
import com.evolveum.midpoint.web.model.dto.PropertyChange;
import com.evolveum.midpoint.web.model.dto.RoleDto;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.RoleType;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author lazyman
 */
public class RoleManagerImpl extends ObjectManagerImpl<RoleType, RoleDto> implements RoleManager {

    private static final long serialVersionUID = -5937335028068858595L;
    private static final Trace LOGGER = TraceManager.getTrace(RoleManagerImpl.class);

    @Override
    protected Class<? extends ObjectType> getSupportedObjectClass() {
        return RoleType.class;
    }

    @Override
    protected RoleDto createObject(RoleType objectType) {
        return new RoleDto(objectType, getModel());
    }

    @Override
    public Collection<RoleDto> list(PagingType paging) {
        return list(paging, ObjectTypes.ROLE);
    }

    @Override
    public Set<PropertyChange> submit(RoleDto newRole, Task task, OperationResult parentResult) {
        boolean isNew = false;
        if (StringUtils.isEmpty(newRole.getOid())) {
            isNew = true;
        }

        OperationResult result = parentResult.createSubresult("Save role");
        try {
            if (!isNew) {
                RoleDto oldRole = get(newRole.getOid(), new PropertyReferenceListType());

                ObjectDelta<RoleType> delta = DiffUtil.diff(oldRole.getXmlObject().asPrismObject(),
                        newRole.getXmlObject().asPrismObject());

                getModel().modifyObject(RoleType.class, delta.getOid(), delta.getModifications(), task, result);
                result.recordSuccess();
            } else {
                add(newRole);
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't submit role {}", ex, newRole.getName());
            result.recordFatalError("Couldn't submit role '" + newRole.getName() + "'.", ex);
        } finally {
            result.computeStatus();
        }

        // if role is new, operation result will be printed during add operation
        if (!isNew) {
//			ControllerUtil.printResults(LOGGER, result);
        }

        return new HashSet<PropertyChange>();
    }
}
