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
package com.evolveum.midpoint.web.controller.account;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttributeDefinition;
import com.evolveum.midpoint.schema.processor.SchemaProcessorException;
import com.evolveum.midpoint.web.bean.AccountFormBean;
import com.evolveum.midpoint.web.controller.TemplateController;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.jsf.form.AttributeType;
import com.evolveum.midpoint.web.jsf.form.FormAttribute;
import com.evolveum.midpoint.web.jsf.form.FormAttributeDefinition;
import com.evolveum.midpoint.web.jsf.form.FormAttributeDefinition.Flag;
import com.evolveum.midpoint.web.jsf.form.FormAttributeDefinitionBuilder;
import com.evolveum.midpoint.web.jsf.form.FormObject;
import com.evolveum.midpoint.web.model.AccountManager;
import com.evolveum.midpoint.web.model.ObjectManager;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.ResourceManager;
import com.evolveum.midpoint.web.model.UserManager;
import com.evolveum.midpoint.web.model.WebModelException;
import com.evolveum.midpoint.web.model.dto.AccountShadowDto;
import com.evolveum.midpoint.web.model.dto.GuiResourceDto;
import com.evolveum.midpoint.web.model.dto.GuiUserDto;
import com.evolveum.midpoint.web.model.dto.PropertyChange;
import com.evolveum.midpoint.web.model.dto.ResourceDto;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.web.util.SchemaFormParser;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author lazyman
 */
@Controller("userDetails")
@Scope("session")
public class UserDetailsController implements Serializable {

	public static final String PAGE_NAVIGATION = "/account/userDetails?faces-redirect=true";
	private static final long serialVersionUID = -4537350724118181063L;
	private static final Trace TRACE = TraceManager.getTrace(UserDetailsController.class);
	private static final String TAB_USER = "0";
	@Autowired(required = true)
	private transient TemplateController template;
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

		// we are going to work with user details, we will get it's fresh
		// version from model because we need resolved accountRefs to accounts
		try {
			this.user = user;

			if (this.user != null) {
				accountList = createFormBeanList(this.user.getAccount(), false);
				getAvailableResourceList().clear();
				availableResourceList = createResourceList(this.user.getAccount());
			}
		} catch (Exception ex) {
			LoggingUtils.logException(TRACE, "Couldn't create account list for user {}", ex, user.getName());
			FacesUtils.addErrorMessage("Couldn't create account list for user.", ex);
		}
	}

	private UserManager getUserManager() {
		ObjectManager<GuiUserDto> objectManager = objectTypeCatalog.getObjectManager(UserType.class,
				GuiUserDto.class);
		return (UserManager) (objectManager);
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
		template.setSelectedLeftId("leftList");

		return UserListController.PAGE_NAVIGATION;
	}

	/**
	 * TODO: remove account from user which are in accountListDeleted save
	 * accounts from accountList save user attributes from form
	 */
	public void savePerformed(ActionEvent evt) {
		try {
			UserManager userManager = getUserManager();
			AccountManager accountManager = ControllerUtil.getAccountManager(objectTypeCatalog);

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

		} catch (SchemaProcessorException ex) {
			TRACE.error("Dynamic form generator error", ex);
			// TODO: What action should we fire in GUI if error occurs ???
			String loginFailedMessage = FacesUtils.translateKey("save.failed");
			FacesUtils.addErrorMessage(loginFailedMessage + " " + ex.toString());

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
			} catch (SchemaProcessorException ex) {
				throw new WebModelException("Failed to update account attributes, reason: " + ex.getMessage()
						+ ".", "Failed to update account attributes.", ex);
			}

			AccountManager accountManager = ControllerUtil.getAccountManager(objectTypeCatalog);
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
			ObjectManager<GuiUserDto> objectManager = objectTypeCatalog.getObjectManager(UserType.class,
					GuiUserDto.class);
			UserManager userManager = (UserManager) (objectManager);
			AccountShadowDto account = userManager.addAccount(user, resource.getOid());
			if (account == null) {
				continue;
			}
			account.setName(resource.getName() + "-" + user.getName());

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
		ResourceManager resManager = ControllerUtil.getResourceManager(objectTypeCatalog);

		List<ResourceDto> resources = new ArrayList<ResourceDto>();
		try {
			Collection<ResourceDto> list = resManager.list();
			if (list != null) {
				resources.addAll(list);
			}
		} catch (Exception ex) {
			LoggingUtils.logException(TRACE, "Couldn't list resources", ex);
			FacesUtils.addErrorMessage("Couldn't list resources.", ex);
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
				} catch (SchemaProcessorException ex) {
					LoggingUtils.logException(TRACE, "Can't parse schema for account {}", ex,
							new Object[] { account.getName() });
					FacesUtils.addErrorMessage("Can't parse schema for account '" + account.getName() + "'.",
							ex);
				}
			}
		}

		return list;
	}

	private AccountFormBean generateForm(AccountShadowDto account, int index, boolean createNew)
			throws SchemaProcessorException {
		return generateForm(account, null, index, createNew);
	}

	private AccountFormBean generateForm(AccountShadowDto account, QName accountType, int index,
			boolean createNew) throws SchemaProcessorException {
		if (account == null) {
			throw new IllegalArgumentException("Account object can't be null.");
		}

		FormObject object = new FormObject();
		QName defaultAccountType = null;
		try {
			SchemaFormParser parser = new SchemaFormParser();
			List<ResourceObjectAttributeDefinition> list = parser.parseSchemaForAccount(account, accountType);
			object.setDisplayName(parser.getDisplayName());
			Map<QName, List<Object>> formValues = parser.getAttributeValueMap();
			for (ResourceObjectAttributeDefinition attribute : list) {
				FormAttributeDefinition definition = createDefinition(attribute);
				List<Object> values = formValues.get(attribute.getName());

				object.getAttributes().add(new FormAttribute(definition, values));
			}
			defaultAccountType = parser.getDefaultAccountType();
		} catch (SchemaProcessorException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new SchemaProcessorException("Unknown error, reason: " + ex.getMessage(), ex);
		}

		return new AccountFormBean(index, account, defaultAccountType, object, createNew);
	}

	private FormAttributeDefinition createDefinition(ResourceObjectAttributeDefinition def) {
		FormAttributeDefinitionBuilder builder = new FormAttributeDefinitionBuilder();
		if (def.getAllowedValues() != null) {
			List<Object> availableValues = new ArrayList<Object>();
			availableValues.addAll(Arrays.asList(def.getAllowedValues()));
			builder.setAvailableValues(availableValues);
		}
		builder.setDescription(def.getHelp());

		if (StringUtils.isEmpty(def.getDisplayName())) {
			builder.setDisplayName(def.getName().getLocalPart());
		} else {
			builder.setDisplayName(def.getDisplayName());
		}
		builder.setElementName(def.getName());
		if (!def.isReadable() || !def.isUpdateable()) {
			if (def.isReadable()) {
				builder.addFlag(Flag.READ);
			}
			if (def.isUpdateable()) {
				builder.addFlag(Flag.UPDATE);
				builder.addFlag(Flag.CREATE);
			}
		}
		builder.setMaxOccurs(def.getMaxOccurs());
		builder.setMinOccurs(def.getMinOccurs());
		builder.setType(AttributeType.getType(def.getTypeName()));
		// builder.setFilledWithExpression(def.isFilledWithExpression());
		// //TODO: where can I get this?????

		return builder.build();
	}

	private AccountShadowDto updateAccountAttributes(AccountFormBean bean) throws SchemaProcessorException {
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
			throw new SchemaProcessorException("Unknown error: Can't update account attributes: "
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
