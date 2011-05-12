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

package com.evolveum.midpoint.util;

import com.evolveum.midpoint.util.jaxb.JAXBUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType.Value;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SchemaHandlingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SchemaHandlingType.AccountType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import com.evolveum.midpoint.xml.schema.XPathType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author semancik
 */
public class ObjectTypeUtil {

    static Random rnd = new Random();

    public static List<String> extractOids(List<? extends ObjectType> objects, List<? extends ObjectReferenceType> refs) {

        List<String> oids = new ArrayList<String>();

        if (objects != null) {
            for (ObjectType object : objects) {
                oids.add(object.getOid());
            }
        }

        if (refs != null) {
            for (ObjectReferenceType ref : refs) {
                oids.add(ref.getOid());
            }
        }

        return oids;
    }

    public static ObjectReferenceType findRef(String oid, List<ObjectReferenceType> refs) {
        for (ObjectReferenceType ref : refs) {
            if (ref.getOid().equals(oid)) {
                return ref;
            }
        }
        return null;
    }

//    TODO: refactor to use one code base for the method
    public static PropertyModificationType createPropertyModificationType(PropertyModificationTypeType changeType, XPathType xpathType, Node node) {
        PropertyModificationType change = new PropertyModificationType();
        change.setValue(new Value());
        change.setModificationType(changeType);
        change.setPath(xpathType.toElement(SchemaConstants.NS_C, "path"));
        change.getValue().getAny().add((Element) node);

        return change;
    }
//    public static PropertyModificationType createPropertyModificationType(PropertyModificationTypeType changetype, XPathType xpath, Element element) {
//        return createPropertyModificationType(changetype, xpath, new QName(element.getNamespaceURI(), element.getLocalName()), element.getTextContent());
//    }

    public static PropertyModificationType createPropertyModificationType(PropertyModificationTypeType changetype, XPathType xpath, QName property, Object value) {

        PropertyModificationType propertyChange = new PropertyModificationType();
        propertyChange.setModificationType(changetype);

        Document doc = DOMUtil.getDocument();

        if (xpath == null) {
            // Default XPath is empty XPath, which means "."
            xpath = new XPathType();
        }

        if (property.getPrefix() == null) {
            // If the prefix was not specified, generate a random prefix
            // to avoid collisions with standard "nsXX" prefixes
            String prefix = "ch" + rnd.nextInt(10000);
            property = new QName(property.getNamespaceURI(), property.getLocalPart(), prefix);
        }

        propertyChange.setPath(xpath.toElement(SchemaConstants.NS_C, "path", doc));

        Value jaxbValue = new Value();

        if (value instanceof String) {
            Element e = doc.createElementNS(property.getNamespaceURI(), property.getLocalPart());
            e.appendChild(doc.createTextNode((String) value));
            jaxbValue.getAny().add(e);

        } else if (value.getClass().getPackage().equals(ObjectFactory.class.getPackage())) {
            // JAXB Object from a common schema
            Element e;
            try {
                e = JAXBUtil.jaxbToDom(value, property, doc);
            } catch (JAXBException ex) {
                // This should not happen
                throw new IllegalStateException(ex);
            }
            jaxbValue.getAny().add(e);

        } else {
            throw new IllegalArgumentException("Unsupported value type " + value.getClass().getName());
        }

        propertyChange.setValue(jaxbValue);

        return propertyChange;
    }

    public static AccountType getAccountTypeDefinitionFromSchemaHandling(ResourceObjectShadowType accountShadow, ResourceType resource) {
        Validate.notNull(accountShadow);
        Validate.notNull(resource);
        SchemaHandlingType schemaHandling = resource.getSchemaHandling();
        QName accountObjectClass = accountShadow.getObjectClass();
        
        for (AccountType accountType : schemaHandling.getAccountType()) {
            if (accountObjectClass.equals(accountType.getObjectClass())) {
                return accountType;
            }
        }

        //no suitable definition found, then use default account
        for (AccountType accountType : schemaHandling.getAccountType()) {
            if (accountType.isDefault()) {
                return accountType;
            }
        }

        throw new IllegalArgumentException("Provided wrong AccountShadow or SchemaHandling. No AccountType definition found for provided account's object class: " + accountObjectClass);
    }
}