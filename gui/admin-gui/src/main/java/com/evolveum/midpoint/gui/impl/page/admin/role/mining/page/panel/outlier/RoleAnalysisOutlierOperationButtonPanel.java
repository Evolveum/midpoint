/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.page.admin.component.InlineOperationalButtonsPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

import org.apache.wicket.model.IModel;

public class RoleAnalysisOutlierOperationButtonPanel extends InlineOperationalButtonsPanel<RoleAnalysisOutlierType> {

    public RoleAnalysisOutlierOperationButtonPanel(String id, LoadableModel<PrismObjectWrapper<RoleAnalysisOutlierType>> wrapperModel) {
        super(id, wrapperModel);
    }

    @Override
    protected IModel<String> getDeleteButtonLabelModel(PrismObjectWrapper<RoleAnalysisOutlierType> modelObject) {
        return () -> LocalizationUtil.translate("RoleAnalysisOutlierOperationButtonPanel.delete");
    }

    @Override
    protected IModel<String> createSubmitButtonLabelModel(PrismObjectWrapper<RoleAnalysisOutlierType> modelObject) {
        return () -> LocalizationUtil.translate("RoleAnalysisOutlierOperationButtonPanel.save");
    }

    @Override
    protected IModel<String> getTitle() {
        return createStringResource("RoleAnalysis.page.outlier.title");
    }
}
