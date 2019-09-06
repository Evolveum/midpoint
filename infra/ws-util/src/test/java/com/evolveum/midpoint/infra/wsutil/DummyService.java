/**
 * Copyright (c) 2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.infra.wsutil;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

/**
 * @author semancik
 *
 */
@WebServiceClient(name = "dumyService",
				  targetNamespace = "http://midpoint.evolveum.com/xml/ns/test/dummy-1")
public class DummyService extends Service {

    public final static QName SERVICE = new QName("http://midpoint.evolveum.com/xml/ns/test/dummy-1", "dumyService");
    public final static QName DummyPort = new QName("http://midpoint.evolveum.com/xml/ns/test/dummy-1", "dummyPort");

    public DummyService(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public DummyService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public DummyService() {
        super(null, SERVICE);
    }

    //This constructor requires JAX-WS API 2.2. You will need to endorse the 2.2
    //API jar or re-run wsdl2java with "-frontend jaxws21" to generate JAX-WS 2.1
    //compliant code instead.
    public DummyService(WebServiceFeature ... features) {
        super(null, SERVICE, features);
    }

    //This constructor requires JAX-WS API 2.2. You will need to endorse the 2.2
    //API jar or re-run wsdl2java with "-frontend jaxws21" to generate JAX-WS 2.1
    //compliant code instead.
    public DummyService(URL wsdlLocation, WebServiceFeature ... features) {
        super(wsdlLocation, SERVICE, features);
    }

    //This constructor requires JAX-WS API 2.2. You will need to endorse the 2.2
    //API jar or re-run wsdl2java with "-frontend jaxws21" to generate JAX-WS 2.1
    //compliant code instead.
    public DummyService(URL wsdlLocation, QName serviceName, WebServiceFeature ... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     *
     * @return
     *     returns ModelPortType
     */
    @WebEndpoint(name = "dummyPort")
    public DummyPort getModelPort() {
        return super.getPort(DummyPort, DummyPort.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns ModelPortType
     */
    @WebEndpoint(name = "dummyPort")
    public DummyPort getModelPort(WebServiceFeature... features) {
        return super.getPort(DummyPort, DummyPort.class, features);
    }
}
