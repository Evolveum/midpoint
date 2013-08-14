/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.processors.primary.user;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.processors.primary.PrimaryApprovalProcessWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author mederly
 */
public abstract class AbstractUserWrapper extends AbstractWrapper {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractUserWrapper.class);

    protected ObjectType resolveObjectRef(AssignmentType a, OperationResult result) {

        if (a == null) {
            return null;
        }

        ObjectType object = a.getTarget();
        if (object == null) {
            if (a.getTargetRef() == null || a.getTargetRef().getOid() == null) {
                return null;
            }
            try {
                object = repositoryService.getObject(ObjectType.class, a.getTargetRef().getOid(), null, result).asObjectable();
            } catch (ObjectNotFoundException e) {
                throw new SystemException(e);
            } catch (SchemaException e) {
                throw new SystemException(e);
            }
            a.setTarget(object);
        }
        return object;
    }

}
