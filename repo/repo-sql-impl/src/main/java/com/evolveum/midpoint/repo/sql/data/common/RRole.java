/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * @author lazyman
 */
@Entity
@ForeignKey(name = "fk_role")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name_norm"}))
public class RRole extends RAbstractRole {

    private RPolyString name;
    private String roleType;

    public String getRoleType() {
        return roleType;
    }

    @Embedded
    public RPolyString getName() {
        return name;
    }

    public void setName(RPolyString name) {
        this.name = name;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

        RRole rRole = (RRole) o;

        if (name != null ? !name.equals(rRole.name) : rRole.name != null)
            return false;
        if (roleType != null ? !roleType.equals(rRole.roleType) : rRole.roleType != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (roleType != null ? roleType.hashCode() : 0);
        return result;
    }

    public static void copyToJAXB(RRole repo, RoleType jaxb, PrismContext prismContext)
            throws DtoTranslationException {
        RAbstractRole.copyToJAXB(repo, jaxb, prismContext);

        jaxb.setName(RPolyString.copyToJAXB(repo.getName()));
        jaxb.setRoleType(repo.getRoleType());
    }

    public static void copyFromJAXB(RoleType jaxb, RRole repo, PrismContext prismContext)
            throws DtoTranslationException {
        RAbstractRole.copyFromJAXB(jaxb, repo, prismContext);

        repo.setRoleType(jaxb.getRoleType());
        repo.setName(RPolyString.copyFromJAXB(jaxb.getName()));
    }

    @Override
    public RoleType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        RoleType object = new RoleType();
        RRole.copyToJAXB(this, object, prismContext);
        RUtil.revive(object, prismContext);
        return object;
    }
}
