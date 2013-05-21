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
