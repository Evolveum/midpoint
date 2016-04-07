/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.web.component.prism.show.SceneDto;
import com.evolveum.midpoint.web.component.prism.show.ScenePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

/**
 * @author mederly
 */
public class TaskOtherChangesPanel extends BasePanel<TaskOtherChangesDto> {

    private static final String ID_STATE = "state";
    private static final String ID_PRIMARY_DELTA = "primaryDelta";

    public TaskOtherChangesPanel(String id, IModel<TaskOtherChangesDto> model) {
        super(id, model);
		initLayout();
    }

    protected void initLayout() {
        add(new Label(ID_STATE, new StringResourceModel("taskOtherChangesPanel.state.${}", new PropertyModel<ModelState>(getModel(), TaskOtherChangesDto.F_STATE))));

        ScenePanel deltaPanel = new ScenePanel(ID_PRIMARY_DELTA, new PropertyModel<SceneDto>(getModel(), TaskOtherChangesDto.F_PRIMARY_DELTAS));
        deltaPanel.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return getModelObject() != null && getModelObject().getPrimaryDeltas() != null;
            }
        });
        add(deltaPanel);
    }
}
