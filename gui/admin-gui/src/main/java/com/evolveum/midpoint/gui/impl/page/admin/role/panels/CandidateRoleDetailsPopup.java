/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.panels;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.CandidateRole;
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.CostResult;
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.CostResultSingle;
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.UrType;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.panels.tables.TableSimpleCostPopup;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class CandidateRoleDetailsPopup extends BasePanel<String> implements Popupable {

    private static final String ID_CANCEL_OK = "cancel";
    private static final String ID_FORM = "form";
    private static final String ID_DATATABLE = "datatable_extra_rbac";

    public CandidateRoleDetailsPopup(String id, IModel<String> messageModel, CostResult costResult, boolean modePermission,
            boolean modeRole) {
        super(id, messageModel);

        initLayout(costResult, modePermission, modeRole);
    }

    public List<CostResultSingle> generateCostResultSingle(CostResult costResult, boolean modeRole, boolean modePermission) {
        List<CostResultSingle> costResultList = new ArrayList<>();

        List<UrType> candidateUserRole = costResult.getCandidateUserRole();

        CandidateRole candidateRole = costResult.getCandidateRole();

        if (modeRole) {
            for (UrType urType : candidateUserRole) {

                UserType userObjectType = urType.getUserObjectType();
                List<RoleType> userOriginalRoles = urType.getRoleMembers();
                int userOriginalRolesSize = userOriginalRoles.size();

                List<String> userPossibleRoles = new ArrayList<>();
                userPossibleRoles.add("key-" + candidateRole.getKey());
                for (RoleType roleType : userOriginalRoles) {
                    if (!candidateRole.getCandidateRoles().contains(roleType)) {
                        userPossibleRoles.add(roleType.getName().toString());
                    }
                }

                int userPossibleRolesSize = userPossibleRoles.size();
                double reduceValue = ((userOriginalRolesSize - userPossibleRolesSize) / (double) userOriginalRolesSize) * 100;

                costResultList.add(new CostResultSingle(userObjectType, userOriginalRoles, userPossibleRoles, reduceValue,candidateRole,false));
            }
        } else if (modePermission) {
            for (UrType urType : candidateUserRole) {

                UserType userObjectType = urType.getUserObjectType();
                List<RoleType> userOriginalRoles = urType.getRoleMembers();
                int userOriginalRolesSize = userOriginalRoles.size();

                List<String> userPossibleRoles = new ArrayList<>();
                userPossibleRoles.add("key-" + candidateRole.getKey());
                for (RoleType roleType : userOriginalRoles) {
                    if (!candidateRole.getCandidatePermissions().containsAll(roleType.getAuthorization())) {
                        userPossibleRoles.add(roleType.getName().toString());
                    }
                }

                int userPossibleRolesSize = userPossibleRoles.size();
                double reduceValue = ((userOriginalRolesSize - userPossibleRolesSize) / (double) userOriginalRolesSize) * 100;

                costResultList.add(new CostResultSingle(userObjectType, userOriginalRoles, userPossibleRoles,
                        reduceValue,candidateRole,false));
            }
        }

        return costResultList;
    }

    private void initLayout(CostResult costResult, boolean modePermission,
            boolean modeRole) {

        Form<?> form = new MidpointForm<>(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);

        form.add(new TableSimpleCostPopup(ID_DATATABLE, generateCostResultSingle(costResult, modeRole, modePermission)));

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL_OK,
                createStringResource("Button.cancel")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ((PageBase) getPage()).getMainPopup().close(target);
            }
        };
        add(cancelButton);

    }

    @Override
    public int getWidth() {
        return 700;
    }

    @Override
    public int getHeight() {
        return 600;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return new StringResourceModel("RoleMining.candidateRoleDetails.popup.title");
    }

    protected IModel<String> getWarningMessageModel() {
        return new StringResourceModel("RoleMining.generateDataPanel.warning");
    }

}
