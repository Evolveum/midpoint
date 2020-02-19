/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.objectdetails.AssignmentHolderTypeAssignmentsTabPanel;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.users.component.AllAssignmentsPreviewDialog;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentInfoDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Created by honchar
 */
public class SwitchAssignmentTypePanel extends BasePanel<PrismContainerWrapper<AssignmentType>> {
    private static final long serialVersionUID = 1L;

    private static final String ID_ASSIGNMENT_TYPE_BUTTONS = "assignmentTypeButtons";
    private static final String ID_ALL_ASSIGNMENTS = "allAssignments";
    private static final String ID_ROLE_TYPE_ASSIGNMENTS = "roleTypeAssignments";
    private static final String ID_ORG_TYPE_ASSIGNMENTS = "orgTypeAssignments";
    private static final String ID_SERVICE_TYPE_ASSIGNMENTS = "serviceTypeAssignments";
    private static final String ID_RESOURCE_TYPE_ASSIGNMENTS = "resourceTypeAssignments";
    private static final String ID_POLICY_RULE_TYPE_ASSIGNMENTS = "policyRuleTypeAssignments";
    private static final String ID_ENTITLEMENT_ASSIGNMENTS = "entitlementAssignments";
    private static final String ID_FOCUS_MAPPING_ASSIGNMENTS = "focusMappingAssignments";
    private static final String ID_CONSENT_ASSIGNMENTS = "consentAssignments";
    private static final String ID_SHOW_INDIRECT_ASSIGNMENTS = "showIndirectAssignmentsButton";
    private static final String ID_ASSIGNMENTS = "assignmentsPanel";
    private static final String ID_DATA_PROTECTION_ASSIGNMENTS = "dataProtectionAssignments";

    private String activeButtonId = ID_ALL_ASSIGNMENTS;

    public SwitchAssignmentTypePanel(String id, IModel<PrismContainerWrapper<AssignmentType>> assignmentContainerWrapperModel) {
        super(id, assignmentContainerWrapperModel);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        add(new VisibleBehaviour(() -> getModelObject() != null));
        initButtonsPanel();
        initAssignmentsPanel();

        setOutputMarkupId(true);
    }

    private void initButtonsPanel(){
        WebMarkupContainer buttonsContainer = new WebMarkupContainer(ID_ASSIGNMENT_TYPE_BUTTONS);
        buttonsContainer.setOutputMarkupId(true);
        buttonsContainer.add(new VisibleBehaviour(() -> ID_SHOW_INDIRECT_ASSIGNMENTS.equals(activeButtonId) ||
                getButtonsContainerVisibilityModel().getObject()));
        add(buttonsContainer);

        AjaxButton allAssignmentsButton = new AjaxButton(ID_ALL_ASSIGNMENTS, createStringResource("AssignmentPanel.allLabel")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                AssignmentPanel assignmentPanel =
                        new AssignmentPanel(ID_ASSIGNMENTS, SwitchAssignmentTypePanel.this.getModel()) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected void assignmentDetailsPerformed(AjaxRequestTarget target) {
                                target.add(SwitchAssignmentTypePanel.this);
                            }

                            @Override
                            protected void cancelAssignmentDetailsPerformed(AjaxRequestTarget target) {
                                target.add(SwitchAssignmentTypePanel.this);
                            }

                            @Override
                            protected boolean isNewObjectButtonVisible(PrismObject focusObject){
                                return !isReadonly() && super.isNewObjectButtonVisible(focusObject);
                            }

                        };
                assignmentPanel.setOutputMarkupId(true);
                switchAssignmentTypePerformed(target, assignmentPanel, ID_ALL_ASSIGNMENTS);
            }
        };
        allAssignmentsButton.add(AttributeAppender.append("class", getButtonStyleModel(ID_ALL_ASSIGNMENTS)));
        allAssignmentsButton.setOutputMarkupId(true);
        allAssignmentsButton.setOutputMarkupPlaceholderTag(true);
        buttonsContainer.add(allAssignmentsButton);

        AjaxButton roleTypeAssignmentsButton = new AjaxButton(ID_ROLE_TYPE_ASSIGNMENTS, createStringResource("ObjectType.RoleType")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                AbstractRoleAssignmentPanel assignmentPanel =
                        new AbstractRoleAssignmentPanel(ID_ASSIGNMENTS, SwitchAssignmentTypePanel.this.getModel()) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected QName getAssignmentType() {
                                return RoleType.COMPLEX_TYPE;
                            }

                            @Override
                            protected void assignmentDetailsPerformed(AjaxRequestTarget target) {
                                target.add(SwitchAssignmentTypePanel.this);
                            }

                            @Override
                            protected void cancelAssignmentDetailsPerformed(AjaxRequestTarget target) {
                                target.add(SwitchAssignmentTypePanel.this);
                            }

                            @Override
                            protected boolean isNewObjectButtonVisible(PrismObject focusObject){
                                return !isReadonly() && super.isNewObjectButtonVisible(focusObject);
                            }

                        };
                assignmentPanel.setOutputMarkupId(true);
                switchAssignmentTypePerformed(target, assignmentPanel, ID_ROLE_TYPE_ASSIGNMENTS);
            }
        };
        roleTypeAssignmentsButton.add(AttributeAppender.append("class", getButtonStyleModel(ID_ROLE_TYPE_ASSIGNMENTS)));
        roleTypeAssignmentsButton.setOutputMarkupId(true);
        buttonsContainer.add(roleTypeAssignmentsButton);

        AjaxButton orgTypeAssignmentsButton = new AjaxButton(ID_ORG_TYPE_ASSIGNMENTS, createStringResource("ObjectType.OrgType")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                AbstractRoleAssignmentPanel assignmentPanel =
                        new AbstractRoleAssignmentPanel(ID_ASSIGNMENTS, SwitchAssignmentTypePanel.this.getModel()) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected QName getAssignmentType() {
                                return OrgType.COMPLEX_TYPE;
                            }

                            @Override
                            protected void assignmentDetailsPerformed(AjaxRequestTarget target) {
                                target.add(SwitchAssignmentTypePanel.this);
                            }

                            @Override
                            protected void cancelAssignmentDetailsPerformed(AjaxRequestTarget target) {
                                target.add(SwitchAssignmentTypePanel.this);
                            }

                            @Override
                            protected boolean isNewObjectButtonVisible(PrismObject focusObject){
                                return !isReadonly() && super.isNewObjectButtonVisible(focusObject);
                            }
                        };
                assignmentPanel.setOutputMarkupId(true);
                switchAssignmentTypePerformed(target, assignmentPanel, ID_ORG_TYPE_ASSIGNMENTS);
            }
        };
        orgTypeAssignmentsButton.add(AttributeAppender.append("class", getButtonStyleModel(ID_ORG_TYPE_ASSIGNMENTS)));
        orgTypeAssignmentsButton.setOutputMarkupId(true);
        buttonsContainer.add(orgTypeAssignmentsButton);

        AjaxButton serviceTypeAssignmentsButton = new AjaxButton(ID_SERVICE_TYPE_ASSIGNMENTS, createStringResource("ObjectType.ServiceType")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                AbstractRoleAssignmentPanel assignmentPanel =
                        new AbstractRoleAssignmentPanel(ID_ASSIGNMENTS, SwitchAssignmentTypePanel.this.getModel()) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected QName getAssignmentType() {
                                return ServiceType.COMPLEX_TYPE;
                            }

                            @Override
                            protected void assignmentDetailsPerformed(AjaxRequestTarget target) {
                                target.add(SwitchAssignmentTypePanel.this);
                            }

                            @Override
                            protected void cancelAssignmentDetailsPerformed(AjaxRequestTarget target) {
                                target.add(SwitchAssignmentTypePanel.this);
                            }

                            @Override
                            protected boolean isNewObjectButtonVisible(PrismObject focusObject){
                                return !isReadonly() && super.isNewObjectButtonVisible(focusObject);
                            }
                        };
                assignmentPanel.setOutputMarkupId(true);
                switchAssignmentTypePerformed(target, assignmentPanel, ID_SERVICE_TYPE_ASSIGNMENTS);

            }
        };
        serviceTypeAssignmentsButton.add(AttributeAppender.append("class", getButtonStyleModel(ID_SERVICE_TYPE_ASSIGNMENTS)));
        serviceTypeAssignmentsButton.setOutputMarkupId(true);
        buttonsContainer.add(serviceTypeAssignmentsButton);

        AjaxButton resourceTypeAssignmentsButton = new AjaxButton(ID_RESOURCE_TYPE_ASSIGNMENTS, createStringResource("ObjectType.ResourceType")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ConstructionAssignmentPanel constructionsPanel =
                        new ConstructionAssignmentPanel(ID_ASSIGNMENTS, SwitchAssignmentTypePanel.this.getModel()){
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected void assignmentDetailsPerformed(AjaxRequestTarget target) {
                                target.add(SwitchAssignmentTypePanel.this);
                            }

                            @Override
                            protected void cancelAssignmentDetailsPerformed(AjaxRequestTarget target) {
                                target.add(SwitchAssignmentTypePanel.this);
                            }

                            @Override
                            protected boolean isNewObjectButtonVisible(PrismObject focusObject){
                                return !isReadonly() && super.isNewObjectButtonVisible(focusObject);
                            }
                        };
                constructionsPanel.setOutputMarkupId(true);
                switchAssignmentTypePerformed(target, constructionsPanel, ID_RESOURCE_TYPE_ASSIGNMENTS);
            }
        };
        resourceTypeAssignmentsButton.add(AttributeAppender.append("class", getButtonStyleModel(ID_RESOURCE_TYPE_ASSIGNMENTS)));
        resourceTypeAssignmentsButton.setOutputMarkupId(true);
        buttonsContainer.add(resourceTypeAssignmentsButton);

        AjaxButton policyRuleTypeAssignmentsButton = new AjaxButton(ID_POLICY_RULE_TYPE_ASSIGNMENTS, createStringResource("AssignmentType.policyRule")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                PolicyRulesPanel policyRulesPanel =
                        new PolicyRulesPanel(ID_ASSIGNMENTS, SwitchAssignmentTypePanel.this.getModel()){
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected void assignmentDetailsPerformed(AjaxRequestTarget target) {
                                target.add(SwitchAssignmentTypePanel.this);
                            }

                            @Override
                            protected void cancelAssignmentDetailsPerformed(AjaxRequestTarget target) {
                                target.add(SwitchAssignmentTypePanel.this);
                            }

                            @Override
                            protected boolean isNewObjectButtonVisible(PrismObject focusObject){
                                return !isReadonly() && super.isNewObjectButtonVisible(focusObject);
                            }
                        } ;
                policyRulesPanel.setOutputMarkupId(true);
                switchAssignmentTypePerformed(target, policyRulesPanel, ID_POLICY_RULE_TYPE_ASSIGNMENTS);

            }
        };
        policyRuleTypeAssignmentsButton.add(AttributeAppender.append("class", getButtonStyleModel(ID_POLICY_RULE_TYPE_ASSIGNMENTS)));
        policyRuleTypeAssignmentsButton.setOutputMarkupId(true);

        policyRuleTypeAssignmentsButton.add(new VisibleBehaviour(()  -> isAssignmentPanelVisible()));

        buttonsContainer.add(policyRuleTypeAssignmentsButton);

        AjaxButton dataProtectionButton = new AjaxButton(ID_DATA_PROTECTION_ASSIGNMENTS, createStringResource("pageAdminFocus.dataProtection")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                GenericAbstractRoleAssignmentPanel dataProtectionPanel =
                        new GenericAbstractRoleAssignmentPanel(ID_ASSIGNMENTS, SwitchAssignmentTypePanel.this.getModel()) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected void assignmentDetailsPerformed(AjaxRequestTarget target) {
                                target.add(SwitchAssignmentTypePanel.this);
                            }

                            @Override
                            protected void cancelAssignmentDetailsPerformed(AjaxRequestTarget target) {
                                target.add(SwitchAssignmentTypePanel.this);
                            }

                            @Override
                            protected boolean isNewObjectButtonVisible(PrismObject focusObject){
                                return !isReadonly() && super.isNewObjectButtonVisible(focusObject);
                            }
                        };
                dataProtectionPanel.setOutputMarkupId(true);
                switchAssignmentTypePerformed(target, dataProtectionPanel, ID_DATA_PROTECTION_ASSIGNMENTS);

            }
        };
        dataProtectionButton.add(AttributeAppender.append("class", getButtonStyleModel(ID_DATA_PROTECTION_ASSIGNMENTS)));
        dataProtectionButton.setOutputMarkupId(true);
        dataProtectionButton.add(new VisibleBehaviour(()  ->
                WebModelServiceUtils.isEnableExperimentalFeature(SwitchAssignmentTypePanel.this.getPageBase())));
        buttonsContainer.add(dataProtectionButton);

        AjaxButton entitlementAssignmentsButton = new AjaxButton(ID_ENTITLEMENT_ASSIGNMENTS, createStringResource("AbstractRoleMainPanel.inducedEntitlements")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                InducedEntitlementsPanel entitlementAssignments =
                        new InducedEntitlementsPanel(ID_ASSIGNMENTS, SwitchAssignmentTypePanel.this.getModel()) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected void assignmentDetailsPerformed(AjaxRequestTarget target) {
                                target.add(SwitchAssignmentTypePanel.this);
                            }

                            @Override
                            protected void cancelAssignmentDetailsPerformed(AjaxRequestTarget target) {
                                target.add(SwitchAssignmentTypePanel.this);
                            }
                        };
                entitlementAssignments.setOutputMarkupId(true);
                switchAssignmentTypePerformed(target, entitlementAssignments, ID_ENTITLEMENT_ASSIGNMENTS);

            }
        };
        entitlementAssignmentsButton.add(AttributeAppender.append("class", getButtonStyleModel(ID_ENTITLEMENT_ASSIGNMENTS)));
        entitlementAssignmentsButton.setOutputMarkupId(true);

        entitlementAssignmentsButton.add(new VisibleBehaviour(()  -> isAssignmentPanelVisible() && isInducement()));
        buttonsContainer.add(entitlementAssignmentsButton);

        AjaxButton focusMappingAssignmentsButton = new AjaxButton(ID_FOCUS_MAPPING_ASSIGNMENTS, createStringResource("AssignmentType.focusMappings")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                AssignmentPanel assignmentPanel =
                        new AssignmentPanel(ID_ASSIGNMENTS, SwitchAssignmentTypePanel.this.getModel()) {
                            private static final long serialVersionUID = 1L;

                            //TODO may be we will need FocusMappingsAssignmentsPanel later
                            @Override
                            protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> initBasicColumns() {
                                List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> columns = new ArrayList<>();

                                columns.add(new IconColumn<PrismContainerValueWrapper<AssignmentType>>(Model.of("")) {

                                    private static final long serialVersionUID = 1L;

                                    @Override
                                    protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
                                        return WebComponentUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(
                                                AssignmentsUtil.getTargetType(rowModel.getObject().getRealValue())));
                                    }

                                });

                                columns.add(new AbstractColumn<PrismContainerValueWrapper<AssignmentType>, String>(createStringResource("PolicyRulesPanel.nameColumn")){
                                    private static final long serialVersionUID = 1L;

                                    @Override
                                    public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<AssignmentType>>> cellItem,
                                                                          String componentId, final IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
                                        String name = AssignmentsUtil.getName(rowModel.getObject(), getParentPage());
                                        if (StringUtils.isBlank(name)) {
                                            name = createStringResource("AssignmentPanel.noName").getString();
                                        }
                                        cellItem.add(new Label(componentId, Model.of(name)));
                                    }
                                });
                                return columns;
                            }

                            @Override
                            protected ObjectQuery createObjectQuery(){
                                ObjectQuery query = super.createObjectQuery();
                                ObjectQuery focusMappingsQuery = SwitchAssignmentTypePanel.this.getPageBase().getPrismContext()
                                        .queryFor(AssignmentType.class)
                                        .exists(AssignmentType.F_FOCUS_MAPPINGS)
                                        .build();
                                query.addFilter(focusMappingsQuery.getFilter());
                                return query;
                            }

                            @Override
                            protected boolean isNewObjectButtonVisible(PrismObject focusObject){
                                return false;
                            }

                            @Override
                            protected QName getAssignmentType() {
                                return AssignmentType.F_FOCUS_MAPPINGS;
                            }

                        };
                assignmentPanel.setOutputMarkupId(true);
                switchAssignmentTypePerformed(target, assignmentPanel, ID_FOCUS_MAPPING_ASSIGNMENTS);
            }
        };
        focusMappingAssignmentsButton.add(AttributeAppender.append("class", getButtonStyleModel(ID_FOCUS_MAPPING_ASSIGNMENTS)));
        focusMappingAssignmentsButton.setOutputMarkupId(true);

        focusMappingAssignmentsButton.add(new VisibleBehaviour(()  -> isAssignmentPanelVisible()));
        buttonsContainer.add(focusMappingAssignmentsButton);

        //GDPR feature.. temporary disabled MID-4281
//        AjaxButton consentsButton = new AjaxButton(ID_CONSENT_ASSIGNMENTS, createStringResource("FocusType.consents")) {
//                            private static final long serialVersionUID = 1L;
//
//            @Override
//            public void onClick(AjaxRequestTarget target) {
//                GdprAssignmentPanel gdprAssignmentPanel =
//                        new GdprAssignmentPanel(ID_ASSIGNMENTS, SwitchAssignmentTypePanel.this.getModel()){
// private static final long serialVersionUID = 1L;
//
//        @Override
//        protected void assignmentDetailsPerformed(AjaxRequestTarget target) {
//            target.add(SwitchAssignmentTypePanel.this);
//        }
//
//        @Override
//        protected void cancelAssignmentDetailsPerformed(AjaxRequestTarget target) {
//            target.add(SwitchAssignmentTypePanel.this);
//        }
// };
//                gdprAssignmentPanel.setOutputMarkupId(true);
//                SwitchAssignmentTypePanel.this.addOrReplace(gdprAssignmentPanel);
//                target.add(SwitchAssignmentTypePanel.this);
//            }
//        };
//        consentsButton.setOutputMarkupId(true);
//        buttonsContainer.add(consentsButton);

        AjaxIconButton showAllAssignmentsButton = new AjaxIconButton(ID_SHOW_INDIRECT_ASSIGNMENTS, new Model<>("fa fa-address-card"),
                createStringResource("AssignmentTablePanel.menu.showAllAssignments")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                showAllAssignments(ajaxRequestTarget);
            }
        };
        showAllAssignmentsButton.add(AttributeAppender.append("class", getButtonStyleModel(ID_SHOW_INDIRECT_ASSIGNMENTS)));
        showAllAssignmentsButton.setOutputMarkupId(true);

        showAllAssignmentsButton.add(new VisibleBehaviour(()  -> !isInducement()));
        buttonsContainer.add(showAllAssignmentsButton);

    }

    protected void showAllAssignments(AjaxRequestTarget target) {
        PageBase pageBase = getPageBase();
        List<AssignmentInfoDto> previewAssignmentsList;
        if (pageBase instanceof PageAdminFocus) {
            previewAssignmentsList = ((PageAdminFocus<?>) pageBase).showAllAssignmentsPerformed(target);
        } else {
            previewAssignmentsList = Collections.emptyList();
        }
        AllAssignmentsPreviewDialog assignmentPanel = new AllAssignmentsPreviewDialog(ID_ASSIGNMENTS, previewAssignmentsList,
                pageBase);
        assignmentPanel.setOutputMarkupId(true);
        switchAssignmentTypePerformed(target, assignmentPanel, ID_SHOW_INDIRECT_ASSIGNMENTS);
    }

    private boolean isAssignmentPanelVisible() {
        if (getModelObject() == null){
            return false;
        }
        PrismObjectWrapper<?> objectWrapper = getModelObject().findObjectWrapper();
        if (objectWrapper == null ) {
            return true;
        }
        return objectWrapper.getObject().asObjectable() instanceof AbstractRoleType;
    }

    private LoadableModel<Boolean> getButtonsContainerVisibilityModel(){
        return new LoadableModel<Boolean>() {
            @Override
            protected Boolean load() {
                return getAssignmentsPanel().getMultivalueContainerListPanel().isListPanelVisible();
            }
        };
    }

    private void switchAssignmentTypePerformed(AjaxRequestTarget target, Component assignmentsPanel, String buttonId){
        activeButtonId = buttonId;
        addOrReplace(assignmentsPanel);
        target.add(SwitchAssignmentTypePanel.this);
    }

    private LoadableModel<String> getButtonStyleModel(String buttonId){
        return new LoadableModel<String>() {
            @Override
            protected String load() {
                if (activeButtonId.equals(buttonId)){
                    return "btn btn-primary";
                } else {
                    return "btn btn-default";
                }
            }
        };
    }

    private void initAssignmentsPanel(){
        AssignmentPanel assignmentsPanel = new AssignmentPanel(ID_ASSIGNMENTS, getModel()){
            private static final long serialVersionUID = 1L;

            @Override
            protected void assignmentDetailsPerformed(AjaxRequestTarget target) {
                target.add(SwitchAssignmentTypePanel.this);
            }

            @Override
            protected void cancelAssignmentDetailsPerformed(AjaxRequestTarget target){
                target.add(SwitchAssignmentTypePanel.this);
            }
        };
        assignmentsPanel.setOutputMarkupId(true);
        add(assignmentsPanel);
    }

    public AssignmentPanel getAssignmentsPanel(){
        return (AssignmentPanel) get(ID_ASSIGNMENTS);
    }

    protected boolean isInducement(){
        return false;
    }

    protected boolean isReadonly(){
        return false;
    }
}
