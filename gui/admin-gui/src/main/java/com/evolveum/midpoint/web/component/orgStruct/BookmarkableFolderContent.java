/*
 * Copyright 2009 Sven Meier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.component.orgStruct;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.form.LabeledWebMarkupContainer;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import wickettree.AbstractTree;
import wickettree.content.Folder;

/**
 * @author Sven Meier
 */
public class BookmarkableFolderContent extends Content {

	private static final long serialVersionUID = 1L;

	public BookmarkableFolderContent(final AbstractTree<NodeDto> tree) {
		String id = tree.getRequest().getRequestParameters().getParameterValue("foo").toString();
		if (id != null) {
			NodeDto foo = NodeProvider.get(id);
			while (foo != null) {
				tree.getModel().getObject().add(foo);
				foo = foo.getParent();
			}
		}
	}

	@Override
	public Component newContentComponent(String id, final AbstractTree<NodeDto> tree, IModel<NodeDto> model) {
		return new Folder<NodeDto>(id, tree, model) {
			private static final long serialVersionUID = 1L;

			@Override
			protected MarkupContainer newLinkComponent(String id, IModel<NodeDto> model) {
				NodeDto foo = model.getObject();

				if (tree.getProvider().hasChildren(foo)) {
					return super.newLinkComponent(id, model);
				} else {
					PageParameters parameters = new PageParameters();
					parameters.add("foo", foo.getId());
					return new LabeledWebMarkupContainer(id, model) {
					};
				}
			}

			@Override
			protected String getOtherStyleClass(NodeDto user) {
				switch (user.getType()) {
					case BOSS:
						return "tree-folder-other folder_boss";
					case MANAGER:
						return "tree-folder-other folder_manager";
					case USER:
					default:
						return "tree-folder-other folder_user";
				}
			}
		};
	}
}