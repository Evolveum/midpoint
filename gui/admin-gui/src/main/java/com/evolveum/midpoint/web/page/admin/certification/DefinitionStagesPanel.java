/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.web.page.admin.certification;

import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertDefinitionDto;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

/**
 * @author mederly
 */

public class DefinitionStagesPanel extends SimplePanel<CertDefinitionDto> {

    private static final String ID_STAGE_LIST = "stageList";
    private static final String ID_STAGE_EDITOR = "stageEditor";

    public DefinitionStagesPanel(String id, IModel<CertDefinitionDto> model) {
        super(id, model);
        initLayout();
    }

    @Override
    protected void initLayout() {
        AbstractReadOnlyModel<List> stageListModel = new AbstractReadOnlyModel<List>() {
            @Override
            public List getObject() {
                return getModelObject().getDefinition().getStageDefinition();
            }
        };
		ListView list = new ListView(ID_STAGE_LIST, stageListModel) {

			@Override
			protected void populateItem(ListItem item) {
                // TODO work with model
				StageEditorPanel editor = new StageEditorPanel(ID_STAGE_EDITOR);
				item.add(editor);
			}
		};
		list.setOutputMarkupId(true);
		add(list);
    }
}
