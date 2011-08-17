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

import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.Objects;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import org.xml.sax.SAXParseException;

/**
 * 
 * 
 * This is not thread-safe! Only a single thread is supposed to use a single
 * instace of validator.
 * 
 * @author semancik
 */
public class Validator {

	private boolean verbose = false;
	private List<ValidationMessage> errors;
	private List<ValidationMessage> objectErrors;
	ObjectHandler handler;

	public Validator() {
		handler = null;
	}

	public Validator(ObjectHandler handler) {
		this.handler = handler;
	}

	public ObjectHandler getHanlder() {
		return handler;
	}

	public void setHandler(ObjectHandler handler) {
		this.handler = handler;
	}

	public boolean getVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public List<ValidationMessage> validate(InputStream inputStream) {

		errors = new ArrayList<ValidationMessage>();
		Unmarshaller u = null;

		try {
			JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
			u = jc.createUnmarshaller();
		} catch (JAXBException ex) {
			errors.add(new ValidationMessage(ValidationMessage.Type.ERROR, "Error initializing JAXB: " + ex));
			if (verbose) {
				ex.printStackTrace();
			}
			return errors;
		}

		Objects objects = null;

		try {
			Object object = u.unmarshal(new InputStreamReader(inputStream, "utf-8"));
			if (object instanceof Objects) {
				objects = (Objects) object;
			} else {
				objects = new Objects();
				objects.getObject().add((JAXBElement<? extends ObjectType>) object);
			}
		} catch (UnsupportedEncodingException ex) {
			// TODO: logging
		} catch (JAXBException ex) {
			if (verbose) {
				ex.printStackTrace();
			}
			Throwable linkedException = ex.getLinkedException();
			if (linkedException instanceof SAXParseException) {
				SAXParseException saxex = (SAXParseException) linkedException;

				errors.add(new ValidationMessage(ValidationMessage.Type.ERROR, "XML Parse error: "
						+ saxex.getMessage() + " (line " + saxex.getLineNumber() + " col "
						+ saxex.getColumnNumber() + ")"));

			} else if (ex instanceof UnmarshalException) {
				errors.add(new ValidationMessage(ValidationMessage.Type.ERROR, "Unmarshalling error: "
						+ ex.getMessage()));
			} else {
				errors.add(new ValidationMessage(ValidationMessage.Type.ERROR, "Unmarshalling error: "
						+ (linkedException != null ? linkedException.getMessage() : "unknown: " + ex)));
			}
			return errors;
		}

		for (JAXBElement<? extends ObjectType> jaxbObject : objects.getObject()) {
			objectErrors = new ArrayList<ValidationMessage>();

			ObjectType object = jaxbObject.getValue();

			if (verbose) {
				System.out.println("Processing OID " + object.getOid());
			}

			// Check generic object properties

			checkName(object, object.getName(), "name");

			// Type-specific checks

			if (object instanceof ResourceType) {
				ResourceType resource = (ResourceType) object;
				checkResource(resource);
			}

			// TODO
			if (handler != null) {
				handler.handleObject(object, objectErrors);
			}

		}

		return errors;
	}

	void checkName(ObjectType object, String value, String propertyName) {
		// TODO: check for all whitespaces
		// TODO: check for bad characters
		if (value == null || value.isEmpty()) {
			error("Empty property", object, propertyName);
		}
	}

	void checkUri(ObjectType object, String value, String propertyName) {
		// TODO: check for all whitespaces
		// TODO: check for bad characters
		if (value == null || value.isEmpty()) {
			error("Empty property", object, propertyName);
		}
		try {
			URI uri = new URI(value);
			if (uri.getScheme() == null) {
				error("URI is supposed to be absolute", object, propertyName);
			}
		} catch (URISyntaxException ex) {
			error("Wrong URI syntax: " + ex, object, propertyName);
		}

	}

	void checkResource(ResourceType resource) {
		checkUri(resource, resource.getNamespace(), "namespace");
	}

	void error(String message, ObjectType object) {
		ValidationMessage vm = new ValidationMessage(ValidationMessage.Type.ERROR, message, object.getOid(),
				object.getName());
		errors.add(vm);
		objectErrors.add(vm);
	}

	void error(String message, ObjectType object, String propertyName) {
		ValidationMessage vm = new ValidationMessage(ValidationMessage.Type.ERROR, message, object.getOid(),
				object.getName(), propertyName);
		errors.add(vm);
		objectErrors.add(vm);
	}
}
