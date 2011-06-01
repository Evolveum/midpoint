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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.provisioning.ucf.impl;

import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.CommunicationException;
import com.evolveum.midpoint.provisioning.ucf.api.ConfiguredConnector;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.Token;
import com.evolveum.midpoint.schema.processor.ResourceObject;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttribute;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.xml.ns._public.common.common_1.DiagnosticsMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceTestResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TestResultType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import org.identityconnectors.framework.api.ConnectorFacade;
import java.util.List;
import java.util.Set;
import javax.xml.bind.JAXBElement;

/**
 *
 * @author Radovan Semancik
 */
public class ConfiguredConnectorImpl implements ConfiguredConnector {
	
	ConnectorFacade connector;

	public ConfiguredConnectorImpl(ConnectorFacade connector) {
		this.connector = connector;
	}
	
	@Override
	public Schema fetchResourceSchema() throws CommunicationException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ResourceObject fetchObject(Set<ResourceObjectAttribute> identifiers) throws CommunicationException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Set<ResourceObjectAttribute> addObject(ResourceObject object, Set<Operation> additionalOperations) throws CommunicationException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void modifyObject(Set<ResourceObjectAttribute> identifiers, Set<Operation> changes) throws CommunicationException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void deleteObject(Set<ResourceObjectAttribute> identifiers) throws CommunicationException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Token deserializeToken(String serializedToken) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Token fetchCurrentToken() throws CommunicationException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public List<Change> fetchChanges(Token lastToken) throws CommunicationException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ResourceTestResultType test() {
        ResourceTestResultType result = new ResourceTestResultType();

        TestResultType testResult = new TestResultType();
        result.setConnectorConnection(testResult);
        try {
            connector.test();
            testResult.setSuccess(true);
        } catch (RuntimeException ex) {
            testResult.setSuccess(false);
            List<JAXBElement<DiagnosticsMessageType>> errorOrWarning = testResult.getErrorOrWarning();
            DiagnosticsMessageType message = new DiagnosticsMessageType();
            message.setMessage(ex.getClass().getName()+": "+ex.getMessage());
            // TODO: message.setDetails();
            JAXBElement<DiagnosticsMessageType> element = new JAXBElement<DiagnosticsMessageType>(SchemaConstants.I_DIAGNOSTICS_MESSAGE_ERROR,DiagnosticsMessageType.class,message);
            errorOrWarning.add(element);
        }
        
        return result;
	}
	
}
