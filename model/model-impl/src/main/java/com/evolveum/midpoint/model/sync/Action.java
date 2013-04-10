/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
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


    String executeChanges(String userOid, ResourceObjectShadowChangeDescription change,
            SynchronizationSituationType situation, AuditEventRecord auditRecord, Task task, OperationResult result) 
    		throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException;

    void setParameters(List<Object> parameters);

    List<Object> getParameters();
}
