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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import javax.persistence.*;
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
    private String globalAccountSynchronizationSettings;
    private REmbeddedReference globalPasswordPolicyRef;
    //    private Set<RObjectReference> orgRootRef;
    private String modelHooks;
    private String logging;
    private REmbeddedReference defaultUserTemplateRef;
    private String connectorFramework;
    private String notificationConfiguration;
    private String cleanupPolicy;
    private String profilingConfiguration;

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getProfilingConfiguration() {
        return profilingConfiguration;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getCleanupPolicy() {
        return cleanupPolicy;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getConnectorFramework() {
        return connectorFramework;
    }

    @Embedded
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public REmbeddedReference getGlobalPasswordPolicyRef() {
        return globalPasswordPolicyRef;
    }

//    @Where(clause = RObjectReference.REFERENCE_TYPE + "=" + ROrgRootRef.DISCRIMINATOR)
//    @OneToMany(mappedBy = "owner", orphanRemoval = true)
//    @ForeignKey(name = "none")
//    @Cascade({org.hibernate.annotations.CascadeType.ALL})
//    public Set<RObjectReference> getOrgRootRef() {
//        if (orgRootRef == null) {
//            orgRootRef = new HashSet<RObjectReference>();
//        }
//        return orgRootRef;
//    }

    @Embedded
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public REmbeddedReference getDefaultUserTemplateRef() {
        return defaultUserTemplateRef;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getGlobalAccountSynchronizationSettings() {
        return globalAccountSynchronizationSettings;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getLogging() {
        return logging;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getModelHooks() {
        return modelHooks;
    }

    @Lob
    @Type(type = RUtil.LOB_STRING_TYPE)
    public String getNotificationConfiguration() {
        return notificationConfiguration;
    }

    @Embedded
    public RPolyString getName() {
        return name;
    }

    public void setName(RPolyString name) {
        this.name = name;
    }

    public void setConnectorFramework(String connectorFramework) {
        this.connectorFramework = connectorFramework;
    }

    public void setDefaultUserTemplateRef(REmbeddedReference defaultUserTemplateRef) {
        this.defaultUserTemplateRef = defaultUserTemplateRef;
    }

    public void setGlobalAccountSynchronizationSettings(String globalAccountSynchronizationSettings) {
        this.globalAccountSynchronizationSettings = globalAccountSynchronizationSettings;
    }

    public void setGlobalPasswordPolicyRef(REmbeddedReference globalPasswordPolicyRef) {
        this.globalPasswordPolicyRef = globalPasswordPolicyRef;
    }

//    public void setOrgRootRef(Set<RObjectReference> orgRootRef) {
//        this.orgRootRef = orgRootRef;
//    }

    public void setLogging(String logging) {
        this.logging = logging;
    }

    public void setModelHooks(String modelHooks) {
        this.modelHooks = modelHooks;
    }

    public void setNotificationConfiguration(String notificationConfiguration) {
        this.notificationConfiguration = notificationConfiguration;
    }

    public void setCleanupPolicy(String cleanupPolicy) {
        this.cleanupPolicy = cleanupPolicy;
    }

    public void setProfilingConfiguration(String profilingConfiguration) {
        this.profilingConfiguration = profilingConfiguration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RSystemConfiguration that = (RSystemConfiguration) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (connectorFramework != null ? !connectorFramework.equals(that.connectorFramework) :
                that.connectorFramework != null)
            return false;
        if (defaultUserTemplateRef != null ? !defaultUserTemplateRef.equals(that.defaultUserTemplateRef) :
                that.defaultUserTemplateRef != null)
            return false;
        if (globalPasswordPolicyRef != null ? !globalPasswordPolicyRef.equals(that.globalPasswordPolicyRef) :
                that.globalPasswordPolicyRef != null)
            return false;
        if (globalAccountSynchronizationSettings != null ?
                !globalAccountSynchronizationSettings.equals(that.globalAccountSynchronizationSettings) :
                that.globalAccountSynchronizationSettings != null)
            return false;
//        if (orgRootRef != null ? !orgRootRef.equals(that.orgRootRef) : that.orgRootRef != null)
//            return false;
        if (logging != null ? !logging.equals(that.logging) : that.logging != null) return false;
        if (modelHooks != null ? !modelHooks.equals(that.modelHooks) : that.modelHooks != null) return false;
        if (notificationConfiguration != null ? !notificationConfiguration.equals(that.notificationConfiguration)
                : that.notificationConfiguration != null)
            return false;
        if (cleanupPolicy != null ? !cleanupPolicy.equals(that.cleanupPolicy) : that.cleanupPolicy != null)
            return false;
        if (profilingConfiguration != null ? !profilingConfiguration.equals(that.profilingConfiguration) :
                that.profilingConfiguration != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (globalAccountSynchronizationSettings != null ?
                globalAccountSynchronizationSettings.hashCode() : 0);
        result = 31 * result + (globalPasswordPolicyRef != null ? globalPasswordPolicyRef.hashCode() : 0);
//        result = 31 * result + (orgRootRef != null ? orgRootRef.hashCode() : 0);
        result = 31 * result + (modelHooks != null ? modelHooks.hashCode() : 0);
        result = 31 * result + (notificationConfiguration != null ? notificationConfiguration.hashCode() : 0);
        result = 31 * result + (logging != null ? logging.hashCode() : 0);
        result = 31 * result + (connectorFramework != null ? connectorFramework.hashCode() : 0);
        result = 31 * result + (cleanupPolicy != null ? cleanupPolicy.hashCode() : 0);
        result = 31 * result + (profilingConfiguration != null ? profilingConfiguration.hashCode() : 0);
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
//
//        List orgRefs = RUtil.safeSetReferencesToList(repo.getOrgRootRef(), prismContext);
//        if (!orgRefs.isEmpty()) {
//            jaxb.getOrgRootRef().addAll(orgRefs);
//        }

        try {
            jaxb.setConnectorFramework(RUtil.toJAXB(SystemConfigurationType.class,
                    new ItemPath(SystemConfigurationType.F_CONNECTOR_FRAMEWORK),
                    repo.getConnectorFramework(),
                    ConnectorFrameworkType.class, prismContext));
            jaxb.setGlobalAccountSynchronizationSettings(RUtil.toJAXB(SystemConfigurationType.class,
                    new ItemPath(SystemConfigurationType.F_GLOBAL_ACCOUNT_SYNCHRONIZATION_SETTINGS),
                    repo.getGlobalAccountSynchronizationSettings(), ProjectionPolicyType.class,
                    prismContext));
            jaxb.setLogging(RUtil.toJAXB(SystemConfigurationType.class,
                    new ItemPath(SystemConfigurationType.F_LOGGING),
                    repo.getLogging(),
                    LoggingConfigurationType.class, prismContext));
            jaxb.setModelHooks(RUtil.toJAXB(SystemConfigurationType.class,
                    new ItemPath(SystemConfigurationType.F_MODEL_HOOKS),
                    repo.getModelHooks(),
                    ModelHooksType.class, prismContext));
            jaxb.setNotificationConfiguration(RUtil.toJAXB(SystemConfigurationType.class,
                    new ItemPath(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION),
                    repo.getNotificationConfiguration(),
                    NotificationConfigurationType.class, prismContext));
            jaxb.setCleanupPolicy(RUtil.toJAXB(SystemConfigurationType.class,
                    new ItemPath(SystemConfigurationType.F_CLEANUP_POLICY),
                    repo.getCleanupPolicy(),
                    CleanupPoliciesType.class, prismContext));
            jaxb.setProfilingConfiguration(RUtil.toJAXB(SystemConfigurationType.class,
                    new ItemPath(SystemConfigurationType.F_PROFILING_CONFIGURATION),
                    repo.getProfilingConfiguration(),
                    ProfilingConfigurationType.class, prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
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

//        if (jaxb.getOrgRoot() != null) {
//            LOGGER.warn("Root organization from system configuration type won't be saved. It should be " +
//                    "translated to root organization reference.");
//        }
//        repo.getOrgRootRef().addAll(RUtil.safeListReferenceToSet(jaxb.getOrgRootRef(), prismContext, repo, RReferenceOwner.SYSTEM_CONFIGURATION_ORG_ROOT));
        try {
            repo.setConnectorFramework(RUtil.toRepo(jaxb.getConnectorFramework(), prismContext));
            repo.setGlobalAccountSynchronizationSettings(RUtil.toRepo(jaxb.getGlobalAccountSynchronizationSettings(), prismContext));
            repo.setLogging(RUtil.toRepo(jaxb.getLogging(), prismContext));
            repo.setModelHooks(RUtil.toRepo(jaxb.getModelHooks(), prismContext));
            repo.setNotificationConfiguration(RUtil.toRepo(jaxb.getNotificationConfiguration(), prismContext));

            repo.setCleanupPolicy(RUtil.toRepo(jaxb.getCleanupPolicy(), prismContext));
            repo.setProfilingConfiguration(RUtil.toRepo(jaxb.getProfilingConfiguration(), prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
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
