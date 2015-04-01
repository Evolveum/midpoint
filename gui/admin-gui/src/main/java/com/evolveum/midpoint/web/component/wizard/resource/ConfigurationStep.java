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

package com.evolveum.midpoint.web.component.wizard.resource;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismObjectPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.ObjectWrapperUtil;
import com.evolveum.midpoint.web.component.wizard.WizardStep;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.Collection;

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

    private IModel<PrismObject<ResourceType>> resourceModel;
    private boolean isNewResource;
    private IModel<ObjectWrapper> configurationProperties;

    public ConfigurationStep(IModel<PrismObject<ResourceType>> resourceModel, boolean isNewResource) {
        this.resourceModel = resourceModel;
        this.isNewResource = isNewResource;

        this.configurationProperties = new LoadableModel<ObjectWrapper>(false) {

            @Override
            protected ObjectWrapper load() {
            	ObjectWrapper wrapper = ObjectWrapperUtil.createObjectWrapper(null, null, ConfigurationStep.this.resourceModel.getObject(),
                        ContainerStatus.MODIFYING, getPageBase());
//                ObjectWrapper wrapper = new ObjectWrapper(null, null, ConfigurationStep.this.resourceModel.getObject(),
//                        ContainerStatus.MODIFYING);
                wrapper.setMinimalized(false);
                wrapper.setShowEmpty(true);

                if (wrapper.getResult() != null && !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())) {
                    ((PageBase) getPage()).showResultInSession(wrapper.getResult());
                }

                return wrapper;
            }
        };

        initLayout();
    }

    private void initLayout() {
    	com.evolveum.midpoint.web.component.form.Form form = new com.evolveum.midpoint.web.component.form.Form<>("main", true);
        form.setOutputMarkupId(true);
        add(form);
        
        final PrismObjectPanel configuration = new PrismObjectPanel(ID_CONFIGURATION, configurationProperties, null, null, getPageBase());
        form.add(configuration);

        AjaxSubmitButton testConnection = new AjaxSubmitButton(ID_TEST_CONNECTION,
                createStringResource("ConfigurationStep.button.testConnection")) {

            @Override
            protected void onError(final AjaxRequestTarget target, Form<?> form) {
                WebMiscUtil.refreshFeedbacks(form, target);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                testConnectionPerformed(target);
            }
        };
        add(testConnection);
    }

    private void testConnectionPerformed(AjaxRequestTarget target) {
        saveChanges();

        PageBase page = getPageBase();
        ModelService model = page.getModelService();

        OperationResult result = null;
        try {
            Task task = page.createSimpleTask(TEST_CONNECTION);
            String oid = resourceModel.getObject().getOid();
            result = model.testResource(oid, task);
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Error occurred during resource test connection", ex);
            if (result == null) {
                result = new OperationResult(TEST_CONNECTION);
            }

            result.recordFatalError("Couldn't perform test connection.", ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        page.showResult(result);
        target.add(page.getFeedbackPanel());
    }

    @Override
    public void applyState() {
        saveChanges();
    }

    private void saveChanges() {
        PageBase page = getPageBase();

        OperationResult result = new OperationResult(OPERATION_SAVE);
        try {
            PrismObject<ResourceType> newResource = resourceModel.getObject();
            //apply configuration to old resource
            ObjectWrapper wrapper = configurationProperties.getObject();
            ObjectDelta delta = wrapper.getObjectDelta();
            delta.applyTo(newResource);

            page.getPrismContext().adopt(newResource);

            PrismObject<ResourceType> oldResource;

            if(isNewResource){
                Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createRaw());
                oldResource = WebModelUtils.loadObject(ResourceType.class, newResource.getOid(), options, result, page);
            } else {
                oldResource = WebModelUtils.loadObject(ResourceType.class, newResource.getOid(), result, page);
            }

            delta = DiffUtil.diff(oldResource, newResource);

            WebModelUtils.save(delta, result, page);

            newResource = WebModelUtils.loadObject(ResourceType.class, newResource.getOid(), result, page);
            resourceModel.setObject(newResource);
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Error occurred during resource test connection", ex);
            result.recordFatalError("Couldn't save configuration changes.", ex);
        } finally {
            result.computeStatusIfUnknown();
            setResult(result);
        }

        if (WebMiscUtil.showResultInPage(result)) {
            page.showResult(result);
        }
    }
}
