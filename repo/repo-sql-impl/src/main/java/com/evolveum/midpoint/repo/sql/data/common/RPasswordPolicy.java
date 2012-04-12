/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.repo.sql.DtoTranslationException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PasswordLifeTimeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PasswordPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.StringPolicyType;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author lazyman
 */
@Entity
@Table(name = "password_policy")
@ForeignKey(name = "fk_password_policy")
public class RPasswordPolicy extends RObject {

    private String lifetime;
    private String stringPolicy;

    @Type(type = "org.hibernate.type.MaterializedClobType")
    public String getLifetime() {
        return lifetime;
    }

    public void setLifetime(String lifetime) {
        this.lifetime = lifetime;
    }

    @Type(type = "org.hibernate.type.MaterializedClobType")
    public String getStringPolicy() {
        return stringPolicy;
    }

    public void setStringPolicy(String stringPolicy) {
        this.stringPolicy = stringPolicy;
    }

    public static void copyToJAXB(RPasswordPolicy repo, PasswordPolicyType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        RObject.copyToJAXB(repo, jaxb, prismContext);

        try {
            jaxb.setLifetime(RUtil.toJAXB(PasswordPolicyType.class, new PropertyPath(PasswordPolicyType.F_LIFETIME), repo.getLifetime(),
                    PasswordLifeTimeType.class, prismContext));
            jaxb.setStringPolicy(RUtil.toJAXB(PasswordPolicyType.class, new PropertyPath(PasswordPolicyType.F_STRING_POLICY), repo.getStringPolicy(),
                    StringPolicyType.class, prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static void copyFromJAXB(PasswordPolicyType jaxb, RPasswordPolicy repo, boolean pushCreateIdentificators
            , PrismContext prismContext) throws DtoTranslationException {
        RObject.copyFromJAXB(jaxb, repo, pushCreateIdentificators, prismContext);

        try {
            repo.setLifetime(RUtil.toRepo(jaxb.getLifetime(), prismContext));
            repo.setStringPolicy(RUtil.toRepo(jaxb.getStringPolicy(), prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    @Override
    public PasswordPolicyType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        PasswordPolicyType policy = new PasswordPolicyType();
        RUtil.revive(policy.asPrismObject(), PasswordPolicyType.class, prismContext);
        RPasswordPolicy.copyToJAXB(this, policy, prismContext);

        return policy;
    }
}
