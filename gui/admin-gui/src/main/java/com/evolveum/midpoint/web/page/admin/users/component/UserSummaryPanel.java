/*
 * Copyright (c) 2015-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.users.component;

import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.util.RelationUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.web.component.util.SummaryTag;

/**
 * @author semancik
 */
public class UserSummaryPanel extends FocusSummaryPanel<UserType> {
    private static final long serialVersionUID = -5077637168906420769L;

    public UserSummaryPanel(String id, IModel<UserType> model, SummaryPanelSpecificationType summaryPanelSpecificationType) {
        super(id, UserType.class, model, summaryPanelSpecificationType);
    }

    @Override
    protected List<SummaryTag<UserType>> getSummaryTagComponentList() {
        List<SummaryTag<UserType>> summaryTagList = super.getSummaryTagComponentList();

        SummaryTag<UserType> tagSecurity = new SummaryTag<>(ID_SUMMARY_TAG, getModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void initialize(UserType object) {
                List<AssignmentType> assignments = object.getAssignment();
                if (assignments.isEmpty()) {
                    setIconCssClass(GuiStyleConstants.CLASS_ICON_NO_OBJECTS);
                    setLabel(getString("user.noAssignments"));
                    setCssClass(GuiStyleConstants.CLASS_ICON_STYLE_DISABLED);
                    return;
                }

                List<ObjectReferenceType> roleMembershipRef = object.getRoleMembershipRef();

                boolean isSuperuser = false;
                boolean isEndUser = false;

                for (ObjectReferenceType objectReferenceType : roleMembershipRef) {
                    if (objectReferenceType.getOid() == null) {
                        continue;
                    }

                    QName relation = objectReferenceType.getRelation();
                    if (!RelationUtil.isDefaultRelation(relation)) {
                        continue;
                    }

                    if (SystemObjectsType.ROLE_SUPERUSER.value().equals(objectReferenceType.getOid())) {
                        isSuperuser = true;
                    } else if (SystemObjectsType.ROLE_END_USER.value().equals(objectReferenceType.getOid())) {
                        isEndUser = true;
                    }

                }

                if (isSuperuser) {
                    setIconCssClass(GuiStyleConstants.CLASS_ICON_SUPERUSER);
                    setLabel(getString("user.superuser"));
                    setCssClass(GuiStyleConstants.CLASS_ICON_STYLE_PRIVILEGED);
                } else if (isEndUser) {
                    setIconCssClass(GuiStyleConstants.CLASS_OBJECT_USER_ICON);
                    setLabel(getString("user.enduser"));
                    setCssClass(GuiStyleConstants.CLASS_ICON_STYLE_END_USER);
                } else {
                    setHideTag(true);
                }
            }
        };
        summaryTagList.add(tagSecurity);

        SummaryTag<UserType> tagOrg = new SummaryTag<>(ID_SUMMARY_TAG, getModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void initialize(UserType object) {
                List<ObjectReferenceType> parentOrgRefs = object.getParentOrgRef();
                if (parentOrgRefs.isEmpty()) {
                    setIconCssClass(GuiStyleConstants.CLASS_ICON_NO_OBJECTS);
                    setLabel(getString("user.noOrgs"));
                    setCssClass(GuiStyleConstants.CLASS_ICON_STYLE_DISABLED);
                    return;
                }
                boolean isManager = false;
                boolean isMember = false;
                for (ObjectReferenceType parentOrgRef : object.getParentOrgRef()) {
                    if (RelationUtil.isManagerRelation(parentOrgRef.getRelation())) {
                        isManager = true;
                    } else {
                        isMember = true;
                    }
                }
                if (isManager) {
                    setIconCssClass(GuiStyleConstants.CLASS_OBJECT_ORG_ICON);
                    setLabel(getString("user.orgManager"));
                    setCssClass(GuiStyleConstants.CLASS_ICON_STYLE_MANAGER);
                } else if (isMember) {
                    setIconCssClass(GuiStyleConstants.CLASS_OBJECT_ORG_ICON);
                    setLabel(getString("user.orgMember"));
                } else {
                    setHideTag(true);
                }
            }
        };
        summaryTagList.add(tagOrg);

        return summaryTagList;
    }

    @Override
    protected QName getDisplayNamePropertyName() {
        return UserType.F_FULL_NAME;
    }

    @Override
    protected QName getTitlePropertyName() {
        return UserType.F_TITLE;
    }

    @Override
    protected String getDefaultIconCssClass() {
        return GuiStyleConstants.CLASS_OBJECT_USER_ICON;
    }

    @Override
    protected String getIconBoxAdditionalCssClass() {
        return "summary-panel-user";
    }

    @Override
    protected String getBoxAdditionalCssClass() {
        return "summary-panel-user";
    }

}
