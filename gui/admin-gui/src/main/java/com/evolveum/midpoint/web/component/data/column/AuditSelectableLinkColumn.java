/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import java.io.Serial;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.reports.PageAuditLogDetails;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectColumnType;

/**
 * Clickable (first) column for audit records. Maybe might be somehow unified with ObjectNameColumn.
 */
public class AuditSelectableLinkColumn extends AbstractNameColumn<SelectableBean<AuditEventRecordType>, AuditEventRecordType> {

    public AuditSelectableLinkColumn(IModel<String> displayModel, String sortProperty, GuiObjectColumnType customColumn, ExpressionType expression, PageBase pageBase) {
        super(displayModel, sortProperty, customColumn, expression, pageBase);
    }

    @Override
    protected IModel<String> getContainerName(SelectableBean< AuditEventRecordType > selectableBean) {
        return () -> {
            AuditEventRecordType record = selectableBean == null ? null : selectableBean.getValue() ;
            if (record == null) {
                return null;
            }
            return WebComponentUtil.formatDate(record.getTimestamp());
        };
    }

    @Override
    protected Component createComponent(String componentId, IModel<String> labelModel, IModel<SelectableBean<AuditEventRecordType>> rowModel) {
        return new LinkPanel(componentId, labelModel) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                AuditEventRecordType record = unwrapModel(rowModel);
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, record.getRepoId());
                getPageBase().navigateToNext(PageAuditLogDetails.class, parameters);
            }

            @Override
            public boolean isEnabled() {
                return unwrapModel(rowModel) != null;
            }
        };
    }

    protected AuditEventRecordType unwrapModel(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
        if (rowModel == null || rowModel.getObject() == null) {
            return null;
        }
        return rowModel.getObject().getValue();
    }

}
