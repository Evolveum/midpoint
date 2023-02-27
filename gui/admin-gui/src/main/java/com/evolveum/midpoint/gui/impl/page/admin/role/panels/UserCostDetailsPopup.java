/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.panels;

import static com.evolveum.midpoint.gui.api.component.mining.DataStorage.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.mining.RoleMiningFilter;
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.CandidateRole;
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.CostResult;
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.UpType;
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.UrType;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.panels.tables.TableResultCost;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class UserCostDetailsPopup extends BasePanel<String> implements Popupable {

    private static final String ID_CANCEL_OK = "cancel";
    private static final String ID_FORM = "form";
    private static final String ID_DATATABLE = "datatable_extra_rbac";

    PageBase pageBase;

    public UserCostDetailsPopup(String id, IModel<String> messageModel, List<UserType> userType, boolean modePermission,
            boolean modeRole, boolean specific, PageBase pageBase) {
        super(id, messageModel);
        this.pageBase = pageBase;
        initLayout(userType, modePermission, modeRole, specific,pageBase);
    }



    private void initLayout(List<UserType> userType, boolean modePermission,
            boolean modeRole, boolean specific, PageBase pageBase) {

        Form<?> form = new MidpointForm<>(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);

        if (specific) {
            if (modeRole) {
                form.add(new TableResultCost(ID_DATATABLE, generateCostResultUrSpecific(userType,pageBase),
                        false, true,false));
            } else if (modePermission) {
                form.add(new TableResultCost(ID_DATATABLE, generateCostResultUpSpecific(userType,pageBase),
                        true, false,false));
            }
        } else {
            if (modeRole) {
                form.add(new TableResultCost(ID_DATATABLE, generateCostResultUR(),
                        false, true,false));
            } else if (modePermission) {
                form.add(new TableResultCost(ID_DATATABLE, generateCostResultUP(pageBase),
                        true, false,false));
            }
        }

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL_OK,
                createStringResource("Button.cancel")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                pageBase.getMainPopup().close(target);
            }
        };

        add(cancelButton);

    }

    public List<CostResult> generateCostResultUR() {
        List<CostResult> costResults = new ArrayList<>();

        HashMap<Integer, List<UrType>> candidateKeyUpStructureMap = getCandidateKeyUpStructureMapUR();
        List<HashMap<Integer, CandidateRole>> rolesDegrees = getRolesDegrees();

        List<PrismObject<RoleType>> checkRoles = new RoleMiningFilter().filterRoles(getPageBase());

        for (int i = 1; i < rolesDegrees.size(); i++) {
            HashMap<Integer, CandidateRole> integerCandidateRoleHashMap = rolesDegrees.get(i);

            for (Map.Entry<Integer, CandidateRole> entry : integerCandidateRoleHashMap.entrySet()) {

                CandidateRole candidateRole = entry.getValue();
                double roleCost = candidateRole.getActualSupport();

                if (roleCost == 0) {
                    continue;
                }

                int roleKey = entry.getKey();
                int roleDegree = candidateRole.getDegree() + 1;
                List<UrType> urTypeList = candidateKeyUpStructureMap.get(roleKey);

                List<RoleType> candidateRoles = candidateRole.getCandidateRoles();

                int urListSize = urTypeList.size();

                int assignBefore = 0;
                int reducedCount = 0;
                for (UrType urType : urTypeList) {
                    List<RoleType> roleMembers = urType.getRoleMembers();
                    int membersSize = roleMembers.size();
                    assignBefore = assignBefore + membersSize;
                    reducedCount += IntStream.range(0, membersSize)
                            .filter(m -> candidateRoles.contains(roleMembers.get(m)))
                            .count();
                }

                reducedCount = reducedCount - urListSize;

                List<RoleType> equivalentRoles = new ArrayList<>();
                for (int j = 0; j < checkRoles.size(); j++) {
                    if (checkRoles.get(j).asObjectable().getAuthorization().equals(candidateRole.getCandidatePermissions())) {
                        equivalentRoles.add(checkRoles.get(j).asObjectable());
                        checkRoles.remove(checkRoles.get(j));
                    }
                }

                double reduceValue = ((assignBefore - reducedCount) / (double) assignBefore) * 100;
                costResults.add(new CostResult(roleKey, roleDegree, roleCost, reduceValue, urTypeList, candidateRole,
                        equivalentRoles));
            }
        }
        return costResults;
    }

    public List<CostResult> generateCostResultUrSpecific(List<UserType> userType, PageBase pageBase) {
        List<CostResult> costResults = new ArrayList<>();

        HashMap<Integer, List<UrType>> candidateKeyUpStructureMap = getCandidateKeyUpStructureMapUR();
        List<HashMap<Integer, CandidateRole>> rolesDegrees = getRolesDegrees();

        List<PrismObject<RoleType>> checkRoles = new RoleMiningFilter().filterRoles(pageBase);

        for (int i = 1; i < rolesDegrees.size(); i++) {
            HashMap<Integer, CandidateRole> integerCandidateRoleHashMap = rolesDegrees.get(i);

            for (Map.Entry<Integer, CandidateRole> entry : integerCandidateRoleHashMap.entrySet()) {

                CandidateRole candidateRole = entry.getValue();
                double roleCost = candidateRole.getActualSupport();

                if (roleCost == 0) {
                    continue;
                }

                int roleKey = entry.getKey();
                int roleDegree = candidateRole.getDegree() + 1;
                List<UrType> urTypeList = candidateKeyUpStructureMap.get(roleKey);

                List<RoleType> candidateRoles = candidateRole.getCandidateRoles();

                int urListSize = urTypeList.size();

                boolean can = false;
                for (UrType type : urTypeList) {

                    if (userType.contains(type.getUserObjectType())) {
                        can = true;
                        break;
                    }
                }

                if (can) {

                    int assignBefore = 0;
                    int reducedCount = 0;
                    for (UrType urType : urTypeList) {
                        List<RoleType> roleMembers = urType.getRoleMembers();
                        int membersSize = roleMembers.size();
                        assignBefore = assignBefore + membersSize;
                        reducedCount += IntStream.range(0, membersSize)
                                .filter(m -> candidateRoles.contains(roleMembers.get(m)))
                                .count();
                    }

                    reducedCount = reducedCount - urListSize;

                    List<RoleType> equivalentRoles = new ArrayList<>();
                    for (int j = 0; j < checkRoles.size(); j++) {
                        if (checkRoles.get(j).asObjectable().getAuthorization().equals(candidateRole.getCandidatePermissions())) {
                            equivalentRoles.add(checkRoles.get(j).asObjectable());
                            checkRoles.remove(checkRoles.get(j));
                        }
                    }

                    double reduceValue = ((assignBefore - reducedCount) / (double) assignBefore) * 100;
                    costResults.add(new CostResult(roleKey, roleDegree, roleCost, reduceValue, urTypeList, candidateRole,
                            equivalentRoles));
                }
            }
        }
        return costResults;
    }

    public List<CostResult> generateCostResultUpSpecific(List<UserType> userType,PageBase pageBase) {
        List<CostResult> costResults = new ArrayList<>();

        HashMap<Integer, List<UpType>> candidateKeyUpStructureMap = getCandidateKeyUpStructureMapUP();
        List<HashMap<Integer, CandidateRole>> rolesDegrees = getRolesDegrees();

        List<PrismObject<RoleType>> checkRoles = new RoleMiningFilter().filterRoles(pageBase);

        for (int i = 1; i < rolesDegrees.size(); i++) {
            HashMap<Integer, CandidateRole> integerCandidateRoleHashMap = rolesDegrees.get(i);

            for (Map.Entry<Integer, CandidateRole> entry : integerCandidateRoleHashMap.entrySet()) {

                CandidateRole candidateRole = entry.getValue();
                double roleCost = candidateRole.getActualSupport();

                if (roleCost == 0) {
                    continue;
                }
                int roleKey = entry.getKey();
                int roleDegree = candidateRole.getDegree() + 1;

                List<AuthorizationType> candidatePermissions = candidateRole.getCandidatePermissions();

                List<UpType> upTypeList = candidateKeyUpStructureMap.get(roleKey);

                List<UrType> urTypeList = new ArrayList<>();
                int upListSize = upTypeList.size();
                int assignBefore = 0;
                int reducedCount = 0;

                boolean can = false;
                for (UpType type : upTypeList) {

                    if (userType.contains(type.getUserObjectType())) {
                        can = true;
                        break;
                    }
                }

                if (can) {
                    for (UpType upType : upTypeList) {
                        UserType userObjectType = upType.getUserObjectType();
                        urTypeList.add(new UrType(userObjectType, new RoleMiningFilter().getUserRoles(userObjectType, pageBase)));
                        List<AuthorizationType> rolePermissions = upType.getPermission();
                        int membersSize = rolePermissions.size();
                        assignBefore = assignBefore + membersSize;
                        long count = 0L;
                        for (AuthorizationType rolePermission : rolePermissions) {
                            if (candidatePermissions.contains(rolePermission)) {
                                count++;
                            }
                        }
                        reducedCount += count;
                    }

                    reducedCount = reducedCount - upListSize;

                    List<RoleType> equivalentRoles = new ArrayList<>();
                    for (int j = 0; j < checkRoles.size(); j++) {
                        if (checkRoles.get(j).asObjectable().getAuthorization().equals(candidateRole.getCandidatePermissions())) {
                            equivalentRoles.add(checkRoles.get(j).asObjectable());
                            checkRoles.remove(checkRoles.get(j));
                        }
                    }

                    double reduceValue = ((assignBefore - reducedCount) / (double) assignBefore) * 100;
                    costResults.add(new CostResult(roleKey, roleDegree, roleCost, reduceValue, urTypeList, candidateRole,
                            equivalentRoles));

                }
            }
        }
        return costResults;
    }

    public List<CostResult> generateCostResultUP(PageBase pageBase) {
        List<CostResult> costResults = new ArrayList<>();

        HashMap<Integer, List<UpType>> candidateKeyUpStructureMap = getCandidateKeyUpStructureMapUP();
        List<HashMap<Integer, CandidateRole>> rolesDegrees = getRolesDegrees();

        List<PrismObject<RoleType>> checkRoles = new RoleMiningFilter().filterRoles(pageBase);

        for (int i = 1; i < rolesDegrees.size(); i++) {
            HashMap<Integer, CandidateRole> integerCandidateRoleHashMap = rolesDegrees.get(i);

            for (Map.Entry<Integer, CandidateRole> entry : integerCandidateRoleHashMap.entrySet()) {

                CandidateRole candidateRole = entry.getValue();
                double roleCost = candidateRole.getActualSupport();

                if (roleCost == 0) {
                    continue;
                }
                int roleKey = entry.getKey();
                int roleDegree = candidateRole.getDegree() + 1;

                List<AuthorizationType> candidatePermissions = candidateRole.getCandidatePermissions();

                List<UpType> upTypeList = candidateKeyUpStructureMap.get(roleKey);

                List<UrType> urTypeList = new ArrayList<>();
                int upListSize = upTypeList.size();
                int assignBefore = 0;
                int reducedCount = 0;
                for (UpType upType : upTypeList) {
                    UserType userObjectType = upType.getUserObjectType();
                    urTypeList.add(new UrType(userObjectType, new RoleMiningFilter().getUserRoles(userObjectType, pageBase)));
                    List<AuthorizationType> rolePermissions = upType.getPermission();
                    int membersSize = rolePermissions.size();
                    assignBefore = assignBefore + membersSize;
                    long count = 0L;
                    for (AuthorizationType rolePermission : rolePermissions) {
                        if (candidatePermissions.contains(rolePermission)) {
                            count++;
                        }
                    }
                    reducedCount += count;
                }

                reducedCount = reducedCount - upListSize;

                List<RoleType> equivalentRoles = new ArrayList<>();
                for (int j = 0; j < checkRoles.size(); j++) {
                    if (checkRoles.get(j).asObjectable().getAuthorization().equals(candidateRole.getCandidatePermissions())) {
                        equivalentRoles.add(checkRoles.get(j).asObjectable());
                        checkRoles.remove(checkRoles.get(j));
                    }
                }

                double reduceValue = ((assignBefore - reducedCount) / (double) assignBefore) * 100;
                costResults.add(new CostResult(roleKey, roleDegree, roleCost, reduceValue, urTypeList, candidateRole,
                        equivalentRoles));

            }
        }
        return costResults;
    }

    @Override
    public int getWidth() {
        return 1000;
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
