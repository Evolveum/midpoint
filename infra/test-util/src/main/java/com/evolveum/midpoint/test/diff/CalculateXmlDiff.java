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

package com.evolveum.midpoint.test.diff;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.custommonkey.xmlunit.ComparisonController;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceConstants;
import org.custommonkey.xmlunit.DifferenceEngine;
import org.custommonkey.xmlunit.XMLUnit;
import org.slf4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.holder.XPathSegment;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.test.util.PrismContextTestUtil;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.sun.org.apache.xerces.internal.dom.AttrNSImpl;

/**
 * Main class responsible to calculate XML differences
 * 
 * @author Igor Farinic
 * @version $Revision$ $Date$
 * @since 0.1
 */
public class CalculateXmlDiff {

	// TODO: WARNING !!! Multivalue properties are not handled properly or for
	// some scenarios are not handled at all (see OPENIDM-316)!!! WARNING

	private static final Logger LOGGER = TraceManager.getTrace(CalculateXmlDiff.class);

	private static PropertyModificationTypeType decideModificationTypeForChildNodeNotFound(Difference diff) {
		// add or replace node
		String newObjectOid = TestUtil.getNodeOid(diff.getTestNodeDetail().getNode());
		PropertyModificationTypeType changeType;
		XPathHolder xpathHolder = new XPathHolder(diff.getTestNodeDetail().getXpathLocation());
		// HACK: accountRef, resourceRef - special treatment, ignore its oid
		if ( (StringUtils.isEmpty(newObjectOid))
				|| (StringUtils.contains(diff.getTestNodeDetail().getNode().getNodeName(), "Ref")) ) {
			changeType = PropertyModificationTypeType.add;
		} else {
			changeType = PropertyModificationTypeType.replace;
		}
		
		//HACK: till we create new diff algorithm
		if ((xpathHolder.toSegments().size()==2 && (xpathHolder.toSegments().get(1).getQName().getLocalPart().equals("eMailAddress")))) {
			changeType = PropertyModificationTypeType.replace;
		}
		
		return changeType;
	}

	private static void setupXmlUnit() {
		// XmlUnit setup
		// Note: compareUnmatched has to be set to false to calculate diff
		// properly,
		// to avoid matching of nodes that are not comparable
		XMLUnit.setCompareUnmatched(false);
		XMLUnit.setIgnoreAttributeOrder(true);
		XMLUnit.setIgnoreWhitespace(true);
		XMLUnit.setNormalize(true);
		XMLUnit.setNormalizeWhitespace(true);
	}

	private static List<Difference> calculateXmlUnitDifferences(InputStream inputStreamOld,
			InputStream inputStreamNew) throws DiffException {
		List<Difference> l;
		try {
			LOGGER.trace("Calculate XmlUnit differences");
			// Following setup is required to explicitly inject midPoint XPath
			// tracker
			// We will use the same behavior for ComparisonController as the one
			// used by DetailedDiff
			ComparisonController controller = new ComparisonController() {

				@Override
				public boolean haltComparison(Difference arg0) {
					return false;
				}
			};
			DifferenceEngine comparator = new MidPointDifferenceEngine(controller);
			Diff d = new Diff(XMLUnit.buildControlDocument(new InputSource(inputStreamOld)),
					XMLUnit.buildTestDocument(new InputSource(inputStreamNew)), comparator);
			// end of setup for midPoint XPath tracker

			// calculate XMLUnit differences
			DetailedDiff dd = new DetailedDiff(d);
			dd.overrideElementQualifier(new OidQualifier());
			dd.overrideDifferenceListener(new MidPointDifferenceListener());
			l = dd.getAllDifferences();
			LOGGER.trace("XmlUnit Differences are ready");
			return l;
		} catch (SAXException ex) {
			LOGGER.error("Error calculating differences", ex);
			throw new DiffException(ex);
		} catch (IOException ex) {
			LOGGER.error("Error calculating differences", ex);
			throw new DiffException(ex);
		}
	}

	public static ObjectModificationType calculateChanges(ObjectType oldObject, ObjectType newObject)
			throws DiffException {

		Validate.notNull(oldObject);
		Validate.notNull(newObject);
        boolean equalOid = oldObject.getOid() != null ?
                oldObject.getOid().equals(newObject.getOid()) : newObject.getOid() == null;
		Validate.isTrue(equalOid);

		try {
			// wrap objects into JAXBElement
			ObjectFactory of = new ObjectFactory();
			JAXBElement<ObjectType> jaxbOld = of.createObject(oldObject);
			JAXBElement<ObjectType> jaxbNew = of.createObject(newObject);

			String stringOld = PrismContextTestUtil.marshalElementToString(jaxbOld);
			String stringNew = PrismContextTestUtil.marshalElementToString(jaxbNew);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Old Object {}\nNew Object {}", new Object[] { stringOld, stringNew });
			}

			return calculateChanges(IOUtils.toInputStream(stringOld, "utf-8"),
					IOUtils.toInputStream(stringNew, "utf-8"), oldObject.getOid());
		} catch (JAXBException ex) {
			throw new DiffException(ex);
		} catch (IOException ex) {
			throw new DiffException(ex);
		}
	}

	public static ObjectModificationType calculateChanges(File oldObjectFile, File newObjectFile)
			throws DiffException {
		try {
			JAXBElement<ObjectType> jaxbObject = (JAXBElement) PrismContextTestUtil.unmarshalElement(oldObjectFile);
			String objectOid = jaxbObject.getValue().getOid();
			return calculateChanges(new FileInputStream(oldObjectFile), new FileInputStream(newObjectFile),
					objectOid);
		} catch (JAXBException ex) {
			throw new DiffException(ex);
		} catch (FileNotFoundException ex) {
			throw new DiffException(ex);
		}
	}

	private static XPathHolder modifyXpath(String originalXpath, Node node) {
		XPathHolder xpathType = new XPathHolder(originalXpath, node);
		return modifyXpath(xpathType);

	}

	private static XPathHolder modifyXpath(XPathHolder originalXpath) {
		LOGGER.trace("XPath generated by XMLUnit {}", originalXpath);
		List<XPathSegment> segments = originalXpath.toSegments();
		List<XPathSegment> modifiedSegments = new ArrayList<XPathSegment>();
		if (segments.size() > 2) {
			modifiedSegments = segments.subList(1, segments.size() - 1);
		}
		XPathHolder modifiedXpath = new XPathHolder(modifiedSegments);
		LOGGER.trace("XPath modified for midPoint functionality {}", modifiedXpath.getXPath());
		return modifiedXpath;
	}

	private static Node getReplacePropertyNode(Node testNode, XPathHolder xpathForChange,
			String replacePropertyName) {
		Node node = testNode;

		List<XPathSegment> segments = xpathForChange.toSegments();
		for (int j = segments.size() - 1; j >= 0; j--) {
			XPathSegment segment = segments.get(j);
			if (replacePropertyName.equals(segment.getQName().getLocalPart())) {
				return node;
			} else {
				// Note: we had do instanceof for specific impl, because node
				// type was null
				if (node instanceof AttrNSImpl) {
					node = ((AttrNSImpl) node).getOwnerElement();
				} else {
					node = node.getParentNode();
				}
			}
		}

		throw new IllegalArgumentException("Error getting node for replace property");
	}

	private static XPathHolder getXPathHolderForReplaceProperty(XPathHolder xpathForChange,
			String replacePropertyName) {
		List<XPathSegment> segments = xpathForChange.toSegments();
		int segmentWithReplaceProperty = 0;

		for (int j = segments.size() - 1; j >= 0; j--) {
			XPathSegment segment = segments.get(j);
			if (replacePropertyName.equals(segment.getQName().getLocalPart())) {
				segmentWithReplaceProperty = j;
				break;
			}
		}

		List<XPathSegment> modifiedSegments = segments.subList(0, segmentWithReplaceProperty + 1);
		return new XPathHolder(modifiedSegments);
	}

	private static boolean isReplacePropertyModificationRegistrered(ObjectModificationType changes,
			XPathHolder xpathType, String replacePropertyName) {
		// Prerequisite: xpath is for property that is "replaceProperty"
		for (PropertyModificationType change : changes.getPropertyModification()) {
			if (PropertyModificationTypeType.replace.equals(change.getModificationType())) {
				XPathHolder registeredXPath = new XPathHolder(change.getPath());
				if (xpathType.getXPath().equals(registeredXPath.getXPath())) {
					// TODO: supported is list with only one element
					String elementLocalName = JAXBUtil.getElementLocalName(change.getValue().getAny().get(0));
					if (replacePropertyName.equals(elementLocalName)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static ObjectModificationType calculateChanges(InputStream inputStreamOld,
			InputStream inputStreamNew, String objectOid) throws DiffException {

		setupXmlUnit();
		List<Difference> l = calculateXmlUnitDifferences(inputStreamOld, inputStreamNew);
		ObjectModificationType changes = new ObjectModificationType();
		// if there are no differences, return immidiatelly
		if (null == l || l.isEmpty()) {
			LOGGER.trace("No differences found. Returning empty list of differences");
			return changes;
		}

		// prerequisite: changes are made only on container objects: user,
		// account, resource, ...
		// String rootOid = getRootNodeOid(l.get(0));
		changes.setOid(objectOid);

		LOGGER.trace("Iterate through differences and create relative changes out of them");
		PropertyModificationType change = null;
		XPathHolder xpathType;
		for (Difference diff : l) {
			LOGGER.trace("Start processing of difference: {}", diff.getDescription());

			// process differences for replace properties
			if (DiffConstants.isForReplaceProperty(diff)) {
				String replacePropertyName = DiffConstants.getReplacePropertyName(diff);
				XPathHolder differenceXpath;
				if (null != diff.getTestNodeDetail().getXpathLocation()) {
					// if xpath in test node is null then we are deleting, and
					// the property delete is handled as standard property
					// if xpath in test node is not null we are replacing

					differenceXpath = new XPathHolder(diff.getTestNodeDetail().getXpathLocation(), diff
							.getTestNodeDetail().getNode());
					xpathType = getXPathHolderForReplaceProperty(differenceXpath, replacePropertyName);

					if (isReplacePropertyModificationRegistrered(changes, modifyXpath(xpathType),
							replacePropertyName)) {
						// if the same converted modification is already in the
						// list of property modification, then do nothing
						continue;
					}

					// convert difference to propertyModification
					Node testNodeWithReplaceProperty = getReplacePropertyNode(diff.getTestNodeDetail()
							.getNode(), differenceXpath, replacePropertyName);
					change = ObjectTypeUtil.createPropertyModificationType(
							PropertyModificationTypeType.replace, modifyXpath(xpathType),
							testNodeWithReplaceProperty);
					changes.getPropertyModification().add(change);
					continue;
				}
			}

			switch (diff.getId()) {
				// comparing two nodes and only one has any children
				case DifferenceConstants.HAS_CHILD_NODES_ID:
					// TODO: now it works, but the scenario for
					// HAS_CHILD_NODES_ID should be treated separately, to
					// generate correct diffs.

					// adding deleting node - account, extension, text node
				case DifferenceConstants.CHILD_NODE_NOT_FOUND_ID:
					// CHILD_NODE_NOT_FOUND_ID == presence of child node
					if (StringUtils.isEmpty(diff.getTestNodeDetail().getXpathLocation())) { // delete
																							// node
						// value removed, because it is not in new xml
						// for removed property xpath is taken from original xml
						xpathType = modifyXpath(diff.getControlNodeDetail().getXpathLocation(), diff
								.getControlNodeDetail().getNode());
						change = ObjectTypeUtil.createPropertyModificationType(
								PropertyModificationTypeType.delete, xpathType, (Element) diff
										.getControlNodeDetail().getNode());
					} else { // add or replace node
						PropertyModificationTypeType changeType = decideModificationTypeForChildNodeNotFound(diff);
						xpathType = modifyXpath(diff.getTestNodeDetail().getXpathLocation(), diff
								.getTestNodeDetail().getNode());
						// for added and replaced property xpath is taken from
						// new xml
						change = ObjectTypeUtil.createPropertyModificationType(changeType, xpathType,
								(Element) diff.getTestNodeDetail().getNode());
					}
					break;
				// change property value
				case DifferenceConstants.TEXT_VALUE_ID:
					// remove /text()
					String generatedXpath = diff.getTestNodeDetail().getXpathLocation();
					String modifiedPath = StringUtils.substring(generatedXpath, 0,
							generatedXpath.lastIndexOf("/"));
					xpathType = modifyXpath(modifiedPath, diff.getTestNodeDetail().getNode().getParentNode());
					change = ObjectTypeUtil.createPropertyModificationType(
							PropertyModificationTypeType.replace, xpathType, (Element) diff
									.getTestNodeDetail().getNode().getParentNode());
					break;
			}

			// if change was accepted then add it to the list of relative
			// changes
			if (null != change) {
				changes.getPropertyModification().add(change);
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(
							"Finished processing of difference {}. Relative change for difference is change = {}",

							diff.getDescription(), SchemaDebugUtil.prettyPrint(change));
					if (null != change.getValue()) {
						try {
							LOGGER.trace("Relative change value= {}",
									PrismContextTestUtil.marshalElementToString(change.getValue().getAny().get(0)));
						} catch (JAXBException e) {
							LOGGER.error("Unexpected JAXB exception while serializing "
									+ change.getValue().getAny().get(0) + ": " + e.getMessage(), e);
							throw new IllegalStateException("Unexpected JAXB exception while serializing "
									+ change.getValue().getAny().get(0) + ": " + e.getMessage(), e);
						}
					} else {
						LOGGER.trace("Relative change value was null");
					}
				}
			}
		}

		LOGGER.trace("Finished relative changes processing");
		LOGGER.debug("Returning differences stored as relative changes = {}", changes);
		return changes;
	}
}
