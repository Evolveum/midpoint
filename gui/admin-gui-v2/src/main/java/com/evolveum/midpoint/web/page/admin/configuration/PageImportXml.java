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

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.xml.ace.AceEditor;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ImportOptionsType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.InputStream;

/**
 * @author lazyman
 * @author mserbak
 */
public class PageImportXml extends PageAdminConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(PageImportXml.class);
    private static final String DOT_CLASS = PageImportXml.class.getName() + ".";
    private static final String OPERATION_IMPORT_XML = DOT_CLASS + "importXml";
    private LoadableModel<ImportOptionsType> model;
    private IModel<String> xmlEditorModel;

    public PageImportXml() {
        model = new LoadableModel<ImportOptionsType>(false) {

            @Override
            protected ImportOptionsType load() {
                return MiscSchemaUtil.getDefaultImportOptions();
            }
        };
        xmlEditorModel = new Model<String>(null);

        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form("mainForm");
        add(mainForm);

        AceEditor<String> xmlEditor = new AceEditor<String>("aceEditor", xmlEditorModel);
        mainForm.add(xmlEditor);

        ImportOptionsPanel importOptions = new ImportOptionsPanel("importOptions", model);
        mainForm.add(importOptions);

        initButtons(mainForm);
    }

    private void initButtons(final Form mainForm) {
        AjaxSubmitLinkButton saveButton = new AjaxSubmitLinkButton("importButton",
                createStringResource("pageImportXml.button.import")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        mainForm.add(saveButton);
    }

    private void savePerformed(AjaxRequestTarget target) {
        String xml = xmlEditorModel.getObject();
        if (StringUtils.isEmpty(xml)) {
            error(getString("pageImportXml.message.emptyXml"));
            target.add(getFeedbackPanel());

            return;
        }

        OperationResult result = new OperationResult(OPERATION_IMPORT_XML);
        InputStream stream = null;
        try {
            Task task = getTaskManager().createTaskInstance(OPERATION_IMPORT_XML);

            stream = IOUtils.toInputStream(xml, "utf-8");
            getModelService().importObjectsFromStream(stream, model.getObject(), task, result);

            result.recomputeStatus();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't import object.", ex);
            LoggingUtils.logException(LOGGER, "Error occured during xml import", ex);
        } finally {
            if (stream != null) {
                IOUtils.closeQuietly(stream);
            }
        }

        if (result.isSuccess()) {
            xmlEditorModel.setObject(null);
        }

        showResult(result);
        target.add(getFeedbackPanel());
    }
}
