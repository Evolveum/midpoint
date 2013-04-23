/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.wizard.resource;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismObjectPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.WizardStep;
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
