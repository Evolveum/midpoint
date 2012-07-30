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

package com.evolveum.midpoint.wf.wrappers;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.wf.messages.ProcessEvent;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: mederly
 * Date: 11.5.2012
 * Time: 15:02
 * To change this template use File | Settings | File Templates.
 */
public interface ProcessWrapper {

    StartProcessInstruction startProcessIfNeeded(ModelContext context, Task task, OperationResult result);

    void finishProcess(ProcessEvent event, Task task, OperationResult result);
}
