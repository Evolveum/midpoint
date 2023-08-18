/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import java.io.Serial;
import javax.xml.namespace.QName;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SummaryPanelSpecificationType;

public class ClusterSummaryPanel extends FocusSummaryPanel<RoleAnalysisClusterType> {
    @Serial private static final long serialVersionUID = 8087858942603720878L;

    public ClusterSummaryPanel(String id, IModel<RoleAnalysisClusterType> model, SummaryPanelSpecificationType summaryPanelSpecificationType) {
        super(id, RoleAnalysisClusterType.class, model, summaryPanelSpecificationType);
    }

    @Override
    protected QName getDisplayNamePropertyName() {
        return RoleAnalysisClusterType.F_NAME;
    }

    @Override
    protected QName getTitlePropertyName() {
        return RoleAnalysisClusterType.F_SUBTYPE;
    }

    @Override
    protected String getDefaultIconCssClass() {
        return "fa fa-users";
    }

    @Override
    protected String getIconBoxAdditionalCssClass() {
        return "summary-panel-role";
    }

    @Override
    protected String getBoxAdditionalCssClass() {
        return "summary-panel-role";
    }

}
