/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.PolyStringPropertyColumn;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

public abstract class PopupObjectListPanel<O extends ObjectType> extends ObjectListPanel<O> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PopupObjectListPanel.class);

    /**
     * @param defaultType specifies type of the object that will be selected by default
     */
    public PopupObjectListPanel(String id, Class<? extends O> defaultType, boolean multiselect, PageBase parentPage) {
        super(id, defaultType, null, multiselect);

    }

    public PopupObjectListPanel(String id, Class<? extends O> defaultType, Collection<SelectorOptions<GetOperationOptions>> options,
                                boolean multiselect, PageBase parentPage) {
        super(id, defaultType, null, options, multiselect);
    }

    @Override
    protected IColumn<SelectableBean<O>, String> createCheckboxColumn() {
        if (isMultiselect()) {
            return new CheckBoxHeaderColumn<SelectableBean<O>>() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdateRow(AjaxRequestTarget target, DataTable table, IModel<SelectableBean<O>> rowModel, IModel<Boolean> selected) {
                    super.onUpdateRow(target, table, rowModel, selected);
                    onUpdateCheckbox(target, rowModel);
                }

                @Override
                protected void onUpdateHeader(AjaxRequestTarget target, boolean selected, DataTable table) {
                    super.onUpdateHeader(target, selected, table);
                    onUpdateCheckbox(target, null);
                }

                @Override
                protected IModel<Boolean> getEnabled(IModel<SelectableBean<O>> rowModel) {
                        return PopupObjectListPanel.this.getCheckBoxEnableModel(rowModel);
                }
            };
        }
        return null;
    }

    @Override
    protected IColumn<SelectableBean<O>, String> createNameColumn(IModel<String> columnNameModel, String itemPath, ExpressionType expression) {
        if (!isMultiselect()) {
            String propertyExpression = SelectableBeanImpl.F_VALUE + "." + (StringUtils.isEmpty(itemPath) ? "name" : itemPath);
            String sortProperty = StringUtils.isEmpty(itemPath) ? ObjectType.F_NAME.getLocalPart() : itemPath;
            if (expression != null) {
                propertyExpression = SelectableBeanImpl.F_VALUE +(StringUtils.isEmpty(itemPath) ? "" : ("." + itemPath));
                sortProperty = StringUtils.isEmpty(itemPath) ? "" : itemPath;
            }
            return new LinkColumn<SelectableBean<O>>(
                    columnNameModel == null ? createStringResource("ObjectType.name") : columnNameModel,
                    sortProperty, propertyExpression) {
                private static final long serialVersionUID = 1L;

                @Override
                protected IModel createLinkModel(IModel<SelectableBean<O>> rowModel) {
                    IModel linkModel = new PropertyModel(rowModel, getPropertyExpression());
                    if (linkModel.getObject() != null && linkModel.getObject() instanceof PolyStringType){
                        return Model.of(WebComponentUtil.getTranslatedPolyString((PolyStringType)linkModel.getObject()));
                    }
                    return linkModel;
                }

                @Override
                public void onClick(AjaxRequestTarget target, IModel<SelectableBean<O>> rowModel) {
                    O object = rowModel.getObject().getValue();
                    onSelectPerformed(target, object);
                }

                @Override
                public IModel<String> getDataModel(IModel<SelectableBean<O>> rowModel) {
                    if (expression != null) {
                        Object object = new PropertyModel<>(rowModel, getPropertyExpression()).getObject();
                        return evaluateColumnExpression(object, expression);
                    } else {
                        return super.getDataModel(rowModel);
                    }
                }
            };
        }

        else {
            if ((StringUtils.isEmpty(itemPath) || ObjectType.F_NAME.getLocalPart().equals(itemPath)) && expression == null){
                return new PolyStringPropertyColumn<SelectableBean<O>>(columnNameModel == null ? createStringResource("userBrowserDialog.name") : columnNameModel,
                        ObjectType.F_NAME.getLocalPart(), "value.name");
            } else {
                String propertyExpression = SelectableBeanImpl.F_VALUE + "." + itemPath;
                String sortProperty = itemPath;
                if (expression != null) {
                    propertyExpression = SelectableBeanImpl.F_VALUE +(StringUtils.isEmpty(itemPath) ? "" : ("." + itemPath));
                    sortProperty = StringUtils.isEmpty(itemPath) ? "" : itemPath;
                }
                return new PropertyColumn(
                        columnNameModel == null ? createStringResource("userBrowserDialog.name") : columnNameModel,
                        sortProperty, propertyExpression){

                    @Override
                    public IModel<?> getDataModel(IModel rowModel) {
                        if (expression != null) {
                            Object object = new PropertyModel<>(rowModel, getPropertyExpression()).getObject();
                            return evaluateColumnExpression(object, expression);
                        } else {
                            return super.getDataModel(rowModel);
                        }
                    }
                };
            }
        }
    }

    private IModel<String> evaluateColumnExpression(Object object, ExpressionType expression) {
        Task task = getPageBase().createSimpleTask("evaluate column expression");
        try {
            ExpressionVariables expressionVariables = new ExpressionVariables();
            expressionVariables.put(ExpressionConstants.VAR_OBJECT, object, object.getClass());
            String stringValue = ExpressionUtil.evaluateStringExpression(expressionVariables, getPageBase().getPrismContext(), expression,
                    MiscSchemaUtil.getExpressionProfile(), getPageBase().getExpressionFactory(), "evaluate column expression",
                    task, task.getResult()).iterator().next();
            return Model.of(stringValue);
        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException
                | ConfigurationException | SecurityViolationException e) {
            LOGGER.error("Couldn't execute expression for name column");
            OperationResult result = task.getResult();
            OperationResultStatusPresentationProperties props = OperationResultStatusPresentationProperties.parseOperationalResultStatus(result.getStatus());
            return getPageBase().createStringResource(props.getStatusLabelKey());
        }
    }

    @Override
    protected List<IColumn<SelectableBean<O>, String>> createColumns() {
        return ColumnUtils.getDefaultColumns(getType());
    }

    protected void onSelectPerformed(AjaxRequestTarget target, O object){

    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        return null;
    }

    @Override
    protected void addCustomActions(@NotNull List<InlineMenuItem> actionsList, SerializableSupplier<Collection<? extends ObjectType>> objectsSupplier) {
    }


    protected void onUpdateCheckbox(AjaxRequestTarget target, IModel<SelectableBean<O>> rowModel){

    }

    protected IModel<Boolean> getCheckBoxEnableModel(IModel<SelectableBean<O>> rowModel){
        return Model.of(true);
    }

//    @Override
//    protected boolean isRefreshEnabled() {
//        return false;
//    }
//
//    @Override
//    protected int getAutoRefreshInterval() {
//        return 0;
//    }
}
