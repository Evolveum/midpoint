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
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertDefinitionDto;
import com.evolveum.midpoint.web.page.admin.certification.dto.StageDefinitionDto;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

/**
 * @author mederly
 */

public class DefinitionStagesPanel extends SimplePanel<List<StageDefinitionDto>> {

    private static final String ID_STAGE_LIST = "stageList";
    private static final String ID_STAGE_EDITOR = "stageEditor";
    private static final String ID_STAGE_DEFINITION_LINK = "stageDefinitionLink";
    private static final String ID_STAGE_NAME_LABEL = "stageNameLabel";

    public DefinitionStagesPanel(String id, IModel<List<StageDefinitionDto>> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
//        RepeatingView rowView = new RepeatingView(ID_STAGE_LIST);
//        if (getModel() != null && getModel().getObject() != null) {
//            final List<StageDefinitionDto> stagesList = getModel().getObject();
//            for (final StageDefinitionDto stageDefinitionDto : stagesList) {
//                WebMarkupContainer row = new WebMarkupContainer(rowView.newChildId());
//                AjaxLink stageDefinitionLink = new AjaxLink(ID_STAGE_DEFINITION_LINK) {
//                    @Override
//                    public void onClick(AjaxRequestTarget target) {
//                        TabbedPanel tabbedPanel = this.findParent(TabbedPanel.class);
//                        IModel<List<ITab>> tabsModel = tabbedPanel.getTabs();
//                        List<ITab> tabsList = tabsModel.getObject();
//                        tabsList.add(new AbstractTab(createStringResource("PageCertDefinition.xmlDefinition")) {
//                            @Override
//                            public WebMarkupContainer getPanel(String panelId) {
//                                return new StageDefinitionPanel(panelId, getStageDefinitionModel(stageDefinitionDto, stagesList.indexOf(stageDefinitionDto)));
//                            }
//                        });
//                        tabbedPanel.setSelectedTab(tabsList.size() - 1);
//                        target.add(tabbedPanel);
//                    }
//                };
//                Label stageNameLabel = new Label(ID_STAGE_NAME_LABEL, stageDefinitionDto.getName());
//                stageNameLabel.setOutputMarkupId(true);
//                stageDefinitionLink.add(stageNameLabel);
//
//                row.add(stageDefinitionLink);
//                rowView.add(row);
//            }
//        }
//        add(rowView);

        AbstractReadOnlyModel<List> stageListModel = new AbstractReadOnlyModel<List>() {
            @Override
            public List getObject() {
                return getModelObject();
            }
        };
		ListView list = new ListView(ID_STAGE_LIST, stageListModel) {

			@Override
			protected void populateItem(ListItem item) {
                // TODO work with model
				StageEditorPanel editor = new StageEditorPanel(ID_STAGE_EDITOR, item.getModel());
				item.add(editor);
			}
		};
		list.setOutputMarkupId(true);
		add(list);
    }

    private IModel<StageDefinitionDto> getStageDefinitionModel(final StageDefinitionDto stageDefinitionDto, final int index){
        return new IModel<StageDefinitionDto>() {
            @Override
            public StageDefinitionDto getObject() {
                return stageDefinitionDto;
            }

            @Override
            public void setObject(StageDefinitionDto object) {
                List<StageDefinitionDto> stagesList = DefinitionStagesPanel.this.getModel().getObject();
                StageDefinitionDto stageDto = stagesList.get(index);
                stageDto = object;
            }

            @Override
            public void detach() {
            }
        };
    }
}
