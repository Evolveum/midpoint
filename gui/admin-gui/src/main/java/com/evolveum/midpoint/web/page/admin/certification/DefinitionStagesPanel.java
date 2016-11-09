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

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.certification.dto.StageDefinitionDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageDefinitionType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author mederly
 */

public class DefinitionStagesPanel extends BasePanel<List<StageDefinitionDto>> {

    private static final String ID_TAB_PANEL = "tabPanel";

    private static final String ID_ADD_NEW_STAGE = "addNewStage";
    private static final String ID_MOVE_STAGE_RIGHT = "moveStageRight";
    private static final String ID_MOVE_STAGE_LEFT = "moveStageLeft";
    private static final String ID_DELETE_STAGE = "deleteStage";
    private static final String DEFAULT_STAGE_NAME_PREFIX = "Stage ";

	private TabbedPanel<ITab> tabPanel;
	private PageCertDefinition parentPage;

    public DefinitionStagesPanel(String id, IModel<List<StageDefinitionDto>> model, PageCertDefinition parentPage) {
        super(id, model);
		this.parentPage = parentPage;
		initLayout();
    }

    private void initLayout() {

		List<ITab> tabs = new ArrayList<>();
		createTabs(tabs);
		tabPanel = WebComponentUtil.createTabPanel(ID_TAB_PANEL, parentPage, tabs, null);
		add(tabPanel);

        AjaxSubmitButton addNewStage = new AjaxSubmitButton(ID_ADD_NEW_STAGE, createStringResource("StageDefinitionPanel.addNewStageButton")) {
            @Override
            public void onSubmit(AjaxRequestTarget target, Form form) {
				super.onSubmit(target, form);
                addPerformed(target);
            }
        };
        add(addNewStage);

		// use the same isVisible for all buttons to avoid changing buttons' placement (especially dangerous is "delete stage" one)
		// we also don't use isEnabled as it seems to have no visual effect
		VisibleEnableBehaviour visibleIfMoreTabs = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return getModelObject().size() > 1;
			}
		};

		AjaxSubmitButton moveLeft = new AjaxSubmitButton(ID_MOVE_STAGE_LEFT, createStringResource("StageDefinitionPanel.moveStageLeftButton")) {
			@Override
			public void onSubmit(AjaxRequestTarget target, Form form) {
				super.onSubmit(target, form);
				moveLeftPerformed(target);
			}
		};
		moveLeft.add(visibleIfMoreTabs);
		add(moveLeft);

		AjaxSubmitButton moveRight = new AjaxSubmitButton(ID_MOVE_STAGE_RIGHT, createStringResource("StageDefinitionPanel.moveStageRightButton")) {
			@Override
			public void onSubmit(AjaxRequestTarget target, Form form) {
				super.onSubmit(target, form);
				moveRightPerformed(target);
			}
		};
		moveRight.add(visibleIfMoreTabs);
		add(moveRight);

		AjaxSubmitButton delete = new AjaxSubmitButton(ID_DELETE_STAGE, createStringResource("StageDefinitionPanel.deleteStageButton")) {
			@Override
			public void onSubmit(AjaxRequestTarget target, Form form) {
				super.onSubmit(target, form);
				deletePerformed(target);
			}
		};
		delete.add(visibleIfMoreTabs);
		add(delete);

        setOutputMarkupId(true);
    }

	private void deletePerformed(AjaxRequestTarget target) {
        ConfirmationPanel dialog = new ConfirmationPanel(getPageBase().getMainPopupBodyId(), new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
                        StageDefinitionDto dto = getModelObject().get(tabPanel.getSelectedTab());
                        return getString("DefinitionStagesPanel.confirmDeleteText", dto.getName());
                    }
                }){
            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
                deleteConfirmedPerformed(target);
            }
        };
        getPageBase().showMainPopup(dialog, target);

	}

	private void addPerformed(AjaxRequestTarget target) {
		StageDefinitionDto newStageDefinitionDto = createNewStageDefinitionDto();
		getModelObject().add(newStageDefinitionDto);
		recreateTabs();
		tabPanel.setSelectedTab(getModelObject().size()-1);
		target.add(parentPage.getTabPanel());
    }

	private void deleteConfirmedPerformed(AjaxRequestTarget target) {
		int selected = tabPanel.getSelectedTab();
		getModelObject().remove(selected);
		recreateTabs();
		if (tabPanel.getSelectedTab() >= getModelObject().size()) {
			tabPanel.setSelectedTab(getModelObject().size()-1);
		}
		target.add(parentPage.getTabPanel());
	}

	private void moveLeftPerformed(AjaxRequestTarget target) {
		int selected = tabPanel.getSelectedTab();
		List<StageDefinitionDto> list = getModelObject();
		if (selected > 0) {
			Collections.swap(list, selected-1, selected);
			setOrder(list, selected-1);
			setOrder(list, selected);
			recreateTabs();
			tabPanel.setSelectedTab(selected-1);
			target.add(this);
		}
	}

	private void moveRightPerformed(AjaxRequestTarget target) {
		int selected = tabPanel.getSelectedTab();
		List<StageDefinitionDto> list = getModelObject();
		if (selected < list.size()-1) {
			Collections.swap(list, selected, selected+1);
			setOrder(list, selected);
			setOrder(list, selected+1);
			recreateTabs();
			tabPanel.setSelectedTab(selected+1);
			target.add(this);
		}
	}

	private void setOrder(List<StageDefinitionDto> list, int i) {
		list.get(i).setNumber(i+1);
	}

	private void recreateTabs() {
		List<ITab> tabs = tabPanel.getTabs().getObject();
		tabs.clear();
		createTabs(tabs);
	}

	private void createTabs(List<ITab> tabs) {
		for (final StageDefinitionDto definitionDto : getModelObject()) {
			tabs.add(createTab(definitionDto, parentPage));
		}
	}

	@NotNull
	private AbstractTab createTab(final StageDefinitionDto definitionDto, final PageCertDefinition parentPage) {
		return new AbstractTab(new Model<>(definitionDto.getName())) {
			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new DefinitionStagePanel(panelId, new Model(definitionDto), parentPage);
			}
		};
	}


	private StageDefinitionDto createNewStageDefinitionDto(){
		try {
			AccessCertificationStageDefinitionType def = new AccessCertificationStageDefinitionType(parentPage.getPrismContext());
			def.setNumber(getModel().getObject().size() + 1);
			def.setName(DEFAULT_STAGE_NAME_PREFIX + def.getNumber());
			return new StageDefinitionDto(def, parentPage.getPrismContext());
		} catch (SchemaException e) {
			throw new IllegalStateException(e);
		}
    }
}
