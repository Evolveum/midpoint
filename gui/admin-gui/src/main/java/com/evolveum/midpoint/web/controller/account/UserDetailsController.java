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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.model.security.api.PrincipalUser;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.namespace.MidPointNamespacePrefixMapper;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.DebugUtil;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.bean.AccountFormBean;
import com.evolveum.midpoint.web.bean.ResourceCapability;
import com.evolveum.midpoint.web.controller.TemplateController;
import com.evolveum.midpoint.web.controller.util.AssignmentEditor;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.jsf.form.AttributeType;
import com.evolveum.midpoint.web.jsf.form.FormAttribute;
import com.evolveum.midpoint.web.jsf.form.FormAttributeDefinition;
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
import com.evolveum.midpoint.web.model.dto.UserDto;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.web.util.SchemaFormParser;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author lazyman
 * 
 */
@Controller("userDetails")
@Scope("session")
public class UserDetailsController implements Serializable {

	public static final String PAGE_NAVIGATION = "/admin/account/userDetails?faces-redirect=true";
	private static final long serialVersionUID = -4537350724118181063L;
	private static final Trace LOGGER = TraceManager.getTrace(UserDetailsController.class);
	private static final String TAB_USER = "0";
	
	@Autowired(required = true)
	private transient TemplateController template;
	@Autowired(required = true)
	private transient AssignmentEditor<UserDto> assignmentEditor;
	@Autowired(required = true)
	private transient ObjectTypeCatalog objectTypeCatalog;
	@Autowired(required = true)
	private transient Protector protector;
	@Autowired(required = true)
	private transient TaskManager taskManager;
	
	private boolean editMode = false;
	private GuiUserDto user;
	private List<AccountFormBean> accountList;
	private List<AccountFormBean> accountListDeleted = new ArrayList<AccountFormBean>();
	private List<AccountFormBean> accountListUnlinked = new ArrayList<AccountFormBean>();
	private List<SelectItem> availableResourceList;
	private List<String> selectedResourceList;
	private String selectedTab = TAB_USER;

	public Integer getAccountListSize(){
		return accountList.size();
	}
	
	public Integer getRoleListSize(){
		return assignmentEditor.getContainsAssignment().getAssignments().size();
	}
	
	public AssignmentEditor<UserDto> getAssignmentEditor() {
		return assignmentEditor;
	}

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
				
				assignmentEditor.initController(user);
			}
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't create account list for user {}", ex, user.getName());
			FacesUtils.addErrorMessage("Couldn't create account list for user.", ex);
		}
	}

	public String startEditMode() {
		editMode = true;
		return PAGE_NAVIGATION;
	}

	private void clearController() {
		selectedTab = TAB_USER;
		editMode = false;
		user = null;

		getAccountList().clear();
		if (accountListDeleted != null) {
			accountListDeleted.clear();
		}
		if (accountListUnlinked != null) {
			accountListUnlinked.clear();
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
		Task task = taskManager.createTaskInstance("Save User Changes");
		SecurityUtils security = new SecurityUtils();
		PrincipalUser principal = security.getPrincipalUser();
        task.setOwner(principal.getUser());
		OperationResult result = task.getResult();
		
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

			// we want submit only user changes (if the account was deleted-
			// unlink account, if added - link and create account)
			// modification of account attributes are submited later ->
			// updateAccounts method
			LOGGER.debug("Submit user modified in GUI");
			Set<PropertyChange> userChanges = userManager.submit(user, task, result);
			LOGGER.debug("Modified user in GUI submitted ");

			// now we need to delete accounts from repository and also from
			// external systems..
			LOGGER.debug("Start processing of deleted accounts");
			for (AccountShadowType account : accountsToDelete) {
				accountManager.delete(account.getOid());
			}
			LOGGER.debug("Finished processing of deleted accounts");
			
			//check if account was changed, if does, execute them..
			updateAccounts(accountList, task, result);
			
			clearController();
			result.recordSuccess();
//			FacesUtils.addSuccessMessage("Changes saved successfully.");
		} catch (SchemaException ex) {
			LOGGER.error("Dynamic form generator error", ex);
			// TODO: What action should we fire in GUI if error occurs ???
			String loginFailedMessage = FacesUtils.translateKey("save.failed");
			result.recordFatalError(loginFailedMessage + " " + ex.toString());
//			FacesUtils.addErrorMessage(loginFailedMessage + " " + ex.toString());
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Unknown error occured during save operation", ex);
			result.recordFatalError("Unknown error occured during save operation, reason: "
					+ ex.getMessage());
//			FacesUtils.addErrorMessage("Unknown error occured during save operation, reason: "
//					+ ex.getMessage());
		} finally{
			result.computeStatus("Failed to save changes.");
			ControllerUtil.printResults(LOGGER, result, "Changes saved sucessfully.");
		}
	}

	private void processNewAccounts() throws SchemaException {
		// new accounts are processed as modification of user in one operation
		LOGGER.debug("Start processing of new accounts");
		for (AccountFormBean formBean : accountList) {
			if (formBean.isNew()) {
				
				AccountShadowDto newAccountShadow = updateAccountAttributes(formBean);
				newAccountShadow.setAdded(true);
				//add new account to the userDto accounts only
				//later by calling user.submit, this added account will be checked, and only those which are new 
				//will be used to detect chages..other will be transformed to the accounts ref..
				user.getAccount().add(newAccountShadow);
			}
		}

		LOGGER.debug("Finished processing of new accounts");
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
			List<AccountShadowDto> accountsDto = user.getAccount();
			for (Iterator<AccountShadowDto> i = accountsDto.iterator(); i.hasNext();) {
				AccountShadowDto account = i.next();
				if (StringUtils.equals(oidToDelete, account.getOid())) {
					i.remove();
					user.getXmlObject().getAccount().remove(account.getXmlObject());
					break;
				}
			}
		}
		LOGGER.debug("Finished processing of deleted accounts");
	}

	private void updateAccounts(List<AccountFormBean> accountBeans, Task task, OperationResult result) throws WebModelException {
        if (accountBeans == null) {
            return;
        }
		LOGGER.debug("Start processing accounts with outbound schema handling");
		for (AccountFormBean bean : accountBeans) {
			if (bean.isNew()) {
				continue;
			}

			AccountShadowDto account = null;
			try {
				account = updateAccountAttributes(bean);
			} catch (SchemaException ex) {
				result.recordFatalError("Failed to update account attributes, reason: " + ex.getMessage()
						+ ".", ex);
				throw new WebModelException("Failed to update account attributes, reason: " + ex.getMessage()
						+ ".", "Failed to update account attributes.", ex);
			}

			AccountManager accountManager = ControllerUtil.getAccountManager(objectTypeCatalog);
			accountManager.submit(account, task, result);
		}
		LOGGER.debug("Finished processing accounts with outbound schema handling");
	}

	public void changeActivationOfUserAccounts(ValueChangeEvent evt) {

		// GUI should not change account activation. Model will do it.
		
//		Boolean newValue = (Boolean) evt.getNewValue();
//		// System.out.println("new value: "+ newValue);
//		for (AccountFormBean account : accountList) {
//			account.getResourceCapability().setEnabled(newValue);
//		}
	}

	public void addResourcePerformed(ActionEvent evt) {
		if (selectedResourceList == null) {
			return;
		}

		try {
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
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't add account", ex);
			FacesUtils.addErrorMessage("Couldn't add account.", ex);
		}
	}

	private AccountFormBean getAccountFormBean(ActionEvent evt) {
		Integer formBeanId = (Integer) evt.getComponent().getAttributes().get("beanId");
		if (formBeanId == null) {
			return null;
		}

		AccountFormBean formBean = null;
		for (AccountFormBean bean : accountList) {
			if (formBeanId == bean.getId()) {
				formBean = bean;
				break;
			}
		}

		return formBean;
	}

	public void removeResourcePerformed(ActionEvent evt) {
		AccountFormBean formBean = getAccountFormBean(evt);
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
			Collection<GuiResourceDto> list = resManager.list();
			if (list != null) {
				resources.addAll(list);
			}
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't list resources", ex);
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

	public void unlinkResourcePerformed(ActionEvent evt) {
		AccountFormBean formBean = getAccountFormBean(evt);
		if (formBean == null) {
			return;
		}

		accountList.remove(formBean);
		accountListUnlinked.add(formBean);
	}
}
