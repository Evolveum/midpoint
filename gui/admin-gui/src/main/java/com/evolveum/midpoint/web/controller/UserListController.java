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
import com.evolveum.midpoint.web.bean.GuiUserDtoList;
import com.evolveum.midpoint.web.dto.GuiUserDto;
import com.evolveum.midpoint.web.model.*;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import com.icesoft.faces.component.ext.RowSelectorEvent;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import javax.faces.event.ActionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/**
 * 
 * @author katuska
 */
@Controller("userList")
@Scope("session")
public class UserListController implements Serializable {

	public static final String PAGE_NAVIGATION = "/account/index?faces-redirect=true";
	private static final long serialVersionUID = -6520469747022260260L;
	private static final Trace TRACE = TraceManager.getTrace(UserListController.class);
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

	public void userSelectionListener(RowSelectorEvent event) {
		try {
			ObjectManager<UserDto> objectManager = objectTypeCatalog.getObjectManager(UserDto.class,
					GuiUserDto.class);
			UserManager userManager = (UserManager) (objectManager);
			TRACE.info("userSelectionListener start");
			String userId = userList.getUsers().get(event.getRow()).getOid();
			user = (GuiUserDto) userManager.get(userId, new PropertyReferenceListType());
			TRACE.info("userSelectionListener end");

			// TODO: handle exception
			userDetailsController.setUser(user);
		} catch (WebModelException ex) {
			TRACE.error("Can't select user, WebModelException error occured, reason: " + ex.getMessage());
			FacesUtils.addErrorMessage("Can't select user, WebModelException error occured, reason: "
					+ ex.getMessage());
		} catch (Exception ex) {
			TRACE.error("Can't select user, unknown error occured, reason: " + ex.getMessage());
			FacesUtils
					.addErrorMessage("Can't select user, unknown error occured, reason: " + ex.getMessage());
		}
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

	public String selectAll() {
		if (selectAll) {
			selectAll = false;
		} else {
			selectAll = true;
		}
		for (GuiUserDto guiUser : userList.getUsers()) {
			guiUser.setSelected(selectAll);
		}
		TRACE.info("setSelectedAll value");
		return "/account/deleteUser";
	}

	public void sortItem(ActionEvent e) {
		userList.sort();
	}

	public String deleteAction() {
		listUsers();
		return "/account/deleteUser";
	}

	public String fillTableList() {
		listUsers();
		return PAGE_NAVIGATION;
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
