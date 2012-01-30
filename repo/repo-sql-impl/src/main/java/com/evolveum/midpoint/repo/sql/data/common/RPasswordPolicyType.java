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

import com.evolveum.midpoint.repo.sql.DtoTranslationException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PasswordLifeTimeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PasswordPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.StringPolicyType;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.Type;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author lazyman
 */
@Entity
@Table(name = "password_policy")
public class RPasswordPolicyType extends RObjectType {

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

    public static void copyToJAXB(RPasswordPolicyType repo, PasswordPolicyType jaxb) throws DtoTranslationException {
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notNull(repo, "Repo object must not be null.");

        try {
            jaxb.setLifetime(RUtil.toJAXB(repo.getLifetime(), PasswordLifeTimeType.class));
            jaxb.setStringPolicy(RUtil.toJAXB(repo.getStringPolicy(), StringPolicyType.class));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static void copyFromJAXB(PasswordPolicyType jaxb, RPasswordPolicyType repo) throws DtoTranslationException {
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notNull(repo, "Repo object must not be null.");

        try {
            repo.setLifetime(RUtil.toRepo(jaxb.getLifetime()));
            repo.setStringPolicy(RUtil.toRepo(jaxb.getStringPolicy()));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    @Override
    public PasswordPolicyType toJAXB() throws DtoTranslationException {
        PasswordPolicyType policy = new PasswordPolicyType();
        RPasswordPolicyType.copyToJAXB(this, policy);
        return policy;
    }
}
