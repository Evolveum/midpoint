/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.MutablePrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportParameterType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.List;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

public class ReportParameterTypeUtil {

    public static ReportParameterType createParameters(@NotNull String paramName, @Nullable Object realValue)
            throws SchemaException {
        ReportParameterType parameters = new ReportParameterType();
        addParameter(parameters, paramName, realValue);
        return parameters;
    }

    public static void addParameters(ReportParameterType parameters, List<String> names, List<Object> realValues)
            throws SchemaException {
        argCheck(
                names.size() == realValues.size(),
                "Parameter names and values do not match: %s vs %s", names, realValues);
        for (int i = 0; i < names.size(); i++) {
            addParameter(parameters, names.get(i), realValues.get(i));
        }
    }

    public static void addParameter(
            @NotNull ReportParameterType parameters, @NotNull String paramName, Object... realValues)
            throws SchemaException {
        if (realValues.length == 0) {
            return;
        }

        Object firstValue = realValues[0];
        QName typeName = PrismContext.get().getSchemaRegistry().determineTypeForClass(firstValue.getClass());
        MutablePrismPropertyDefinition<Object> paramPropDef =
                PrismContext.get().definitionFactory().createPropertyDefinition(
                        new QName(SchemaConstants.NS_REPORT_EXTENSION, paramName), typeName);
        paramPropDef.setDynamic(true);
        paramPropDef.setRuntimeSchema(true);
        paramPropDef.toMutable().setMaxOccurs(1);

        PrismProperty<Object> paramProperty = paramPropDef.instantiate();
        paramProperty.addRealValues(realValues);
        //noinspection unchecked
        ((PrismContainerValue<ReportParameterType>) parameters.asPrismContainerValue()).add(paramProperty);
    }
}
