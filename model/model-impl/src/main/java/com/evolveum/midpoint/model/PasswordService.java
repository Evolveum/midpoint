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

package com.evolveum.midpoint.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.util.ShadowUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationalResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType.Value;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.model.password_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.model.password_1.PasswordChangeRequestType;
import com.evolveum.midpoint.xml.ns._public.model.password_1.PasswordChangeResponseType;
import com.evolveum.midpoint.xml.ns._public.model.password_1.PasswordPortType;
import com.evolveum.midpoint.xml.ns._public.model.password_1.PasswordSynchronizeRequestType;
import com.evolveum.midpoint.xml.ns._public.model.password_1.SelfPasswordChangeRequestType;
import com.evolveum.midpoint.xml.ns._public.provisioning.provisioning_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.provisioning.resource_object_change_listener_1.ResourceObjectChangeListenerPortType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import com.evolveum.midpoint.xml.schema.XPathSegment;
import com.evolveum.midpoint.xml.schema.XPathType;

@Service
public class PasswordService implements PasswordPortType {

	private static final Trace LOGGER = TraceManager.getTrace(PasswordService.class);
	@Autowired(required = true)
	private RepositoryService repositoryService;
	@Autowired(required = true)
	private ProvisioningService provisioningService;
	@Autowired(required = true)
	private ResourceObjectChangeListenerPortType resourceObjectChangeService;

	@Override
	public PasswordChangeResponseType selfChangePassword(SelfPasswordChangeRequestType spcrt) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public PasswordChangeResponseType changePassword(PasswordChangeRequestType pcrt) {
		final PropertyModificationType passwordChange = createPasswordModification(pcrt.getNewPassword());
		PasswordChangeResponseType result = new PasswordChangeResponseType();
		if (null != passwordChange) {
			validatePolicy(pcrt.getUser(), pcrt.getNewPassword());
			for (ObjectReferenceType accountref : pcrt.getTarget()) {
				ObjectModificationType objectChange = new ObjectModificationType();
				objectChange.setOid(accountref.getOid());
				objectChange.getPropertyModification().add(passwordChange);
				OperationalResultType operationalResult = new OperationalResultType();

				// TODO: Handle scripts
				ScriptsType scripts = new ScriptsType();

				try {
					provisioningService.modifyObject(objectChange, scripts, new OperationResult(
							"Modify Object"));
				} catch (Exception ex) {
					LOGGER.error("Failed change password: " + accountref.getOid(), ex);
				}
				result.getOperationalResult().add(operationalResult);
			}
		} else {
			// TODO: do something with the result
		}
		return result;
	}

	@Override
	public void synchronizePassword(PasswordSynchronizeRequestType body) {
		if (LOGGER.isDebugEnabled()) {
			try {
				JAXBContext ctx = JAXBContext.newInstance(ObjectFactory.class);
				ObjectFactory of = new ObjectFactory();
				Marshaller m = ctx.createMarshaller();
				m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
				m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
				m.marshal(of.createPasswordSynchronizeRequest(body), System.out);
			} catch (JAXBException ex) {
				LOGGER.debug("Debug exception", ex);
			}
		}

		String userName = null;
		for (Element e : body.getIdentifier().getAny()) {
			if ("resourceAccountName".equals(e.getLocalName())) {
				userName = e.getTextContent();
				break;
			}
		}

		if (null != userName && null != body.getSubject()) {
			try {
				ResourceObjectShadowType account = getCurrentShadow(body.getSubject().getOid(), userName);
				if (null != account) {
					ResourceType resource = (ResourceType) repositoryService.getObject(body.getSubject()
							.getOid(), new PropertyReferenceListType(), new OperationResult("Get Object"));
					ResourceObjectShadowChangeDescriptionType change = new ResourceObjectShadowChangeDescriptionType();

					change.setResource(resource);
					change.setShadow(account);
					change.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_SYNC));

					ObjectChangeModificationType pwchange = new ObjectChangeModificationType();
					ObjectModificationType mod = new ObjectModificationType();
					mod.setOid(account.getOid());

					PropertyModificationType passwordChange = createPasswordModification(body.getPassword());
					mod.getPropertyModification().add(passwordChange);
					pwchange.setObjectModification(mod);
					change.setObjectChange(pwchange);
					resourceObjectChangeService.notifyChange(change);
				}
			} catch (Exception ex) {
				LOGGER.error("Failed to synchronize password", ex);
			}

		}
	}

	/*
	 * Global password policy validation. The resource based validation will
	 * come later.
	 */
	private boolean validatePolicy(ObjectReferenceType userRef, Element newPassword) {
		return true;
	}

	private PropertyModificationType createPasswordModification(Element newPassword) {
		if (null == newPassword) {
			return null;
		}
		PropertyModificationType modification = null;
		try {
			Document doc = getXmlDocument();
			modification = new PropertyModificationType();
			modification.setModificationType(PropertyModificationTypeType.replace);
			List<XPathSegment> segments = new ArrayList<XPathSegment>();
			segments.add(new XPathSegment(SchemaConstants.I_CREDENTIALS));
			XPathType xpath = new XPathType(segments);
			modification.setPath(xpath.toElement(SchemaConstants.NS_C, "path", doc));

			Element e = doc.createElementNS(SchemaConstants.NS_C, "password");
			Element hash = doc.createElementNS(SchemaConstants.NS_C, "c:base64");
			hash.setTextContent(Base64.encodeBase64String(newPassword.getTextContent().getBytes()));
			e.appendChild(hash);

			modification.setValue(new Value());
			modification.getValue().getAny().add(e);
		} catch (ParserConfigurationException ex) {
			LOGGER.error("getXmlDocument", ex);
		}
		return modification;

	}

	protected Document getXmlDocument() throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder loader = factory.newDocumentBuilder();
		return loader.newDocument();

	}

	/**
	 * Locates the appropriate Shadow in repository that corresponds to the
	 * provided resource object.
	 * 
	 * No update is done, just the current repository state is returned. This
	 * operation is NOT supposed for generic usage. It is expected to be used in
	 * the rare cases where old repository state is required (e.g.
	 * synchronization)
	 * 
	 * TODO: Fix error handling (both runtime exceptions and Fault)
	 * 
	 * @param resourceObject
	 *            any state of resource objects, it just to contain valid
	 *            identifiers
	 * @return current unchanged shadow object that corresponds to provided
	 *         resource object or null if the object does not exist
	 */
	private ResourceObjectShadowType getCurrentShadow(String resourceOID, String userName)
			throws FaultMessage {
		QueryType query = createSearchQuery(userName);
		PagingType paging = new PagingType();
		ObjectListType results = null;
		try {
			results = repositoryService.searchObjects(query, paging, new OperationResult("Search Objects"));
			if (results.getObject().size() == 1 && results.getObject().get(0) instanceof UserType) {
				UserType user = (UserType) results.getObject().get(0);
				for (ObjectReferenceType ref : user.getAccountRef()) {
					ResourceObjectShadowType account = (ResourceObjectShadowType) repositoryService
							.getObject(ref.getOid(), new PropertyReferenceListType(), new OperationResult(
									"Get Object"));
					if (account.getResourceRef().getOid().equals(resourceOID)) {
						return account;
					}
				}
			}
			if (results.getObject().size() > 1) {
				// TODO: Better error handling later
				throw new IllegalStateException("More than one user found for " + userName);
			}
		} catch (Exception ex) {
			throw new FaultMessage(ex.getMessage(), null, ex);
		}
		return null;
	}

	protected QueryType createSearchQuery(String userName) {

		// We are going to query for attributes, so setup appropriate
		// XPath for the filter
		XPathSegment xpathSegment = new XPathSegment(SchemaConstants.C_NAME);
		List<XPathSegment> xpathSegments = new ArrayList<XPathSegment>();
		xpathSegments.add(xpathSegment);
		XPathType xpath = new XPathType(xpathSegments);

		// We have all the data, we can construct the filter now
		Document doc = ShadowUtil.getXmlDocument();
		Element idElement = doc.createElementNS(SchemaConstants.C_NAME.getNamespaceURI(),
				SchemaConstants.C_NAME.getLocalPart());
		idElement.setTextContent(userName);
		Element filter = QueryUtil.createAndFilter(
				doc,
				// TODO: The account type is hardcoded now, it should determined
				// from the shcema later, or maybe we can make it entirelly
				// generic (use ResourceObjectShadowType instead).
				QueryUtil.createTypeFilter(doc, QNameUtil.qNameToUri(SchemaConstants.I_USER_TYPE)),
				QueryUtil.createEqualFilter(doc, xpath, idElement));

		QueryType query = new QueryType();
		query.setFilter(filter);

		return query;
	}
}
