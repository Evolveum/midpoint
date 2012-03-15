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
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author lazyman
 */
@Entity
@Table(name = "account_shadow")
@ForeignKey(name = "fk_account_shadow")
public class RAccountShadowType extends RResourceObjectShadowType {

    private String accountType;
    private RCredentialsType credentials;
    private RActivationType activation;

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    @Embedded
    public RActivationType getActivation() {
        if (activation == null) {
            activation = new RActivationType();
        }
        return activation;
    }

    public void setActivation(RActivationType activation) {
        this.activation = activation;
    }

    @Embedded
    public RCredentialsType getCredentials() {
        if (credentials == null) {
            credentials = new RCredentialsType();
        }
        return credentials;
    }

    public void setCredentials(RCredentialsType credentials) {
        this.credentials = credentials;
    }

    public static void copyToJAXB(RAccountShadowType repo, AccountShadowType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        RResourceObjectShadowType.copyToJAXB(repo, jaxb, prismContext);

        jaxb.setAccountType(repo.getAccountType());

        jaxb.setActivation(repo.getActivation().toJAXB(prismContext));

        PropertyPath path = new PropertyPath(AccountShadowType.F_CREDENTIALS);
        jaxb.setCredentials(repo.getCredentials().toJAXB(jaxb, path, prismContext));
    }

    public static void copyFromJAXB(AccountShadowType jaxb, RAccountShadowType repo, PrismContext prismContext) throws
            DtoTranslationException {
        RResourceObjectShadowType.copyFromJAXB(jaxb, repo, prismContext);

        repo.setAccountType(jaxb.getAccountType());

        RActivationType activation = new RActivationType();
        if (jaxb.getActivation() != null) {
            RActivationType.copyFromJAXB(jaxb.getActivation(), activation, prismContext);
        }
        repo.setActivation(activation);

        RCredentialsType credentials = new RCredentialsType();
        if (jaxb.getCredentials() != null) {
            RCredentialsType.copyFromJAXB(jaxb.getCredentials(), credentials, prismContext);
        }
        repo.setCredentials(credentials);
    }

    public AccountShadowType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        AccountShadowType shadow = new AccountShadowType();
        RAccountShadowType.copyToJAXB(this, shadow, prismContext);
        RUtil.revive(shadow.asPrismObject(), AccountShadowType.class, prismContext);
        return shadow;
    }
}
