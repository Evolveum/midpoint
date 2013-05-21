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

import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.wf.WfHook;
import com.evolveum.midpoint.wf.WfTaskUtil;
import com.evolveum.midpoint.wf.messages.ProcessEvent;
import com.evolveum.midpoint.wf.StartProcessInstruction;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: mederly
 * Date: 11.5.2012
 * Time: 15:06
 * To change this template use File | Settings | File Templates.
 */
//@Component
//@DependsOn("workflowManager")
public class AddUserProcessWrapper { //implements PrimaryApprovalProcessWrapper {

//    @Autowired(required = true)
    private WfHook wfHook;

//    @Autowired(required = true)
    private WfTaskUtil wfTaskUtil;

//    @PostConstruct
    public void register() {
        //wfHook.registerWfProcessWrapper(this);
    }

    //@Override
    public StartProcessInstruction startProcessIfNeeded(ModelState state, Collection<ObjectDelta<Objectable>> changes, Task task) {

        if (state == ModelState.PRIMARY) {
            if (changes.size() == 1) {
                ObjectDelta<Objectable> change = changes.iterator().next();

                if (change.getChangeType() == ChangeType.ADD) {

                    // this causes problems in deltas -- probably it changes their internal state(!)
                    // todo: investigate further
                    //ObjectType objectToAdd = change.getObjectToAdd().getValue().getValue();

                    PrismObject<?> prismToAdd = change.getObjectToAdd();
                    boolean isUser = prismToAdd.getCompileTimeClass().isAssignableFrom(UserType.class);

                    if (isUser) {

                        PolyStringType user = prismToAdd.asObjectable().getName();
                        if (user.getOrig().startsWith("testwf")) {
                            StartProcessInstruction startCommand = new StartProcessInstruction();
                            startCommand.setProcessName("AddUser");
                            startCommand.addProcessVariable("user", user);
                            startCommand.setTaskName(new PolyStringType("Workflow for creating user " + user));
                            startCommand.setSimple(true);
                            return startCommand;
                        }
                    }
                }
            }
        }
        return null;
    }

    //@Override
    public void finishProcess(ProcessEvent event, Task task, OperationResult result) {

//        if (event.getAnswer() == Boolean.TRUE) {
//            //wfTaskUtil.markAcceptation(task, result);
//        } else {
//            //wfTaskUtil.markRejection(task, result);
//        }

    }
}
