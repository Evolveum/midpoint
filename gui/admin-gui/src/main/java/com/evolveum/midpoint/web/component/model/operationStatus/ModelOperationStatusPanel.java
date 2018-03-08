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

package com.evolveum.midpoint.web.component.model.operationStatus;

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
public class ModelOperationStatusPanel extends BasePanel<ModelOperationStatusDto> {

    private static final String ID_STATE = "state";
    private static final String ID_FOCUS_TYPE = "focusType";
    private static final String ID_FOCUS_NAME = "focusName";
    private static final String ID_PRIMARY_DELTA = "primaryDelta";

    public ModelOperationStatusPanel(String id, IModel<ModelOperationStatusDto> model) {
        super(id, model);
		initLayout();
    }

    protected void initLayout() {

        add(new Label(ID_STATE, new StringResourceModel("ModelOperationStatusPanel.state.${}", new PropertyModel<ModelState>(getModel(), ModelOperationStatusDto.F_STATE))));
//        add(new Label(ID_FOCUS_TYPE, new PropertyModel<String>(getModel(), ModelOperationStatusDto.F_FOCUS_TYPE)));
//        add(new Label(ID_FOCUS_NAME, new PropertyModel<String>(getModel(), ModelOperationStatusDto.F_FOCUS_NAME)));


        ScenePanel deltaPanel = new ScenePanel(ID_PRIMARY_DELTA, new PropertyModel<>(getModel(), ModelOperationStatusDto.F_PRIMARY_DELTA));
        deltaPanel.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return getModelObject() != null && getModelObject().getPrimaryDelta() != null;
            }
        });
        add(deltaPanel);

    }
}
