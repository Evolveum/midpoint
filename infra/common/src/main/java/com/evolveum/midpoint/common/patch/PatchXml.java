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

package com.evolveum.midpoint.common.patch;

import java.io.File;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.common.XPathUtil;
import com.evolveum.midpoint.common.XmlUtil;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.patch.PatchException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExtensibleObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.schema.XPathType;

/**
 * Main class for XML patching
 *
 * @author Igor Farinic
 * @version $Revision$ $Date$
 * @since 0.1
 */
public class PatchXml extends XPathUtil {

    private static final Logger logger = TraceManager.getTrace(PatchXml.class);
    private XPath xpath;
    private PatchingListener listener;

    public void setPatchingListener(PatchingListener listener) {
        this.listener = listener;
    }

    private void applyDifference(Document doc, PropertyModificationType change) throws PatchException {

        if (null != listener && !listener.isApplicable(change)) {
            return;
        }

        XPathType xpathUtil = new XPathType(change.getPath());
        //String xpathString = xpathUtil.getXPath();
        List<Element> values = change.getValue().getAny();
        //Note: for now supported is only one existingValue in the list
        Element newOrOldNode = values.get(0);

        NodeList nodes = null;
        switch (change.getModificationType()) {
            case add:
                //handle change type add
                //String parentXPath = xpathString;
                NodeList parentNodes = null;
                try {
                    //parentNodes = matchedNodesByXPath(doc, xpath, parentXPath, xpathUtil.getNamespaceMap());
                    parentNodes = (new XPathUtil()).matchedNodesByXPath(xpathUtil, null, doc.getFirstChild());
                } catch (XPathExpressionException ex) {
                    throw new PatchException(ex);
                }
                if (null == parentNodes || parentNodes.getLength() != 1) {
                    logger.error("XPath '{}' matches incorrect number of nodes (actual match was {})", xpathUtil.getXPath(), (parentNodes != null ? parentNodes.getLength() : 0));
                    throw new PatchException("XPath matches incorrect number of nodes");
                }
                Node parentNode = parentNodes.item(0);
                XmlUtil.addChildNodes(parentNode, newOrOldNode);
                break;

            case replace:
            case delete:
                //handle change types: update and delete
                try {
                    nodes = matchedNodesByXPath(xpathUtil, null, doc.getFirstChild());
                } catch (XPathExpressionException ex) {
                    throw new PatchException(ex);
                }
                //following checks are only for change types replace and delete
                if (null == nodes || nodes.getLength() == 0) {
                    logger.warn("No matches for XPath {}", xpathUtil.getXPath());
                    if (PropertyModificationTypeType.replace.equals(change.getModificationType())) {
                        logger.warn("Will create nodes defined by XPath {}", xpathUtil.getXPath());
                        //we will create xml tags defined by xpath
                        XPathUtil.createNodesDefinedByXPath(doc, xpathUtil);
                        try {
                            nodes = matchedNodesByXPath(xpathUtil, null, doc.getFirstChild());
                        } catch (XPathExpressionException ex) {
                            throw new PatchException(ex);
                        }
                    }
                }
                if (nodes.getLength() > 1) {
                    logger.warn("XPath {} matches more than one node ({}). It is ok, for multi value nodes", xpathUtil.getXPath(), nodes.getLength());
                }

                for (int i = 0; i < nodes.getLength(); i++) {
                    Node node = nodes.item(i);

                    if (PropertyModificationTypeType.replace.equals(change.getModificationType())) {
                        XmlUtil.replaceChildNodes(node, newOrOldNode);
                    }
                    if (PropertyModificationTypeType.delete.equals(change.getModificationType())) {
                        //TODO: we should check whole subtree
                        XmlUtil.deleteChildNodes(node, newOrOldNode);
                    }
                }
                break;
        }

        if (null != listener) {
            listener.applied(change);
        }

    }

    private static String serializePatchedXml(Document objectDoc) {
        String patchedXml;
        try {
            patchedXml = DOMUtil.serializeDOMToString(objectDoc);
            logger.trace("Patched xml (original xml with applied relative changes) = {}", patchedXml);
            return patchedXml;
        } catch (Exception ex) {
            logger.error("Failed to serialize DOM to String", ex);
        }
        return null;
    }

    private static JAXBElement prepareJaxbObject(File oldXmlFile) throws PatchException {
        JAXBElement<ExtensibleObjectType> object = null;
        try {
            object = (JAXBElement<ExtensibleObjectType>) JAXBUtil.unmarshal(oldXmlFile);
            return object;
        } catch (JAXBException ex) {
            logger.error("Failed to unmarshall object", ex);
            throw new PatchException("Failed to unmarshall object", ex);
        }

    }

    private static String marshallJaxbObject(ObjectType jaxbObject) throws PatchException {
        try {
            //wrap object into JAXBElement
            ObjectFactory of = new ObjectFactory();
            JAXBElement<ObjectType> jaxb = of.createObject(jaxbObject);

            //Marshal wrapped JAXElement into string
            String xml = JAXBUtil.marshal(jaxb);
            return xml;
        } catch (JAXBException ex) {
            throw new PatchException("Failed to marshall object", ex);
        }
    }
    
    public String applyDifferences(ObjectModificationType changes, ObjectType objectType) throws PatchException {
        //marshall JAXB Object
        String xmlObject = marshallJaxbObject(objectType);
        logger.trace("Original XML that we are going to patch {}", xmlObject);

        //create DOM document for serialized xml
        Document objectDoc = DOMUtil.parseDocument(xmlObject);
        //setup JAXP 
        xpath = setupXPath();

        logger.debug("Iterate through relative changes and apply them");
        for (PropertyModificationType change : changes.getPropertyModification()) {
            logger.debug("Apply change: changeType = {}, changePath = {}", new Object[]{change.getModificationType(), (null == change.getPath()) ? null : change.getPath().getTextContent()});
            if (change.getValue().getAny().isEmpty() || change.getModificationType() == null) {
                logger.warn("Skipping property modification, empty value list or undefined modification type.");
                continue;
            }
            logger.trace("Value of the relative change = {}", DOMUtil.serializeDOMToString(change.getValue().getAny().get(0)));
            applyDifference(objectDoc, change);
            logger.debug("Finished application of change: changeType = {}, changePath = {}", new Object[]{change.getModificationType(), (null == change.getPath()) ? null : change.getPath().getTextContent()});
        }
        logger.debug("Finished iteration through relative changes");
        return serializePatchedXml(objectDoc);
    }

    public String applyDifferences(ObjectModificationType changes, File oldXmlFile) throws PatchException {
        JAXBElement<ObjectType> jaxbObject = prepareJaxbObject(oldXmlFile);
        return applyDifferences(changes, jaxbObject.getValue());
    }

}

