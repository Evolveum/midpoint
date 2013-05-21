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

import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.page.admin.users.dto.OrgStructDto;

import wickettree.theme.WindowsTheme;
import wickettree.util.ProviderSubset;

/**
 * @author mserbak
 * 
 */
abstract class OptionalTree extends Panel{
	private OrgStructProvider provider;
	private AbstractTree<NodeDto> tree;
	private Set<NodeDto> state;
	private Content content;

	protected OptionalTree(String id, IModel<OrgStructDto> model) {
		super(id);
		this.provider = new OrgStructProvider(model);
		this.state = new ProviderSubset<NodeDto>(provider);
		tree = createTree(provider, newStateModel());
		tree.add(new Behavior() {

			@Override
			public void renderHead(Component component, IHeaderResponse response) {
				response.render(CssHeaderItem.forReference(new WindowsTheme()));
			}
		});
		content = new BookmarkableFolderContent();
		add(tree);
	}

	protected abstract AbstractTree<NodeDto> createTree(OrgStructProvider provider, IModel<Set<NodeDto>> state);

	private IModel<Set<NodeDto>> newStateModel() {
		return new AbstractReadOnlyModel<Set<NodeDto>>() {
			@Override
			public Set<NodeDto> getObject() {
				return state;
			}

			@Override
			public void detach() {
				((IDetachable) state).detach();
			}
		};
	}

	protected Component newContentComponent(String id, IModel<NodeDto> model) {
		return content.newContentComponent(id, tree, model);
	}
	
	protected Component newNodeComponent(String id, IModel<NodeDto> model) {
		return content.newNodeComponent(id, tree, model);
	}
}
