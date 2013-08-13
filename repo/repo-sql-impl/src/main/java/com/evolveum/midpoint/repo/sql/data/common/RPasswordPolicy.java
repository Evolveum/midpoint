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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordLifeTimeType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ValuePolicyType;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import javax.persistence.*;

/**
 * @author lazyman
 */
@Entity
@ForeignKey(name = "fk_password_policy")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name_norm"}))
@org.hibernate.annotations.Table(appliesTo = "m_password_policy",
        indexes = {@Index(name = "iPasswordPolicy", columnNames = "name_orig")})
public class RPasswordPolicy extends RObject {

    private RPolyString name;
    private String lifetime;
    private String stringPolicy;

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getLifetime() {
        return lifetime;
    }

    public void setLifetime(String lifetime) {
        this.lifetime = lifetime;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getStringPolicy() {
        return stringPolicy;
    }

    @Embedded
    public RPolyString getName() {
        return name;
    }

    public void setName(RPolyString name) {
        this.name = name;
    }

    public void setStringPolicy(String stringPolicy) {
        this.stringPolicy = stringPolicy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RPasswordPolicy that = (RPasswordPolicy) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (lifetime != null ? !lifetime.equals(that.lifetime) : that.lifetime != null) return false;
        if (stringPolicy != null ? !stringPolicy.equals(that.stringPolicy) : that.stringPolicy != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (lifetime != null ? lifetime.hashCode() : 0);
        result = 31 * result + (stringPolicy != null ? stringPolicy.hashCode() : 0);
        return result;
    }

    public static void copyToJAXB(RPasswordPolicy repo, ValuePolicyType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        RObject.copyToJAXB(repo, jaxb, prismContext);

        jaxb.setName(RPolyString.copyToJAXB(repo.getName()));
        try {
            jaxb.setLifetime(RUtil.toJAXB(ValuePolicyType.class, new ItemPath(ValuePolicyType.F_LIFETIME), repo.getLifetime(),
                    PasswordLifeTimeType.class, prismContext));
            jaxb.setStringPolicy(RUtil.toJAXB(ValuePolicyType.class, new ItemPath(ValuePolicyType.F_STRING_POLICY), repo.getStringPolicy(),
                    StringPolicyType.class, prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static void copyFromJAXB(ValuePolicyType jaxb, RPasswordPolicy repo, PrismContext prismContext) throws
            DtoTranslationException {
        RObject.copyFromJAXB(jaxb, repo, prismContext);

        repo.setName(RPolyString.copyFromJAXB(jaxb.getName()));
        try {
            repo.setLifetime(RUtil.toRepo(jaxb.getLifetime(), prismContext));
            repo.setStringPolicy(RUtil.toRepo(jaxb.getStringPolicy(), prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    @Override
    public ValuePolicyType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        ValuePolicyType policy = new ValuePolicyType();
        RUtil.revive(policy, prismContext);
        RPasswordPolicy.copyToJAXB(this, policy, prismContext);

        return policy;
    }
}
