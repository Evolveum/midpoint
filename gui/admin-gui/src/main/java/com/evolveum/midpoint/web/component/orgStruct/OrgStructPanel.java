/**
 * 
 */
package com.evolveum.midpoint.web.component.orgStruct;

import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;

import wickettree.AbstractTree;
import wickettree.NestedTree;

/**
 * @author mserbak
 * 
 */
public class OrgStructPanel extends OptionalTree {
	private NestedTree<NodeDto> tree;

	public OrgStructPanel(String id, final IModel<String> model) {
		super(id);
		add(new Label("body", model));
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);

		response.renderCSSReference(new PackageResourceReference(OrgStructPanel.class, "OrgStructPanel.css"));
		response.renderJavaScriptReference(new PackageResourceReference(OrgStructPanel.class,
				"OrgStructPanel.js"));
		response.renderOnLoadJavaScript("initOrgStruct()");
	}

	protected AbstractTree<NodeDto> createTree(NodeProvider provider, IModel<Set<NodeDto>> state) {
		tree = new NestedTree<NodeDto>("tabletree", provider, state) {

			@Override
			protected Component newContentComponent(String id, IModel<NodeDto> model) {
				return OrgStructPanel.this.newContentComponent(id, model);
			}
		};
		return tree;
	}
}
