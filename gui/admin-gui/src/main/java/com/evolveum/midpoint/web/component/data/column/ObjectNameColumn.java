/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.data.column;

import java.util.Collection;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;

import com.evolveum.midpoint.util.PrettyPrinter;

import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.web.page.error.PageOperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 */
public class ObjectNameColumn<O extends ObjectType> extends AbstractColumn<SelectableBean<O>, String>
        implements IExportableColumn<SelectableBean<O>, String> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ObjectNameColumn.class);

    private final ExpressionType expression;
    private final PageBase pageBase;
    private final ItemPath itemPath;

    public ObjectNameColumn(IModel<String> displayModel) {
        this(displayModel, null, null, null, true);
    }

    @Override
    public String getCssClass() {
        return super.getCssClass();
    }

    public ObjectNameColumn(IModel<String> displayModel, ItemPath itemPath, ExpressionType expression, PageBase pageBase, boolean useDefaultPath) {
        super(displayModel, useDefaultPath ? ObjectType.F_NAME.getLocalPart() : itemPath.toString());
        this.expression = expression;
        this.pageBase = pageBase;
        this.itemPath = itemPath;
    }

    @Override
    public void populateItem(final Item<ICellPopulator<SelectableBean<O>>> cellItem, String componentId,
            final IModel<SelectableBean<O>> rowModel) {

        IModel<String> labelModel = createLabelModel(cellItem, rowModel);

        cellItem.add(createComponent(componentId, labelModel, rowModel));
    }

    protected Component createComponent(String componentId, IModel<String> labelModel, IModel<SelectableBean<O>> rowModel) {
        if (isClickable(rowModel)) {        // beware: rowModel is very probably resolved at this moment; but it seems to cause no problems
            return new AjaxLinkPanel(componentId, labelModel) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    onClickPerformed(target, rowModel);
                }
            };
        }

        return new Label(componentId, labelModel);

    }

    private void onClickPerformed(AjaxRequestTarget target, IModel<SelectableBean<O>> rowModel) {
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

    //TODO: this is almost the same as in ContainerableListPanel.. should be unified
    protected IModel<String> createLabelModel(Item<ICellPopulator<SelectableBean<O>>> cellItem, IModel<SelectableBean<O>> rowModel) {
        return new IModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                SelectableBean<O> selectableBean = rowModel.getObject();
                O value = selectableBean.getValue();
                if (value == null) {
                    return getResultAsString(cellItem, selectableBean);
                }

                if (expression != null) {
                    return evaluateExpression(cellItem, itemPath, value);
                }

                if (itemPath == null || itemPath.equals("name")) {
                    return getName(value, selectableBean);
                }

                com.evolveum.midpoint.prism.Item item = value.asPrismObject().findItem(itemPath);
                return item != null ? cellItem.getDefaultModelObjectAsString(item.getRealValue()) : "";

            }
        };
    }

    private String getResultAsString(Item<ICellPopulator<SelectableBean<O>>> cellItem, SelectableBean<O> selectableBean) {
        OperationResult result = selectableBean.getResult();
        OperationResultStatusPresentationProperties props = OperationResultStatusPresentationProperties.parseOperationalResultStatus(result.getStatus());
        return cellItem.getString(props.getStatusLabelKey());
    }

    private String evaluateExpression(Item<ICellPopulator<SelectableBean<O>>> cellItem, ItemPath itemPath, O value) {
        Task task = pageBase.createSimpleTask("evaluate column expression");
        try {

            com.evolveum.midpoint.prism.Item<?, ?> item = value.asPrismObject().findItem(itemPath);
            VariablesMap variablesMap = new VariablesMap();
            variablesMap.put(ExpressionConstants.VAR_OBJECT, value, value.getClass());
            if (item != null) {
                variablesMap.put(ExpressionConstants.VAR_INPUT, item, item.getDefinition().getTypeClass());
            }
            Collection<String> evaluatedValues = ExpressionUtil.evaluateStringExpression(variablesMap, pageBase.getPrismContext(), expression,
                    MiscSchemaUtil.getExpressionProfile(), pageBase.getExpressionFactory(), "evaluate column expression",
                    task, task.getResult());
            if (evaluatedValues != null) {
                return evaluatedValues.iterator().next();
            }
            return null;
        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException
                | ConfigurationException | SecurityViolationException e) {
            LOGGER.error("Couldn't execute expression for name column");
            OperationResult result = task.getResult();
            OperationResultStatusPresentationProperties props = OperationResultStatusPresentationProperties.parseOperationalResultStatus(result.getStatus());
            return cellItem.getString(props.getStatusLabelKey());
        }
    }

    private String getName(O value, SelectableBean<O> selectableBean) {
        String name = WebComponentUtil.getName(value, true);
        if (selectableBean.getResult() != null) {
            return name + " (" + selectableBean.getResult().getStatus() + ")";
        }
        return name;
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
