/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl.controller;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectColumnType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Generic support methods, useful in all contexts (e.g. both export and import of reports).
 */
class GenericSupport {

    static ExportedReportHeaderColumn getHeaderColumns(GuiObjectColumnType column,
            ItemDefinition<?> objectDefinition,
            LocalizationService localizationService) {
        String label = getLabel(column, objectDefinition, localizationService);
        String cssClass = column.getDisplay() != null ? column.getDisplay().getCssClass() : null;
        String cssStyle = column.getDisplay() != null ? column.getDisplay().getCssStyle() : null;
        return ExportedReportHeaderColumn.fromLabel(label, cssClass, cssStyle);
    }

    static String getLabel(GuiObjectColumnType column,
            ItemDefinition<?> objectDefinition,
            LocalizationService localizationService) {
        ItemPath path = column.getPath() == null ? null : column.getPath().getItemPath();

        DisplayType columnDisplay = column.getDisplay();
        String label;
        if (columnDisplay != null && columnDisplay.getLabel() != null) {
            label = getMessage(localizationService, columnDisplay.getLabel().getOrig());
        } else {

            String name = column.getName();
            String displayName = null;
            if (path != null) {
                ItemDefinition<Item<?, ?>> def = objectDefinition instanceof PrismContainerDefinition<?>
                        ? ((PrismContainerDefinition<?>) objectDefinition).findItemDefinition(path)
                        : null; // TODO for references
                if (def == null) {
                    throw new IllegalArgumentException("Couldn't find item for path " + path);
                }
                displayName = def.getDisplayName();

            }
            if (StringUtils.isNotEmpty(displayName)) {
                label = getMessage(localizationService, displayName);
            } else {
                label = name;
            }
        }
        return label;
    }

    static boolean evaluateCondition(ExpressionType condition, VariablesMap variables, ExpressionFactory factory, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        PrismPropertyValue<Boolean> conditionValue = ExpressionUtil.evaluateCondition(variables, condition,
                null, factory, "Evaluate condition", task, result);
        return conditionValue != null && !Boolean.FALSE.equals(conditionValue.getRealValue());
    }

    protected static String getMessage(LocalizationService localizationService, String key) {
        return getMessage(localizationService, key, (Object) null);
    }

    protected static String getMessage(LocalizationService localizationService, String key, Object... params) {
        return localizationService.translate(key, params, Locale.getDefault(), key);
    }

    protected static String getMessage(LocalizationService localizationService, PolyStringType polyString) {
        return localizationService.translate(polyString.toPolyString(), Locale.getDefault(), true);
    }
}
