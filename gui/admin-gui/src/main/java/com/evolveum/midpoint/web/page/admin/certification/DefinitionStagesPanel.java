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
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.certification.dto.AccessCertificationReviewerDto;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertDefinitionDto;
import com.evolveum.midpoint.web.page.admin.certification.dto.StageDefinitionDto;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ObjectPolicyConfigurationTypeDto;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
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
import org.apache.wicket.model.ResourceModel;

import java.util.List;

/**
 * @author mederly
 */

public class DefinitionStagesPanel extends SimplePanel<List<StageDefinitionDto>> {

    private static final String ID_STAGE_LIST = "stageList";
    private static final String ID_STAGE_EDITOR = "stageEditor";
    private static final String ID_ADD_NEW_STAGE = "addNewStage";
    private static final String DEFAULT_STAGE_NAME= "Stage ";

    public DefinitionStagesPanel(String id, IModel<List<StageDefinitionDto>> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        AbstractReadOnlyModel<List> stageListModel = new AbstractReadOnlyModel<List>() {
            @Override
            public List getObject() {
                return getModelObject();
            }
        };
		ListView list = new ListView(ID_STAGE_LIST, stageListModel) {

			@Override
			protected void populateItem(ListItem item) {
				StageEditorPanel editor = new StageEditorPanel(ID_STAGE_EDITOR, item.getModel());
				item.add(editor);
			}
		};
		list.setOutputMarkupId(true);
		add(list);

        AjaxButton button = new AjaxButton(ID_ADD_NEW_STAGE, createStringResource("StageDefinitionPanel.addNewStageButton")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                addPerformed(target);
            }
        };

//        AjaxLink addNewStage = new AjaxLink(ID_ADD_NEW_STAGE) {
//
//            @Override
//            public void onClick(AjaxRequestTarget target) {
//                addPerformed(target);
//            }
//        };
        add(button);
        setOutputMarkupId(true);
    }

    private void addPerformed(AjaxRequestTarget target){
        List<StageDefinitionDto> list = getModelObject();
        list.add(createNewStageDefinitionDto());

        target.add(this);
    }
    private StageDefinitionDto createNewStageDefinitionDto(){
        StageDefinitionDto dto = new StageDefinitionDto();
        //set stage number
        dto.setNumber(getModel().getObject().size() + 1);
        dto.setName(DEFAULT_STAGE_NAME + (getModel().getObject().size() + 1));
        //create reviewers objects
        AccessCertificationReviewerDto reviewerDto = new AccessCertificationReviewerDto();
        ObjectViewDto defaultReviewer = new ObjectViewDto();
        defaultReviewer.setType(UserType.class);
        ObjectViewDto additionalReviewer = new ObjectViewDto();
        additionalReviewer.setType(UserType.class);
        reviewerDto.setFirstDefaultReviewerRef(defaultReviewer);
        reviewerDto.setFirstAdditionalReviewerRef(additionalReviewer);

        dto.setReviewerDto(reviewerDto);
        return dto;
    }

}
