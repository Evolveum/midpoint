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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.schema.PagingTypeFactory;
import com.evolveum.midpoint.web.controller.TemplateController;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.controller.util.SearchableListController;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.UserManager;
import com.evolveum.midpoint.web.model.WebModelException;
import com.evolveum.midpoint.web.model.dto.GuiUserDto;
import com.evolveum.midpoint.web.model.dto.UserDto;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.web.util.GuiUserDtoComparator;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OrderDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;

/**
 * 
 * @author katuska
 */
@Controller("userList")
@Scope("session")
public class UserListController extends SearchableListController<GuiUserDto> {

	public static final String PAGE_NAVIGATION = "/account/index?faces-redirect=true";
	public static final String PAGE_NAVIGATION_DELETE = "/account/deleteUser?faces-redirect=true";
	private static final long serialVersionUID = -6520469747022260260L;
	private static final Trace TRACE = TraceManager.getTrace(UserListController.class);
	private static final String PARAM_USER_OID = "userOid";
	@Autowired(required = true)
	private transient UserDetailsController userDetailsController;
	@Autowired(required = true)
	private transient TemplateController template;
	@Autowired(required = true)
	private transient ObjectTypeCatalog objectTypeCatalog;
	private GuiUserDto user;
	private String searchName;
	private boolean selectAll = false;
	private boolean showPopup = false;

	public UserListController() {
		super("name");
	}

	public String showUserDetails() {
		String userOid = FacesUtils.getRequestParameter(PARAM_USER_OID);
		if (StringUtils.isEmpty(userOid)) {
			FacesUtils.addErrorMessage("Couldn't show user details, unidentified oid.");
			return null;
		}

		try {
			UserManager userManager = ControllerUtil.getUserManager(objectTypeCatalog);

			TRACE.info("userSelectionListener start");
			PropertyReferenceListType resolve = new PropertyReferenceListType();
			resolve.getProperty().add(Utils.fillPropertyReference("Account"));
			
			user = (GuiUserDto) userManager.get(userOid, resolve);
			TRACE.info("userSelectionListener end");
			
			userDetailsController.setUser(user);			
		} catch (Exception ex) {
			LoggingUtils.logException(TRACE, "Can't select user, unknown error occured", ex);
			FacesUtils.addErrorMessage("Can't select user, unknown error occured.", ex);
		}

		template.setSelectedLeftId("leftUserDetails");

		return UserDetailsController.PAGE_NAVIGATION;
	}

	public void deleteUsers() {
		showPopup = false;
		for (GuiUserDto guiUserDto : getObjects()) {
			TRACE.info("delete user {} is selected {}", guiUserDto.getFullName(), guiUserDto.isSelected());

			if (guiUserDto.isSelected()) {
				try {
					UserManager userManager = ControllerUtil.getUserManager(objectTypeCatalog);
					userManager.delete(guiUserDto.getOid());
				} catch (Exception ex) {
					LoggingUtils.logException(TRACE, "Delete user failed", ex);
					FacesUtils.addErrorMessage("Delete user failed: " + ex.getMessage());
				}
			}

		}
		listFirst();
	}

	public void searchUser(ActionEvent evt) {
		if (StringUtils.isEmpty(searchName)) {
			setQuery(null);
		} else {
			setQuery(createQuery(searchName));
		}

		listFirst();
	}

	@Override
	protected String listObjects() {
		UserManager userManager = ControllerUtil.getUserManager(objectTypeCatalog);

		OrderDirectionType direction = OrderDirectionType.ASCENDING;
		if (!isAscending()) {
			direction = OrderDirectionType.DESCENDING;
		}
		PagingType paging = PagingTypeFactory.createPaging(getOffset(), getRowsCount(), direction,
				getSortColumnName());

		getObjects().clear();
		if (getQuery() == null) {
			// we're listing objects
			// try {
			Collection<UserDto> list = (Collection<UserDto>) userManager.list(paging);
			for (UserDto userDto : list) {
				getObjects().add((GuiUserDto) userDto);
			}
			// } catch (WebModelException ex) {
			// LoggingUtils.logException(TRACE, "List users failed", ex);
			// // TODO: faces utils error add
			// }
		} else {
			// we're searching for objects
			OperationResult result = new OperationResult("Search");
			try {
				List<UserDto> users = userManager.search(getQuery(), paging, result);
				for (UserDto userDto : users) {
					getObjects().add((GuiUserDto) userDto);
				}
			} catch (WebModelException ex) {
				LoggingUtils.logException(TRACE, "Couldn't search user with name {}", ex, searchName);
				// TODO: error handling
			}
		}

		return null;
	}

	private QueryType createQuery(String name) {
		QueryType query = new QueryType();
		query.setFilter(ControllerUtil.createQuery(name, ObjectTypes.USER));

		return query;
	}

	@Override
	protected void sort() {
		Collections.sort(getObjects(), new GuiUserDtoComparator(getSortColumnName(), isAscending()));
	}

	public boolean isSelectAll() {
		return selectAll;
	}

	public void setSelectAll(boolean selectAll) {
		this.selectAll = selectAll;
	}

	public void selectAllPerformed(ValueChangeEvent event) {
		ControllerUtil.selectAllPerformed(event, getObjects());
	}

	public void selectPerformed(ValueChangeEvent evt) {
		this.selectAll = ControllerUtil.selectPerformed(evt, getObjects());
	}

	public void sortItem(ActionEvent e) {
		sort();
	}

	public String deleteAction() {
		listFirst();
		return PAGE_NAVIGATION_DELETE;
	}

	public String fillTableList() {
		listFirst();

		template.setSelectedLeftId("leftList");
		return PAGE_NAVIGATION;
	}

	public GuiUserDto getUser() {
		return user;
	}

	public void setUser(GuiUserDto user) {
		this.user = user;
	}

	public String getSearchName() {
		return searchName;
	}

	public void setSearchName(String searchName) {
		this.searchName = searchName;
	}

	public boolean isShowPopup() {
		return showPopup;
	}

	public void hideConfirmDelete() {
		showPopup = false;
	}

	public void showConfirmDelete() {
		boolean selected = false;

		for (GuiUserDto user : getObjects()) {
			if (user != null && user.isSelected()) {
				selected = true;
				break;
			}
		}

		if (selected) {
			showPopup = true;
		} else {
			FacesUtils.addErrorMessage("No user selected.");
		}
	}
}
