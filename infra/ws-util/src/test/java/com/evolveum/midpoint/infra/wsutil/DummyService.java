/**
 * Copyright (c) 2014 Evolveum
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
