/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.page.admin.component.InlineOperationalButtonsPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

public class RoleAnalysisClusterOperationButtonPanel extends InlineOperationalButtonsPanel<RoleAnalysisClusterType> {

    public RoleAnalysisClusterOperationButtonPanel(String id, LoadableModel<PrismObjectWrapper<RoleAnalysisClusterType>> wrapperModel) {
        super(id, wrapperModel);
    }

    @Override
    protected IModel<String> getDeleteButtonLabelModel(PrismObjectWrapper<RoleAnalysisClusterType> modelObject) {
        return () -> LocalizationUtil.translate("RoleAnalysisClusterOperationButtonPanel.delete");
    }

    @Override
    protected IModel<String> createSubmitButtonLabelModel(PrismObjectWrapper<RoleAnalysisClusterType> modelObject) {
        return () -> LocalizationUtil.translate("RoleAnalysisClusterOperationButtonPanel.save");
    }

    @Override
    protected IModel<String> getTitle() {
        return createStringResource("RoleAnalysis.page.cluster.title");
    }
}
