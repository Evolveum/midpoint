/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.reports.component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.data.column.ConfigurableExpressionColumn;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectColumnType;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

public class DeltaExpressionColumn extends ConfigurableExpressionColumn<SelectableBean<AuditEventRecordType>, AuditEventRecordType> {

    public DeltaExpressionColumn(IModel<String> displayModel, String sortProperty, GuiObjectColumnType customColumns, SerializableSupplier<VariablesMap> variablesSupplier, ExpressionType expressionType, PageBase modelServiceLocator) {
        super(displayModel, sortProperty, customColumns, variablesSupplier, expressionType, modelServiceLocator);
    }

    @Override
    protected Component createLabel(String componentId, IModel<?> model) {
        IModel<String> labelModel = new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                Object obj = model.getObject();
                String str = obj != null ? obj.toString() : "";
                return DeltaColumnPanel.escapePrettyPrintedValue(str);
            }
        };

        MultiLineLabel label = new MultiLineLabel(componentId, labelModel);
        label.setEscapeModelStrings(false);

        return label;
    }

    @Override
    protected String getStringValueDelimiter() {
        return "\n";
    }
}
