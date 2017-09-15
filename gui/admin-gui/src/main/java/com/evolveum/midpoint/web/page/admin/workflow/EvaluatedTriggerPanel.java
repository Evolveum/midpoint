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

package com.evolveum.midpoint.web.page.admin.workflow;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.LocalizableMessageModel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.EvaluatedTriggerDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.EvaluatedTriggerGroupDto;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author mederly
 */
public class EvaluatedTriggerPanel extends BasePanel<EvaluatedTriggerDto> {

    private static final String ID_MESSAGE = "message";
    private static final String ID_CHILDREN = "children";

    public EvaluatedTriggerPanel(String id, IModel<EvaluatedTriggerDto> model) {
        super(id, model);

        initLayout();
    }

    protected void initLayout() {
	    EvaluatedTriggerDto trigger = getModelObject();
	    add(new Label(ID_MESSAGE,
		        new LocalizableMessageModel(Model.of(trigger.getMessage()), this)));
	    EvaluatedTriggerGroupDto children = trigger.getChildren();
	    EvaluatedTriggerGroupPanel childrenPanel = new EvaluatedTriggerGroupPanel(ID_CHILDREN, Model.of(children));
	    childrenPanel.setVisible(!children.getTriggers().isEmpty());
	    add(childrenPanel);
    }

}
