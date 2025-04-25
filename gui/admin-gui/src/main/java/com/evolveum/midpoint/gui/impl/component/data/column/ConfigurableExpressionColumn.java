/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.data.column;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.util.ProvisioningObjectsUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.util.SelectableRow;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class ConfigurableExpressionColumn<S extends SelectableRow<T>, T extends Serializable> extends AbstractExportableColumn<S, String> {

    private static final Trace LOGGER = TraceManager.getTrace(ConfigurableExpressionColumn.class);

    private static final String DOT_CLASS = ConfigurableExpressionColumn.class.getName() + ".";
    protected static final String OPERATION_EVALUATE_EXPRESSION = DOT_CLASS + "evaluateColumnExpression";
    private static final String OPERATION_LOAD_LOOKUP_TABLE = DOT_CLASS + "loadLookupTable";

    private final GuiObjectColumnType customColumn;
    private final ExpressionType expression;

    private final PageBase modelServiceLocator;

    public ConfigurableExpressionColumn(
            IModel<String> displayModel,
            String sortProperty,
            GuiObjectColumnType customColumns,
            ExpressionType expressionType,
            PageBase modelServiceLocator) {

        super(displayModel, sortProperty);

        this.customColumn = customColumns;
        this.expression = expressionType;
        this.modelServiceLocator = modelServiceLocator;
    }

    protected PageBase getPageBase() {
        return modelServiceLocator;
    }

    @Override
    public void populateItem(org.apache.wicket.markup.repeater.Item<ICellPopulator<S>> item,
            String componentId, IModel<S> rowModel) {
        IModel<?> model = getDataModel(rowModel);
        if (model.getObject() instanceof Collection) {
            RepeatingView listItems = new RepeatingView(componentId);
            for (Object object : (Collection) model.getObject()) {
                listItems.add(new Label(listItems.newChildId(), () -> object));
            }
            item.add(listItems);
        } else {
            item.add(new Label(componentId, model));
        }
        if (customColumn.getDisplay() != null && customColumn.getDisplay().getCssStyle() != null) {
            item.add(AttributeAppender.append("style", customColumn.getDisplay().getCssStyle()));
        }
    }

    @Override
    public IModel<String> getDataModel(IModel<S> rowModel) {
        ItemPath columnPath = WebComponentUtil.getPath(customColumn);

        return new LoadableDetachableModel<>() {

            @Override
            protected String load() {
                return loadExportableColumnDataModel(rowModel, customColumn, columnPath, expression);
            }
        };
    }

    @Override
    public String getCssClass() {
        if (customColumn == null) {
            return super.getCssClass();
        }
        return customColumn.getDisplay() != null ? customColumn.getDisplay().getCssClass() : "";
    }

    public <V> String loadExportableColumnDataModel(IModel<S> rowModel, GuiObjectColumnType customColumn, ItemPath columnPath, ExpressionType expression) {
        S selectable = rowModel.getObject();
        T value = getRowRealValue(selectable);
        if (value == null) {
            return handleNullRowValue(selectable);
        }
        com.evolveum.midpoint.prism.Item<?, ?> item = findItem(value, columnPath);

        if (expression != null) {
            Collection<V> collection = evaluateExpression(value, item, expression, customColumn);
            return getValuesAsString(collection, customColumn.getDisplayValue());
        }
        if (item != null) {
            return evaluateItemValues(item, customColumn.getDisplayValue());
        }

        return handleDefaultValue(selectable);
    }

    protected String handleNullRowValue(S selectable) {
        return "";
    }

    protected String handleDefaultValue(S selectable) {
        return "";
    }

    private Item<?, ?> findItem(T rowRealValue, ItemPath columnPath) {
        if (!isItemPathDefined(columnPath)) {
            return null;
        }
        if (rowRealValue instanceof Referencable) {
            LOGGER.debug("ItemPath expression is not supported for ObjectReferencTypes. Use script expression instead.");
            return null;
        }
        if (rowRealValue instanceof Containerable) {
            return ((Containerable) rowRealValue).asPrismContainerValue().findItem(columnPath);
        }
        return null;
    }

    private boolean isItemPathDefined(ItemPath columnPath) {
        return columnPath != null && !columnPath.isEmpty();
    }

    public T getRowRealValue(S rowValue) {
        return ColumnUtils.unwrapRowRealValue(rowValue);
    }

    private String evaluateItemValues(com.evolveum.midpoint.prism.Item<?, ?> item, DisplayValueType displayValue) {
        return getValuesAsString(item, displayValue, loadLookupTable(item));

    }

    private String getValuesAsString(com.evolveum.midpoint.prism.Item<?, ?> item, DisplayValueType displayValue, PrismObject<LookupTableType> lookupTable) {
        if (DisplayValueType.NUMBER.equals(displayValue)) {
            String number;
            //This is really ugly HACK FIXME TODO
            if (item.getDefinition() != null && UserType.F_LINK_REF.equivalent(item.getDefinition().getItemName())) {
                //noinspection unchecked
                number = ProvisioningObjectsUtil.countLinkFroNonDeadShadows((Collection<ObjectReferenceType>) item.getRealValues());
            } else {
                number = String.valueOf(item.getValues().size());
            }
            return number;
        }
        return item.getValues().stream()
                .filter(Objects::nonNull)
                .map(itemValue -> getStringValue(itemValue, lookupTable))
                .collect(Collectors.joining(", "));
    }

    private <V> String getValuesAsString(Collection<V> collection, DisplayValueType displayValue) {
        if (collection == null) {
            return null;
        }
        if (DisplayValueType.NUMBER.equals(displayValue)) {
            return String.valueOf(collection.size());
        }
        return collection.stream()
                .filter(Objects::nonNull)
                .map(object -> getStringValue(object, null))
                .collect(Collectors.joining(", "));
    }

    protected <V> Collection<V> evaluateExpression(T rowValue, com.evolveum.midpoint.prism.Item<?, ?> columnItem, ExpressionType expression, GuiObjectColumnType customColumn) {
        Task task = getPageBase().createSimpleTask(OPERATION_EVALUATE_EXPRESSION);
        OperationResult result = task.getResult();
        try {
            VariablesMap variablesMap = new VariablesMap();

            if (columnItem != null) {
                variablesMap.put(ExpressionConstants.VAR_INPUT, columnItem, columnItem.getDefinition());
            } else {
                variablesMap.put(ExpressionConstants.VAR_INPUT, null, String.class);
            }

            processVariables(variablesMap, rowValue);
            return evaluate(variablesMap, expression, task, result);
        } catch (Exception e) {
            LOGGER.error("Couldn't execute expression for {} column. Reason: {}", customColumn, e.getMessage(), e);
            result.recomputeStatus();
            OperationResultStatusPresentationProperties props = OperationResultStatusPresentationProperties.parseOperationalResultStatus(result.getStatus());
            return (Collection<V>) Collections.singletonList(getPageBase().createStringResource(props.getStatusLabelKey()).getString());  //TODO: this is not entirely correct
        }
    }

    protected void processVariables(VariablesMap variablesMap, T rowValue) {
        variablesMap.put(ExpressionConstants.VAR_OBJECT, rowValue, rowValue.getClass());
    }

    protected <V> Collection<V> evaluate(VariablesMap variablesMap, ExpressionType expression, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        //noinspection unchecked
        return (Collection<V>) ExpressionUtil.evaluateStringExpression(
                variablesMap, expression,
                MiscSchemaUtil.getExpressionProfile(),
                getPageBase().getExpressionFactory(),
                "evaluate column expression", task, result);
    }

    // todo figure out how to improve this, looks horrible
    private String getStringValue(Object object, PrismObject<LookupTableType> lookupTable) {
        if (object == null) {
            return "";
        }

        if (object instanceof PrismPropertyValue) {
            PrismPropertyValue<?> prismPropertyValue = (PrismPropertyValue<?>) object;
            if (lookupTable == null) {
                if (prismPropertyValue.getValue() == null) {
                    return "";
                } else if (isPolyString(prismPropertyValue.getTypeName())) {
                    return LocalizationUtil.translatePolyString((PolyString) prismPropertyValue.getValue());
                } else if (prismPropertyValue.getValue() instanceof Enum) {
                    object = prismPropertyValue.getValue();
                } else if (prismPropertyValue.getValue() instanceof ObjectType) {
                    object = prismPropertyValue.getValue();
                } else {
                    return String.valueOf(prismPropertyValue.getValue());
                }
            } else {

                String lookupTableKey = prismPropertyValue.getValue().toString();
                LookupTableType lookupTableObject = lookupTable.asObjectable();
                String rowLabel = "";
                for (LookupTableRowType lookupTableRow : lookupTableObject.getRow()) {
                    if (lookupTableRow.getKey().equals(lookupTableKey)) {
                        return lookupTableRow.getLabel() != null ? lookupTableRow.getLabel().getOrig() : lookupTableRow.getValue();
                    }
                }
                return rowLabel;
            }
        }

        if (object instanceof Enum) {
            return getPageBase().createStringResource((Enum) object).getString();
        }
        if (object instanceof ObjectType) {
            return getStringValueForObject((ObjectType) object);
        }
        if (object instanceof PrismObject) {
            return WebComponentUtil.getDisplayName((PrismObject) object);
        }
        if (object instanceof PrismObjectValue) {
            return WebComponentUtil.getDisplayName(((PrismObjectValue) object).asPrismObject());
        }
        if (object instanceof PrismReferenceValue) {
            return WebComponentUtil.getDisplayName(((PrismReferenceValue) object).getRealValue(), true);
        }
        if (object instanceof ObjectReferenceType) {
            return WebComponentUtil.getDisplayName(((ObjectReferenceType) object));
        }
        return object.toString();
    }

    protected String getStringValueForObject(ObjectType object) {
        String displayName = WebComponentUtil.getDisplayNameOrName(object.asPrismObject());
        return StringUtils.isEmpty(displayName) ? object.getOid() : displayName;
    }

    private boolean isPolyString(QName typeName) {
        return QNameUtil.match(typeName, PolyStringType.COMPLEX_TYPE);
    }

    private PrismObject<LookupTableType> loadLookupTable(com.evolveum.midpoint.prism.Item<?, ?> item) {
        String lookupTableOid = getValueEnumerationRefOid(item);
        if (lookupTableOid == null) {
            return null;
        }
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_LOOKUP_TABLE);
        OperationResult result = task.getResult();

        Collection<SelectorOptions<GetOperationOptions>> options = WebModelServiceUtils
                .createLookupTableRetrieveOptions(getPageBase().getSchemaService());
        return WebModelServiceUtils.loadObject(LookupTableType.class,
                lookupTableOid, options, getPageBase(), task, result);
    }

    private String getValueEnumerationRefOid(com.evolveum.midpoint.prism.Item<?, ?> item) {
        ItemDefinition<?> def = item.getDefinition();
        if (def == null) {
            return null;
        }

        PrismReferenceValue valueEnumerationRef = def.getValueEnumerationRef();
        if (valueEnumerationRef == null) {
            return null;
        }

        return valueEnumerationRef.getOid();
    }
}
