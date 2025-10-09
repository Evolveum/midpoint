/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.reports.component;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.component.ColumnTypeConfigContext;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.PropertySearchItemWrapper;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class AuditLogViewerContext extends ColumnTypeConfigContext {

    private IModel<Search<AuditEventRecordType>> searchModel;

    public AuditLogViewerContext(IModel<Search<AuditEventRecordType>> searchModel) {
        this.searchModel = searchModel;
    }

    public IModel<Search<AuditEventRecordType>> getSearchModel() {
        return searchModel;
    }

    public boolean isChangedItemSearchItemVisible() {
        // noinspection unchecked
        PropertySearchItemWrapper<ItemPathType> wrapper = getSearchModel().getObject()
                .findPropertySearchItem(AuditEventRecordType.F_CHANGED_ITEM);

        return wrapper != null && wrapper.isVisible();
    }
}
