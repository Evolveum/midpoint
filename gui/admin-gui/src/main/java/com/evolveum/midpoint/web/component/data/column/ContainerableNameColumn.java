/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import java.util.Collection;

import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Containerable;
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

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class ContainerableNameColumn<C extends Containerable> extends AbstractColumn<SelectableBean<C>, String>
        implements IExportableColumn<SelectableBean<C>, String> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ObjectNameColumn.class);

    private final ExpressionType expression;
    private final PageBase pageBase;
    private final ItemPath itemPath;

    public ContainerableNameColumn(IModel<String> displayModel, ItemPath itemPath, ExpressionType expression, PageBase pageBase) {
        super(displayModel, itemPath.toString());

        this.expression = expression;
        this.pageBase = pageBase;
        this.itemPath = itemPath;
    }

    @Override
    public void populateItem(final Item<ICellPopulator<SelectableBean<C>>> cellItem, String componentId,
            final IModel<SelectableBean<C>> rowModel) {

        IModel<String> labelModel = createLabelModel(cellItem, rowModel);

        cellItem.add(createComponent(componentId, labelModel, rowModel));
    }

    private Component createComponent(String componentId, IModel<String> labelModel, IModel<SelectableBean<C>> rowModel) {
        return new AjaxLinkPanel(componentId, labelModel) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onClickPerformed(target, rowModel);
            }

            @Override
            public boolean isEnabled() {
                return ContainerableNameColumn.this.isClickable(rowModel);
            }
        };
    }

    private void onClickPerformed(AjaxRequestTarget target, IModel<SelectableBean<C>> rowModel) {
        SelectableBean<C> selectableBean = rowModel.getObject();
        C value = selectableBean.getValue();
        if (value == null) {
            OperationResult result = selectableBean.getResult();
            throw new RestartResponseException(new PageOperationResult(result));
        } else {
            if (selectableBean.getResult() != null) {
                throw new RestartResponseException(new PageOperationResult(selectableBean.getResult()));
            } else {
                ContainerableNameColumn.this.onClick(target, rowModel);
            }
        }
    }

    //TODO: this is almost the same as in ContainerableListPanel.. should be unified
    private IModel<String> createLabelModel(Item<ICellPopulator<SelectableBean<C>>> cellItem, IModel<SelectableBean<C>> rowModel) {
        return () -> {
            SelectableBean<C> selectableBean = rowModel.getObject();
            C value = selectableBean.getValue();
            if (value == null) {
                return getResultAsString(cellItem, selectableBean);
            }

            if (expression != null) {
                return evaluateExpression(cellItem, itemPath, value);
            }

            return getName(rowModel);
        };
    }

    private String getResultAsString(Item<ICellPopulator<SelectableBean<C>>> cellItem, SelectableBean<C> selectableBean) {
        OperationResult result = selectableBean.getResult();
        OperationResultStatusPresentationProperties props = OperationResultStatusPresentationProperties.parseOperationalResultStatus(result.getStatus());
        return cellItem.getString(props.getStatusLabelKey());
    }

    private String evaluateExpression(Item<ICellPopulator<SelectableBean<C>>> cellItem, ItemPath itemPath, C value) {
        Task task = pageBase.createSimpleTask("evaluate column expression");
        try {

            com.evolveum.midpoint.prism.Item<?, ?> item = value.asPrismContainerValue().findItem(itemPath);
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

    private String getName(IModel<SelectableBean<C>> rowModel) {
        IModel<String> containerName = getContainerName(rowModel);

        SelectableBean<C> selectableBean = rowModel.getObject();
        String result = "";
        if (selectableBean.getResult() != null) {
            result = " (" + selectableBean.getResult().getStatus() + ")";
        }

        return containerName.getObject() + result;
    }

    public boolean isClickable(IModel<SelectableBean<C>> rowModel) {
        return true;
    }

    public void onClick(AjaxRequestTarget target, IModel<SelectableBean<C>> rowModel) {
    }

    @Override
    public IModel<String> getDataModel(IModel<SelectableBean<C>> rowModel) {
        return getContainerName(rowModel);
    }

    protected abstract IModel<String> getContainerName(IModel<SelectableBean<C>> rowModel);
}
