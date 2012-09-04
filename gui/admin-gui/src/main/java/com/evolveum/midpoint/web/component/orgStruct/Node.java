/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */
package com.evolveum.midpoint.web.component.orgStruct;

import org.apache.wicket.model.IModel;

/**
 * @author mserbak
 */
public class Node<T extends NodeDto> extends StyledLinkLabel<T> {

	public Node(String id, IModel<T> model) {
		super(id, model);
	}

	protected boolean isSelected() {
		return false;
	}

	protected String getOtherStyleClass(T t) {
		switch (t.getType()) {
			case FOLDER:
				return "tree-folder-other folder";
			case BOSS:
				return "tree-folder-other folder_boss";
			case MANAGER:
				return "tree-folder-other folder_manager";
			case USER:
			default:
				return "tree-folder-other folder_user";
		}
	}

	protected String getClosedStyleClass() {
		return "tree-folder-closed";
	}

	protected String getOpenStyleClass() {
		return "tree-folder-open";
	}

	protected String getSelectedStyleClass() {
		return "selected";
	}

	protected String getButtonStyle(T t) {
		String buttonStyleClass;

		if (NodeType.FOLDER.equals(t.getType())) {
			buttonStyleClass = "treeButtonMenu orgUnitButton";
		} else {
			buttonStyleClass = "treeButtonMenu userButton";
		}
		return buttonStyleClass;
	}

	@Override
	protected String getButtonStyleClass() {
		T t = getModelObject();
		return getButtonStyle(t);
	}

	@Override
	protected String getStyleClass() {
		return null;
	}
}