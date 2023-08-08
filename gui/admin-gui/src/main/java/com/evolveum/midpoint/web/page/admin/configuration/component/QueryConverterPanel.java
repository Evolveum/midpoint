/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration.component;

import java.io.Serial;
import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.ObjectTypeListUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.input.DataLanguagePanel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;
import com.evolveum.midpoint.web.page.admin.configuration.dto.QueryDto;
import com.evolveum.midpoint.web.page.admin.reports.component.SimpleAceEditorPanel;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

public class QueryConverterPanel extends BasePanel<QueryDto> {

    private static final Trace LOGGER = TraceManager.getTrace(QueryConverterPanel.class);

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_OBJECT_TYPE = "objectType";
    private static final String ID_XML_QUERY = "xmlQuery";
    private static final String ID_AXIOM_QUERY = "axiomQuery";
    private static final String ID_CONVERT_QUERY = "convertQuery";

    private static final String ID_DATA_LANGUAGE = "viewButtonPanel";

    private String dataLanguage;

    public QueryConverterPanel(String id, IModel<QueryDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        if (dataLanguage == null) {
            dataLanguage = getPageBase().determineDataLanguage();
        }

        initLayout();
    }

    private void initLayout() {

        MidpointForm<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        add(mainForm);

        DropDownChoicePanel<QName> objectTypeChoice = new DropDownChoicePanel<>(ID_OBJECT_TYPE,
                new PropertyModel<>(getModel(), QueryDto.F_OBJECT_TYPE),
                new ListModel<>(ObjectTypeListUtil.createSearchableTypeList()),
                new QNameObjectTypeChoiceRenderer());
        objectTypeChoice.setOutputMarkupId(true);
        objectTypeChoice.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        objectTypeChoice.getBaseFormComponent().setNullValid(true);
        mainForm.add(objectTypeChoice);

        DataLanguagePanel<QueryType> dataLanguagePanel =
                new DataLanguagePanel<>(ID_DATA_LANGUAGE, dataLanguage, QueryType.class, getPageBase()) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected void onLanguageSwitched(AjaxRequestTarget target, int updatedIndex, String updatedLanguage,
                            String objectString) {
                        QueryConverterPanel.this.getModel().getObject().setMidPointQuery(objectString);
                        dataLanguage = updatedLanguage;
                        target.add(mainForm);
                    }

                    @Override
                    protected String getObjectStringRepresentation() {
                        return QueryConverterPanel.this.getModel().getObject().getMidPointQuery();
                    }
                };
        dataLanguagePanel.setOutputMarkupId(true);
        mainForm.add(dataLanguagePanel);

        SimpleAceEditorPanel xmlQuery = new SimpleAceEditorPanel(ID_XML_QUERY, new PropertyModel<>(getModel(), QueryDto.F_XML_QUERY), 400);
        xmlQuery.setOutputMarkupId(true);
        mainForm.add(xmlQuery);

        SimpleAceEditorPanel axiomQuery = new SimpleAceEditorPanel(ID_AXIOM_QUERY, new PropertyModel<>(getModel(), QueryDto.F_MIDPOINT_QUERY), 200);
        axiomQuery.getBaseFormComponent().setEnabled(false);
        axiomQuery.setOutputMarkupId(true);
        mainForm.add(axiomQuery);

        AjaxButton convertQuery = new AjaxButton(ID_CONVERT_QUERY,
                createStringResource("PageRepositoryQuery.button.convertQuery")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                convertQueryPerformed(target);
            }

        };
        mainForm.add(convertQuery);
    }

    private void convertQueryPerformed(AjaxRequestTarget target) {
        Task task = getPageBase().createSimpleTask("evaluate query expressions");
        OperationResult result = task.getResult();
        try {
            ObjectQuery query = getQueryTypeFromXmlQuery(task, result);
            String axiomQuery = getPrismContext()
                    .querySerializer()
                    .serialize(query.getFilter(), getPrismContext().getSchemaRegistry().staticNamespaceContext())
                    .filterText();
            getModelObject().setMidPointQuery(axiomQuery);
            result.recordSuccess();
        } catch (Throwable e) {
            LoggingUtils.logException(LOGGER, "Couldn't evaluate query expressions", e);
            result.recordFatalError("Couldn't evaluate query expressions", e);
        }
        if (!result.isSuccess()) {
            showResult(result);
        }
        target.add(get(createComponentPath(ID_MAIN_FORM, ID_AXIOM_QUERY)));
        target.add(getFeedbackPanel());
    }

    private ObjectQuery getQueryTypeFromXmlQuery(Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        String xmlQuery = getXmlQuery();
        QueryType queryType = getPrismContext().parserFor(xmlQuery).language(dataLanguage).parseRealValue(QueryType.class);
        ObjectQuery objectQuery = getPrismContext().getQueryConverter().createObjectQuery(getClassFromObjectType(), queryType);

        return ExpressionUtil.evaluateQueryExpressions(
                    objectQuery, new VariablesMap(),
                    MiscSchemaUtil.getExpressionProfile(),
                    getPageBase().getExpressionFactory(),
                    "evaluate query expressions", task, result);
    }

    private String getXmlQuery() {
        return getModelObject().getXmlQuery();
    }

    private Class<?> getClassFromObjectType() {
        return WebComponentUtil.qnameToAnyClass(getPrismContext(), getModelObject().getObjectType());
    }

}
