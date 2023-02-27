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

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectColumnType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

public class ReportExpressionColumn<C extends Serializable> extends ConfigurableExpressionColumn<SelectableBean<C>, C> {

    public ReportExpressionColumn(IModel<String> displayModel, String sortProperty, GuiObjectColumnType customColumns, ExpressionType expressionType, PageBase modelServiceLocator) {
        super(displayModel, sortProperty, customColumns, expressionType, modelServiceLocator);
    }

    @Override
    protected void processVariables(VariablesMap variablesMap, C rowValue) {
        processReportSpecificVariables(variablesMap);
        if (!variablesMap.containsKey(ExpressionConstants.VAR_OBJECT)) {
            variablesMap.put(ExpressionConstants.VAR_OBJECT, rowValue, rowValue != null ? rowValue.getClass() : null);
        }
    }

    protected void processReportSpecificVariables(VariablesMap variablesMap) {

    }

    @Override
    protected <V> Collection<V> evaluate(VariablesMap variablesMap, ExpressionType expression, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        Object object = getPageBase().getReportManager().evaluateScript(getReport(), expression, variablesMap, "evaluate column expression", task, result);
        if (object instanceof Collection) {
            return (Collection) object;
        }
        return (Collection<V>) Collections.singletonList(object);
    }

    protected PrismObject<ReportType> getReport() {
        return null;
    }
}
