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

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import java.io.Serializable;

/**
 * @author mederly
 */
public class AssignmentsPreviewDto implements Serializable, Comparable {

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

    // target = role, org or resource
    private String targetOid;
    private String targetName;
    private String targetDescription;
    private Class targetClass;
    private boolean direct;                     // directly assigned?
    // for resource assignments
    private ShadowKindType kind;
    private String intent;
    // for role/org assignments
    private String tenantName;
    private String orgRefName;
    // generic
    private String remark;

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
        return result;
    }

    @Override
    public int compareTo(Object o) {
        if (!(o instanceof AssignmentsPreviewDto)) {
            return -1;
        }
        AssignmentsPreviewDto other = (AssignmentsPreviewDto) o;
        if (this.getTargetName() != null) {
            if (other.getTargetName() != null) {
                return this.getTargetName().compareTo(other.getTargetName());
            } else {
                return -1;      // named are before unnamed
            }
        } else {
            if (other.getTargetName() != null) {
                return 1;       // unnamed are after named
            } else {
                return 0;       // undeterminable
            }
        }
    }
}
