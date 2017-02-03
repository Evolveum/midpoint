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

package com.evolveum.midpoint.wf.impl.processes;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.wf.impl.messages.ProcessEvent;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpWfTask;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfProcessSpecificWorkItemPartType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemResultType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Default process interface, doing (almost) nothing.
 *
 * @author mederly
 */
@Component
public class DefaultProcessMidPointInterface extends BaseProcessMidPointInterface {

    @Override public WorkItemResultType extractWorkItemResult(Map<String, Object> variables) {
        return null;
    }

    @Override
    public WfProcessSpecificWorkItemPartType extractProcessSpecificWorkItemPart(Map<String, Object> variables) {
        return null;
    }

    @Override
    public List<ObjectReferenceType> prepareApprovedBy(ProcessEvent event, PcpWfTask job, OperationResult result) {
        return new ArrayList<>();
    }
}
