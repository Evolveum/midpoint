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
package com.evolveum.midpoint.web.controller.enduser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.faces.event.ActionEvent;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.namespace.MidPointNamespacePrefixMapper;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.bean.AccountFormBean;
import com.evolveum.midpoint.web.bean.ResourceCapability;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.jsf.form.AttributeType;
import com.evolveum.midpoint.web.jsf.form.FormAttribute;
import com.evolveum.midpoint.web.jsf.form.FormAttributeDefinition;
import com.evolveum.midpoint.web.jsf.form.FormObject;
import com.evolveum.midpoint.web.model.AccountManager;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.UserManager;
import com.evolveum.midpoint.web.model.dto.AccountShadowDto;
import com.evolveum.midpoint.web.model.dto.GuiUserDto;
import com.evolveum.midpoint.web.model.dto.PropertyChange;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.web.util.SchemaFormParser;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ProtectedStringType;

/**
 * 
 * @author Serbak
 * 
 */
@Controller("endUserDetails")
@Scope("session")
public class EndUserDetailsController implements Serializable {

	private static final long serialVersionUID = 7795454033519006167L;
	public static final String PAGE_NAVIGATION_TO_USER = "/user-gui/profile/userProfile?faces-redirect=true";
	public static final String PAGE_NAVIGATION_TO_ACCOUNT = "/user-gui/account/index?faces-redirect=true";
	private static final Trace LOGGER = TraceManager.getTrace(EndUserDetailsController.class);
	@Autowired(required = true)
	private ObjectTypeCatalog objectTypeCatalog;
	private Protector protector;
	private List<AccountFormBean> accountList;
	private List<AccountFormBean> accountListDeleted = new ArrayList<AccountFormBean>();
	private List<AccountFormBean> accountListUnlinked = new ArrayList<AccountFormBean>();
	private boolean editMode = false;
	private boolean editPasswordMode = false;
	private boolean editAccount = false;

	public boolean isEditPasswordMode() {
		return editPasswordMode;
	}

	public void setEditPasswordMode(boolean editPasswordMode) {
		this.editPasswordMode = editPasswordMode;
	}

	private GuiUserDto user;

	public List<AccountFormBean> getAccountList() {
		if (accountList == null) {
			accountList = new ArrayList<AccountFormBean>();
		}

		return accountList;
	}
	
	public List<FormObject> getFormObjects() {
		List<FormObject> list = new ArrayList<FormObject>();
		for (AccountFormBean bean : getAccountList()) {
			list.add(bean.getBean());
		}

		return list;
	}

	public void setEditMode(boolean editMode) {
		this.editMode = editMode;
	}

	public boolean isEditMode() {
		return editMode;
	}

	public boolean isEditAccount() {
		return editAccount;
	}

	public void setEditAccount(boolean editAccount) {
		this.editAccount = editAccount;
	}

	public GuiUserDto getUser() {
		return user;
	}

	public void setUser(GuiUserDto user) {
		this.user = user;
	}
	
	public void fillContent(){
		if (editMode) {
			editPasswordMode = false;
		} else if (editPasswordMode) {
			editMode = false;
		}

		SecurityUtils secUtils = new SecurityUtils();
		String userOid = secUtils.getUserOid();

		GuiUserDto curUser = null;
		if (StringUtils.isEmpty(userOid)) {
			FacesUtils.addErrorMessage("Couldn't show user details, unidentified oid.");
		}

		try {
			UserManager userManager = ControllerUtil.getUserManager(objectTypeCatalog);

			LOGGER.info("userSelectionListener start");
			PropertyReferenceListType resolve = new PropertyReferenceListType();
			resolve.getProperty().add(Utils.fillPropertyReference("Account"));
			resolve.getProperty().add(Utils.fillPropertyReference("Resource"));

			curUser = (GuiUserDto) userManager.get(userOid, resolve);
			accountList = createFormBeanList(curUser.getAccount(), false);
			LOGGER.info("userSelectionListener end");
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Can't select user, unknown error occured", ex);
			FacesUtils.addErrorMessage("Can't select user, unknown error occured.", ex);
		}
		user = curUser;
	}

	public String fillUser() {
		fillContent();
		return PAGE_NAVIGATION_TO_USER;
	}
	
	public String fillAccount() {
		fillContent();
		return PAGE_NAVIGATION_TO_ACCOUNT;
	}

	public void startEditMode(ActionEvent evt) {
		editMode = true;
	}

	public void startEditPasswordMode(ActionEvent evt) {
		editPasswordMode = true;
	}

	public String profileBackPerformed() {
		editMode = false;
		return null;
	}

	public String passwordBackPerformed() {
		editPasswordMode = false;
		return null;
	}

	/**
	 * TODO: remove account from user which are in accountListDeleted save
	 * accounts from accountList save user attributes from form
	 */
	public void saveProfilePerformed(ActionEvent evt) {
		OperationResult result = new OperationResult("Save User Changes");
		if (user != null) {
			LOGGER.debug("Normalizing user assignments");
			user.normalizeAssignments();
		}
		try {
			UserManager userManager = ControllerUtil.getUserManager(objectTypeCatalog);
			AccountManager accountManager = ControllerUtil.getAccountManager(objectTypeCatalog);

			processNewAccounts();
			List<AccountShadowType> accountsToDelete = processDeletedAccounts();
			processUnlinkedAccounts();

			LOGGER.debug("Submit user modified in GUI");
			Set<PropertyChange> userChanges = userManager.submit(user, result);
			LOGGER.debug("Modified user in GUI submitted ");

			LOGGER.debug("Start processing of deleted accounts");
			for (AccountShadowType account : accountsToDelete) {
				accountManager.delete(account.getOid());
			}
			LOGGER.debug("Finished processing of deleted accounts");
			profileBackPerformed();
			FacesUtils.addSuccessMessage("Changes saved successfully.");

		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Unknown error occured during save operation", ex);
			FacesUtils.addErrorMessage("Unknown error occured during save operation, reason: "
					+ ex.getMessage());
		}
	}

	public void savePasswordPerformed(ActionEvent evt) {
		OperationResult result = new OperationResult("Save User Changes");
		if (user != null) {
			LOGGER.debug("Normalizing user assignments");
			user.normalizeAssignments();
		}
		try {
			UserManager userManager = ControllerUtil.getUserManager(objectTypeCatalog);
			AccountManager accountManager = ControllerUtil.getAccountManager(objectTypeCatalog);

			processNewAccounts();
			List<AccountShadowType> accountsToDelete = processDeletedAccounts();
			processUnlinkedAccounts();

			LOGGER.debug("Submit user modified in GUI");
			Set<PropertyChange> userChanges = userManager.submit(user, result);
			LOGGER.debug("Modified user in GUI submitted ");

			LOGGER.debug("Start processing of deleted accounts");
			for (AccountShadowType account : accountsToDelete) {
				accountManager.delete(account.getOid());
			}
			LOGGER.debug("Finished processing of deleted accounts");
			passwordBackPerformed();
			FacesUtils.addSuccessMessage("Changes saved successfully.");

		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Unknown error occured during save operation", ex);
			FacesUtils.addErrorMessage("Unknown error occured during save operation, reason: "
					+ ex.getMessage());
		}
	}

	private void processNewAccounts() throws SchemaException {
		LOGGER.debug("Start processing of new accounts");
		for (AccountFormBean formBean : accountList) {
			if (formBean.isNew()) {
				AccountShadowDto newAccountShadow = updateAccountAttributes(formBean);
				newAccountShadow.setAdded(true);
				user.getAccount().add(newAccountShadow);
			}
		}

		LOGGER.debug("Finished processing of new accounts");
	}

	private AccountShadowDto updateAccountAttributes(AccountFormBean bean) throws SchemaException {
		LOGGER.trace("updateAccountAttributes::begin");
		AccountShadowDto account = bean.getAccount();
		LOGGER.trace("Account {}, oid '{}'", new Object[] { account.getName(), account.getOid() });

		List<Element> attrList = new ArrayList<Element>();
		try {
			Document doc = DOMUtil.getDocument();

			List<FormAttribute> attributes = bean.getBean().getAttributes();
			for (FormAttribute attribute : attributes) {
				attribute.clearEmptyValues();
				if (attribute.getValuesSize() == 0) {
					continue;
				}

				FormAttributeDefinition definition = attribute.getDefinition();
				String namespace = definition.getElementName().getNamespaceURI();
				String name = definition.getElementName().getLocalPart();

				for (Object object : attribute.getValues()) {
					LOGGER.trace("Creating element: \\{{}\\}{}: {}",
							new Object[] { namespace, name, object.toString() });

					Element element = null;

					if (AttributeType.PASSWORD.equals(definition.getType())) {
						ProtectedStringType protectedString = protector.encryptString(object.toString());
						element = JAXBUtil.jaxbToDom(protectedString, definition.getElementName(), doc);
						element.setPrefix(MidPointNamespacePrefixMapper.getPreferredPrefix(namespace));
					} else {
						element = doc.createElementNS(namespace, name);
						element.setPrefix(MidPointNamespacePrefixMapper.getPreferredPrefix(namespace));
						element.setTextContent(object.toString());
					}
					attrList.add(element);
				}
			}

			if (bean.getDefaultAccountType() != null) {
				account.setObjectClass(bean.getDefaultAccountType());
			}
		} catch (Exception ex) {
			throw new SchemaException("Unknown error: Can't update account attributes: " + ex.getMessage(),
					ex);
		}
		account.setAttributes(attrList);

		ResourceCapability capabilities = bean.getResourceCapability();
		account.setCredentials(capabilities.getCredentialsType());
		account.setActivation(capabilities.getActivationType());

		LOGGER.trace("updateAccountAttributes::end");
		return account;
	}

	private List<AccountFormBean> createFormBeanList(List<AccountShadowDto> accounts, boolean createNew) {
		List<AccountFormBean> list = new ArrayList<AccountFormBean>();
		if (accounts != null) {
			for (AccountShadowDto account : accounts) {
				try {
					if (createNew) {
						list.add(generateForm(account, 0, createNew));
					} else {
						list.add(generateForm(account, account.getObjectClass(), 0, createNew));
					}
				} catch (SchemaException ex) {
					LoggingUtils.logException(LOGGER, "Can't parse schema for account {}", ex,
							new Object[] { account.getName() });
					FacesUtils.addErrorMessage("Can't parse schema for account '" + account.getName() + "'.",
							ex);
				}
			}
		}

		return list;
	}

	private AccountFormBean generateForm(AccountShadowDto account, int index, boolean createNew)
			throws SchemaException {
		return generateForm(account, null, index, createNew);
	}

	private AccountFormBean generateForm(AccountShadowDto account, QName accountType, int index,
			boolean createNew) throws SchemaException {
		if (account == null) {
			throw new IllegalArgumentException("Account object can't be null.");
		}

		FormObject object = null;
		ResourceCapability capability = null;
		try {
			SchemaFormParser parser = new SchemaFormParser();
			object = parser.parseSchemaForAccount(account, accountType);
			AccountManager manager = ControllerUtil.getAccountManager(objectTypeCatalog);
			capability = manager.getResourceCapability(account);
			// capability.setActivation(account.getActivation().isEnabled());
		} catch (SchemaException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new SchemaException("Unknown error, reason: " + ex.getMessage(), ex);
		}

		return new AccountFormBean(index, account, capability, object.getTypeName(), object, createNew);
	}

	private List<AccountShadowType> processDeletedAccounts() {
		// delete accounts are also processed as modification of user in one
		// operation deleted account are removed from the current user

		LOGGER.debug("Start processing of deleted accounts");
		List<AccountShadowType> accountsToDelete = new ArrayList<AccountShadowType>();
		for (AccountFormBean formBean : accountListDeleted) {
			String oidToDelete = formBean.getAccount().getOid();
			LOGGER.debug("Following account is marked as candidate for delete in GUI: {}",
					DebugUtil.prettyPrint(formBean.getAccount().getXmlObject()));

			List<AccountShadowDto> accountsDto = user.getAccount();
			for (Iterator<AccountShadowDto> i = accountsDto.iterator(); i.hasNext();) {
				AccountShadowDto account = i.next();
				if (StringUtils.equals(oidToDelete, account.getOid())) {
					i.remove();
					user.getXmlObject().getAccount().remove(account.getXmlObject());
					accountsToDelete.add(account.getXmlObject());
					break;
				}
			}
		}
		LOGGER.debug("Finished processing of deleted accounts");

		return accountsToDelete;
	}

	private void processUnlinkedAccounts() {
		LOGGER.debug("Start processing of unlinked accounts");
		for (AccountFormBean formBean : accountListUnlinked) {
			String oidToDelete = formBean.getAccount().getOid();
			LOGGER.debug("Following account is marked as candidate for unlink in GUI: {}",
					DebugUtil.prettyPrint(formBean.getAccount().getXmlObject()));
			List<AccountShadowType> accounts = user.getXmlObject().getAccount();
			for (Iterator<AccountShadowType> i = accounts.iterator(); i.hasNext();) {
				AccountShadowType account = i.next();
				if (StringUtils.equals(oidToDelete, account.getOid())) {
					i.remove();
				}
			}
		}
		LOGGER.debug("Finished processing of deleted accounts");
	}

	/*
	 * private AccountShadowDto updateAccountAttributes(AccountFormBean bean)
	 * throws SchemaException { LOGGER.trace("updateAccountAttributes::begin");
	 * AccountShadowDto account = bean.getAccount();
	 * LOGGER.trace("Account {}, oid '{}'", new Object[] { account.getName(),
	 * account.getOid() });
	 * 
	 * List<Element> attrList = new ArrayList<Element>(); try { Document doc =
	 * DOMUtil.getDocument();
	 * 
	 * List<FormAttribute> attributes = bean.getBean().getAttributes(); for
	 * (FormAttribute attribute : attributes) { attribute.clearEmptyValues(); if
	 * (attribute.getValuesSize() == 0) { continue; }
	 * 
	 * FormAttributeDefinition definition = attribute.getDefinition(); String
	 * namespace = definition.getElementName().getNamespaceURI(); String name =
	 * definition.getElementName().getLocalPart();
	 * 
	 * for (Object object : attribute.getValues()) {
	 * LOGGER.trace("Creating element: \\{{}\\}{}: {}", new Object[] {
	 * namespace, name, object.toString() });
	 * 
	 * Element element = null;
	 * 
	 * if (AttributeType.PASSWORD.equals(definition.getType())) {
	 * ProtectedStringType protectedString =
	 * protector.encryptString(object.toString()); element =
	 * JAXBUtil.jaxbToDom(protectedString, definition.getElementName(), doc);
	 * element
	 * .setPrefix(MidPointNamespacePrefixMapper.getPreferredPrefix(namespace));
	 * } else { element = doc.createElementNS(namespace, name);
	 * element.setPrefix
	 * (MidPointNamespacePrefixMapper.getPreferredPrefix(namespace));
	 * element.setTextContent(object.toString()); } attrList.add(element); } }
	 * 
	 * if (bean.getDefaultAccountType() != null) {
	 * account.setObjectClass(bean.getDefaultAccountType()); } } catch
	 * (Exception ex) { throw new
	 * SchemaException("Unknown error: Can't update account attributes: " +
	 * ex.getMessage(), ex); } account.setAttributes(attrList);
	 * 
	 * ResourceCapability capabilities = bean.getResourceCapability();
	 * account.setCredentials(capabilities.getCredentialsType());
	 * account.setActivation(capabilities.getActivationType());
	 * 
	 * LOGGER.trace("updateAccountAttributes::end"); return account; }
	 */
}
