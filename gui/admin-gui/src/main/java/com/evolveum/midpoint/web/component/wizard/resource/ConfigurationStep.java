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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismObjectPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.wizard.WizardStep;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class ConfigurationStep extends WizardStep {

    private static final Trace LOGGER = TraceManager.getTrace(ConfigurationStep.class);

    private static final String DOT_CLASS = ConfigurationStep.class.getName() + ".";
    private static final String TEST_CONNECTION = DOT_CLASS + "testConnection";

    private static final String ID_CONFIGURATION = "configuration";
    private static final String ID_TIMEOUTS = "timeouts";
    private static final String ID_TEST_CONNECTION = "testConnection";

    private IModel<ResourceType> resourceModel;

    public ConfigurationStep(IModel<ResourceType> resourceModel) {
        this.resourceModel = resourceModel;

        initLayout(resourceModel);
    }

    private void initLayout(final IModel<ResourceType> resourceModel) {
        IModel<ObjectWrapper> wrapperModel = new LoadableModel<ObjectWrapper>(false) {

            @Override
            protected ObjectWrapper load() {
                ObjectWrapper wrapper = new ObjectWrapper(null, null, resourceModel.getObject().asPrismObject(),
                        ContainerStatus.MODIFYING);
                wrapper.setMinimalized(false);
                wrapper.setShowEmpty(true);

                if (wrapper.getResult() != null && !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())) {
                    ((PageBase) getPage()).showResultInSession(wrapper.getResult());
                }

                return wrapper;
            }
        };

        PrismObjectPanel configuration = new PrismObjectPanel(ID_CONFIGURATION, wrapperModel, null, null);
        add(configuration);

        AjaxButton testConnection = new AjaxButton(ID_TEST_CONNECTION,
                createStringResource("ConfigurationStep.button.testConnection")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                testConnectionPerformed(target);
            }
        };
        add(testConnection);
    }

    private void testConnectionPerformed(AjaxRequestTarget target) {
        PageBase page = getPageBase();
        ModelService model = page.getModelService();

        OperationResult result;
        try {
            Task task = page.createSimpleTask(TEST_CONNECTION);
            String oid = resourceModel.getObject().getOid();
            result = model.testResource(oid, task);

            page.showResult(result);
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Error occurred during resource test connection", ex);
        }

        target.add(page.getFeedbackPanel());

        //todo show more precise information about test connection operation [lazyman]
    }

    @Override
    public void applyState() {
        super.applyState();

        PrismObjectPanel configuration = (PrismObjectPanel) get(ID_CONFIGURATION);

    }
}
