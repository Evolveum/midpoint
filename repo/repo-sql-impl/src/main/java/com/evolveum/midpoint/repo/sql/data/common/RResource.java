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
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
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
@Table(name = "m_resource")
@ForeignKey(name = "fk_resource")
public class RResource extends RObject {

    private static final Trace LOGGER = TraceManager.getTrace(RResource.class);
    private RObjectReference connectorRef;
    private String namespace;
    private String configuration;
    private String xmlSchema;
    private String schemaHandling;
    private String nativeCapabilities;
    private String capabilities;
    private String scripts;
    private String synchronization;

    @Type(type = "org.hibernate.type.TextType")
    public String getScripts() {
        return scripts;
    }

    @Type(type = "org.hibernate.type.TextType")
    public String getCapabilities() {
        return capabilities;
    }

    @Type(type = "org.hibernate.type.TextType")
    public String getNativeCapabilities() {
        return nativeCapabilities;
    }

    @Type(type = "org.hibernate.type.TextType")
    public String getConfiguration() {
        return configuration;
    }

    @OneToOne(optional = true, mappedBy = "owner", orphanRemoval = true)
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public RObjectReference getConnectorRef() {
        return connectorRef;
    }

    @Type(type = "org.hibernate.type.TextType")
    public String getXmlSchema() {
        return xmlSchema;
    }

    @Type(type = "org.hibernate.type.TextType")
    public String getSchemaHandling() {
        return schemaHandling;
    }

    @Type(type = "org.hibernate.type.TextType")
    public String getSynchronization() {
        return synchronization;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setXmlSchema(String xmlSchema) {
        this.xmlSchema = xmlSchema;
    }

    public void setConnectorRef(RObjectReference connectorRef) {
        this.connectorRef = connectorRef;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public void setSchemaHandling(String schemaHandling) {
        this.schemaHandling = schemaHandling;
    }

    public void setSynchronization(String synchronization) {
        this.synchronization = synchronization;
    }

    public void setCapabilities(String capabilities) {
        this.capabilities = capabilities;
    }

    public void setNativeCapabilities(String nativeCapabilities) {
        this.nativeCapabilities = nativeCapabilities;
    }

    public void setScripts(String scripts) {
        this.scripts = scripts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RResource rResource = (RResource) o;

        if (capabilities != null ? !capabilities.equals(rResource.capabilities) : rResource.capabilities != null)
            return false;
        if (configuration != null ? !configuration.equals(rResource.configuration) : rResource.configuration != null)
            return false;
        if (connectorRef != null ? !connectorRef.equals(rResource.connectorRef) : rResource.connectorRef != null)
            return false;
        if (namespace != null ? !namespace.equals(rResource.namespace) : rResource.namespace != null) return false;
        if (nativeCapabilities != null ? !nativeCapabilities.equals(rResource.nativeCapabilities) : rResource.nativeCapabilities != null)
            return false;
        if (schemaHandling != null ? !schemaHandling.equals(rResource.schemaHandling) : rResource.schemaHandling != null)
            return false;
        if (scripts != null ? !scripts.equals(rResource.scripts) : rResource.scripts != null) return false;
        if (synchronization != null ? !synchronization.equals(rResource.synchronization) : rResource.synchronization != null)
            return false;
        if (xmlSchema != null ? !xmlSchema.equals(rResource.xmlSchema) : rResource.xmlSchema != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (namespace != null ? namespace.hashCode() : 0);
        result = 31 * result + (configuration != null ? configuration.hashCode() : 0);
        result = 31 * result + (xmlSchema != null ? xmlSchema.hashCode() : 0);
        result = 31 * result + (schemaHandling != null ? schemaHandling.hashCode() : 0);
        result = 31 * result + (nativeCapabilities != null ? nativeCapabilities.hashCode() : 0);
        result = 31 * result + (capabilities != null ? capabilities.hashCode() : 0);
        result = 31 * result + (scripts != null ? scripts.hashCode() : 0);
        result = 31 * result + (synchronization != null ? synchronization.hashCode() : 0);
        return result;
    }

    public static void copyToJAXB(RResource repo, ResourceType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        RObject.copyToJAXB(repo, jaxb, prismContext);

        jaxb.setNamespace(repo.getNamespace());

        if (repo.getConnectorRef() != null) {
            jaxb.setConnectorRef(repo.getConnectorRef().toJAXB(prismContext));
        }

        try {
            jaxb.setConfiguration(RUtil.toJAXB(ResourceType.class, new PropertyPath(ResourceType.F_CONFIGURATION), repo.getConfiguration(),
                    ResourceConfigurationType.class, prismContext));
            jaxb.setSchema(RUtil.toJAXB(ResourceType.class, new PropertyPath(ResourceType.F_SCHEMA), repo.getXmlSchema(),
                    XmlSchemaType.class, prismContext));
            jaxb.setSchemaHandling(RUtil.toJAXB(ResourceType.class, new PropertyPath(ResourceType.F_SCHEMA_HANDLING), repo.getSchemaHandling(),
                    SchemaHandlingType.class, prismContext));
            jaxb.setSynchronization(RUtil.toJAXB(ResourceType.class, new PropertyPath(ResourceType.F_SYNCHRONIZATION), repo.getSynchronization(),
                    SynchronizationType.class, prismContext));
            jaxb.setCapabilities(RUtil.toJAXB(ResourceType.class, new PropertyPath(ResourceType.F_CAPABILITIES), repo.getCapabilities(),
                    CapabilitiesType.class, prismContext));
            jaxb.setNativeCapabilities(RUtil.toJAXB(ResourceType.class, new PropertyPath(ResourceType.F_NATIVE_CAPABILITIES),
                    repo.getNativeCapabilities(), CachedCapabilitiesType.class, prismContext));
            jaxb.setScripts(RUtil.toJAXB(ResourceType.class, new PropertyPath(ResourceType.F_SCRIPTS), repo.getScripts(),
                    ScriptsType.class, prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static void copyFromJAXB(ResourceType jaxb, RResource repo, PrismContext prismContext) throws
            DtoTranslationException {
        RObject.copyFromJAXB(jaxb, repo, prismContext);

        repo.setNamespace(ResourceTypeUtil.getResourceNamespace(jaxb));
        repo.setConnectorRef(RUtil.jaxbRefToRepo(jaxb.getConnectorRef(), repo, prismContext));

        if (jaxb.getConnector() != null) {
            LOGGER.warn("Connector from resource type won't be saved. It should be translated to connector reference.");
        }

        try {
            repo.setConfiguration(RUtil.toRepo(jaxb.getConfiguration(), prismContext));
            repo.setXmlSchema(RUtil.toRepo(jaxb.getSchema(), prismContext));
            repo.setSchemaHandling(RUtil.toRepo(jaxb.getSchemaHandling(), prismContext));
            repo.setSynchronization(RUtil.toRepo(jaxb.getSynchronization(), prismContext));
            repo.setCapabilities(RUtil.toRepo(jaxb.getCapabilities(), prismContext));
            repo.setNativeCapabilities(RUtil.toRepo(jaxb.getNativeCapabilities(), prismContext));
            repo.setScripts(RUtil.toRepo(jaxb.getScripts(), prismContext));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    @Override
    public ResourceType toJAXB(PrismContext prismContext) throws DtoTranslationException {
        ResourceType object = new ResourceType();
        RUtil.revive(object, prismContext);
        RResource.copyToJAXB(this, object, prismContext);

        return object;
    }
}
