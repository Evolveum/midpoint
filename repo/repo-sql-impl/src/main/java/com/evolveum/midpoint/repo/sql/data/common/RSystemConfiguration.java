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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * @author lazyman
 */
@Entity
@Table(name = "system_configuration")
@ForeignKey(name = "fk_system_configuration")
public class RSystemConfiguration extends RObject {

    private static final Trace LOGGER = TraceManager.getTrace(RSystemConfiguration.class);
    private String globalAccountSynchronizationSettings;
    private String modelHooks;
    private String logging;
    private RObjectReference defaultUserTemplateRef;
    private String connectorFramework;

    @Type(type = "org.hibernate.type.TextType")
    public String getConnectorFramework() {
        return connectorFramework;
    }

    @OneToOne(optional = true, mappedBy = "owner")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public RObjectReference getDefaultUserTemplateRef() {
        return defaultUserTemplateRef;
    }

    @Type(type = "org.hibernate.type.TextType")
    public String getGlobalAccountSynchronizationSettings() {
        return globalAccountSynchronizationSettings;
    }

    @Type(type = "org.hibernate.type.TextType")
    public String getLogging() {
        return logging;
    }

    @Type(type = "org.hibernate.type.TextType")
    public String getModelHooks() {
        return modelHooks;
    }

    public void setConnectorFramework(String connectorFramework) {
        this.connectorFramework = connectorFramework;
    }

    public void setDefaultUserTemplateRef(RObjectReference defaultUserTemplateRef) {
        this.defaultUserTemplateRef = defaultUserTemplateRef;
    }

    public void setGlobalAccountSynchronizationSettings(String globalAccountSynchronizationSettings) {
        this.globalAccountSynchronizationSettings = globalAccountSynchronizationSettings;
    }

    public void setLogging(String logging) {
        this.logging = logging;
    }

    public void setModelHooks(String modelHooks) {
        this.modelHooks = modelHooks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RSystemConfiguration that = (RSystemConfiguration) o;

        if (connectorFramework != null ? !connectorFramework.equals(that.connectorFramework) : that.connectorFramework != null)
            return false;
        if (defaultUserTemplateRef != null ? !defaultUserTemplateRef.equals(that.defaultUserTemplateRef) : that.defaultUserTemplateRef != null)
            return false;
        if (globalAccountSynchronizationSettings != null ? !globalAccountSynchronizationSettings.equals(that.globalAccountSynchronizationSettings) : that.globalAccountSynchronizationSettings != null)
            return false;
        if (logging != null ? !logging.equals(that.logging) : that.logging != null) return false;
        if (modelHooks != null ? !modelHooks.equals(that.modelHooks) : that.modelHooks != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (globalAccountSynchronizationSettings != null ? globalAccountSynchronizationSettings.hashCode() : 0);
        result = 31 * result + (modelHooks != null ? modelHooks.hashCode() : 0);
        result = 31 * result + (logging != null ? logging.hashCode() : 0);
        result = 31 * result + (connectorFramework != null ? connectorFramework.hashCode() : 0);
        return result;
    }

    public static void copyToJAXB(RSystemConfiguration repo, SystemConfigurationType jaxb,
            PrismContext prismContext) throws DtoTranslationException {
        RObject.copyToJAXB(repo, jaxb, prismContext);

        if (repo.getDefaultUserTemplateRef() != null) {
            jaxb.setDefaultUserTemplateRef(repo.getDefaultUserTemplateRef().toJAXB(prismContext));
        }

        try {
            jaxb.setConnectorFramework(RUtil.toJAXB(SystemConfigurationType.class, new PropertyPath(SystemConfigurationType.F_CONNECTOR_FRAMEWORK),
                    repo.getConnectorFramework(), ConnectorFrameworkType.class, prismContext));
            jaxb.setGlobalAccountSynchronizationSettings(RUtil.toJAXB(SystemConfigurationType.class,
                    new PropertyPath(SystemConfigurationType.F_GLOBAL_ACCOUNT_SYNCHRONIZATION_SETTINGS),
                    repo.getGlobalAccountSynchronizationSettings(), AccountSynchronizationSettingsType.class,
                    prismContext));
            jaxb.setLogging(RUtil.toJAXB(SystemConfigurationType.class, new PropertyPath(SystemConfigurationType.F_LOGGING),
                    repo.getLogging(), LoggingConfigurationType.class, prismContext));
            jaxb.setModelHooks(RUtil.toJAXB(SystemConfigurationType.class, new PropertyPath(SystemConfigurationType.F_MODEL_HOOKS),
                    repo.getModelHooks(), ModelHooksType.class, prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static void copyFromJAXB(SystemConfigurationType jaxb, RSystemConfiguration repo,
            boolean pushCreateIdentificators, PrismContext prismContext) throws DtoTranslationException {
        RObject.copyFromJAXB(jaxb, repo, pushCreateIdentificators, prismContext);

        if (jaxb.getDefaultUserTemplate() != null) {
            LOGGER.warn("Default user template from system configuration type won't be saved. It should be " +
                    "translated to user template reference.");
        }

        repo.setDefaultUserTemplateRef(RUtil.jaxbRefToRepo(jaxb.getDefaultUserTemplateRef(), repo, prismContext));

        try {
            repo.setConnectorFramework(RUtil.toRepo(jaxb.getConnectorFramework(), prismContext));
            repo.setGlobalAccountSynchronizationSettings(RUtil.toRepo(jaxb.getGlobalAccountSynchronizationSettings(), prismContext));
            repo.setLogging(RUtil.toRepo(jaxb.getLogging(), prismContext));
            repo.setModelHooks(RUtil.toRepo(jaxb.getModelHooks(), prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    @Override
    public SystemConfigurationType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        SystemConfigurationType object = new SystemConfigurationType();
        RUtil.revive(object.asPrismObject(), SystemConfigurationType.class, prismContext);
        RSystemConfiguration.copyToJAXB(this, object, prismContext);

        return object;
    }
}
