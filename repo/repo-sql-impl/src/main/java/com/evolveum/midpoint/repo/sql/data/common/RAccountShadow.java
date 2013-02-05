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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RCredentials;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.query.QueryAttribute;
import com.evolveum.midpoint.repo.sql.query.QueryEntity;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;

import org.hibernate.annotations.ForeignKey;

import javax.persistence.Embedded;
import javax.persistence.Entity;

/**
 * @author lazyman
 */
@Entity
@ForeignKey(name = "fk_account_shadow")
public class RAccountShadow extends RResourceObjectShadow {

    @QueryAttribute
    private String accountType;
    @QueryEntity(embedded = true)
    private RCredentials credentials;

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    @Embedded
    public RCredentials getCredentials() {
        return credentials;
    }

    public void setCredentials(RCredentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RAccountShadow that = (RAccountShadow) o;

        if (accountType != null ? !accountType.equals(that.accountType) : that.accountType != null) return false;
        if (credentials != null ? !credentials.equals(that.credentials) : that.credentials != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (accountType != null ? accountType.hashCode() : 0);
        result = 31 * result + (credentials != null ? credentials.hashCode() : 0);
        return result;
    }

    public static void copyToJAXB(RAccountShadow repo, AccountShadowType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        RResourceObjectShadow.copyToJAXB(repo, jaxb, prismContext);

        jaxb.setAccountType(repo.getAccountType());

        if (repo.getCredentials() != null) {
            ItemPath path = new ItemPath(AccountShadowType.F_CREDENTIALS);
            jaxb.setCredentials(repo.getCredentials().toJAXB(jaxb, path, prismContext));
        }
    }

    public static void copyFromJAXB(AccountShadowType jaxb, RAccountShadow repo,
            PrismContext prismContext) throws DtoTranslationException {
        RResourceObjectShadow.copyFromJAXB(jaxb, repo, prismContext);

        repo.setAccountType(jaxb.getAccountType());

        if (jaxb.getCredentials() != null) {
            RCredentials credentials = new RCredentials();
            RCredentials.copyFromJAXB(jaxb.getCredentials(), credentials, prismContext);
            repo.setCredentials(credentials);
        }
    }

    public AccountShadowType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        AccountShadowType shadow = new AccountShadowType();
        RUtil.revive(shadow, prismContext);
        RAccountShadow.copyToJAXB(this, shadow, prismContext);

        return shadow;
    }
}
