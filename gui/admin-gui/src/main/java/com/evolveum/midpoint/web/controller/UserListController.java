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

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.web.bean.GuiUserDtoList;
import com.evolveum.midpoint.web.dto.GuiUserDto;
import com.evolveum.midpoint.web.model.ObjectManager;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.PagingDto;
import com.evolveum.midpoint.web.model.UserDto;
import com.evolveum.midpoint.web.model.UserManager;
import com.evolveum.midpoint.web.model.WebModelException;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OrderDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;

/**
 * 
 * @author katuska
 */
@Controller("userList")
@Scope("session")
public class UserListController implements Serializable {

	public static final String PAGE_NAVIGATION_LIST = "/account/index?faces-redirect=true";
	public static final String PAGE_NAVIGATION_DETAILS = "/account/detailUser?faces-redirect=true";
	public static final String PAGE_NAVIGATION_DELETE = "/account/deleteUser?faces-redirect=true";
	private static final long serialVersionUID = -6520469747022260260L;
	private static final Trace TRACE = TraceManager.getTrace(UserListController.class);
	private static final String PARAM_USER_OID = "userOid";
	@Autowired(required = true)
	private transient UserDetailsController userDetailsController;
	@Autowired(required = true)
	private transient ObjectTypeCatalog objectTypeCatalog;
	private GuiUserDtoList userList = new GuiUserDtoList("name");
	private GuiUserDto user;
	private String searchOid;
	private boolean selectAll = false;
	private int offset = 0;
	private int rowsCount = 20;
	private boolean showPopup = false;

	public String showUserDetails() {
		String userOid = FacesUtils.getRequestParameter(PARAM_USER_OID);
		if (StringUtils.isEmpty(userOid)) {
			FacesUtils.addErrorMessage("Couldn't show user details, unidentified oid.");
			return null;
		}

		try {
			ObjectManager<UserDto> objectManager = objectTypeCatalog.getObjectManager(UserDto.class,
					GuiUserDto.class);
			UserManager userManager = (UserManager) (objectManager);
			TRACE.info("userSelectionListener start");
			user = (GuiUserDto) userManager.get(userOid, new PropertyReferenceListType());
			TRACE.info("userSelectionListener end");

			// TODO: handle exception
			userDetailsController.setUser(user);
		} catch (WebModelException ex) {
			TRACE.error("Can't select user, WebModelException error occured, reason: " + ex.getMessage());
			FacesUtils.addErrorMessage("Can't select user, WebModelException error occured.", ex);
		} catch (Exception ex) {
			TRACE.error("Can't select user, unknown error occured, reason: " + ex.getMessage());
			FacesUtils.addErrorMessage("Can't select user, unknown error occured.", ex);
		}

		return PAGE_NAVIGATION_DETAILS;
	}

	public void listUsers() {
		ObjectManager<UserDto> objectManager = objectTypeCatalog.getObjectManager(UserDto.class,
				GuiUserDto.class);
		UserManager userManager = (UserManager) (objectManager);

		userList.getUsers().clear();
		OrderDirectionType direction = OrderDirectionType.ASCENDING;
		if (!userList.isAscending()) {
			direction = OrderDirectionType.DESCENDING;
		}
		TRACE.debug("sortColumn name : {}", userList.getSortColumnName());
		try {
			Collection<UserDto> list = (Collection<UserDto>) userManager.list(new PagingDto(userList
					.getSortColumnName(), offset, rowsCount, direction));
			for (UserDto userDto : list) {
				GuiUserDto guiUserDto = (GuiUserDto) userDto;
				userList.getUsers().add(guiUserDto);
			}
		} catch (WebModelException ex) {
			TRACE.error("List users failed: ", ex);
			return;
		}
	}

	public void listLast() {
		offset = -1;
		listUsers();
	}

	public void listNext() {
		offset += rowsCount;
		listUsers();
	}

	public void listFirst() {
		offset = 0;
		listUsers();
	}

	public void listPrevious() {
		if (offset < rowsCount) {
			return;
		}
		offset -= rowsCount;
		listUsers();
	}

	public void deleteUsers() {
		showPopup = false;
		ObjectManager<UserDto> objectManager = objectTypeCatalog.getObjectManager(UserDto.class,
				GuiUserDto.class);
		UserManager userManager = (UserManager) (objectManager);
		for (GuiUserDto guiUserDto : userList.getUsers()) {
			TRACE.info("delete user {} is selected {}", guiUserDto.getFullName(), guiUserDto.isSelected());

			if (guiUserDto.isSelected()) {
				try {
					userManager.delete(guiUserDto.getOid());
				} catch (WebModelException ex) {
					TRACE.error("Delete user failed: {}", ex);
					FacesUtils.addErrorMessage("Delete user failed: " + ex.getMessage());
				}
			}

		}
		listUsers();
	}

	public void searchUser(ActionEvent evt) {
		if (searchOid == null || searchOid.isEmpty()) {
			FacesUtils.addErrorMessage("Can't search, reason: User ID must not be null or empty.");
			return;
		}
		ObjectManager<UserDto> objectManager = objectTypeCatalog.getObjectManager(UserDto.class,
				GuiUserDto.class);
		UserManager userManager = (UserManager) (objectManager);
		listUsers();
		if (null != searchOid) {
			GuiUserDto guiUserDto = null;
			try {
				guiUserDto = (GuiUserDto) (userManager.get(searchOid, new PropertyReferenceListType()));
			} catch (WebModelException ex) {
				TRACE.error("Get user with oid {} failed : {}", searchOid, ex);
				FacesUtils.addErrorMessage("Get user failed, reason: " + ex.getTitle() + ", "
						+ ex.getMessage());
				return;
			}
			userList.getUsers().clear();
			userList.getUsers().add(guiUserDto);
		}
	}

	public boolean isSelectAll() {
		return selectAll;
	}

	public void setSelectAll(boolean selectAll) {
		this.selectAll = selectAll;
	}

	public void selectAllPerformed(ValueChangeEvent event) {
		if (event.getPhaseId() != PhaseId.INVOKE_APPLICATION) {
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
		} else {
			boolean selectAll = ((Boolean) event.getNewValue()).booleanValue();
			for (GuiUserDto guiUser : userList.getUsers()) {
				guiUser.setSelected(selectAll);
			}
		}
	}

	public void selectPerformed(ValueChangeEvent evt) {
		if (evt.getPhaseId() != PhaseId.INVOKE_APPLICATION) {
			evt.setPhaseId(PhaseId.INVOKE_APPLICATION);
			evt.queue();
		} else {
			boolean selected = ((Boolean) evt.getNewValue()).booleanValue();
			if (!selected) {
				selectAll = false;
			} else {
				boolean selectedAll = true;
				for (GuiUserDto item : userList.getUsers()) {
					if (!item.isSelected()) {
						selectedAll = false;
						break;
					}
				}
				this.selectAll = selectedAll;
			}
		}
	}

	public void sortItem(ActionEvent e) {
		userList.sort();
	}

	public String deleteAction() {
		listUsers();
		return PAGE_NAVIGATION_DELETE;
	}

	public String fillTableList() {
		listUsers();
		return PAGE_NAVIGATION_LIST;
	}

	public GuiUserDtoList getUserList() {
		return userList;
	}

	public List<GuiUserDto> getUserData() {
		return userList.getUsers();
	}

	public GuiUserDto getUser() {
		return user;
	}

	public void setUser(GuiUserDto user) {
		this.user = user;
	}

	public String getSearchOid() {
		return searchOid;
	}

	public void setSearchOid(String searchOid) {
		this.searchOid = searchOid;
	}

	public int getRowsCount() {
		return rowsCount;
	}

	public void setRowsCount(int rowsCount) {
		this.rowsCount = rowsCount;
	}

	public boolean isShowPopup() {
		return showPopup;
	}

	public void hideConfirmDelete() {
		showPopup = false;
	}

	public void showConfirmDelete() {
		boolean selected = false;

		for (GuiUserDto user : userList.getUsers()) {
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
