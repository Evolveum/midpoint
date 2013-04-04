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

package com.evolveum.midpoint.wf.processes.obsolete;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.wf.WfHook;
import com.evolveum.midpoint.wf.WfTaskUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.communication.workflow_1.WfProcessInstanceEventType;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: mederly
 * Date: 11.5.2012
 * Time: 15:06
 * To change this template use File | Settings | File Templates.
 */
//@Component
public class ModifyUserSecondaryProcessWrapper //implements PrimaryApprovalProcessWrapper {
{

    @Autowired(required = true)
    private WfHook wfHook;

    @Autowired(required = true)
    private WfTaskUtil wfTaskUtil;

    @PostConstruct
    public void register() {
        //wfHook.registerWfProcessWrapper(this);
    }

    //@Override
//    public StartProcessInstruction prepareStartCommandIfApplicable(ModelOperationStageType stage, Collection<ObjectDelta<Objectable>> changes, Task task) {
//
//        if (true)
//            return null;
//
//        if (stage == ModelOperationStageType.SECONDARY) {
//            ObjectDelta<Objectable> change = changes.iterator().next();
//            if (change.getObjectTypeClass() == UserType.class) {
//                for (ItemDelta delta : change.getModifications()) {
//                    if (delta.getValuesToReplace() != null) {
//                        for (Object o : delta.getValuesToReplace()) {
//                            if (o instanceof PrismPropertyValue) {
//                                Object real = ((PrismPropertyValue<Object>) o).getValue();
//                                if (real instanceof String && ((String) real).startsWith("testwf2")) {
//                                    StartProcessInstruction startCommand = new StartProcessInstruction();
//                                    startCommand.setProcessName("ModifyUserSecondary");
//                                    startCommand.addProcessVariable("changes", dump(changes));
//                                    startCommand.setSimple(true);
//                                    return startCommand;
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        return null;
//    }

    //@Override
//    public void finishProcess(WfProcessInstanceEventType event, Task task, OperationResult result) throws Exception {
//
//        Map<String,String> variables = wfTaskUtil.unwrapWfVariables(event);
//        if ("true".equals(variables.get("approved"))) {
//            wfTaskUtil.markAcceptation(task, result);
//        } else {
//            wfTaskUtil.markRejection(task, result);
//        }
//
//    }

    private String dump(Collection<ObjectDelta<Objectable>> changes) {
        StringBuffer sb = new StringBuffer();
        for (ObjectDelta<?> change : changes) {
            sb.append(change.debugDump());
            sb.append('\n');
        }
        return sb.toString();
    }

}
