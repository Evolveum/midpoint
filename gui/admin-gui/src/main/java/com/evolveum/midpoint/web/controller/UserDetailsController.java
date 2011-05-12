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

package com.evolveum.midpoint.web.controller;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.provisioning.schema.ResourceAttributeDefinition;
import com.evolveum.midpoint.provisioning.schema.util.SchemaParserException;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Utils;
import com.evolveum.midpoint.web.bean.AccountFormBean;
import com.evolveum.midpoint.web.component.form.AttributeType;
import com.evolveum.midpoint.web.component.form.FormAttribute;
import com.evolveum.midpoint.web.component.form.FormAttributeDefinition;
import com.evolveum.midpoint.web.component.form.FormAttributeDefinition.Flag;
import com.evolveum.midpoint.web.component.form.FormAttributeDefinitionBuilder;
import com.evolveum.midpoint.web.component.form.FormObject;
import com.evolveum.midpoint.web.dto.GuiAccountShadowDto;
import com.evolveum.midpoint.web.dto.GuiResourceDto;
import com.evolveum.midpoint.web.dto.GuiUserDto;
import com.evolveum.midpoint.web.model.AccountShadowDto;
import com.evolveum.midpoint.web.model.AccountShadowManager;
import com.evolveum.midpoint.web.model.ObjectManager;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.PropertyChange;
import com.evolveum.midpoint.web.model.ResourceDto;
import com.evolveum.midpoint.web.model.ResourceManager;
import com.evolveum.midpoint.web.model.UserDto;
import com.evolveum.midpoint.web.model.UserManager;
import com.evolveum.midpoint.web.model.WebModelException;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.web.util.SchemaFormParser;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * 
 * @author lazyman
 */
@Controller("userDetails")
@Scope("session")
public class UserDetailsController implements Serializable {

	private static final long serialVersionUID = -4537350724118181063L;
	private static final Trace TRACE = TraceManager.getTrace(UserDetailsController.class);
	private static final String TAB_USER = "0";
	private static final String TAB_ACCOUNTS = "1";
	@Autowired(required = true)
	private ObjectTypeCatalog objectTypeCatalog;
	private boolean editMode = false;
	private GuiUserDto user;
	private List<AccountFormBean> accountList;
	private List<AccountFormBean> accountListDeleted = new ArrayList<AccountFormBean>();
	private List<SelectItem> availableResourceList;
	private List<String> selectedResourceList;
	private String selectedTab = TAB_USER;

	public String getSelectedTab() {
		return selectedTab;
	}

	public void setSelectedTab(String selectedTab) {
		this.selectedTab = selectedTab;
	}

	public boolean isEditMode() {
		return editMode;
	}

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

	public int getSize() {
		return getAccountList().size();
	}

	public List<String> getSelectedResourceList() {
		if (selectedResourceList == null) {
			selectedResourceList = new ArrayList<String>();
		}

		return selectedResourceList;
	}

	public void setSelectedResourceList(List<String> selectedResourceList) {
		this.selectedResourceList = selectedResourceList;
	}

	public List<SelectItem> getAvailableResourceList() {
		if (availableResourceList == null) {
			availableResourceList = new ArrayList<SelectItem>();
		}

		return availableResourceList;
	}

	public GuiUserDto getUser() {
		return user;
	}

	public void setUser(GuiUserDto user) {
		editMode = false;

		// if we are going to work with user details, we will get it's fresh
		// version from model
		// Requirement: we will need resolved accountRefs to accounts
		if (null != user) {
			ObjectManager<UserDto> objectManager = objectTypeCatalog.getObjectManager(UserDto.class,
					GuiUserDto.class);
			UserManager userManager = (UserManager) (objectManager);
			try {
				this.user = (GuiUserDto) userManager.get(user.getOid(), Utils.getResolveResourceList());
				accountList = createFormBeanList(this.user.getAccount(), false);
				getAvailableResourceList().clear();
				availableResourceList = createResourceList(this.user.getAccount());
			} catch (WebModelException ex) {
				StringBuilder message = new StringBuilder();
				message.append("Get user failed. Reason: ");
				message.append(ex.getTitle());
				message.append(" (");
				message.append(ex.getMessage());
				message.append(").");
				FacesUtils.addErrorMessage(message.toString());
			}
		} else {
			this.user = user;
		}
	}

	public void startEditMode(ActionEvent evt) {
		editMode = true;
	}

	private void clearController() {
		selectedTab = TAB_USER;
		editMode = false;
		user = null;

		getAccountList().clear();
		if (accountListDeleted != null) {
			accountListDeleted.clear();
		}
		getAvailableResourceList().clear();
		getSelectedResourceList().clear();
	}

	public String backPerformed() {
		clearController();

		return "/account/listUser";
	}

	/**
	 * TODO: remove account from user which are in accountListDeleted save
	 * accounts from accountList save user attributes from form
	 */
	public void savePerformed(ActionEvent evt) {
		try {
			// for add account we have to call method modify for User Object
			ObjectManager<UserDto> usrManager = objectTypeCatalog.getObjectManager(UserDto.class,
					GuiUserDto.class);
			UserManager userManager = (UserManager) (usrManager);
			ObjectManager<AccountShadowDto> accManager = objectTypeCatalog.getObjectManager(
					AccountShadowDto.class, GuiAccountShadowDto.class);
			AccountShadowManager accountManager = (AccountShadowManager) (accManager);

			// new accounts are processed as modification of user in one
			// operation
			TRACE.debug("Start processing of new accounts");
			for (AccountFormBean formBean : accountList) {
				if (formBean.isNew()) {
					// Note: we have to add new account directly to xmlObject
					// add and delete of accounts will be process later by call
					// to userManager.submit(user);
					AccountShadowType newAccountShadowType = (AccountShadowType) updateAccountAttributes(
							formBean).getXmlObject();
					TRACE.debug("Found new account in GUI: {}", DebugUtil.prettyPrint(newAccountShadowType));
					((UserType) user.getXmlObject()).getAccount().add(newAccountShadowType);
				}
			}
			TRACE.debug("Finished processing of new accounts");

			// delete accounts are also processed as modification of user in one
			// operation
			TRACE.debug("Start processing of deleted accounts");
			for (AccountFormBean formBean : accountListDeleted) {
				String oidToDelete = formBean.getAccount().getOid();
				TRACE.debug("Following account is marked as candidate for delete in GUI: {}",
						DebugUtil.prettyPrint(formBean.getAccount().getXmlObject()));
				List<AccountShadowType> accounts = ((UserType) user.getXmlObject()).getAccount();
				for (AccountShadowType account : accounts) {
					if (StringUtils.equals(oidToDelete, account.getOid())) {
						accounts.remove(account);
						accountManager.delete(account.getOid());
						break;
					}
				}
			}
			TRACE.debug("Finished processing of deleted accounts");

			TRACE.debug("Submit user modified in GUI");
			Set<PropertyChange> userChanges = userManager.submit(user);
			TRACE.debug("Modified user in GUI submitted ");
			if (userChanges.isEmpty()) {
				// account changes are processed as modification of account,
				// every account is processed separately
				TRACE.debug("Start processing of modified accounts");
				for (AccountFormBean formBean : accountList) {
					if (!formBean.isNew()) {
						AccountShadowDto modifiedAccountShadowDto = updateAccountAttributes(formBean);
						TRACE.debug("Found modified account in GUI: {}",
								DebugUtil.prettyPrint(modifiedAccountShadowDto.getXmlObject()));
						TRACE.debug("Submit account modified in GUI");
						accountManager.submit(modifiedAccountShadowDto);
						TRACE.debug("Modified account in GUI submitted");
					}
				}
				TRACE.debug("Finished processing of modified accounts");
			} else {
				updateAccounts(accountList);
			}

			// action is done in clearController
			// accountListDeleted.clear();
			clearController();

			FacesUtils.addSuccessMessage("Save changes successfully.");

		} catch (SchemaParserException ex) {
			TRACE.error("Dynamic form generator error", ex);
			// TODO: What action should we fire in GUI if error occurs ???
			String loginFailedMessage = FacesUtils.getBundleKey("msg", "save.failed");
			FacesUtils.addErrorMessage(loginFailedMessage + " " + ex.toString());

			return;
		} catch (WebModelException ex) {
			TRACE.error("Web error {} : {}", ex.getTitle(), ex.getMessage());
			// TODO: What action should we fire in GUI if error occurs ???
			StringBuilder message = new StringBuilder();
			message.append(FacesUtils.getBundleKey("msg", "save.failed"));
			message.append(" Reason: ");
			message.append(ex.getTitle());
			message.append(" (");
			message.append(ex.getMessage());
			message.append(").");
			FacesUtils.addErrorMessage(message.toString());

			return;
		} catch (Exception ex) {
			// should not be here, it's only because bad error handling
			TRACE.error("Unknown error occured during save operation, reason: {}.", ex.getMessage());
			TRACE.trace("Unknown error occured during save operation.", ex);
			FacesUtils.addErrorMessage("Unknown error occured during save operation, reason: "
					+ ex.getMessage());
		}

		return;
	}

	private void updateAccounts(List<AccountFormBean> accountBeans) throws WebModelException {
		TRACE.debug("Start processing accounts with outbound schema handling");
		for (AccountFormBean bean : accountBeans) {
			if (bean.isNew()) {
				continue;
			}

			AccountShadowDto account = null;
			try {
				account = updateAccountAttributes(bean);
			} catch (SchemaParserException ex) {
				throw new WebModelException("Failed to update account attributes, reason: " + ex.getMessage()
						+ ".", "Failed to update account attributes.", ex);
			}

			ObjectManager<AccountShadowDto> accManager = objectTypeCatalog.getObjectManager(
					AccountShadowDto.class, GuiAccountShadowDto.class);
			AccountShadowManager accountManager = (AccountShadowManager) (accManager);
			accountManager.submit(account);
		}
		TRACE.debug("Finished processing accounts with outbound schema handling");
	}

	public void addResourcePerformed(ActionEvent evt) {
		if (selectedResourceList == null) {
			return;
		}

		List<ResourceDto> selectedResources = new ArrayList<ResourceDto>();

		// TODO: handle exception
		List<ResourceDto> resources = listResources();
		for (String resName : selectedResourceList) {
			for (ResourceDto resource : resources) {
				if (resource.getName().equals(resName)) {
					selectedResources.add(resource);
					break;
				}
			}
		}
		selectedResourceList.clear();

		if (selectedResources.isEmpty()) {
			return;
		}

		List<AccountShadowDto> newAccounts = new ArrayList<AccountShadowDto>();
		for (ResourceDto resource : selectedResources) {
			ObjectManager<UserDto> objectManager = objectTypeCatalog.getObjectManager(UserDto.class,
					GuiUserDto.class);
			UserManager userManager = (UserManager) (objectManager);
			AccountShadowDto account = null;
			try {
				account = userManager.addAccount(user, resource.getOid());
			} catch (WebModelException ex) {
				StringBuilder message = new StringBuilder();
				message.append("Error occured. Reason: ");
				message.append(ex.getTitle());
				message.append(" (");
				message.append(ex.getMessage());
				message.append(").");
				FacesUtils.addErrorMessage(message.toString());
			}

			// TODO: HACK
			try {
				setAccountDetails(account);
			} catch (Exception ex) {
				FacesUtils.addErrorMessage("Unknown error: Can't update account attributes: "
						+ ex.getMessage());
			}
			newAccounts.add(account);
		}
		getAccountList().addAll(createFormBeanList(newAccounts, true));

		// update available resource list
		getAvailableResourceList().clear();
		List<AccountShadowDto> existingAccounts = new ArrayList<AccountShadowDto>();
		for (AccountFormBean form : accountList) {
			existingAccounts.add(form.getAccount());
		}

		availableResourceList = createResourceList(existingAccounts);
	}

	private AccountShadowDto setAccountDetails(AccountShadowDto accountShadow)
			throws ParserConfigurationException {
		String resourceNamespace = accountShadow.getResource().getNamespace();
		// TODO: hack - setting default values for account and some account's
		// attributes
		accountShadow.setName(accountShadow.getResource().getName() + "-" + user.getName());
		List<Element> attributes = new ArrayList<Element>();

		TRACE.info("starting DocumnetBuilderFactory");
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		dbf.setValidating(false);
		DocumentBuilder db = dbf.newDocumentBuilder();
		// attributes/uid = user.name
		Document doc = db.newDocument();
		Element element = doc.createElementNS(resourceNamespace, "uid");
		element.setPrefix("ns76");
		element.setTextContent(user.getName());
		attributes.add(element);
		// attributes/name = "uid="+user.name+",ou=people,ou=example,ou=com"
		// !!! another namespace
		doc = db.newDocument();
		element = doc.createElementNS(
				"http://midpoint.evolveum.com/xml/ns/public/resource/idconnector/resource-schema-1.xsd",
				"__NAME__");
		element.setPrefix("ns75");
		//
		// element.setTextContent("uid=" + user.getName() +
		// ",ou=people,dc=example,dc=com");
		// attributes.add(element);
		// attributes/cn = user.fullName
		doc = db.newDocument();
		element = doc.createElementNS(resourceNamespace, "cn");
		element.setPrefix("ns76");
		element.setTextContent(user.getFullName());
		attributes.add(element);
		// attributes/sn = user.familyName
		doc = db.newDocument();
		element = doc.createElementNS(resourceNamespace, "sn");
		element.setPrefix("ns76");
		element.setTextContent(user.getFamilyName());
		attributes.add(element);
		// attributes/givenName = user.givenName
		doc = db.newDocument();
		element = doc.createElementNS(resourceNamespace, "givenName");
		element.setPrefix("ns76");
		element.setTextContent(user.getGivenName());
		attributes.add(element);
		TRACE.info("ending Documnet BuilderFactory");

		accountShadow.setAttributes(attributes);
		return accountShadow;
	}

	public void removeResourcePerformed(ActionEvent evt) {
		Integer formBeanId = (Integer) evt.getComponent().getAttributes().get("beanId");
		if (formBeanId == null) {
			return;
		}

		AccountFormBean formBean = null;
		for (AccountFormBean bean : accountList) {
			if (formBeanId == bean.getId()) {
				formBean = bean;
				break;
			}
		}

		if (formBean == null) {
			return;
		}

		accountList.remove(formBean);
		accountListDeleted.add(formBean);
	}

	private List<SelectItem> createResourceList(List<AccountShadowDto> existingAccounts) {
		List<SelectItem> list = new ArrayList<SelectItem>();

		List<ResourceDto> resources = listAvailableResources(listResources(), existingAccounts);
		for (ResourceDto resourceDto : resources) {
			SelectItem si = new SelectItem((GuiResourceDto) resourceDto);
			list.add(si);
		}

		return list;
	}

	private List<ResourceDto> listResources() {
		ObjectManager<ResourceDto> manager = objectTypeCatalog.getObjectManager(ResourceDto.class,
				GuiResourceDto.class);
		ResourceManager resManager = (ResourceManager) (manager);

		List<ResourceDto> resources = new ArrayList<ResourceDto>();
		try {
			Collection<ResourceDto> list = resManager.list();
			if (list != null) {
				resources.addAll(list);
			}
		} catch (WebModelException ex) {
			StringBuilder message = new StringBuilder();
			message.append(FacesUtils.getBundleKey("msg", "resource.list.failed"));
			message.append(" Reason: ");
			message.append(ex.getTitle());
			message.append(" (");
			message.append(ex.getMessage());
			message.append(").");
			FacesUtils.addErrorMessage(message.toString());
		}

		return resources;
	}

	private List<ResourceDto> listAvailableResources(List<ResourceDto> resources,
			List<AccountShadowDto> accounts) {
		if (resources == null) {
			return null;
		}
		if (accounts == null || accounts.isEmpty()) {
			return resources;
		}

		List<ResourceDto> list = new ArrayList<ResourceDto>();
		list.addAll(resources);

		List<ResourceDto> toDelete = new ArrayList<ResourceDto>();
		for (int i = 0; i < list.size(); i++) {
			for (AccountShadowDto accountDto : accounts) {
				if (list.get(i).getOid().equals(accountDto.getResource().getOid())) {
					toDelete.add(list.get(i));
				}
			}
		}
		list.removeAll(toDelete);

		return list;
	}

	private int getFormBeanNextId() {
		int maxId = getMaxId(accountList, 0);
		maxId = getMaxId(accountListDeleted, maxId);
		maxId++;

		return maxId;
	}

	private int getMaxId(List<AccountFormBean> list, int maxId) {
		if (list != null) {
			for (AccountFormBean bean : list) {
				if (maxId < bean.getId()) {
					maxId = bean.getId();
				}
			}
		}

		return maxId;
	}

	private List<AccountFormBean> createFormBeanList(List<AccountShadowDto> accounts, boolean createNew) {
		int maxId = getFormBeanNextId();

		List<AccountFormBean> list = new ArrayList<AccountFormBean>();
		if (accounts != null) {
			for (AccountShadowDto account : accounts) {
				try {
					if (createNew) {
						list.add(generateForm(account, maxId++, createNew));
					} else {
						list.add(generateForm(account, account.getObjectClass(), maxId++, createNew));
					}
				} catch (SchemaParserException ex) {
					TRACE.error("Can't parse schema for account '{}': {}", new Object[] { account.getName(),
							ex.getMessage(), ex });
					FacesContext context = FacesContext.getCurrentInstance();
					context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
							"Can't parse schema for account '" + account.getName() + "'.",
							"Can't parse schema for account '" + account.getName() + "': " + ex.getMessage()));
				}
			}
		}

		return list;
	}

	private AccountFormBean generateForm(AccountShadowDto account, int index, boolean createNew)
			throws SchemaParserException {
		return generateForm(account, null, index, createNew);
	}

	private AccountFormBean generateForm(AccountShadowDto account, QName accountType, int index,
			boolean createNew) throws SchemaParserException {
		if (account == null) {
			throw new IllegalArgumentException("Account object can't be null.");
		}

		FormObject object = new FormObject();
		QName defaultAccountType = null;
		try {
			SchemaFormParser parser = new SchemaFormParser();
			List<ResourceAttributeDefinition> list = parser.parseSchemaForAccount(account, accountType);
			object.setDisplayName(parser.getDisplayName());
			Map<QName, List<Object>> formValues = parser.getAttributeValueMap();
			for (ResourceAttributeDefinition attribute : list) {
				FormAttributeDefinition definition = createDefinition(attribute);
				List<Object> values = formValues.get(attribute.getQName());

				object.getAttributes().add(new FormAttribute(definition, values));
			}
			defaultAccountType = parser.getDefaultAccountType();
		} catch (SchemaParserException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new SchemaParserException("Unknown error: " + ex.getMessage(), ex);
		}

		return new AccountFormBean(index, account, defaultAccountType, object, createNew);
	}

	private FormAttributeDefinition createDefinition(ResourceAttributeDefinition def) {
		FormAttributeDefinitionBuilder builder = new FormAttributeDefinitionBuilder();
		if (def.getRestriction() != null && def.getRestriction().getEnumeration() != null) {
			List<Object> availableValues = new ArrayList<Object>();
			availableValues.addAll(Arrays.asList(def.getRestriction().getEnumeration()));
			builder.setAvailableValues(availableValues);
		}
		builder.setDescription(def.getHelp());
		builder.setDisplayName(def.getAttributeDisplayName());
		builder.setElementName(def.getQName());
		if (!def.canRead() || !def.canUpdate()) {
			if (def.canRead()) {
				builder.addFlag(Flag.READ);
			}
			if (def.canUpdate()) {
				builder.addFlag(Flag.UPDATE);
				builder.addFlag(Flag.CREATE);
			}
		}
		builder.setMaxOccurs(def.getMaxOccurs());
		builder.setMinOccurs(def.getMinOccurs());
		builder.setType(AttributeType.getType(def.getType()));
		builder.setFilledWithExpression(def.isFilledWithExpression());

		return builder.build();
	}

	private AccountShadowDto updateAccountAttributes(AccountFormBean bean) throws SchemaParserException {
		TRACE.trace("updateAccountAttributes::begin");
		AccountShadowDto account = bean.getAccount();
		TRACE.trace("Account " + account);

		List<Element> attrList = new ArrayList<Element>();
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			dbf.setValidating(false);
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.newDocument();

			Map<String, String> prefixMap = new HashMap<String, String>();
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
					TRACE.trace("Creating element: {" + namespace + "}" + name + ": " + object.toString());

					Element element = doc.createElementNS(namespace, name);
					element.setPrefix(buildElementName(namespace, prefixMap));
					element.setTextContent(object.toString());
					attrList.add(element);
				}
			}

			if (bean.getDefaultAccountType() != null) {
				account.setObjectClass(bean.getDefaultAccountType());
			}
		} catch (Exception ex) {
			throw new SchemaParserException("Unknown error: Can't update account attributes: "
					+ ex.getMessage(), ex);
		}
		account.setAttributes(attrList);

		TRACE.trace("updateAccountAttributes::end");
		return account;
	}

	private String buildElementName(String namespace, Map<String, String> prefixMap) {
		String prefix = prefixMap.get(namespace);
		if (prefix == null) {
			prefix = "af" + prefixMap.size();
			prefixMap.put(namespace, prefix);
		}

		return prefix;
	}
}
