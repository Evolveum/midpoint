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

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
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
import org.apache.wicket.model.StringResourceModel;

/**
 * @author lazyman
 */
public class ConfigurationStep extends WizardStep {

    private static final Trace LOGGER = TraceManager.getTrace(ConfigurationStep.class);
    private static final String ID_CONFIGURATION = "configuration";
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

        AjaxLinkButton testConnection = new AjaxLinkButton(ID_TEST_CONNECTION,
                new StringResourceModel("ConfigurationStep.button.testConnection", this, null, "Test connection")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                testConnectionPerformed(target);
            }
        };
        add(testConnection);
    }

    private void testConnectionPerformed(AjaxRequestTarget target) {
        //todo implement
    }

    @Override
    public void applyState() {
        super.applyState();

        PrismObjectPanel configuration = (PrismObjectPanel) get(ID_CONFIGURATION);

    }
}
