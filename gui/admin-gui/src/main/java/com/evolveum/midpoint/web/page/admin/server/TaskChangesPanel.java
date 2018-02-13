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

package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.prism.show.ScenePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskChangesDto;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author mederly
 */
public class TaskChangesPanel extends BasePanel<TaskChangesDto> {

//    private static final String ID_TITLE = "title";
    private static final String ID_PRIMARY_DELTA = "primaryDelta";

    public TaskChangesPanel(String id, IModel<TaskChangesDto> model) {
        super(id, model);
		initLayout();
    }

    protected void initLayout() {
//        add(new Label(ID_TITLE, new AbstractReadOnlyModel<String>() {
//			@Override
//			public String getObject() {
//				return getString(getModelObject().getTitleKey());
//			}
//		}));

        ScenePanel deltaPanel = new ScenePanel(ID_PRIMARY_DELTA, new PropertyModel<>(getModel(), TaskChangesDto.F_PRIMARY_DELTAS));
        deltaPanel.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return getModelObject() != null && getModelObject().getPrimaryDeltas() != null;
            }
        });
        add(deltaPanel);
    }
}
