/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common.validator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import com.evolveum.midpoint.schema.constants.MidPointConstants;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.staxmate.dom.DOMConverter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismParserNoIO;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Class to validate (and possible transform) large sets of objects.
 * <p>
 * LEGACY: This is all very old code. And it is XML-only. It will be probably
 * thrown away and re-written in a more reasonable way.
 *
 * @author Radovan Semancik
 */
public class LegacyValidator<T extends Containerable> {

    private static final Trace LOGGER = TraceManager.getTrace(LegacyValidator.class);
    private static final Charset INPUT_STREAM_CHARSET = StandardCharsets.UTF_8;
    private static final String OPERATION_PREFIX = LegacyValidator.class.getName() + ".";
    private static final String OPERATION_RESOURCE_NAMESPACE_CHECK = OPERATION_PREFIX + "resourceNamespaceCheck";
    private static final String OPERATION_RESOURCE_BASICS_CHECK = OPERATION_PREFIX + "objectBasicsCheck";
    private static final String START_LINE_NUMBER = "startLineNumber";
    private static final String END_LINE_NUMBER = "endLineNumber";

    private final PrismContext prismContext;

    private boolean verbose = false;
    private boolean validateSchemas = true;
    private boolean validateName = true;
    private boolean allowAnyType = false;
    private EventHandler<T> handler;
    private javax.xml.validation.Validator xsdValidator;
    private long progress = 0;
    private long errors = 0;
    private long stopAfterErrors = 0;
    private boolean compatMode = false;
    private boolean convertMissingType = false;

    public LegacyValidator(PrismContext prismContext) {
        this.prismContext = prismContext;
        this.handler = null;
        initialize();
    }

    public LegacyValidator(PrismContext prismContext, EventHandler<T> handler) {
        this.prismContext = prismContext;
        this.handler = handler;
        initialize();
    }

    private void initialize() {
        if (prismContext == null) {
            throw new IllegalStateException("No prism context set during validator initialization");
        }
        xsdValidator = prismContext.getSchemaRegistry().getJavaxSchemaValidator();
    }

    public EventHandler<T> getHandler() {
        return handler;
    }

    public void setHandler(EventHandler<T> handler) {
        this.handler = handler;
    }

    public PrismContext getPrismContext() {
        return prismContext;
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

    public boolean isValidateName() {
        return validateName;
    }

    public void setValidateName(boolean validateName) {
        this.validateName = validateName;
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

    public boolean isCompatMode() {
        return compatMode;
    }

    public void setCompatMode(boolean compatMode) {
        this.compatMode = compatMode;
    }

    public void validate(String lexicalRepresentation, OperationResult validationResult, String objectResultOperationName) {
        try {
            try (ByteArrayInputStream is = new ByteArrayInputStream(lexicalRepresentation.getBytes(INPUT_STREAM_CHARSET))) {
                validate(is, validationResult, objectResultOperationName);
            }
        } catch (IOException e) {
            throw new SystemException(e);       // shouldn't really occur
        }
    }

    public void validate(InputStream inputStream, OperationResult validatorResult, String objectResultOperationName) {

        DOMConverter domConverter = new DOMConverter();

        XMLStreamReader stream;
        try {

            Map<String, String> rootNamespaceDeclarations = new HashMap<>();

            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
//            xmlInputFactory.setProperty(XMLInputFactory2.P_REPORT_CDATA, false);
            stream = xmlInputFactory.createXMLStreamReader(inputStream);

            int eventType = stream.nextTag();
            if (eventType == XMLStreamConstants.DTD || eventType == XMLStreamConstants.ENTITY_DECLARATION
                    || eventType == XMLStreamConstants.ENTITY_REFERENCE || eventType == XMLStreamConstants.NOTATION_DECLARATION) {
                // We do not want those, e.g. we want to void XXE vulnerabilities. Make this check explicit.
                throw new SystemException("Use of " + eventType + " in XML is prohibited");
            }
            if (eventType == XMLStreamConstants.START_ELEMENT) {
                if (!QNameUtil.match(stream.getName(), SchemaConstants.C_OBJECTS)) {
                    // This has to be an import file with a single objects. Try
                    // to process it.
                    OperationResult objectResult = validatorResult.createSubresult(objectResultOperationName);
                    progress++;
                    objectResult.addContext(OperationResult.CONTEXT_PROGRESS, progress);

                    EventResult cont;
                    try {
                        cont = readFromStreamAndValidate(stream, objectResult, rootNamespaceDeclarations, validatorResult, domConverter);
                    } catch (RuntimeException e) {
                        // Make sure that unexpected error is recorded.
                        objectResult.recordFatalError(e);
                        throw e;
                    }

                    if (!cont.isCont()) {
                        String message;
                        if (cont.getReason() != null) {
                            message = cont.getReason();
                        } else {
                            message = "Object validation failed (no reason given)";
                        }
                        if (objectResult.isUnknown()) {
                            objectResult.recordFatalError(message);
                        }
                        validatorResult.recordFatalError(message);
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

                    EventResult cont;
                    try {
                        // Read and validate individual object from the stream
                        cont = readFromStreamAndValidate(stream, objectResult,
                                rootNamespaceDeclarations, validatorResult, domConverter);
                    } catch (RuntimeException e) {
                        if (objectResult.isUnknown()) {
                            // Make sure that unexpected error is recorded.
                            objectResult.recordFatalError(e);
                        }
                        throw e;
                    } finally {
                        objectResult.close();
                    }

                    if (objectResult.isError()) {
                        errors++;
                    }

                    objectResult.cleanupResult();
                    validatorResult.summarize();

                    if (cont.isStop()) {
                        if (cont.getReason() != null) {
                            validatorResult.recordFatalError("Processing has been stopped: "
                                    + cont.getReason());
                        } else {
                            validatorResult.recordFatalError("Processing has been stopped");
                        }
                        // This means total stop, no other objects will be
                        // processed
                        return;
                    }
                    if (!cont.isCont()) {
                        if (stopAfterErrors > 0 && errors >= stopAfterErrors) {
                            if (errors == 1) {
                                validatorResult.recordFatalError("Stopping on error; " + (progress - errors) + " passed");
                            } else {
                                validatorResult.recordFatalError("Too many errors (" + errors + "); " + (progress - errors) + " passed");
                            }
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
                handler.handleGlobalError(validatorResult, ex);
            }
            return;
        }

        // Error count is sufficient. Detailed messages are in subresults
        validatorResult.computeStatus(errors + " errors, " + (progress - errors) + " passed");

    }

    private EventResult readFromStreamAndValidate(XMLStreamReader stream, OperationResult objectResult,
            Map<String, String> rootNamespaceDeclarations, OperationResult validatorResult,
            DOMConverter domConverter) {

        objectResult.addContext(START_LINE_NUMBER, stream.getLocation().getLineNumber());

        Document objectDoc;
        try {
            // Parse the object from stream to DOM
            objectDoc = domConverter.buildDocument(stream);
        } catch (XMLStreamException ex) {
            validatorResult.recordFatalError("XML parsing error: " + ex.getMessage(), ex);
            if (handler != null) {
                handler.handleGlobalError(validatorResult, ex);
            }
            objectResult.recordFatalError(ex);
            return EventResult.skipObject(ex.getMessage());
        }

        objectResult.addContext(END_LINE_NUMBER, stream.getLocation().getLineNumber());

        // This element may not have complete namespace definitions for a stand-alone
        // processing, therefore copy namespace definitions from the root element.
        Element objectElement = DOMUtil.getFirstChildElement(objectDoc);
        DOMUtil.setNamespaceDeclarations(objectElement, rootNamespaceDeclarations);

        return validateObjectInternal(objectElement, objectResult, validatorResult);
    }

    public EventResult validateObject(String stringXml, OperationResult objectResult) {
        Document objectDoc = DOMUtil.parseDocument(stringXml);
        Element objectElement = DOMUtil.getFirstChildElement(objectDoc);
        return validateObjectInternal(objectElement, objectResult, objectResult);
    }

    public EventResult validateObject(Element objectElement, OperationResult objectResult) {
        return validateObjectInternal(objectElement, objectResult, objectResult);
    }

    private EventResult validateObjectInternal(Element objectElement, OperationResult objectResult, OperationResult validatorResult) {
        try {
            Node postValidationTree = null;

            if (validateSchemas) {
                postValidationTree = validateSchema(objectElement, objectResult);
                if (postValidationTree == null) {
                    // There was an error
                    return EventResult.skipObject(objectResult.getMessage());
                }
            }

            if (handler != null) {
                EventResult cont;
                try {
                    cont = handler.preMarshall(objectElement, postValidationTree, objectResult);
                } catch (RuntimeException e) {
                    objectResult.recordFatalError("Internal error: preMarshall call failed: " + e.getMessage(), e);
                    throw e;
                }
                if (!cont.isCont()) {
                    if (objectResult.isUnknown()) {
                        objectResult.recordFatalError("Stopped after preMarshall, no reason given");
                    }
                    return cont;
                }
            }

            if (!objectResult.isAcceptable()) {
                // Schema validation or preMarshall has failed. No point to
                // continue with this object.
                if (objectResult.isUnknown()) {
                    objectResult.recordFatalError("Result not acceptable after preMarshall, no reason given");
                }
                return EventResult.skipObject();
            }

            PrismParserNoIO parser = prismContext.parserFor(objectElement);
            if (compatMode) {
                parser = parser.compat();
            }
            if (convertMissingType) {
                parser = parser.convertMissingTypes();
            }
            T containerable = parser.parseRealValue();

            objectResult.addContext(OperationResult.CONTEXT_OBJECT, containerable.toString());

            if (containerable instanceof Objectable) {
                Objectable objectable = (Objectable) containerable;

                try {
                    objectable.asPrismObject().checkConsistence();
                } catch (RuntimeException e) {
                    objectResult.recordFatalError("Internal object inconsistence, probably a parser bug: " + e.getMessage(), e);
                    return EventResult.skipObject(e.getMessage());
                }
                if (verbose) {
                    LOGGER.trace("Processing OID {}", objectable.getOid());
                }
                validateObject(objectable, objectResult);
            }

            if (handler != null) {
                EventResult cont;
                try {
                    cont = handler.postMarshall(containerable, objectElement, objectResult);
                } catch (RuntimeException e) {
                    // Make sure that unhandled exceptions are recorded in object result before they are rethrown
                    objectResult.recordFatalError("Internal error: postMarshall call failed: " + e.getMessage(), e);
                    throw e;
                }
                if (!cont.isCont()) {
                    if (objectResult.isUnknown()) {
                        objectResult.recordFatalError("Stopped after postMarshall, no reason given");
                    }
                    return cont;
                }
            }

            objectResult.recomputeStatus();

            return EventResult.cont();
        } catch (SchemaException ex) {
            if (verbose) {
                LOGGER.trace("Schema exception", ex);
            }
            if (handler != null) {
                try {
                    handler.handleGlobalError(validatorResult, ex);
                } catch (RuntimeException e) {
                    // Make sure that unhandled exceptions are recorded in object result before they are rethrown
                    objectResult.recordFatalError("Internal error: handleGlobalError call failed: " + e.getMessage(), e);
                    throw e;
                }
            }
            objectResult.recordFatalError(ex);
            return EventResult.skipObject(ex.getMessage());
        } catch (RuntimeException ex) {
            validatorResult.recordFatalError("Couldn't parse object: " + ex.getMessage(), ex);
            if (verbose) {
                LOGGER.trace("Couldn't parse object", ex);
            }
            if (handler != null) {
                try {
                    handler.handleGlobalError(validatorResult);
                } catch (RuntimeException e) {
                    // Make sure that unhandled exceptions are recorded in object result before they are rethrown
                    objectResult.recordFatalError("Internal error: handleGlobalError call failed: " + e.getMessage(), e);
                    throw e;
                }
            }
            objectResult.recordFatalError(ex);
            return EventResult.skipObject(ex.getMessage());
        }
    }

    // this was made public to allow validation of pre-parsed non-prism documents
    public Node validateSchema(Element objectDoc, OperationResult objectResult) {
        OperationResult result = objectResult.createSubresult(LegacyValidator.class.getName() + ".validateSchema");
        DOMResult validationResult = new DOMResult();
        try {
            xsdValidator.validate(new DOMSource(objectDoc), validationResult);
        } catch (SAXException e) {
            result.recordFatalError("Validation error: " + e.getMessage(), e);
            objectResult.computeStatus("Validation error: " + e.getMessage());
            return null;
        } catch (IOException e) {
            result.recordFatalError("IO error during validation: " + e.getMessage(), e);
            objectResult.computeStatus("IO error during validation: " + e.getMessage());
            return null;
        }
        result.recordSuccess();
        return validationResult.getNode();
    }

    public void validateObject(Objectable object, OperationResult objectResult) {
        // Check generic object properties

        checkBasics(object, objectResult);

        // TODO: more checks

        objectResult.recomputeStatus("Object validation has failed", "Validation warning");
    }

    // BIG checks - checks that create subresults

    private void checkBasics(Objectable object, OperationResult objectResult) {
        OperationResult subresult = objectResult.createSubresult(OPERATION_RESOURCE_BASICS_CHECK);
        if (validateName) {
            checkName(object, object.getName(), "name", subresult);
        }
        subresult.recordSuccessIfUnknown();
    }

    // Small checks - checks that don't create subresults

    private void checkName(Objectable object, PolyStringType value, String propertyName, OperationResult subResult) {
        // TODO: check for all whitespaces
        // TODO: check for bad characters
        if (value == null) {
            error("Null property", object, propertyName, subResult);
            return;
        }
        String orig = value.getOrig();
        if (orig == null || orig.isEmpty()) {
            error("Empty property", object, propertyName, subResult);
        }
    }

    private void checkUri(Objectable object, String value, String propertyName, OperationResult subResult) {
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

    private void error(String message, Objectable object, String propertyName, OperationResult subResult) {
        subResult.addContext(OperationResult.CONTEXT_OBJECT, object.toString());
        subResult.addContext(OperationResult.CONTEXT_ITEM, propertyName);
        subResult.recordFatalError("<" + propertyName + ">: " + message);
    }

    public boolean isConvertMissingType() {
        return convertMissingType;
    }

    public void setConvertMissingType(boolean convertMissingType) {
        this.convertMissingType = convertMissingType;
    }
}
