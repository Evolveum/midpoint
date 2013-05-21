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

package com.evolveum.midpoint.web.page.admin.resources;

import java.util.Collection;

import com.evolveum.midpoint.web.page.admin.resources.dto.*;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceImportDto;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;

public class PageResourceImport extends PageAdminResources {

	public static final String PARAM_RESOURCE_IMPORT_ID = "resourceImportId";
	private static final String DOT_CLASS = PageResourceImport.class.getName() + ".";
	private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";
	private ResourceImportDto resImport;

	private IModel<ResourceDto> model;

	public PageResourceImport() {
		model = new LoadableModel<ResourceDto>() {

			@Override
			protected ResourceDto load() {
				return loadResource();
			}
		};
		if(resImport == null){
			resImport = new ResourceImportDto();
		}
		initLayout();
	}

	private ResourceDto loadResource() {
		OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCE);
		PrismObject<ResourceType> resource = null;

		try {
			Collection<SelectorOptions<GetOperationOptions>> options =
					SelectorOptions.createCollection(ResourceType.F_CONNECTOR, GetOperationOptions.createResolve());

			Task task = createSimpleTask(OPERATION_LOAD_RESOURCE);

			StringValue resourceOid = getPageParameters().get(PARAM_RESOURCE_IMPORT_ID);
			resource = getModelService().getObject(ResourceType.class, resourceOid.toString(), options, task,
					result);

			result.recordSuccess();
		} catch (Exception ex) {
			result.recordFatalError("Couldn't get resource.", ex);
		}

		if (!result.isSuccess()) {
			showResult(result);
		}
		
		if (resource == null) {
            getSession().error(getString("pageResourceImport.message.cantLoadResource"));

            if (!result.isSuccess()) {
                showResultInSession(result);
            }
            throw new RestartResponseException(PageResources.class);
        }
		
		return new ResourceDto(resource, resource.asObjectable().getConnector());
	}

	@Override
	protected IModel<String> createPageTitleModel() {
		return new LoadableModel<String>(false) {

			@Override
			protected String load() {
				String name = model.getObject().getName();
				return new StringResourceModel("page.title", PageResourceImport.this, null, null, name).getString();
			}
		};
	}

	private void initLayout() {		
		Form mainForm = new Form("mainForm");
		add(mainForm);
		initColumns(mainForm);
		initButtons(mainForm);
	}
	
	private void initColumns(Form mainForm){
		mainForm.add(new CheckBox("running", new PropertyModel<Boolean>(resImport, "running")));
		mainForm.add(new Label("launchTime", new PropertyModel<String>(resImport, "launchTime")));
		mainForm.add(new Label("finishTime", new PropertyModel<String>(resImport, "finishTime")));
		mainForm.add(new Label("progress", new PropertyModel<String>(resImport, "progress")));
		mainForm.add(new Label("lastStatus", new PropertyModel<String>(resImport, "lastStatus")));
		mainForm.add(new Label("numberOfErrors", new PropertyModel<String>(resImport, "numberOfErrors")));
		mainForm.add(new Label("message", new PropertyModel<String>(resImport, "lastError.message")));
		mainForm.add(new Label("details", new PropertyModel<String>(resImport, "lastError.details")));
		//mainForm.add(new Label("timeStamp", new PropertyModel<String>(this, "resImport.lastError.details")));
	}
	
	private void initButtons(Form mainForm){
		AjaxLinkButton back = new AjaxLinkButton("back", createStringResource("pageResourceImport.button.back")) {
			
			@Override
			public void onClick(AjaxRequestTarget target) {
				setResponsePage(PageResources.class);
			}
		};
		mainForm.add(back);
	}

}
