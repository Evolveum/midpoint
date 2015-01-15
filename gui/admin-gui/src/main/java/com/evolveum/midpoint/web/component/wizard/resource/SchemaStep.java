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

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.wizard.WizardStep;
import com.evolveum.midpoint.web.component.wizard.resource.component.SchemaListPanel;
import com.evolveum.midpoint.web.component.wizard.resource.component.XmlEditorPanel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
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

    private static final String ID_TABPANEL = "tabPanel";
    private static final String ID_RELOAD = "reload";
    private IModel<PrismObject<ResourceType>> model;

    public SchemaStep(IModel<PrismObject<ResourceType>> model) {
        this.model = model;

        initLayout();
    }

    private void initLayout() {
        List<ITab> tabs = new ArrayList<>();
        tabs.add(createSimpleSchemaView());
        tabs.add(createSchemaEditor());

        TabbedPanel tabpanel = new TabbedPanel(ID_TABPANEL, tabs);
        tabpanel.setOutputMarkupId(true);
        add(tabpanel);

        AjaxButton reload = new AjaxButton(ID_RELOAD, createStringModel("SchemaStep.button.reload")) {

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
                PrismObject<ResourceType> resource = model.getObject();
                PrismContainer xmlSchema = resource.findContainer(ResourceType.F_SCHEMA);
                if (xmlSchema == null) {
                    return null;
                }

                PageBase page = (PageBase) SchemaStep.this.getPage();

                try {
                    // probably not correct... test and fix [pm]
                    return page.getPrismContext().serializeContainerValueToString(xmlSchema.getValue(), SchemaConstantsGenerated.C_SCHEMA, PrismContext.LANG_XML);
//                    Element root = page.getPrismContext().getParserDom().serializeToDom(xmlSchema.getValue(),
//                            DOMUtil.createElement(SchemaConstantsGenerated.C_SCHEMA));
//
//                    Element schema = root != null ? DOMUtil.getFirstChildElement(root) : null;
//                    if (schema == null) {
//                        return null;
//                    }
//
//                    return DOMUtil.serializeDOMToString(schema);
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
                return new SchemaListPanel(panelId, model);
            }
        };
    }
}
