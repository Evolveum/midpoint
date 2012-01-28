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
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemConfigurationType;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author lazyman
 */
@Entity
@Table(name = "system_configuration")
public class RSystemConfigurationType extends RExtensibleObjectType {

    private String configuration;

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public static void copyToJAXB(RSystemConfigurationType repo, SystemConfigurationType jaxb) throws
            DtoTranslationException {
        RExtensibleObjectType.copyToJAXB(repo, jaxb);

//        try {
//            if (StringUtils.isNotEmpty(repo.getConfiguration())) {
//                JAXBElement<SystemConfigurationType> password = (JAXBElement<SystemConfigurationType>)
//                        JAXBUtil.unmarshal(repo.getConfiguration());
//                //todo: do we split configuration to multiple columns?
//            }
//        } catch (Exception ex) {
//            throw new DtoTranslationException(ex.getMessage(), ex);
//        }
    }

    public static void copyFromJAXB(SystemConfigurationType jaxb, RSystemConfigurationType repo) throws
            DtoTranslationException {
        RExtensibleObjectType.copyFromJAXB(jaxb, repo);

//        try {
//            if (jaxb.getPassword() != null) {
//                PasswordType password = jaxb.getPassword();
//                repo.setPassword(JAXBUtil.marshalWrap(password));
//            }
//            //todo: do we split configuration to multiple columns?
//        } catch (Exception ex) {
//            throw new DtoTranslationException(ex.getMessage(), ex);
//        }
    }

    @Override
    public SystemConfigurationType toJAXB() throws DtoTranslationException {
        SystemConfigurationType object = new SystemConfigurationType();
        RSystemConfigurationType.copyToJAXB(this, object);
        return object;
    }
}
