/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Created by honchar.
 */
public class PolicyRuleDetailsPanel extends AbstractAssignmentDetailsPanel {
    private static final long serialVersionUID = 1L;

    public PolicyRuleDetailsPanel(String id, IModel<AssignmentDto> model, PageBase pageBase){
        super(id, model, pageBase);
    }

    protected Component initPropertiesContainer(String id){
        return new PolicyRulePropertiesPanel(id, getModel(), pageBase);
    }

       
}
