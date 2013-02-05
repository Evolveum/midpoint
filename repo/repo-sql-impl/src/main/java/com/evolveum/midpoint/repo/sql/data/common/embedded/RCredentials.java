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

package com.evolveum.midpoint.repo.sql.data.common.embedded;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordType;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Lob;

/**
 * @author lazyman
 */
@Embeddable
public class RCredentials {

    private String password;
    private Boolean allowedIdmAdminGuiAccess;

    @Column(nullable = true)
    public Boolean isAllowedIdmAdminGuiAccess() {
        return allowedIdmAdminGuiAccess;
    }

    public void setAllowedIdmAdminGuiAccess(Boolean allowedIdmAdminGuiAccess) {
        this.allowedIdmAdminGuiAccess = allowedIdmAdminGuiAccess;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    @Column(name = "passwordXml", nullable = true)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RCredentials that = (RCredentials) o;

        if (allowedIdmAdminGuiAccess != null ? !allowedIdmAdminGuiAccess.equals(that.allowedIdmAdminGuiAccess) :
                that.allowedIdmAdminGuiAccess != null) return false;
        if (password != null ? !password.equals(that.password) : that.password != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = password != null ? password.hashCode() : 0;
        result = 31 * result + (allowedIdmAdminGuiAccess != null ? allowedIdmAdminGuiAccess.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public static void copyToJAXB(RCredentials repo, CredentialsType jaxb, ObjectType parent, ItemPath path,
            PrismContext prismContext) throws DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        try {
            jaxb.setAllowedIdmAdminGuiAccess(repo.isAllowedIdmAdminGuiAccess());
            ItemPath passwordPath = new ItemPath(path, CredentialsType.F_PASSWORD);
            jaxb.setPassword(RUtil.toJAXB(parent.getClass(), passwordPath, repo.getPassword(), PasswordType.class, prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static void copyFromJAXB(CredentialsType jaxb, RCredentials repo, PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        try {
            repo.setAllowedIdmAdminGuiAccess(jaxb.isAllowedIdmAdminGuiAccess());
            repo.setPassword(RUtil.toRepo(jaxb.getPassword(), prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public CredentialsType toJAXB(ObjectType parent, ItemPath path, PrismContext prismContext) throws
            DtoTranslationException {
        CredentialsType credentials = new CredentialsType();
        RCredentials.copyToJAXB(this, credentials, parent, path, prismContext);
        return credentials;
    }
}
