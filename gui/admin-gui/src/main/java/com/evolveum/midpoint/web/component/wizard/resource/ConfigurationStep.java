/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.web.component.wizard.resource;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.ColumnTypeDto;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismObjectPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.ObjectWrapperUtil;
import com.evolveum.midpoint.web.component.wizard.WizardStep;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.web.page.admin.resources.dto.TestConnectionResultDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.util.ListModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author lazyman
 */
public class ConfigurationStep extends WizardStep {

	private static final Trace LOGGER = TraceManager.getTrace(ConfigurationStep.class);

    private static final String DOT_CLASS = ConfigurationStep.class.getName() + ".";
    private static final String TEST_CONNECTION = DOT_CLASS + "testConnection";
    private static final String OPERATION_SAVE = DOT_CLASS + "saveResource";

    private static final String ID_CONFIGURATION = "configuration";
    private static final String ID_TIMEOUTS = "timeouts";
    private static final String ID_TEST_CONNECTION = "testConnection";
	public static final String ID_MAIN = "main";

	final private LoadableModel<PrismObject<ResourceType>> resourceModelNoFetch;
	final private LoadableModel<ObjectWrapper> configurationProperties;
	final private PageResourceWizard parentPage;

    public ConfigurationStep(LoadableModel<PrismObject<ResourceType>> modelNoFetch, PageResourceWizard parentPage) {
        super(parentPage);
        this.resourceModelNoFetch = modelNoFetch;
		this.parentPage = parentPage;

        this.configurationProperties = new LoadableModel<ObjectWrapper>(false) {

            @Override
            protected ObjectWrapper load() {

				PrismObject<ResourceType> resource = resourceModelNoFetch.getObject();
				ObjectWrapper wrapper = ObjectWrapperUtil.createObjectWrapper(null, null, resource,
                        ContainerStatus.MODIFYING, getPageBase());
                wrapper.setMinimalized(false);
                wrapper.setShowEmpty(true);

				getPageBase().showResult(wrapper.getResult(), false);

                return wrapper;
            }
        };

        initLayout();
    }

	@Override
	protected void onConfigure() {
		PrismObjectPanel configurationPanel = (PrismObjectPanel) get(createComponentPath(ID_MAIN, ID_CONFIGURATION));
		if (configurationPanel != null) {
			configurationPanel.removeAllContainerWrappers();            // to allow switching to connector with different configuration schema
		}
		configurationProperties.reset();
	}

	private void initLayout() {
    	com.evolveum.midpoint.web.component.form.Form form = new com.evolveum.midpoint.web.component.form.Form<>(ID_MAIN, true);
        form.setOutputMarkupId(true);
        add(form);
        
        form.add(new PrismObjectPanel(ID_CONFIGURATION, configurationProperties, null, null, getPageBase()));

        AjaxSubmitButton testConnection = new AjaxSubmitButton(ID_TEST_CONNECTION,
                createStringResource("ConfigurationStep.button.testConnection")) {

            @Override
            protected void onError(final AjaxRequestTarget target, Form<?> form) {
                WebComponentUtil.refreshFeedbacks(form, target);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                testConnectionPerformed(target);
            }
        };
        add(testConnection);
    }

	// copied from PageResource, TODO deduplicate
	private void testConnectionPerformed(AjaxRequestTarget target) {
		saveChanges();

		PageBase page = getPageBase();
		ModelService model = page.getModelService();

		OperationResult result = new OperationResult(TEST_CONNECTION);
		List<TestConnectionResultDto> resultDtoList = new ArrayList<>();
		try {
			Task task = page.createSimpleTask(TEST_CONNECTION);
			String oid = resourceModelNoFetch.getObject().getOid();
			result = model.testResource(oid, task);
			resultDtoList = TestConnectionResultDto.getResultDtoList(result, this);
		} catch (ObjectNotFoundException ex) {
			result.recordFatalError("Failed to test resource connection", ex);
		}

		page.setMainPopupContent(createConnectionResultTable(new ListModel<>(resultDtoList)));
		page.getMainPopup().setInitialHeight(400);
		page.getMainPopup().setInitialWidth(600);
		page.showMainPopup(target);

		page.showResult(result, "Test connection failed", false);
		target.add(page.getFeedbackPanel());
	}

	private TablePanel createConnectionResultTable(ListModel<TestConnectionResultDto> model) {
		ListDataProvider<TestConnectionResultDto> listprovider = new ListDataProvider<TestConnectionResultDto>(this,
				model);
		List<ColumnTypeDto> columns = Arrays.asList(new ColumnTypeDto<String>("Operation Name", "operationName", null),
				new ColumnTypeDto("Status", "status", null),
				new ColumnTypeDto<String>("Error Message", "errorMessage", null));

		TablePanel table = new TablePanel(getPageBase().getMainPopupBodyId(), listprovider, ColumnUtils.createColumns(columns));
		table.setOutputMarkupId(true);
		return table;
	}


	@Override
    public void applyState() {
        saveChanges();
    }

    private void saveChanges() {
        Task task = parentPage.createSimpleTask(OPERATION_SAVE);
        OperationResult result = task.getResult();
        try {
            ObjectWrapper wrapper = configurationProperties.getObject();
            ObjectDelta delta = wrapper.getObjectDelta();

			LOGGER.info("Applying delta:\n{}", delta.debugDump());
			WebModelServiceUtils.save(delta, result, parentPage);

			parentPage.resetModels();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Error occurred during resource test connection", ex);
            result.recordFatalError("Couldn't save configuration changes.", ex);
        } finally {
            result.computeStatusIfUnknown();
            setResult(result);
        }

        if (WebComponentUtil.showResultInPage(result)) {
            parentPage.showResult(result);
        }

		configurationProperties.reset();
	}
}
