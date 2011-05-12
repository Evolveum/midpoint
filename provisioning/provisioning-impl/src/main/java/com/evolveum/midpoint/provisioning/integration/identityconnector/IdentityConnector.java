/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.provisioning.integration.identityconnector;

import com.evolveum.midpoint.provisioning.service.BaseResourceIntegration;
import com.evolveum.midpoint.provisioning.service.ResourceConnector;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.resource.idconnector.configuration_1.ConnectorConfiguration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import org.w3c.dom.Element;

/**
 * Connector configuration implementation.
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class IdentityConnector extends ResourceConnector<ConnectorConfiguration> {

    public static final String code_id = "$Id$";

    public static final String IC_BUNDLE_NAMESPACE_PREFIX = "http://midpoint.evolveum.com/xml/ns/resource/idconnector/bundle";

    private ConnectorConfiguration _connectorConfiguration;

    private IdentityConnectorRAI ic = new IdentityConnectorRAI();

    /**
     * Dummy constructor for test purposes.
     * @param _connectorConfiguration
     */
    public IdentityConnector(ConnectorConfiguration connectorConfiguration) {
        super();
        this._connectorConfiguration = connectorConfiguration;
    }




    public IdentityConnector(ResourceType resourceType) {
        super(resourceType);
        getConfiguration();
    }

    public IdentityConnector(BaseResourceIntegration res) {
        super(res);
        getConfiguration();
    }

    /**
     * Calculate unique namespace to a bundle type.
     * Used in configuration part of Resource object.
     *
     * @todo probably we nee some more prettier mapping.
     */
    public String getBundleNamespace() {
        return IC_BUNDLE_NAMESPACE_PREFIX + "/" + _connectorConfiguration.getConnectorRef().getBundleName() + "/" + _connectorConfiguration.getConnectorRef().getConnectorName() + "/" + _connectorConfiguration.getConnectorRef().getBundleVersion();
    }

    /**
     * Find and unmarshall ConnectorConfiguration from configuration element.
     * @return
     */
    @Override
    public ConnectorConfiguration getConfiguration() {
        if (null != _connectorConfiguration) {
            return _connectorConfiguration;
        }
        for (Object o : _resource.getConfiguration().getAny()) {
            if (o instanceof Element) {
                Element e = (Element) o;
                if ("ConnectorConfiguration".equalsIgnoreCase(e.getLocalName())) {
                    try {
                        //TODO: don't create context here, get it from the pool
                        JAXBContext c = JAXBContext.newInstance("com.evolveum.midpoint.xml.ns._public.resource.idconnector.configuration_1");
                        _connectorConfiguration = (ConnectorConfiguration) c.createUnmarshaller().unmarshal((Element) o);
                    } catch (JAXBException ex) {
                        Logger.getLogger(IdentityConnector.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }
            } else if (o instanceof ConnectorConfiguration) {
                _connectorConfiguration = (ConnectorConfiguration) o;

            } else if (o instanceof JAXBElement) {
                JAXBElement e = (JAXBElement) o;
                if (e.getDeclaredType().equals(ConnectorConfiguration.class) || "ConnectorConfiguration".equals(e.getName().getLocalPart())) {
                    _connectorConfiguration = ((JAXBElement<ConnectorConfiguration>) e).getValue();
                }
            }
            if (null != _connectorConfiguration) {
                break;
            }
        }
        return _connectorConfiguration;
    }
}
