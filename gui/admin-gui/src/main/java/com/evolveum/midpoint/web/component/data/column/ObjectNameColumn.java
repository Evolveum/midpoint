/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.web.page.error.PageOperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

/**
 * @author semancik
 *
 */
public class ObjectNameColumn<O extends ObjectType> extends AbstractColumn<SelectableBean<O>, String>
        implements IExportableColumn<SelectableBean<O>, String>{
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ObjectNameColumn.class);

    private ExpressionType expression;
    private PageBase pageBase;

    public ObjectNameColumn(IModel<String> displayModel) {
        super(displayModel, ObjectType.F_NAME.getLocalPart());
    }

    public ObjectNameColumn(IModel<String> displayModel, String itemPath, ExpressionType expression, PageBase pageBase) {
        super(displayModel, itemPath);
        this.expression = expression;
        this.pageBase = pageBase;
    }

    @Override
    public void populateItem(final Item<ICellPopulator<SelectableBean<O>>> cellItem, String componentId,
                             final IModel<SelectableBean<O>> rowModel) {

        IModel<String> labelModel = new IModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                SelectableBean<O> selectableBean = rowModel.getObject();
                O value = selectableBean.getValue();
                if (value == null) {
                    OperationResult result = selectableBean.getResult();
                    OperationResultStatusPresentationProperties props = OperationResultStatusPresentationProperties.parseOperationalResultStatus(result.getStatus());
                    return cellItem.getString(props.getStatusLabelKey());
                } else {
                    String name;
                    if (expression != null){
                        Task task = pageBase.createSimpleTask("evaluate column expression");
                        try {
                            Object object;
                            if (getSortProperty().isEmpty()) {
                                object = value;
                            } else {
                                object = new PropertyModel<>(value, getSortProperty()).getObject();
                            }
                            ExpressionVariables expressionVariables = new ExpressionVariables();
                            expressionVariables.put(ExpressionConstants.VAR_OBJECT, object, object.getClass());
                            name = ExpressionUtil.evaluateStringExpression(expressionVariables, pageBase.getPrismContext(), expression,
                                    MiscSchemaUtil.getExpressionProfile(), pageBase.getExpressionFactory(), "evaluate column expression",
                                    task, task.getResult()).iterator().next();
                        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException
                                | ConfigurationException | SecurityViolationException e) {
                            LOGGER.error("Couldn't execute expression for name column");
                            OperationResult result = task.getResult();
                            OperationResultStatusPresentationProperties props = OperationResultStatusPresentationProperties.parseOperationalResultStatus(result.getStatus());
                            return cellItem.getString(props.getStatusLabelKey());
                        }
                    } else {
                        name = WebComponentUtil.getName(value, true);
                        if (selectableBean.getResult() != null) {
                            StringBuilder complexName = new StringBuilder();
                            complexName.append(name);
                            complexName.append(" (");
                            complexName.append(selectableBean.getResult().getStatus());
                            complexName.append(")");
                            return complexName.toString();
                        }
                    }
                        return name;


                }
            }
        };

        if (isClickable(rowModel)) {        // beware: rowModel is very probably resolved at this moment; but it seems to cause no problems
            cellItem.add(new AjaxLinkPanel(componentId, labelModel) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    SelectableBean<O> selectableBean = rowModel.getObject();
                    O value = selectableBean.getValue();
                    if (value == null) {
                        OperationResult result = selectableBean.getResult();
                        throw new RestartResponseException(new PageOperationResult(result));
                    } else {
                        if (selectableBean.getResult() != null) {
                            throw new RestartResponseException(new PageOperationResult(selectableBean.getResult()));
                        } else {
                            ObjectNameColumn.this.onClick(target, rowModel);
                        }
                    }
                }
            });
        } else {
            cellItem.add(new Label(componentId, labelModel));
        }
    }

    public boolean isClickable(IModel<SelectableBean<O>> rowModel) {
        return true;
    }

    public void onClick(AjaxRequestTarget target, IModel<SelectableBean<O>> rowModel) {
    }

    public IModel<String> getDataModel(IModel<SelectableBean<O>> rowModel) {
        SelectableBean<O> selectableBean = rowModel.getObject();
        O value = selectableBean.getValue();
        return Model.of(value == null ? "" : WebComponentUtil.getName(value, true));
    }

}
