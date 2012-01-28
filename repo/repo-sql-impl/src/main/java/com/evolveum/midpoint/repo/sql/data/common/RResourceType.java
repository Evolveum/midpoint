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
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author lazyman
 */
@Entity
@Table(name = "resource")
public class RResourceType extends RExtensibleObjectType {

    private RConnectorType connector;
    //    private ObjectReferenceType connectorRef;     //todo mapping
    private String namespace;
//    private ResourceConfigurationType configuration;  // todo mapping
//    private XmlSchemaType schema;
//    private SchemaHandlingType schemaHandling;
//    private CapabilitiesType nativeCapabilities;
//    private CapabilitiesType capabilities;
//    private ScriptsType scripts;
//    private SynchronizationType synchronization;

    //todo: implement


    @ManyToOne
    @Cascade({CascadeType.SAVE_UPDATE}) //todo cascade check
    public RConnectorType getConnector() {
        return connector;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setConnector(RConnectorType connector) {
        this.connector = connector;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public static void copyToJAXB(RConnectorType repo, ConnectorType jaxb) throws DtoTranslationException {
        RExtensibleObjectType.copyToJAXB(repo, jaxb);

        //todo implement
    }

    public static void copyFromJAXB(ConnectorType jaxb, RConnectorType repo) throws DtoTranslationException {
        RExtensibleObjectType.copyFromJAXB(jaxb, repo);

        //todo implement
    }

    @Override
    public ResourceType toJAXB() throws DtoTranslationException {
        ResourceType object = new ResourceType();
        RResourceType.copyToJAXB(this, object);
        return object;
    }
}
