/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import java.util.HashMap;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.panel.RoleAnalysisCandidateRoleTable;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisCandidateRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

public class CandidateRolesPopupPanel extends BasePanel<String> implements Popupable {

    private static final String ID_PANEL = "panel";

    public CandidateRolesPopupPanel(String id, IModel<String> messageModel,
            RoleAnalysisClusterType cluster, HashMap<String,
            RoleAnalysisCandidateRoleType> cacheCandidate,
            List<RoleType> roles, List<String> selectedCandidates) {
        super(id, messageModel);

        initLayout(cluster, cacheCandidate, roles, selectedCandidates);
    }

    public void initLayout(RoleAnalysisClusterType cluster, HashMap<String, RoleAnalysisCandidateRoleType> cacheCandidate,
            List<RoleType> roles, List<String> selectedCandidates) {
        RoleAnalysisCandidateRoleTable components = new RoleAnalysisCandidateRoleTable(ID_PANEL,
                cluster, cacheCandidate, roles,selectedCandidates) {
            @Override
            protected boolean isMigrateButtonEnabled() {
                return false;
            }

            @Override
            protected boolean isDeleteOperationEnabled() {
                return false;
            }
        };

        components.setOutputMarkupId(true);
        add(components);
    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    @Override
    public int getWidth() {
        return 60;
    }

    @Override
    public int getHeight() {
        return 50;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        //TODO
        return null;
    }
}
