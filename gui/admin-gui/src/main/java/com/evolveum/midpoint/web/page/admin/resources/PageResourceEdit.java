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

package com.evolveum.midpoint.web.page.admin.resources;

import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.wizard.resource.*;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

/**
 * @author lazyman
 */
public class PageResourceEdit extends PageAdminResources {

    private static final String ID_WIZARD = "wizard";

    public PageResourceEdit() {
        initLayout();
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return new LoadableModel<String>(false) {

            @Override
            protected String load() {
                if (!isEditingResource()) {
                    return PageResourceEdit.super.createPageTitleModel().getObject();
                }

                return new StringResourceModel("page.title.editResource", PageResourceEdit.this, null, null).getString();
            }
        };
    }

    private void initLayout() {
        WizardModel model = new WizardModel();
        model.add(new NameStep());
        model.add(new ConfigurationStep());
        model.add(new SchemaStep());
        model.add(new SchemaHandlingStep());
        model.add(new CapabilityStep());
        model.add(new SynchronizationStep());

        ResourceWizard wizard = new ResourceWizard(ID_WIZARD, model);
        add(wizard);
    }
}
