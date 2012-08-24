/**
 * 
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

import wickettree.AbstractTree;
import wickettree.theme.HumanTheme;
import wickettree.theme.WindowsTheme;
import wickettree.util.ProviderSubset;

/**
 * @author mserbak
 * 
 */
abstract class OptionalTree extends Panel{
	private NodeProvider provider = new NodeProvider();
	private AbstractTree<NodeDto> tree;
	private Set<NodeDto> state = new ProviderSubset<NodeDto>(provider);
	private Content content;

	protected OptionalTree(String id) {
		super(id);
		tree = createTree(provider, newStateModel());
		tree.add(new Behavior() {
			@Override
			public void renderHead(Component component, IHeaderResponse response) {
				response.renderCSSReference(new WindowsTheme());
			}
		});
		content = new BookmarkableFolderContent(tree);
		add(tree);
	}

	protected abstract AbstractTree<NodeDto> createTree(NodeProvider provider, IModel<Set<NodeDto>> state);

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

	public AbstractTree<NodeDto> getTree() {
		return tree;
	}

	protected Component newContentComponent(String id, IModel<NodeDto> model) {
		return content.newContentComponent(id, tree, model);
	}
}
