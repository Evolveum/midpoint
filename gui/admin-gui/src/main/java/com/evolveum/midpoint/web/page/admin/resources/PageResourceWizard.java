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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.resources;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.wizard.resource.*;
import com.evolveum.midpoint.web.component.xml.ace.AceEditor;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

/**
 * @author lazyman
 */
public class PageResourceWizard extends PageAdminResources {

    private static final String ID_WIZARD = "wizard";
    private IModel<ResourceType> model;

    public PageResourceWizard() {
        model = new LoadableModel<ResourceType>(false) {

            @Override
            protected ResourceType load() {
                try {
                    if (!isResourceOidAvailable()) {
                        ResourceType resource = new ResourceType();
                        PageResourceWizard.this.getPrismContext().adopt(resource);

                        return resource;
                    }

                    PrismObject<ResourceType> resource = loadResource(null);
                    return resource.asObjectable();
                } catch (Exception ex) {
                    //todo error handling
                    ex.printStackTrace();
                }

                return null;
            }
        };

        initLayout();
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return new LoadableModel<String>(false) {

            @Override
            protected String load() {
                if (!isResourceOidAvailable()) {
                    return PageResourceWizard.super.createPageTitleModel().getObject();
                }

                return new StringResourceModel("page.title.editResource", PageResourceWizard.this, null).getString();
            }
        };
    }

    private void initLayout() {
        WizardModel wizardModel = new WizardModel();
        wizardModel.add(new NameStep(model));
        wizardModel.add(new ConfigurationStep(model));
        wizardModel.add(new SchemaStep(model));
        wizardModel.add(new SchemaHandlingStep());
        wizardModel.add(new CapabilityStep());
        wizardModel.add(new SynchronizationStep());

        ResourceWizard wizard = new ResourceWizard(ID_WIZARD, wizardModel);
        add(wizard);

        //todo remove
        final AceEditor editor = new AceEditor("editor", new AbstractReadOnlyModel<Object>() {

            @Override
            public Object getObject() {
                try {
                    PrismDomProcessor domProcessor = PageResourceWizard.this.getPrismContext().getPrismDomProcessor();
                    return domProcessor.serializeObjectToString(model.getObject().asPrismObject());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return "error";
            }
        });
        editor.setReadonly(true);
        editor.setOutputMarkupId(true);
        add(editor);
        AjaxLinkButton reload = new AjaxLinkButton("reload", new Model<String>("reload")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                target.add(editor);
                target.appendJavaScript(editor.createJavascriptEditableRefresh());
            }
        };
        add(reload);
    }
}
