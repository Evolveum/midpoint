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
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import java.util.Collection;

/**
 * @author lazyman
 */
@Entity
@ForeignKey(name = "fk_system_configuration")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name_norm"}))
@org.hibernate.annotations.Table(appliesTo = "m_system_configuration",
        indexes = {@Index(name = "iSystemConfigurationName", columnNames = "name_orig")})
public class RSystemConfiguration extends RObject<SystemConfigurationType> {

    private static final Trace LOGGER = TraceManager.getTrace(RSystemConfiguration.class);
    private RPolyString name;
    private REmbeddedReference globalPasswordPolicyRef;
    private REmbeddedReference defaultUserTemplateRef;

    @Embedded
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public REmbeddedReference getGlobalPasswordPolicyRef() {
        return globalPasswordPolicyRef;
    }

    @Embedded
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public REmbeddedReference getDefaultUserTemplateRef() {
        return defaultUserTemplateRef;
    }

    @Embedded
    public RPolyString getName() {
        return name;
    }

    public void setName(RPolyString name) {
        this.name = name;
    }

    public void setDefaultUserTemplateRef(REmbeddedReference defaultUserTemplateRef) {
        this.defaultUserTemplateRef = defaultUserTemplateRef;
    }

    public void setGlobalPasswordPolicyRef(REmbeddedReference globalPasswordPolicyRef) {
        this.globalPasswordPolicyRef = globalPasswordPolicyRef;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RSystemConfiguration that = (RSystemConfiguration) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (defaultUserTemplateRef != null ? !defaultUserTemplateRef.equals(that.defaultUserTemplateRef) :
                that.defaultUserTemplateRef != null)
            return false;
        if (globalPasswordPolicyRef != null ? !globalPasswordPolicyRef.equals(that.globalPasswordPolicyRef) :
                that.globalPasswordPolicyRef != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (globalPasswordPolicyRef != null ? globalPasswordPolicyRef.hashCode() : 0);
        return result;
    }

    public static void copyToJAXB(RSystemConfiguration repo, SystemConfigurationType jaxb,
                                  PrismContext prismContext, Collection<SelectorOptions<GetOperationOptions>> options)
            throws DtoTranslationException {
        RObject.copyToJAXB(repo, jaxb, prismContext, options);

        jaxb.setName(RPolyString.copyToJAXB(repo.getName()));
        if (repo.getDefaultUserTemplateRef() != null) {
            jaxb.setDefaultUserTemplateRef(repo.getDefaultUserTemplateRef().toJAXB(prismContext));
        }

        if (repo.getGlobalPasswordPolicyRef() != null) {
            jaxb.setGlobalPasswordPolicyRef(repo.getGlobalPasswordPolicyRef().toJAXB(prismContext));
        }
    }

    public static void copyFromJAXB(SystemConfigurationType jaxb, RSystemConfiguration repo,
                                    PrismContext prismContext) throws DtoTranslationException {
        RObject.copyFromJAXB(jaxb, repo, prismContext);

        repo.setName(RPolyString.copyFromJAXB(jaxb.getName()));
        if (jaxb.getDefaultUserTemplate() != null) {
            LOGGER.warn("Default user template from system configuration type won't be saved. It should be " +
                    "translated to user template reference.");
        }

        repo.setDefaultUserTemplateRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getDefaultUserTemplateRef(), prismContext));

        if (jaxb.getGlobalPasswordPolicy() != null) {
            LOGGER.warn("Global password policy from system configuration type won't be saved. It should be " +
                    "translated to global password policy reference.");
        }

        repo.setGlobalPasswordPolicyRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getGlobalPasswordPolicyRef(), prismContext));
    }

    @Override
    public SystemConfigurationType toJAXB(PrismContext prismContext,
                                          Collection<SelectorOptions<GetOperationOptions>> options) throws DtoTranslationException {
        SystemConfigurationType object = new SystemConfigurationType();
        RUtil.revive(object, prismContext);
        RSystemConfiguration.copyToJAXB(this, object, prismContext, options);

        return object;
    }
}
