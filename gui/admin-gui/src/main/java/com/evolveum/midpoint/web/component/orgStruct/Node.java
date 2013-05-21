/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
			case MANAGER:
				return "tree-folder-other folder_manager";
			case MEMBER:
				return "tree-folder-other folder_member";
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