/**
 * Copyright (c) 2013 Evolveum
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
 * Portions Copyrighted 2013 [name of copyright owner]
 */
package com.evolveum.midpoint.testing.model.client.sample;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.Holder;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.frontend.ClientProxy;

import com.evolveum.midpoint.xml.ns._public.common.api_types_2.OperationOptionsType;
//import com.evolveum.midpoint.util.JAXBUtil;
//import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1_wsdl.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1_wsdl.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.model.model_1_wsdl.ModelService;

import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.handler.WSHandlerConstants;

/**
 * @author semancik
 *
 */
public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			
			String endpointUrl = "http://localhost:8080/midpoint/model/model";
			String username = "administrator";
			
			if (args.length > 0) {
				endpointUrl = args[0];
			}

			System.out.println("Endpoint URL: "+endpointUrl);
			
			ModelService modelService = new ModelService();
			ModelPortType modelPort = modelService.getModelPort();
			BindingProvider bp = (BindingProvider)modelPort;
			Map<String, Object> requestContext = bp.getRequestContext();
			requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);
			
			org.apache.cxf.endpoint.Client client = ClientProxy.getClient(modelPort);
			org.apache.cxf.endpoint.Endpoint cxfEndpoint = client.getEndpoint();
			
			Map<String,Object> outProps = new HashMap<String,Object>();
			
			outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
			outProps.put(WSHandlerConstants.USER, username);
			outProps.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_DIGEST);
			outProps.put(WSHandlerConstants.PW_CALLBACK_CLASS, ClientPasswordHandler.class.getName());
			
			WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor(outProps);
			cxfEndpoint.getOutInterceptors().add(wssOut);

//			QName typeQName = JAXBUtil.getTypeQName(SystemConfigurationType.class);
//			String typeUri = QNameUtil.qNameToUri(typeQName);
			String typeUri = "http://midpoint.evolveum.com/xml/ns/public/common/common-2a#SystemConfigurationType";

			Holder<ObjectType> objectHolder = new Holder<ObjectType>();
			Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
			OperationOptionsType options = new OperationOptionsType();
			
			modelPort.getObject(typeUri, SystemObjectsType.SYSTEM_CONFIGURATION.value(), options, 
					objectHolder, resultHolder);
			
			System.out.println("Got system configuration");
			System.out.println(objectHolder.value);
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

}
