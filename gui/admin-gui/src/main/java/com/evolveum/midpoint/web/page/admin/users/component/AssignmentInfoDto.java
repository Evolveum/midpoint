/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.users.component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.util.RelationUtil;
import com.evolveum.midpoint.model.api.context.AssignmentPath;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Objects;

/**
 * Not to be confused with AssignmentDto. This one is used in assignment dialog (preview, selecting for delegation limitations, ...).
 */
@SuppressWarnings("unused")
public class AssignmentInfoDto extends Selectable<AssignmentInfoDto> implements Serializable, Comparable<AssignmentInfoDto> {

    public static final String F_TARGET_OID = "targetOid";
    public static final String F_TARGET_NAME = "targetName";
    public static final String F_TARGET_DESCRIPTION = "targetDescription";
    public static final String F_TARGET_TYPE = "targetType";
    public static final String F_DIRECT = "direct";
    public static final String F_KIND = "kind";
    public static final String F_INTENT = "intent";
    public static final String F_TENANT_NAME = "tenantName";
    public static final String F_ORG_REF_NAME = "orgRefName";
    public static final String F_REMARK = "remark";
    public static final String F_RELATION = "relation";

    // target = role, org or resource
    private String targetOid;
    private String targetName;
    private String targetDescription;
    private Class<? extends ObjectType> targetClass;
    private boolean direct;                                 // true if directly assigned; used only in some contexts
    private ObjectType assignmentParent;
    private QName targetType;
    // for resource assignments
    private ShadowKindType kind;
    private String intent;
    // for role/org assignments
    private String tenantName;
    private String orgRefName;
    private ObjectReferenceType tenantRef;
    private ObjectReferenceType orgRef;
    // generic
    private String remark;
    private QName relation;

    public String getTargetOid() {
        return targetOid;
    }

    public void setTargetOid(String targetOid) {
        this.targetOid = targetOid;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getTargetDescription() {
        return targetDescription;
    }

    public void setTargetDescription(String targetDescription) {
        this.targetDescription = targetDescription;
    }

    public Class<? extends ObjectType> getTargetClass() {
        return targetClass;
    }

    public void setTargetClass(Class<? extends ObjectType> targetClass) {
        this.targetClass = targetClass;
    }

    public boolean isDirect() {
        return direct;
    }

    public void setDirect(boolean direct) {
        this.direct = direct;
    }

    public void setAssignmentParent(AssignmentPath assignmentPath) {
        if (assignmentPath != null && assignmentPath.size() > 1 ) {
            assignmentParent = assignmentPath.last().getSource();
        }
    }

    public ObjectType getAssignmentParent() {
        return assignmentParent;
    }

    public ShadowKindType getKind() {
        return kind;
    }

    public void setKind(ShadowKindType kind) {
        this.kind = kind;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getOrgRefName() {
        return orgRefName;
    }

    public void setOrgRefName(String orgRefName) {
        this.orgRefName = orgRefName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public ObjectReferenceType getTenantRef() {
        return tenantRef;
    }

    public void setTenantRef(ObjectReferenceType tenantRef) {
        this.tenantRef = tenantRef;
    }

    public ObjectReferenceType getOrgRef() {
        return orgRef;
    }

    public void setOrgRef(ObjectReferenceType orgRef) {
        this.orgRef = orgRef;
    }

    public QName getTargetType() {
        return targetType;
    }

    public void setTargetType(QName targetType) {
        this.targetType = targetType;
    }

    public QName getRelation() {
        return relation;
    }

    public void setRelation(QName relation) {
        this.relation = relation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof AssignmentInfoDto))
            return false;
        AssignmentInfoDto that = (AssignmentInfoDto) o;
        return direct == that.direct &&
                Objects.equals(targetOid, that.targetOid) &&
                Objects.equals(targetName, that.targetName) &&
                Objects.equals(targetDescription, that.targetDescription) &&
                Objects.equals(targetClass, that.targetClass) &&
                Objects.equals(targetType, that.targetType) &&
                kind == that.kind &&
                Objects.equals(intent, that.intent) &&
                Objects.equals(tenantName, that.tenantName) &&
                Objects.equals(orgRefName, that.orgRefName) &&
                Objects.equals(tenantRef, that.tenantRef) &&
                Objects.equals(orgRef, that.orgRef) &&
                Objects.equals(remark, that.remark) &&
                Objects.equals(relation, that.relation);
    }

    @Override
    public int hashCode() {
        return Objects
                .hash(targetOid, targetName, targetDescription, targetClass, direct, targetType, kind, intent, tenantName,
                        orgRefName, tenantRef, orgRef, remark, relation);
    }

    @Override
    public int compareTo(AssignmentInfoDto other) {
        if (this.equals(other)) {
            return 0;
        }

        // firstly sorting by type: orgs -> roles -> resources -> all the other (in the future)
        int co1 = getClassOrder(this.getTargetClass());
        int co2 = getClassOrder(other.getTargetClass());
        if (co1 != co2) {
            return co1 - co2;
        }

        // then by name
        if (this.getTargetName() != null && other.getTargetName() != null) {
            int order = this.getTargetName().compareToIgnoreCase(other.getTargetName());
            if (order != 0) {
                return order;
            }
        } else if (this.getTargetName() != null && other.getTargetName() == null) {
            return -1;      // named are before unnamed
        } else if (this.getTargetName() == null && other.getTargetName() != null) {
            return 1;       // unnamed are after named
        } else {
            // both unnamed - no decision
        }

        // if class and names are equal, the order can be arbitrary

        if (this.hashCode() <= other.hashCode()) {
            return -1;
        } else {
            return 1;
        }
    }

    private int getClassOrder(Class targetClass) {
        if (OrgType.class.equals(targetClass)) {
            return 0;
        } else if (RoleType.class.equals(targetClass)) {
            return 1;
        } else if (ResourceType.class.equals(targetClass)) {
            return 2;
        } else {
            return 3;
        }
    }

    public IModel<String> getRelationDisplayNameModel(Component component) {
        if (relation == null) {
            return Model.of("");
        }
        String relationDisplayName = RelationUtil.getRelationHeaderLabelKeyIfKnown(relation);
        return StringUtils.isNotEmpty(relationDisplayName) ?
                PageBase.createStringResourceStatic(relationDisplayName) :
                PageBase.createStringResourceStatic(relation.getLocalPart());
    }
}
