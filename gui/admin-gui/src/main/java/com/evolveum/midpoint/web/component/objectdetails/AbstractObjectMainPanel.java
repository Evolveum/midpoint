/**
 * Copyright (c) 2015-2016 Evolveum
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
package com.evolveum.midpoint.web.component.objectdetails;

import java.util.List;

import com.evolveum.midpoint.gui.api.page.PageBase;
import org.apache.commons.lang.Validate;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 *
 */
public abstract class AbstractObjectMainPanel<O extends ObjectType> extends Panel {

	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_TAB_PANEL = "tabPanel";
	private static final String ID_EXECUTE_OPTIONS = "executeOptions";
	private static final String ID_BACK = "back";
	private static final String ID_SAVE = "save";
	private static final String ID_PREVIEW_CHANGES = "previewChanges";

	private static final Trace LOGGER = TraceManager.getTrace(AbstractObjectMainPanel.class);
	
	private Form mainForm;
	
	private LoadableModel<ObjectWrapper<O>> objectModel;
	private LoadableModel<ExecuteChangeOptionsDto> executeOptionsModel = new LoadableModel<ExecuteChangeOptionsDto>(false) {
		@Override
		protected ExecuteChangeOptionsDto load() {
			return new ExecuteChangeOptionsDto();
		}
	};

	public AbstractObjectMainPanel(String id, LoadableModel<ObjectWrapper<O>> objectModel, PageAdminObjectDetails<O> parentPage) {
		super(id, objectModel);
		Validate.notNull(objectModel, "Null object model");
		this.objectModel = objectModel;
		initLayout(parentPage);
	}

	public LoadableModel<ObjectWrapper<O>> getObjectModel() {
		return objectModel;
	}
	
	public ObjectWrapper<O> getObjectWrapper() {
		return objectModel.getObject();
	}

	public PrismObject<O> getObject() {
		return objectModel.getObject().getObject();
	}
	
	public Form getMainForm() {
		return mainForm;
	}

	private void initLayout(PageAdminObjectDetails<O> parentPage) {
		mainForm = new Form<>(ID_MAIN_FORM, true);
		add(mainForm);
		initLayoutTabs(parentPage);
		initLayoutOptions(parentPage);
		initLayoutButtons(parentPage);
	}
	
	protected void initLayoutTabs(final PageAdminObjectDetails<O> parentPage) {
		List<ITab> tabs = createTabs(parentPage);
		TabbedPanel<ITab> tabPanel = createTabPanel(parentPage, tabs);
		LOGGER.info("Adding {} to {}", tabPanel, mainForm);
		mainForm.add(tabPanel);
	}

	// TODO move to some utility class
	@NotNull
	public static TabbedPanel<ITab> createTabPanel(final PageBase parentPage, final List<ITab> tabs) {
		TabbedPanel<ITab> tabPanel = new TabbedPanel<ITab>(ID_TAB_PANEL, tabs) {
			@Override
			protected WebMarkupContainer newLink(String linkId, final int index) {
				return new AjaxSubmitLink(linkId) {

					@Override
					protected void onError(AjaxRequestTarget target,
							org.apache.wicket.markup.html.form.Form<?> form) {
						super.onError(target, form);
						target.add(parentPage.getFeedbackPanel());
					}

					@Override
					protected void onSubmit(AjaxRequestTarget target,
							org.apache.wicket.markup.html.form.Form<?> form) {
						super.onSubmit(target, form);

						setSelectedTab(index);
						if (target != null) {
							target.add(findParent(TabbedPanel.class));
						}
					}

				};
			}
		};
		tabPanel.setOutputMarkupId(true);
		return tabPanel;
	}

	protected abstract List<ITab> createTabs(PageAdminObjectDetails<O> parentPage);

	protected void initLayoutOptions(PageAdminObjectDetails<O> parentPage) {
		ExecuteChangeOptionsPanel optionsPanel = new ExecuteChangeOptionsPanel(ID_EXECUTE_OPTIONS,
				executeOptionsModel, true, false);
		mainForm.add(optionsPanel);
	}
	
	protected void initLayoutButtons(PageAdminObjectDetails<O> parentPage) {
		initLayoutPreviewButton(parentPage);
		initLayoutSaveButton(parentPage);
		initLayoutBackButton(parentPage);
	}

	protected void initLayoutSaveButton(final PageAdminObjectDetails<O> parentPage) {
		AjaxSubmitButton saveButton = new AjaxSubmitButton(ID_SAVE, parentPage.createStringResource("pageAdminFocus.button.save")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target,
					org.apache.wicket.markup.html.form.Form<?> form) {
				getDetailsPage().savePerformed(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target,
					org.apache.wicket.markup.html.form.Form<?> form) {
				target.add(parentPage.getFeedbackPanel());
			}
		};
		mainForm.setDefaultButton(saveButton);
		mainForm.add(saveButton);
	}

	// TEMPORARY
	protected void initLayoutPreviewButton(final PageAdminObjectDetails<O> parentPage) {
		AjaxSubmitButton previewButton = new AjaxSubmitButton(ID_PREVIEW_CHANGES, parentPage.createStringResource("pageAdminFocus.button.previewChanges")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target,
					org.apache.wicket.markup.html.form.Form<?> form) {
				getDetailsPage().previewPerformed(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target,
					org.apache.wicket.markup.html.form.Form<?> form) {
				target.add(parentPage.getFeedbackPanel());
			}
		};
		mainForm.add(previewButton);
	}

	protected void initLayoutBackButton(PageAdminObjectDetails<O> parentPage) {
		AjaxButton back = new AjaxButton(ID_BACK, parentPage.createStringResource("pageAdminFocus.button.back")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				backPerformed(target);
			}
			
		};
		mainForm.add(back);
	}
	
	public ExecuteChangeOptionsDto getExecuteChangeOptionsDto() {
		return executeOptionsModel.getObject();
	}

	private void backPerformed(AjaxRequestTarget target) {
		getDetailsPage().redirectBack();
	}
	
	protected PageAdminObjectDetails<O> getDetailsPage() {
		return (PageAdminObjectDetails<O>)getPage();
	}
}
