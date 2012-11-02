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

import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.page.admin.users.dto.OrgStructDto;

import wickettree.theme.HumanTheme;
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
				response.renderCSSReference(new WindowsTheme());
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
