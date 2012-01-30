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
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PasswordType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.annotations.Type;

import javax.persistence.Embeddable;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lazyman
 */
@Embeddable
public class RCredentialsType {

    private String password;
    private boolean allowedIdmAdminGuiAccess = false;

    public boolean isAllowedIdmAdminGuiAccess() {
        return allowedIdmAdminGuiAccess;
    }

    public void setAllowedIdmAdminGuiAccess(boolean allowedIdmAdminGuiAccess) {
        this.allowedIdmAdminGuiAccess = allowedIdmAdminGuiAccess;
    }

    @Type(type = "org.hibernate.type.TextType")
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static void copyToJAXB(RCredentialsType repo, CredentialsType jaxb) throws DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        try {
            jaxb.setAllowedIdmAdminGuiAccess(repo.isAllowedIdmAdminGuiAccess());

            if (StringUtils.isNotEmpty(repo.getPassword())) {
                JAXBElement<PasswordType> password = (JAXBElement<PasswordType>) JAXBUtil.unmarshal(repo.getPassword());
                jaxb.setPassword(password.getValue());
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static void copyFromJAXB(CredentialsType jaxb, RCredentialsType repo) throws DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        boolean allowed = jaxb.isAllowedIdmAdminGuiAccess() != null ? jaxb.isAllowedIdmAdminGuiAccess() : false;
        repo.setAllowedIdmAdminGuiAccess(allowed);

        try {
            jaxb.setAllowedIdmAdminGuiAccess(repo.isAllowedIdmAdminGuiAccess());

            if (jaxb.getPassword() != null) {
                PasswordType password = jaxb.getPassword();
                
                Map<String, Object> properties = new HashMap<String, Object>();
                properties.put(Marshaller.JAXB_FORMATTED_OUTPUT, false);

                repo.setPassword(JAXBUtil.marshalWrap(password, properties));
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }
}
