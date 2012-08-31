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

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.form.LabeledWebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.util.LoadableModel;

import wickettree.AbstractTree;

/**
 * @author mserbak
 */
public class BookmarkableFolderContent extends Content {

	public BookmarkableFolderContent(final AbstractTree<NodeDto> tree) {
	}

	@Override
	public Component newContentComponent(String id, final AbstractTree<NodeDto> tree, IModel<NodeDto> model) {
		return new Node<NodeDto>(id, tree, model) {
			private static final long serialVersionUID = 1L;

			@Override
			protected MarkupContainer newLinkComponent(String id, IModel<NodeDto> model) {
				NodeDto node = model.getObject();

				if (tree.getProvider().hasChildren(node)) {
					return super.newLinkComponent(id, model);
				} else {
					return new LabeledWebMarkupContainer(id, model) {
					};
				}
			}

			@Override
			protected String getOtherStyleClass(NodeDto user) {
				switch (user.getType()) {
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

			@Override
			protected IModel<List<String>> createMenuItemModel(final IModel<NodeDto> model) {
				return new LoadableModel<List<String>>() {

					@Override
					protected List<String> load() {
						List<String> list = new ArrayList<String>();
						NodeDto dto = model.getObject();
						if (NodeType.FOLDER.equals(dto.getType())) {
							list.add("Edit");
							list.add("Rename");
							list.add("Create sub-unit");
							list.add("Delete / Deprecate");
						} else {
							list.add("Edit");
							list.add("Move");
							list.add("Rename");
							list.add("Enable");
							list.add("Disable");
							list.add("Change attributes");
						}
						return list;
					}
				};
			}

			@Override
			protected String getButtonStyle(NodeDto t) {
				String buttonStyleClass;

				if (NodeType.FOLDER.equals(t.getType())) {
					buttonStyleClass = "treeButtonMenu orgUnitButton";
				} else {
					buttonStyleClass = "treeButtonMenu userButton";
				}
				return buttonStyleClass;
			}
		};
	}
}