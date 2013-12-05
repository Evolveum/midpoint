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

package com.evolveum.midpoint.report;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author lazyman
 */
@Component
public class ReportManagerImpl implements ReportManager, ChangeHook {

    public static final String HOOK_URI = "http://midpoint.evolveum.com/model/report-hook-1";

    @Autowired
    private HookRegistry hookRegistry;
    @Autowired
    private ModelService modelService;
    @Autowired
    private PrismContext prismContext;

    @PostConstruct
    public void init() {
        hookRegistry.registerChangeHook(HOOK_URI, this);
    }

    /**
     * Creates and starts task with proper handler, also adds necessary information to task
     * (like ReportType reference and so on).
     *
     * @param report
     * @param task
     * @param parentResult describes report which has to be created
     */
    @Override
    public void runReport(PrismObject<ReportType> report, Task task, OperationResult parentResult) {
        //todo implement
    }

    /**
     * Transforms change:
     * 1/ ReportOutputType DELETE to MODIFY some attribute to mark it for deletion.
     * 2/ ReportType ADD and MODIFY should compute jasper design and styles if necessary
     *
     * @param context
     * @param task
     * @param result
     * @return
     */
    @Override
    public HookOperationMode invoke(ModelContext context, Task task, OperationResult result) {
        //todo implement
        return HookOperationMode.FOREGROUND;
    }

    @Override
    public void invokeOnException(ModelContext context, Throwable throwable, Task task, OperationResult result) {

    }
}
