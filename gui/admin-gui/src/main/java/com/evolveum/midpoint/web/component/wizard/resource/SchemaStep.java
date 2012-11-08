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

import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.wizard.resource.component.SchemaListPanel;
import com.evolveum.midpoint.web.component.wizard.resource.component.XmlEditorPanel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.XmlSchemaType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class SchemaStep extends WizardStep {

    private static final String ID_TABPANEL = "tabpanel";
    private static final String ID_RELOAD = "reload";
    private IModel<ResourceType> model;

    public SchemaStep(IModel<ResourceType> model) {
        this.model = model;

        initLayout();
    }

    private void initLayout() {
        List<ITab> tabs = new ArrayList<ITab>();
        tabs.add(createSimpleSchemaView());
        tabs.add(createSchemaEditor());

        AjaxTabbedPanel tabpanel = new AjaxTabbedPanel(ID_TABPANEL, tabs);
        tabpanel.setOutputMarkupId(true);
        add(tabpanel);

        AjaxLinkButton reload = new AjaxLinkButton(ID_RELOAD, createStringModel("SchemaStep.button.reload")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                reloadPerformed(target);
            }
        };
        add(reload);
    }

    private IModel<String> createStringModel(String resourceKey) {
        return new StringResourceModel(resourceKey, this, null, resourceKey);
    }

    private IModel<String> createXmlEditorModel() {
        return new IModel<String>() {

            @Override
            public String getObject() {
                ResourceType resource = model.getObject();
                XmlSchemaType xmlSchema = resource.getSchema();
                if (xmlSchema == null) {
                    return null;
                }

                PageBase page = (PageBase) SchemaStep.this.getPage();
                PrismDomProcessor domProcessor = page.getPrismContext().getPrismDomProcessor();

                try {
                    Element root = domProcessor.serializeToDom(xmlSchema.asPrismContainerValue(),
                            DOMUtil.createElement(SchemaConstantsGenerated.C_SCHEMA));

                    Element schema = root != null ? DOMUtil.getFirstChildElement(root) : null;
                    if (schema == null) {
                        return null;
                    }

                    return DOMUtil.serializeDOMToString(schema);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    //todo error handling
                }

                return null;
            }

            @Override
            public void setObject(String object) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void detach() {
            }
        };
    }

    private void reloadPerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private ITab createSchemaEditor() {
        return new AbstractTab(createStringModel("SchemaStep.xml")) {

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return new XmlEditorPanel(panelId, createXmlEditorModel());
            }
        };
    }

    private ITab createSimpleSchemaView() {
        return new AbstractTab(createStringModel("SchemaStep.schema")) {

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return new SchemaListPanel(panelId, model); //todo model
            }
        };
    }
}
