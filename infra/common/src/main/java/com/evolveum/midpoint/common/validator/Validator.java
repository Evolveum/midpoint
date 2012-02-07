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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;

import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.dom.DOMConverter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.evolveum.midpoint.prism.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;

/**
 * 
 * 
 * @author Radovan Semancik
 * 
 */
public class Validator {

	private static final Trace LOGGER = TraceManager.getTrace(Validator.class);
	private static final String INPUT_STREAM_CHARSET = "utf-8";
	private static final String OPERATION_PREFIX = Validator.class.getName() + ".";
	private static final String OPERATION_RESOURCE_NAMESPACE_CHECK = OPERATION_PREFIX
			+ "resourceNamespaceCheck";
	private static final String OPERATION_RESOURCE_BASICS_CHECK = OPERATION_PREFIX + "objectBasicsCheck";
	private static final String START_LINE_NUMBER = "startLineNumber";
	private static final String END_LINE_NUMBER = "endLineNumber";
	private boolean verbose = false;
	private boolean validateSchemas = true;
	private boolean allowAnyType = false;
	private EventHandler handler;
	private DOMConverter domConverter = new DOMConverter();
	private Unmarshaller unmarshaller = null;
	private SchemaRegistry schemaRegistry;
	private Schema midPointXsdSchema;
	private javax.xml.validation.Validator xsdValidator;
	long progress = 0;
	long errors = 0;
	long stopAfterErrors = 0;

	public Validator() {
		handler = null;
		initialize();
	}

	public Validator(EventHandler handler) {
		this.handler = handler;
		initialize();
	}

	private void initialize() {
		schemaRegistry = new SchemaRegistry();
		try {
			schemaRegistry.initialize();
		} catch (SAXException e) {
			throw new IllegalStateException("Error in system schemas: " + e.getMessage(), e);
		} catch (IOException e) {
			throw new IllegalStateException("Error reading schemas: " + e.getMessage(), e);
		} catch (SchemaException e) {
			throw new IllegalStateException("Error processing schemas: " + e.getMessage(), e);
		}
		midPointXsdSchema = schemaRegistry.getJavaxSchema();
		xsdValidator = midPointXsdSchema.newValidator();
		xsdValidator.setResourceResolver(schemaRegistry);
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

	public void setValidateSchema(boolean validateSchemas) {
		this.validateSchemas = validateSchemas;
	}

	public boolean getValidateSchema() {
		return validateSchemas;
	}

	public void setAllowAnyType(boolean allowAnyType) {
		this.allowAnyType = allowAnyType;
	}

	public boolean getAllowAnyType() {
		return allowAnyType;
	}

	public long getStopAfterErrors() {
		return stopAfterErrors;
	}

	public void setStopAfterErrors(long stopAfterErrors) {
		this.stopAfterErrors = stopAfterErrors;
	}

	public long getProgress() {
		return progress;
	}

	public long getErrors() {
		return errors;
	}

	private Unmarshaller createUnmarshaller(OperationResult validatorResult) {
		if (unmarshaller != null) {
			return unmarshaller;
		}
		try {
			JAXBContext jc = JAXBUtil.getContext();
			unmarshaller = jc.createUnmarshaller();
		} catch (JAXBException ex) {
			validatorResult.recordFatalError("Error initializing JAXB: " + ex.getMessage(), ex);
			if (handler != null) {
				handler.handleGlobalError(validatorResult);
			}
			// This is a severe error.
			throw new SystemException("Error initializing JAXB: " + ex.getMessage(), ex);
		}
		return unmarshaller;
	}

	public void validate(InputStream inputStream, OperationResult validatorResult,
			String objectResultOperationName) {

		XMLStreamReader stream = null;
		try {

			Map<String, String> rootNamespaceDeclarations = new HashMap<String, String>();

			XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
			stream = xmlInputFactory.createXMLStreamReader(inputStream);
			int eventType = stream.nextTag();
			if (eventType == XMLStreamConstants.START_ELEMENT) {
				if (!stream.getName().equals(SchemaConstants.C_OBJECTS)) {
					// This has to be an import file with a single objects. Try
					// to process it.
					OperationResult objectResult = validatorResult.createSubresult(objectResultOperationName);
					progress++;
					objectResult.addContext(OperationResult.CONTEXT_PROGRESS, progress);
					
					EventResult cont = readFromStreamAndValidate(stream, objectResult,
							rootNamespaceDeclarations, validatorResult);
					
					if (!cont.isCont()) {
						if (cont.getReason() != null) {
							validatorResult.recordFatalError(cont.getReason());
						} else {
							validatorResult.recordFatalError("Object validation failed");
						}
						return;
					}
					// return to avoid processing objects in loop
					validatorResult.computeStatus("Validation failed", "Validation warnings");
					return;
				}
				// Extract root namespace declarations
				for (int i = 0; i < stream.getNamespaceCount(); i++) {
					rootNamespaceDeclarations.put(stream.getNamespacePrefix(i), stream.getNamespaceURI(i));
				}
			} else {
				throw new SystemException("StAX Malfunction?");
			}
			
			while (stream.hasNext()) {
				eventType = stream.next();
				if (eventType == XMLStreamConstants.START_ELEMENT) {
					
					OperationResult objectResult = validatorResult.createSubresult(objectResultOperationName);
					progress++;
					objectResult.addContext(OperationResult.CONTEXT_PROGRESS, progress);
					
					// Read and validate individual object from the stream
					EventResult cont = readFromStreamAndValidate(stream, objectResult,
							rootNamespaceDeclarations, validatorResult);
					
					if (objectResult.isError()) {
						errors++;
					}
					
					if (cont.isStop()) {
						if (cont.getReason() != null) {
							validatorResult.recordFatalError("Processing has been stopped: "+cont.getReason());
						} else {
							validatorResult.recordFatalError("Processing has been stopped");
						}
						// This means total stop, no other objects will be
						// processed
						return;
					}
					if (!cont.isCont()) {
						if (stopAfterErrors > 0 && errors >= stopAfterErrors) {
							validatorResult.recordFatalError("Too many errors (" + errors + ")");
							return;
						}
					}
				}
			}

		} catch (XMLStreamException ex) {
			// validatorResult.recordFatalError("XML parsing error: " +
			// ex.getMessage()+" on line "+stream.getLocation().getLineNumber(),ex);
			validatorResult.recordFatalError("XML parsing error: " + ex.getMessage(), ex);
			if (handler != null) {
				handler.handleGlobalError(validatorResult);
			}
			return;
		}
		
		// Error count is sufficient. Detailed messages are in subresults
		validatorResult.computeStatus(errors + " errors, " + (progress - errors) +" passed");

	}

	private EventResult readFromStreamAndValidate(XMLStreamReader stream, OperationResult objectResult,
			Map<String, String> rootNamespaceDeclarations, OperationResult validatorResult) {

		try {
			objectResult.addContext(START_LINE_NUMBER, stream.getLocation().getLineNumber());

			// Parse the object from stream to DOM
			Document objectDoc = domConverter.buildDocument(stream);

			objectResult.addContext(END_LINE_NUMBER, stream.getLocation().getLineNumber());

			// This element may not have complete namespace definitions for a
			// stand-alone
			// processing, therefore copy namespace definitions from the root
			// element
			Element objectElement = DOMUtil.getFirstChildElement(objectDoc);
			DOMUtil.setNamespaceDeclarations(objectElement, rootNamespaceDeclarations);

			Node postValidationTree = null;

			if (validateSchemas) {
				postValidationTree = validateSchema(objectDoc, objectResult);
			}

			if (handler != null) {
				EventResult cont = handler.preMarshall(objectElement, postValidationTree, objectResult);
				if (!cont.isCont()) {
					return cont;
				}
			}

			if (!objectResult.isAcceptable()) {
				// Schema validation or preMarshall has failed. No point to
				// continue with this object.
				return EventResult.skipObject();
			}

			JAXBElement<?> jaxbElement = (JAXBElement<?>) createUnmarshaller(validatorResult).unmarshal(objectDoc);
			Object jaxbValue = jaxbElement.getValue();
			ObjectType object = null;

			if (jaxbValue instanceof ObjectType) {
				object = (ObjectType) jaxbElement.getValue();

				if (verbose) {
					LOGGER.debug("Processing OID " + object.getOid());
				}

				objectResult.addContext(OperationResult.CONTEXT_OBJECT, object);

				validateObject(object, objectResult);

				if (handler != null) {
					EventResult cont = handler.postMarshall(object, objectElement, objectResult);
					if (!cont.isCont()) {
						return cont;
					}
				}

				objectResult.recomputeStatus();

			} else {
				if (!allowAnyType) {
					objectResult.recordFatalError("Found valid type that is not ObjectType: " + jaxbValue);
				} else {
					objectResult.recordSuccess();
				}
			}

			return EventResult.cont();

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
				validatorResult.recordFatalError("Unmarshalling error: " + ex.getMessage(), ex);
			} else {
				validatorResult.recordFatalError("Unmarshalling error: "
						+ (linkedException != null ? linkedException.getMessage() : "unknown: " + ex), ex);
			}
			if (handler != null) {
				handler.handleGlobalError(validatorResult);
			}
			return EventResult.skipObject();

		} catch (XMLStreamException ex) {
			validatorResult.recordFatalError("XML parsing error: " + ex.getMessage(), ex);
			if (handler != null) {
				handler.handleGlobalError(validatorResult);
			}
			return EventResult.skipObject();
		}
	}

	private Node validateSchema(Document objectDoc, OperationResult objectResult) {
		OperationResult result = objectResult.createSubresult(Validator.class.getName() + ".validateSchema");
		DOMResult validationResult = new DOMResult();
		try {
			xsdValidator.validate(new DOMSource(objectDoc), validationResult);
		} catch (SAXException e) {
			result.recordFatalError("Validation error: " + e.getMessage(), e);
			objectResult.computeStatus("Validation error: " + e.getMessage());
			return null;
		} catch (IOException e) {
			result.recordFatalError("OI error during validation: " + e.getMessage(), e);
			objectResult.computeStatus("OI error during validation: " + e.getMessage());
			return null;
		}
		result.recordSuccess();
		return validationResult.getNode();
	}

	public void validateObject(ObjectType object, OperationResult objectResult) {
		// Check generic object properties

		checkBasics(object, objectResult);

		// Type-specific checks

		if (object instanceof ResourceType) {
			ResourceType resource = (ResourceType) object;
			checkResource(resource, objectResult);
		}

		// TODO: more checks

		objectResult.recomputeStatus("Object validation has failed", "Validation warning");
		objectResult.recordSuccessIfUnknown();

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
