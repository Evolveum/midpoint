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

package com.evolveum.midpoint.ws;

import com.evolveum.midpoint.logging.TraceManager;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import javax.jws.WebService;
import javax.xml.ws.BindingProvider;


import java.util.List;
import java.util.Map;

import java.util.Properties;
import javax.xml.ws.handler.Handler;
import org.slf4j.Logger;

/**
 * Web Service helper class.
 * 
 * 
 * @author elek
 */
public class WSClientTool {

    Logger logger = TraceManager.getTrace(WSClientTool.class);

    private static WSClientTool instance = new WSClientTool();

    private File configFile;

    protected WSClientTool() {
        String userHome = System.getProperty("user.home");
        configFile = new File(userHome, ".openidm.dev");
    }

    /**
     * Singleton getter.
     *
     * @return
     */
    public static WSClientTool getInstance() {
        return instance;
    }

    /**
     * Setter for test purposes.
     *
     * @param instance
     */
    public static void setInstance(WSClientTool instance) {
        WSClientTool.instance = instance;
    }

    /**
     * Add a SOAPHandler to a WS port.
     *
     * @param port
     * @param handler
     */
    public void addHandler(BindingProvider port, Handler handler) {
        List<Handler> handlerList = port.getBinding().getHandlerChain();
        handlerList.add(handler);
        port.getBinding().setHandlerChain(handlerList);
    }

    /**
     * Set endpoint URL on a outgoing WS request.
     *
     * @param port 
     * @param endpointURL 
     */
    public void setEndpoint(BindingProvider port, String endpointURL) {
        port.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointURL);
    }

    public Properties loadProperties() {
        Properties p = new Properties();
        FileReader reader = null;
        try {
            reader = new FileReader(configFile);
            p.load(reader);
        } catch (IOException ex) {
            logger.error("Error on loading properties file " + configFile.getAbsolutePath(), ex);
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                logger.error("Error on closing developer properties file", ex);
            }
        }
        return p;

    }

    /**
     * Add utility handlers for a ws client port.
     *
     *
     * @param port
     */
    public Object fixWebServicePort(Object port) {
        if (port instanceof BindingProvider) {
            BindingProvider provider = (BindingProvider) port;
            if (configFile.exists()) {

                Properties p = loadProperties();
                //TODO put it to an enum or constant field
                if (p.getProperty("ws.debug") != null) {
                    addHandler(provider, new MessageLoggerHandler());
                }
                if ("standalone".equals(p.getProperty("endpoint.strategy"))) {
                    String endpoint = "http://localhost:8080/" + getPortName(port);
                    logger.info("Setting endpoint to " + endpoint);
                    setEndpoint(provider, endpoint);
                }
            }

        }
        return port;
    }

    /**
     * Getting the name of a client stub.
     *
     * @param port
     * @return
     */
    public String getPortName(Object port) {
        Map<String,String> urlMapping = new HashMap();
        urlMapping.put("ModelPortType", "modelService/ModelService");
        urlMapping.put("ProvisioningPortType", "provisioningService/ProvisioningService");
        urlMapping.put("RepositoryPortType", "repositoryService/RepositoryService");
        for (Class interFace : port.getClass().getInterfaces()) {
            if (interFace.isAnnotationPresent(WebService.class)) {
                logger.info("Finding endpoint for interface "+interFace.getSimpleName());
                return urlMapping.get(interFace.getSimpleName());
            }
        }
        return null;
    }
}
