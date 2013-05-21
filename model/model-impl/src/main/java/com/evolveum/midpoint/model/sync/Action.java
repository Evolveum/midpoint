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

package com.evolveum.midpoint.model.sync;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationType;

import java.util.List;

/**
 * @author Vilo Repan
 */
public interface Action {

	String ACTION_SYNCHRONIZE = Action.class.getName() + ".synchronizeAction";
    String ACTION_ADD_USER = Action.class.getName() + ".addUserAction";
    String ACTION_MODIFY_USER = Action.class.getName() + ".modifyUserAction";
    String ACTION_DISABLE_USER = Action.class.getName() + ".disableUserAction";
    String ACTION_DELETE_USER = Action.class.getName() + ".deleteUser";
    String ACTION_ADD_ACCOUNT = Action.class.getName() + ".addAccount";
    String ACTION_LINK_ACCOUNT = Action.class.getName() + ".linkAccount";
    String ACTION_UNLINK_ACCOUNT = Action.class.getName() + ".unlinkAccount";
    String ACTION_DELETE_ACCOUNT = Action.class.getName() + ".deleteAccount";
    String ACTION_DISABLE_ACCOUNT = Action.class.getName() + ".disableAccount";
    String ACTION_MODIFY_PASSWORD = Action.class.getName() + ".modifyPassword";


    String executeChanges(String userOid, ResourceObjectShadowChangeDescription change, ObjectTemplateType userTemplate,
            SynchronizationSituationType situation, AuditEventRecord auditRecord, Task task, OperationResult result) 
    		throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException;

    void setParameters(List<Object> parameters);

    List<Object> getParameters();
}
