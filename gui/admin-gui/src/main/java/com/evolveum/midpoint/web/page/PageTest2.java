/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.web.page;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.wizard.resource.component.capability.CapabilityPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.model.IModel;

/**
 *  TODO - this is just a testing page for testing CapabilityEditor during development, it should be removed before release
 *
 *  @author shood
 */
@PageDescriptor(url = "/capability", action = {@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_DENY_ALL)})
public class PageTest2 extends PageBase {

    private static final String ID_CAPABILITY = "capability";

    private static final String OID_RESOURCE_CSV_LIVESYNC = "ef2bc95b-76e0-48e2-86d6-3d4f02d3fafe";


    private IModel<PrismObject<ResourceType>> model;

    public PageTest2(){
        model = new LoadableModel<PrismObject<ResourceType>>(false) {

            @Override
            protected PrismObject<ResourceType> load() {
                return loadModel();
            }
        };

        initLayout();
    }

    private PrismObject<ResourceType> loadModel(){

        OperationResult result = new OperationResult("loadResource");
        PrismObject<ResourceType> resource = null;

        try{
            Task task = getTaskManager().createTaskInstance("loadResource");
            resource = getModelService().getObject(ResourceType.class, OID_RESOURCE_CSV_LIVESYNC,
                    null, task, result);

            result.recordSuccess();
        } catch (Exception e){
            result.recordFatalError(e.getMessage(), e);
        }

        if (!result.isSuccess()) {
            showResult(result);
        }

        return resource;
    }

    private void initLayout(){
        model.getObject();
        add(new CapabilityPanel(ID_CAPABILITY, model));
    }

}
