/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.users.component;

import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;
import java.io.Serializable;

/**
 * @author mederly
 */
public class AssignmentsPreviewDto extends SelectableBean implements Serializable, Comparable {

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
    private Class targetClass;
    private boolean direct;                     // directly assigned?
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

    public Class getTargetClass() {
        return targetClass;
    }

    public void setTargetClass(Class targetClass) {
        this.targetClass = targetClass;
    }

    public boolean isDirect() {
        return direct;
    }

    public void setDirect(boolean direct) {
        this.direct = direct;
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AssignmentsPreviewDto that = (AssignmentsPreviewDto) o;

        if (direct != that.direct) return false;
        if (targetOid != null ? !targetOid.equals(that.targetOid) : that.targetOid != null) return false;
        if (targetName != null ? !targetName.equals(that.targetName) : that.targetName != null) return false;
        if (targetClass != null ? !targetClass.equals(that.targetClass) : that.targetClass != null) return false;
        if (kind != that.kind) return false;
        if (intent != null ? !intent.equals(that.intent) : that.intent != null) return false;
        if (tenantName != null ? !tenantName.equals(that.tenantName) : that.tenantName != null) return false;
        if (orgRefName != null ? !orgRefName.equals(that.orgRefName) : that.orgRefName != null) return false;
        if (relation != null ? !relation.equals(that.relation) : that.relation != null) return false;
        return !(remark != null ? !remark.equals(that.remark) : that.remark != null);

    }

    @Override
    public int hashCode() {
        int result = targetOid != null ? targetOid.hashCode() : 0;
        result = 31 * result + (targetName != null ? targetName.hashCode() : 0);
        result = 31 * result + (targetClass != null ? targetClass.hashCode() : 0);
        result = 31 * result + (direct ? 1 : 0);
        result = 31 * result + (kind != null ? kind.hashCode() : 0);
        result = 31 * result + (intent != null ? intent.hashCode() : 0);
        result = 31 * result + (tenantName != null ? tenantName.hashCode() : 0);
        result = 31 * result + (orgRefName != null ? orgRefName.hashCode() : 0);
        result = 31 * result + (remark != null ? remark.hashCode() : 0);
        result = 31 * result + (relation != null ? relation.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(Object o) {
        if (!(o instanceof AssignmentsPreviewDto)) {
            return -1;      // should not occur
        }

        if (this.equals(o)) {
            return 0;
        }

        AssignmentsPreviewDto other = (AssignmentsPreviewDto) o;

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

        if (this.hashCode() <= o.hashCode()) {
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
}
