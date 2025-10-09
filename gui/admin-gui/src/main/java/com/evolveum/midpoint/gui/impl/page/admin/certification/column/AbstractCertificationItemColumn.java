/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.column;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertificationColumnTypeConfigContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectColumnType;

import org.apache.wicket.model.IModel;

public abstract class AbstractCertificationItemColumn extends AbstractGuiColumn<AccessCertificationWorkItemType,
        PrismContainerValueWrapper<AccessCertificationWorkItemType>>{

    CertificationColumnTypeConfigContext context;

    public AbstractCertificationItemColumn(GuiObjectColumnType columnConfig, CertificationColumnTypeConfigContext context) {
        super(columnConfig);
        this.context = context;
    }

    public AccessCertificationWorkItemType unwrapRowModel(IModel<PrismContainerValueWrapper<AccessCertificationWorkItemType>> rowModel) {
        return rowModel != null && rowModel.getObject() != null ? rowModel.getObject().getRealValue() : null;
    }
}
