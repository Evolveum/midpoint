/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.wf.processors.primary;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.wf.jobs.Job;
import com.evolveum.midpoint.wf.jobs.JobCreationInstruction;
import com.evolveum.midpoint.wf.processors.ChangeProcessor;

/**
 * @author mederly
 */
public class PcpChildJobCreationInstruction extends JobCreationInstruction {

    private boolean executeApprovedChangeImmediately;     // should the child job execute approved change immediately (i.e. executeModelOperationHandler must be set as well!)

    protected PcpChildJobCreationInstruction(ChangeProcessor changeProcessor) {
        super(changeProcessor);
    }

    protected PcpChildJobCreationInstruction(Job parentJob) {
        super(parentJob);
    }

    public static PcpChildJobCreationInstruction createInstruction(ChangeProcessor changeProcessor) {
        PcpChildJobCreationInstruction pcpjci = new PcpChildJobCreationInstruction(changeProcessor);
        prepareWfProcessChildJobInternal(pcpjci);
        return pcpjci;
    }

    public static PcpChildJobCreationInstruction createInstruction(Job parentJob) {
        PcpChildJobCreationInstruction pcpjci = new PcpChildJobCreationInstruction(parentJob);
        prepareWfProcessChildJobInternal(pcpjci);
        return pcpjci;
    }

    public boolean isExecuteApprovedChangeImmediately() {
        return executeApprovedChangeImmediately;
    }

    public void setExecuteApprovedChangeImmediately(boolean executeApprovedChangeImmediately) {
        this.executeApprovedChangeImmediately = executeApprovedChangeImmediately;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();

        DebugUtil.indentDebugDump(sb, indent);
        sb.append("PrimaryChangeProcessor ChildJobCreationInstruction: (execute approved change immediately = ")
                .append(executeApprovedChangeImmediately)
                .append(")\n");
        sb.append(super.debugDump(indent+1));
        return sb.toString();
    }
}
