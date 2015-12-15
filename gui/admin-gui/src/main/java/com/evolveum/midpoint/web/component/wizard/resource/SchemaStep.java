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

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.wizard.WizardStep;
import com.evolveum.midpoint.web.component.wizard.resource.component.SchemaListPanel;
import com.evolveum.midpoint.web.component.wizard.resource.component.XmlEditorPanel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class SchemaStep extends WizardStep {

    private static final Trace LOGGER = TraceManager.getTrace(SchemaStep.class);
    private static final String DOT_CLASS = SchemaStep.class.getName() + ".";
    private static final String OPERATION_RELOAD_RESOURCE_SCHEMA = DOT_CLASS + "reloadResourceSchema";

    private static final String ID_TAB_PANEL = "tabPanel";
    private static final String ID_RELOAD = "reload";
    private static final String ID_ACE_EDITOR = "aceEditor";
    private IModel<PrismObject<ResourceType>> model;

    public SchemaStep(IModel<PrismObject<ResourceType>> model, PageBase pageBase) {
        super(pageBase);
        this.model = model;
        setOutputMarkupId(true);

        initLayout();
    }

    private void initLayout() {
        List<ITab> tabs = new ArrayList<>();
        tabs.add(createSimpleSchemaView());
        tabs.add(createSchemaEditor());

        TabbedPanel tabPanel = new TabbedPanel(ID_TAB_PANEL, tabs);
        tabPanel.setOutputMarkupId(true);
        add(tabPanel);

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
        if(model == null || model.getObject() == null){
            return;
        }

        PrismObject<ResourceType> resource = model.getObject();
        resource.asObjectable().setSchema(new XmlSchemaType());
        Task task = getPageBase().createSimpleTask(OPERATION_RELOAD_RESOURCE_SCHEMA);
        OperationResult result = task.getResult();

        try {
            resource = WebModelUtils.loadObject(ResourceType.class, resource.getOid(), getPageBase(), task, result);
            getPageBase().getPrismContext().adopt(resource);

            model.getObject().asObjectable().setSchema(resource.asObjectable().getSchema());
        } catch (SchemaException e) {
            LOGGER.error(getString("SchemaStep.message.reload.fail", WebMiscUtil.getName(resource)));
            result.recordFatalError(getString("SchemaStep.message.reload.fail", WebMiscUtil.getName(resource)));
        }

        result.computeStatusIfUnknown();
        if(result.isSuccess()){
            LOGGER.info(getString("SchemaStep.message.reload.ok", WebMiscUtil.getName(resource)));
            result.recordSuccess();
        } else {
            LOGGER.error(getString("SchemaStep.message.reload.fail", WebMiscUtil.getName(resource)));
            result.recordFatalError(getString("SchemaStep.message.reload.fail", WebMiscUtil.getName(resource)));
        }

        getPageBase().showResult(result);
        target.add(getPageBase().getFeedbackPanel(), this);
    }

    private ITab createSchemaEditor() {
        return new AbstractTab(createStringModel("SchemaStep.xml")) {

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                XmlEditorPanel xmlEditorPanel = new XmlEditorPanel(panelId, createXmlEditorModel());
                // quick fix: now changes from XmlEditorPanel are not saved anyhow
                //(e.g. by clicking Finish button in wizard). For now,
                //panel is made disabled for editing
                AceEditor aceEditor = (AceEditor) xmlEditorPanel.get(ID_ACE_EDITOR);
                aceEditor.setReadonly(true);
                return xmlEditorPanel;
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
