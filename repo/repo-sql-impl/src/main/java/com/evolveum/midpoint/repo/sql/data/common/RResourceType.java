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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Type;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author lazyman
 */
@Entity
@Table(name = "resource")
public class RResourceType extends RExtensibleObjectType {

    private static final Trace LOGGER = TraceManager.getTrace(RResourceType.class);
    private RObjectReferenceType connectorRef;
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

    @ManyToOne
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public RObjectReferenceType getConnectorRef() {
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

    public void setConnectorRef(RObjectReferenceType connectorRef) {
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

    public static void copyToJAXB(RResourceType repo, ResourceType jaxb) throws DtoTranslationException {
        RExtensibleObjectType.copyToJAXB(repo, jaxb);

        jaxb.setNamespace(repo.getNamespace());

        if (repo.getConnectorRef() != null) {
            jaxb.setConnectorRef(repo.getConnectorRef().toJAXB());
        }

        try {
            jaxb.setConfiguration(RUtil.toJAXB(repo.getConfiguration(), ResourceConfigurationType.class));
            jaxb.setSchema(RUtil.toJAXB(repo.getXmlSchema(), XmlSchemaType.class));
            jaxb.setSchemaHandling(RUtil.toJAXB(repo.getSchemaHandling(), SchemaHandlingType.class));
            jaxb.setSynchronization(RUtil.toJAXB(repo.getSynchronization(), SynchronizationType.class));
            jaxb.setCapabilities(RUtil.toJAXB(repo.getCapabilities(), CapabilitiesType.class));
            jaxb.setNativeCapabilities(RUtil.toJAXB(repo.getNativeCapabilities(), CapabilitiesType.class));
            jaxb.setScripts(RUtil.toJAXB(repo.getScripts(), ScriptsType.class));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static void copyFromJAXB(ResourceType jaxb, RResourceType repo) throws DtoTranslationException {
        RExtensibleObjectType.copyFromJAXB(jaxb, repo);

        repo.setNamespace(jaxb.getNamespace());
        repo.setConnectorRef(RUtil.jaxbRefToRepo(jaxb.getConnectorRef(), jaxb));

        if (jaxb.getConnector() != null) {
            LOGGER.warn("Connector from resource type won't be saved. It should be translated to connector reference.");
        }

        try {
            repo.setConfiguration(RUtil.toRepo(jaxb.getConfiguration()));
            repo.setXmlSchema(RUtil.toRepo(jaxb.getSchema()));
            repo.setSchemaHandling(RUtil.toRepo(jaxb.getSchemaHandling()));
            repo.setSynchronization(RUtil.toRepo(jaxb.getSynchronization()));
            repo.setCapabilities(RUtil.toRepo(jaxb.getCapabilities()));
            repo.setNativeCapabilities(RUtil.toRepo(jaxb.getNativeCapabilities()));
            repo.setScripts(RUtil.toRepo(jaxb.getScripts()));
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    @Override
    public ResourceType toJAXB() throws DtoTranslationException {
        ResourceType object = new ResourceType();
        RResourceType.copyToJAXB(this, object);
        return object;
    }
}
