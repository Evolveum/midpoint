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

package com.evolveum.midpoint.common.validator;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXParseException;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.Objects;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;

/**
 * 
 * 
 * @author Radovan Semancik
 * 
 */
public class Validator {

	private static final Trace LOGGER = TraceManager.getTrace(Validator.class);
	private static final String INPUT_STEAM_CHARSET = "utf-8";
	private static final String OPERATION_PREFIX = Validator.class.getName() + ".";
	private static final String OPERATION_RESOURCE_NAMESPACE_CHECK = OPERATION_PREFIX
			+ "resourceNamespaceCheck";
	private static final String OPERATION_RESOURCE_BASICS_CHECK = OPERATION_PREFIX + "objectBasicsCheck";;
	private boolean verbose = false;
	EventHandler handler;

	public Validator() {
		handler = null;
	}

	public Validator(EventHandler handler) {
		this.handler = handler;
	}

	public EventHandler getHandler() {
		return handler;
	}

	public void setHandler(EventHandler handler) {
		this.handler = handler;
	}

	public boolean getVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public void validate(InputStream inputStream, OperationResult validatorResult,
			String objectResultOperationName) {

		// TODO: this needs to be switched to stream parsing

		// The result should already be initialized here.
		Unmarshaller u = null;

		try {
			JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
			u = jc.createUnmarshaller();
		} catch (JAXBException ex) {
			validatorResult.recordFatalError("Error initializing JAXB: " + ex.getMessage(), ex);
			if (handler != null) {
				handler.handleGlobalError(validatorResult);
			}
			// This is a severe error.
			throw new SystemException("Error initializing JAXB: " + ex.getMessage(), ex);
		}

		Objects objects = null;

		try {
			Object object = u.unmarshal(new InputStreamReader(inputStream, INPUT_STEAM_CHARSET));
			if (object instanceof Objects) {
				objects = (Objects) object;
			} else {
				objects = new Objects();
				objects.getObject().add((JAXBElement<? extends ObjectType>) object);
			}
		} catch (UnsupportedEncodingException ex) {
			validatorResult.recordFatalError(
					"Unsupported encoding " + INPUT_STEAM_CHARSET + ": " + ex.getMessage(), ex);
			if (handler != null) {
				handler.handleGlobalError(validatorResult);
			}
			// This is a severe error.
			throw new SystemException("Unsupported encoding " + INPUT_STEAM_CHARSET + ": " + ex.getMessage(),
					ex);
		} catch (JAXBException ex) {
			if (verbose) {
				ex.printStackTrace();
			}
			Throwable linkedException = ex.getLinkedException();
			if (linkedException instanceof SAXParseException) {
				SAXParseException saxex = (SAXParseException) linkedException;

				validatorResult.recordFatalError(
						"XML Parse error: " + saxex.getMessage() + " (line " + saxex.getLineNumber()
								+ " col " + saxex.getColumnNumber() + ")", ex);

			} else if (ex instanceof UnmarshalException) {
				validatorResult.recordFatalError("Unmarshalling error: " + ex.getMessage());
			} else {
				validatorResult.recordFatalError("Unmarshalling error: "
						+ (linkedException != null ? linkedException.getMessage() : "unknown: " + ex), ex);
			}
			if (handler != null) {
				handler.handleGlobalError(validatorResult);
			}
			return;
		}

		for (JAXBElement<? extends ObjectType> jaxbObject : objects.getObject()) {

			ObjectType object = jaxbObject.getValue();

			if (verbose) {
				LOGGER.debug("Processing OID " + object.getOid());
			}

			OperationResult objectResult = validatorResult.createSubresult(objectResultOperationName);
			objectResult.addContext(OperationResult.CONTEXT_OBJECT, object);

			validate(object, objectResult);

		}

		validatorResult.computeStatus("Validation failed");

	}

	public void validate(ObjectType object, OperationResult objectResult) {
		// Check generic object properties

		checkBasics(object, objectResult);

		// Type-specific checks

		if (object instanceof ResourceType) {
			ResourceType resource = (ResourceType) object;
			checkResource(resource, objectResult);
		}

		objectResult.recomputeStatus("Object validation has failed", "Validation warning");
		objectResult.recordSuccessIfUnknown();

		// TODO
		if (handler != null) {
			handler.handleObject(object, objectResult);
		}

		objectResult.recomputeStatus("Object processing has failed", "Validation warning");
	}

	// BIG checks - checks that create subresults

	void checkBasics(ObjectType object, OperationResult objectResult) {
		OperationResult subresult = objectResult.createSubresult(OPERATION_RESOURCE_BASICS_CHECK);
		checkName(object, object.getName(), "name", subresult);
		subresult.recordSuccessIfUnknown();
	}

	void checkResource(ResourceType resource, OperationResult objectResult) {
		OperationResult subresult = objectResult.createSubresult(OPERATION_RESOURCE_NAMESPACE_CHECK);
		checkUri(resource, resource.getNamespace(), "namespace", subresult);
		subresult.recordSuccessIfUnknown();
	}

	// Small checks - checks that don't create subresults

	void checkName(ObjectType object, String value, String propertyName, OperationResult subResult) {
		// TODO: check for all whitespaces
		// TODO: check for bad characters
		if (value == null || value.isEmpty()) {
			error("Empty property", object, propertyName, subResult);
		}
	}

	void checkUri(ObjectType object, String value, String propertyName, OperationResult subResult) {
		// TODO: check for all whitespaces
		// TODO: check for bad characters
		if (StringUtils.isEmpty(value)) {
			error("Empty property", object, propertyName, subResult);
			return;
		}
		try {
			URI uri = new URI(value);
			if (uri.getScheme() == null) {
				error("URI is supposed to be absolute", object, propertyName, subResult);
			}
		} catch (URISyntaxException ex) {
			error("Wrong URI syntax: " + ex, object, propertyName, subResult);
		}

	}

	void error(String message, ObjectType object, OperationResult subResult) {
		subResult.addContext(OperationResult.CONTEXT_OBJECT, object);
		subResult.recordFatalError(message);
	}

	void error(String message, ObjectType object, String propertyName, OperationResult subResult) {
		subResult.addContext(OperationResult.CONTEXT_OBJECT, object);
		subResult.addContext(OperationResult.CONTEXT_PROPERTY, propertyName);
		subResult.recordFatalError("<" + propertyName + ">: " + message);
	}
}
