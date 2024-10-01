/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.column;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertificationGuiConfigContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectColumnType;

import org.apache.wicket.model.IModel;

public abstract class AbstractCertificationItemColumn extends AbstractGuiColumn<AccessCertificationWorkItemType,
        PrismContainerValueWrapper<AccessCertificationWorkItemType>>{

    CertificationGuiConfigContext context;

    public AbstractCertificationItemColumn(GuiObjectColumnType columnConfig, CertificationGuiConfigContext context) {
        super(columnConfig);
        this.context = context;
    }

    public AccessCertificationWorkItemType unwrapRowModel(IModel<PrismContainerValueWrapper<AccessCertificationWorkItemType>> rowModel) {
        return rowModel != null && rowModel.getObject() != null ? rowModel.getObject().getRealValue() : null;
    }
}
