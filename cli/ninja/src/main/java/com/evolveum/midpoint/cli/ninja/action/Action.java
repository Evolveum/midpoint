/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.cli.ninja.action;

import com.beust.jcommander.JCommander;
import com.evolveum.midpoint.cli.common.ToolsUtils;
import com.evolveum.midpoint.cli.ninja.command.Command;
import com.evolveum.midpoint.cli.ninja.util.ClientPasswordHandler;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelService;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import org.apache.commons.lang.Validate;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import java.util.Map;

/**
 * @author lazyman
 */
public abstract class Action<T extends Command> {

    protected Logger STD_OUT = LoggerFactory.getLogger(ToolsUtils.LOGGER_SYS_OUT);
    protected Logger STD_ERR = LoggerFactory.getLogger(ToolsUtils.LOGGER_SYS_ERR);

    private JCommander commander;
    private T params;

    public Action(T params, JCommander commander) {
        Validate.notNull(params, "Action parameters must not be null.");
        Validate.notNull(commander, "Commander must not be null.");

        this.params = params;
        this.commander = commander;
    }

    public T getParams() {
        return params;
    }

    public void execute() throws Exception {
        if (params.isHelp()) {
            StringBuilder sb = new StringBuilder();
            commander.usage(commander.getParsedCommand(), sb);

            STD_OUT.info(sb.toString());

            return;
        }

        executeAction();
    }

    protected abstract void executeAction() throws Exception;

    protected ModelPortType createModelPort() {
        ModelService modelService = new ModelService();
        ModelPortType port = modelService.getModelPort();
        BindingProvider bp = (BindingProvider) port;

        Client client = ClientProxy.getClient(port);
        Endpoint endpoint = client.getEndpoint();
        HTTPConduit http = (HTTPConduit) client.getConduit();
        HTTPClientPolicy httpClientPolicy = http.getClient();
        if (httpClientPolicy == null) {
            httpClientPolicy = new HTTPClientPolicy();
            http.setClient(httpClientPolicy);
        }

        httpClientPolicy.setConnectionTimeout(10000);

        Map<String, Object> requestContext = bp.getRequestContext();
        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, params.getUrl().toString());

        requestContext.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
        requestContext.put(WSHandlerConstants.USER, params.getUsername());
        requestContext.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_DIGEST);

        ClientPasswordHandler handler = new ClientPasswordHandler();
        handler.setPassword(params.getInsertedPassword());
        requestContext.put(WSHandlerConstants.PW_CALLBACK_REF, handler);

        WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor(requestContext);
        endpoint.getOutInterceptors().add(wssOut);
        if (params.isVerbose()) {
            endpoint.getOutInterceptors().add(new LoggingOutInterceptor());
            endpoint.getInInterceptors().add(new LoggingInInterceptor());
        }

        return port;
    }

    protected ObjectDeltaType createDeleteDelta(String oid, QName type) {
        ObjectDeltaType delta = new ObjectDeltaType();
        delta.setOid(oid);
        delta.setChangeType(ChangeTypeType.DELETE);
        if (type == null) {
            type = com.evolveum.midpoint.cli.ninja.util.ObjectType.OBJECT.getType();
        }
        delta.setObjectType(type);

        return delta;
    }

    protected ObjectDeltaType createAddDelta(ObjectType object) {
        ObjectDeltaType delta = new ObjectDeltaType();
        delta.setChangeType(ChangeTypeType.ADD);
        delta.setObjectToAdd(object);

        return delta;
    }

    protected ObjectDeltaListType createDeltaList(ObjectDeltaType... deltas) {
        ObjectDeltaListType list = new ObjectDeltaListType();
        for (ObjectDeltaType delta : deltas) {
            list.getDelta().add(delta);
        }
        return list;
    }

    protected void handleError(String message, Exception ex) throws Exception {
        STD_ERR.info(message);
        STD_ERR.info("Error occurred: {}", ex.getMessage());

        if (ex instanceof FaultMessage) {
            FaultMessage faultMessage = (FaultMessage) ex;
            FaultType fault = faultMessage.getFaultInfo();
            if (fault != null && fault.getOperationResult() != null) {
                OperationResultType result = fault.getOperationResult();
                STD_ERR.info("Operation result: {}", result.getMessage());

                if (getParams().isVerbose()) {
                    try {
                        STD_ERR.debug(ToolsUtils.serializeObject(result));
                    } catch (JAXBException e) {
                        STD_ERR.debug("Couldn't serialize operation result, reason: {}", e.getMessage());
                    }
                }
            }
        }

        if (getParams().isVerbose()) {
            STD_ERR.debug("Error details", ex);
        }

        throw ex;
    }
}
