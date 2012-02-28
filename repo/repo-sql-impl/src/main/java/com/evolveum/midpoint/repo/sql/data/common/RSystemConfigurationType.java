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
import com.evolveum.midpoint.repo.sql.DtoTranslationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountSynchronizationSettingsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ModelHooksType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemConfigurationType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Type;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author lazyman
 */
@Entity
@Table(name = "system_configuration")
public class RSystemConfigurationType extends RExtensibleObjectType {

    private static final Trace LOGGER = TraceManager.getTrace(RSystemConfigurationType.class);
    private String globalAccountSynchronizationSettings;
    private String modelHooks;
    private String logging;
    private RObjectReferenceType defaultUserTemplateRef;
    private String connectorFramework;

    @Type(type = "org.hibernate.type.TextType")
    public String getConnectorFramework() {
        return connectorFramework;
    }

    @ManyToOne
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public RObjectReferenceType getDefaultUserTemplateRef() {
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

    public void setDefaultUserTemplateRef(RObjectReferenceType defaultUserTemplateRef) {
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

    public static void copyToJAXB(RSystemConfigurationType repo, SystemConfigurationType jaxb,
            PrismContext prismContext) throws DtoTranslationException {
        RExtensibleObjectType.copyToJAXB(repo, jaxb, prismContext);

        if (repo.getDefaultUserTemplateRef() != null) {
            jaxb.setDefaultUserTemplateRef(repo.getDefaultUserTemplateRef().toJAXB(prismContext));
        }

        try {
            jaxb.setConnectorFramework(RUtil.toJAXB(repo.getConnectorFramework(),
                    SystemConfigurationType.ConnectorFramework.class, prismContext));
            jaxb.setGlobalAccountSynchronizationSettings(RUtil.toJAXB(repo.getGlobalAccountSynchronizationSettings(),
                    AccountSynchronizationSettingsType.class, prismContext));
            jaxb.setLogging(RUtil.toJAXB(repo.getLogging(), LoggingConfigurationType.class, prismContext));
            jaxb.setModelHooks(RUtil.toJAXB(repo.getModelHooks(), ModelHooksType.class, prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static void copyFromJAXB(SystemConfigurationType jaxb, RSystemConfigurationType repo,
            PrismContext prismContext) throws DtoTranslationException {
        RExtensibleObjectType.copyFromJAXB(jaxb, repo, prismContext);

        if (jaxb.getDefaultUserTemplate() != null) {
            LOGGER.warn("Default user template from system configuration type won't be saved. It should be " +
                    "translated to user template reference.");
        }

        repo.setDefaultUserTemplateRef(RUtil.jaxbRefToRepo(jaxb.getDefaultUserTemplateRef(), jaxb, prismContext));

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
        RSystemConfigurationType.copyToJAXB(this, object, prismContext);
        return object;
    }
}
