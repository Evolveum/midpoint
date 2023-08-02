/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role;

import static com.evolveum.midpoint.gui.api.component.mining.DataStorage.fillRolesAndUsers;
import static com.evolveum.midpoint.gui.api.component.mining.DataStorage.resetAll;
import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.grouper.Preparer.prepareMiningSet;
import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.grouper.Preparer.prepareMiningSetIntersected;
import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.jaccard.JacquardSorter.sortUserSets;
import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.jaccard.JacquardSorter.sortUserSetsJc;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.Tools.getScaleScript;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.component.mining.RoleMiningFilter;
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.RpType;
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.RuType;
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.UpType;
import com.evolveum.midpoint.gui.api.component.mining.analyse.structure.prune.UrType;
import com.evolveum.midpoint.gui.api.component.mining.analyse.tools.jaccard.UrTypeGroup;
import com.evolveum.midpoint.gui.api.component.mining.analyse.tools.jaccard.UserSet;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.panels.GenerateDataPanelRBAM;
import com.evolveum.midpoint.gui.impl.page.admin.role.panels.PrunePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.panels.tables.*;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/roleMiningPrune", matchUrlForSecurity = "/admin/roleMiningPrune")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(
                actionUri = AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL,
                label = "PageAdminRoles.auth.roleAll.label",
                description = "PageAdminRoles.auth.roleAll.description"),
        @AuthorizationAction(
                actionUri = AuthorizationConstants.AUTZ_UI_ROLE_URL,
                label = "PageRole.auth.role.label",
                description = "PageRole.auth.role.description") })

public class PageRoleMiningRBAM extends PageAdmin {

    private static final String ID_GENERATE_DATA_PANEL = "generate_data_panel";
    private static final String ID_BASIC_TABLE_SELECTOR = "basic_table_selector";
    private static final String ID_PRUNE_PANEL = "prune_panel";

    private static final String ID_FORM_TABLES = "table_dropdown";
    private static final String ID_DROPDOWN_TABLE = "dropdown_choice";
    private static final String ID_SUBMIT_DROPDOWN = "ajax_submit_link_dropdown";

    private static final String ID_DATATABLE_EXTRA = "datatable_extra";

    private static final List<String> SEARCH_ENGINES = Arrays.asList("UR", "UP", "RP", "RU", "JC", "JC2", "JCF", "ISF");
    public String selected = "UR";

    double jcThreshold = 0.80;
    int minIntersection = 5;

    public PageRoleMiningRBAM() {
        super();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        fillRolesAndUsers(getPageBase());

        add(generateObjectsPanel());

        add(new BasicTableSelector(ID_BASIC_TABLE_SELECTOR, PageRoleMiningRBAM.this));

        add(new PrunePanel(ID_PRUNE_PANEL, JavaScriptHeaderItem.forReference(
                new PackageResourceReference(PageRoleMiningRBAM.class, "js/network_graph_auth.js"))));

        add(choiceTableForm());

    }
    private @NotNull
    AjaxButton generateObjectsPanel() {
        AjaxButton ajaxLinkAssign = new AjaxButton(ID_GENERATE_DATA_PANEL, Model.of("Generate data")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                resetAll();

                GenerateDataPanelRBAM pageGenerateData = new GenerateDataPanelRBAM(
                        getPageBase().getMainPopupBodyId(),
                        createStringResource("RoleMining.generateDataPanel.title"));
                getPageBase().showMainPopup(pageGenerateData, target);
            }
        };
        ajaxLinkAssign.setOutputMarkupId(true);
        return ajaxLinkAssign;
    }

    private @NotNull Form<?> choiceTableForm() {

        DropDownChoice<String> listSites = new DropDownChoice<>(
                ID_DROPDOWN_TABLE, new PropertyModel<>(this, "selected"), SEARCH_ENGINES);

        Form<?> formDropdown = new Form<Void>(ID_FORM_TABLES);

        formDropdown.setOutputMarkupId(true);

        formDropdown.add(new Label(ID_DATATABLE_EXTRA).setOutputMarkupId(true));

        TextField<Double> thresholdField = new TextField<>("threshold_field", Model.of(jcThreshold));
        thresholdField.setOutputMarkupId(true);
        thresholdField.setOutputMarkupPlaceholderTag(true);
        thresholdField.setVisible(false);
        formDropdown.add(thresholdField);

        TextField<Integer> minIntersectionField = new TextField<>("intersection_field", Model.of(minIntersection));
        minIntersectionField.setOutputMarkupId(true);
        minIntersectionField.setOutputMarkupPlaceholderTag(true);
        minIntersectionField.setVisible(false);
        formDropdown.add(minIntersectionField);

        AjaxSubmitLink ajaxSubmitDropdown = new AjaxSubmitLink(ID_SUBMIT_DROPDOWN, formDropdown) {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {

                switch (selected) {
                    case "UR" -> {
                        if (thresholdField.isVisible()) {
                            thresholdField.setVisible(false);
                        }
                        if (minIntersectionField.isVisible()) {
                            minIntersectionField.setVisible(false);
                        }
                        target.add(thresholdField);
                        getBoxedTableExtra().replaceWith(new TableUR(ID_DATATABLE_EXTRA, generateUR(),
                                new RoleMiningFilter().filterRoles(getPageBase()), true));
                        target.add(getBoxedTableExtra().setOutputMarkupId(true));
                    }
                    case "UP" -> {
                        if (thresholdField.isVisible()) {
                            thresholdField.setVisible(false);
                        }
                        if (minIntersectionField.isVisible()) {
                            minIntersectionField.setVisible(false);
                        }
                        target.add(thresholdField);
                        getBoxedTableExtra().replaceWith(new TableUP(ID_DATATABLE_EXTRA, generateUP(), generatePermissions()));
                        target.add(getBoxedTableExtra().setOutputMarkupId(true));
                    }
                    case "RP" -> {
                        if (thresholdField.isVisible()) {
                            thresholdField.setVisible(false);
                        }
                        if (minIntersectionField.isVisible()) {
                            minIntersectionField.setVisible(false);
                        }
                        target.add(thresholdField);
                        getBoxedTableExtra().replaceWith(new TableRP(ID_DATATABLE_EXTRA, generateRP(), generatePermissions()));
                        target.add(getBoxedTableExtra().setOutputMarkupId(true));
                    }
                    case "RU" -> {
                        if (thresholdField.isVisible()) {
                            thresholdField.setVisible(false);
                        }
                        if (minIntersectionField.isVisible()) {
                            minIntersectionField.setVisible(false);
                        }
                        target.add(thresholdField);
                        getBoxedTableExtra().replaceWith(new TableRU(ID_DATATABLE_EXTRA, generateRU(),
                                new RoleMiningFilter().filterUsers(getPageBase())));
                        target.add(getBoxedTableExtra().setOutputMarkupId(true));
                    }
                    case "JC" -> {
                        if (!thresholdField.isVisible()) {
                            thresholdField.setVisible(true);
                        }
                        if (minIntersectionField.isVisible()) {
                            minIntersectionField.setVisible(false);
                        }
                        getBoxedTableExtra().replaceWith(new TableUR(ID_DATATABLE_EXTRA, generateJC(),
                                new RoleMiningFilter().filterRoles(getPageBase()), false));
                        target.add(getBoxedTableExtra().setOutputMarkupId(true));
                    }
                    case "JC2" -> {
                        if (minIntersectionField.isVisible()) {
                            minIntersectionField.setVisible(false);
                        }
                        if (!thresholdField.isVisible()) {
                            thresholdField.setVisible(true);
                        }
                        if (thresholdField.getModelObject() == null) {
                            thresholdField.setDefaultModelObject(jcThreshold);
                        }
                        jcThreshold = thresholdField.getModelObject();
                        target.add(thresholdField);
                        getBoxedTableExtra().replaceWith(new TableJC(ID_DATATABLE_EXTRA, generateJC2(jcThreshold),
                                new RoleMiningFilter().filterRoles(getPageBase()), false));
                        target.add(getBoxedTableExtra().setOutputMarkupId(true));
                    }
                    case "JCF" -> {
                        if (minIntersectionField.isVisible()) {
                            minIntersectionField.setVisible(false);
                        }
                        if (!thresholdField.isVisible()) {
                            thresholdField.setVisible(true);
                        }
                        if (thresholdField.getModelObject() == null) {
                            thresholdField.setDefaultModelObject(jcThreshold);
                        }
                        jcThreshold = thresholdField.getModelObject();
                        target.add(thresholdField);
                        getBoxedTableExtra().replaceWith(new TableJCF(ID_DATATABLE_EXTRA, prepareMiningSet(
                                new RoleMiningFilter().filterUsers(getPageBase()), jcThreshold),
                                new RoleMiningFilter().filterRoles(getPageBase()), true, true)
                                .add(new AbstractDefaultAjaxBehavior() {
                                    @Override
                                    protected void respond(AjaxRequestTarget target) {
                                    }

                                    @Override
                                    public void renderHead(Component component, IHeaderResponse response) {
                                        super.renderHead(component, response);
                                        response.render(OnDomReadyHeaderItem.forScript(getScaleScript()));
                                    }
                                }));
                        target.add(getBoxedTableExtra().setOutputMarkupId(true));
                    }
                    case "ISF" -> {
                        if (thresholdField.isVisible()) {
                            thresholdField.setVisible(false);
                        }
                        if (!minIntersectionField.isVisible()) {
                            minIntersectionField.setVisible(true);
                        }
                        if (minIntersectionField.getModelObject() == null) {
                            minIntersectionField.setDefaultModelObject(jcThreshold);
                        }
                        minIntersection = minIntersectionField.getModelObject();
                        target.add(minIntersectionField);
                        getBoxedTableExtra().replaceWith(new TableJCF(ID_DATATABLE_EXTRA, prepareMiningSetIntersected(
                                new RoleMiningFilter().filterUsers(getPageBase()), minIntersection, jcThreshold),
                                new RoleMiningFilter().filterRoles(getPageBase()), true, true)
                                .add(new AbstractDefaultAjaxBehavior() {
                                    @Override
                                    protected void respond(AjaxRequestTarget target) {
                                    }

                                    @Override
                                    public void renderHead(Component component, IHeaderResponse response) {
                                        super.renderHead(component, response);
                                        response.render(OnDomReadyHeaderItem.forScript(getScaleScript()));
                                    }
                                }));
                        target.add(getBoxedTableExtra().setOutputMarkupId(true));
                    }
                    default -> {
                    }
                }
            }
        };

        formDropdown.add(listSites);
        formDropdown.add(ajaxSubmitDropdown);
        return formDropdown;
    }

    protected Component getBoxedTableExtra() {
        return get(((PageBase) getPage()).createComponentPath(ID_FORM_TABLES, ID_DATATABLE_EXTRA));
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    public List<UrType> generateUR() {

        List<PrismObject<UserType>> userList = new RoleMiningFilter().filterUsers(getPageBase());
        List<UrType> userRolesList = new ArrayList<>();
        for (PrismObject<UserType> userTypePrismObject : userList) {
            UserType userObject = userTypePrismObject.asObjectable();

            if (userObject.getName().toString().equals("administrator")) {
                userRolesList.add(new UrType(userObject, new ArrayList<>()));
                continue;
            }
            List<RoleType> userRoles = new RoleMiningFilter().getUserRoles(userObject, getPageBase());

            userRolesList.add(new UrType(userObject, userRoles));
        }
        return userRolesList;
    }

    public List<UrType> generateJC() {
        List<UserSet> userSets = sortUserSets(new RoleMiningFilter().filterUsers(getPageBase()));

        List<PrismObject<UserType>> userList = new ArrayList<>();
        for (UserSet userSet : userSets) {
            userList.addAll(userSet.getUsers());
        }

        List<UrType> userRolesList = new ArrayList<>();
        for (PrismObject<UserType> userTypePrismObject : userList) {
            UserType userObject = userTypePrismObject.asObjectable();

            if (userObject.getName().toString().equals("administrator")) {
                userRolesList.add(new UrType(userObject, new ArrayList<>()));
                continue;
            }
            List<RoleType> userRoles = new RoleMiningFilter().getUserRoles(userObject, getPageBase());

            userRolesList.add(new UrType(userObject, userRoles));
        }
        return userRolesList;
    }

    public List<UrTypeGroup> generateJC2(double jcThreshold) {
        List<UserSet> userSets = sortUserSetsJc(new RoleMiningFilter().filterUsers(getPageBase()), jcThreshold);

        List<UrTypeGroup> userRolesList = new ArrayList<>();

        for (int i = 0; i < userSets.size(); i++) {
            List<PrismObject<UserType>> arrayList = new ArrayList<>(userSets.get(i).getUsers());
            for (PrismObject<UserType> userTypePrismObject : arrayList) {

                UserType userObject = userTypePrismObject.asObjectable();

                if (userObject.getName().toString().equals("administrator")) {
                    userRolesList.add(new UrTypeGroup(userObject, new ArrayList<>(), i));
                    continue;
                }
                List<RoleType> userRoles = new RoleMiningFilter().getUserRoles(userObject, getPageBase());

                userRolesList.add(new UrTypeGroup(userObject, userRoles, i));

            }
        }

        return userRolesList;
    }

    public List<UpType> generateUP() {

        List<PrismObject<UserType>> userList = new RoleMiningFilter().filterUsers(getPageBase());
        List<UpType> userPermissionList = new ArrayList<>();
        for (PrismObject<UserType> userTypePrismObject : userList) {
            UserType userObject = userTypePrismObject.asObjectable();

            if (userObject.getName().toString().equals("administrator")) {
                userPermissionList.add(new UpType(userObject, new ArrayList<>()));
                continue;
            }
            List<String> rolesIds = new RoleMiningFilter().roleObjectIdRefType(userObject);
            //O(K * N)
            List<AuthorizationType> userAuthorizations = new RoleMiningFilter().getUserAuthorizations(rolesIds, getPageBase());
            userPermissionList.add(new UpType(userObject, userAuthorizations));
        }

        return userPermissionList;
    }

    public List<RpType> generateRP() {

        List<PrismObject<RoleType>> userList = new RoleMiningFilter().filterRoles(getPageBase());
        List<RpType> rolePermissionList = new ArrayList<>();
        for (PrismObject<RoleType> userTypePrismObject : userList) {
            RoleType roleType = userTypePrismObject.asObjectable();

            if (roleType.getName().toString().equals("administrator")) {
                rolePermissionList.add(new RpType(roleType, new ArrayList<>()));
                continue;
            }
            List<AuthorizationType> authorization = roleType.getAuthorization();

            rolePermissionList.add(new RpType(roleType, authorization));
        }

        return rolePermissionList;
    }

    public List<RuType> generateRU() {

        List<PrismObject<RoleType>> userList = new RoleMiningFilter().filterRoles(getPageBase());
        List<RuType> roleUsersList = new ArrayList<>();
        for (PrismObject<RoleType> roles : userList) {
            RoleType roleType = roles.asObjectable();

            if (roleType.getName().toString().equals("administrator")) {
                roleUsersList.add(new RuType(roleType, new ArrayList<>()));
                continue;
            }
            List<PrismObject<UserType>> roleMembers = new RoleMiningFilter().getRoleMembers(getPageBase(), roleType.getOid());

            roleUsersList.add(new RuType(roleType, roleMembers));
        }

        return roleUsersList;
    }

    public List<AuthorizationType> generatePermissions() {

        List<AuthorizationType> permissions = new ArrayList<>();
        List<PrismObject<RoleType>> roleList = new RoleMiningFilter().filterRoles(getPageBase());

        for (PrismObject<RoleType> roleTypePrismObject : roleList) {
            if (!roleTypePrismObject.getName().toString().contains("R_")) {
                continue;
            }
            List<AuthorizationType> roleAuthorizations = roleTypePrismObject.asObjectable().getAuthorization();

            roleAuthorizations.stream().filter(authorizationType -> !permissions
                            .contains(authorizationType))
                    .forEach(permissions::add);
        }
        return permissions;
    }

}

