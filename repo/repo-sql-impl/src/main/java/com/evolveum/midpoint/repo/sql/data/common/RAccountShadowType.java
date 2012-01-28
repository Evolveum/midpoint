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
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import org.apache.commons.lang.Validate;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author lazyman
 */
@Entity
@Table(name = "account_shadow")
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
        return activation;
    }

    public void setActivation(RActivationType activation) {
        this.activation = activation;
    }

    @Embedded
    public RCredentialsType getCredentials() {
        return credentials;
    }

    public void setCredentials(RCredentialsType credentials) {
        this.credentials = credentials;
    }

    public static void copyToJAXB(RAccountShadowType repo, AccountShadowType jaxb) throws DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        jaxb.setAccountType(repo.getAccountType());

        RActivationType activation = new RActivationType();
        if (jaxb.getActivation() != null) {
            RActivationType.copyToJAXB(activation, jaxb.getActivation());
        }
        repo.setActivation(activation);
        RCredentialsType credentials = new RCredentialsType();
        if (jaxb.getCredentials() != null) {
            RCredentialsType.copyToJAXB(credentials, jaxb.getCredentials());
        }
        repo.setCredentials(credentials);
    }

    public static void copyFromJAXB(AccountShadowType jaxb, RAccountShadowType repo) throws DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        repo.setAccountType(jaxb.getAccountType());

        RActivationType activation = new RActivationType();
        if (jaxb.getActivation() != null) {
            RActivationType.copyFromJAXB(jaxb.getActivation(), activation);
        }
        repo.setActivation(activation);
        RCredentialsType credentials = new RCredentialsType();
        if (jaxb.getCredentials() != null) {
            RCredentialsType.copyFromJAXB(jaxb.getCredentials(), credentials);
        }
        repo.setCredentials(credentials);
    }
}
