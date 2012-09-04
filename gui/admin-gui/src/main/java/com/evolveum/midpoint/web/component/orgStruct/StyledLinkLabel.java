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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;

import com.evolveum.midpoint.web.component.util.LoadableModel;

/**
 * @author mserbak
 */
public abstract class StyledLinkLabel<T extends NodeDto> extends Panel {
	private static final StyleBehavior STYLE_CLASS = new StyleBehavior();
	private static final ButtonStyleBehavior BUTTON_STYLE_CLASS = new ButtonStyleBehavior();

	public StyledLinkLabel(String id, IModel<T> model) {
		super(id, model);
		MarkupContainer link = newLinkComponent("link", model);
		link.add(STYLE_CLASS);
		add(link);

		link.add(newLabelComponent("label", model));

		WebMarkupContainer treeButton = new WebMarkupContainer("treeButton");
		treeButton.setOutputMarkupId(true);
		add(treeButton);
		treeButton.add(BUTTON_STYLE_CLASS);
		
		createMenu(treeButton.getMarkupId(), model);
	}

	@SuppressWarnings("unchecked")
	public IModel<NodeDto> getModel() {
		return (IModel<NodeDto>) getDefaultModel();
	}

	public T getModelObject() {
		return (T) getModel().getObject();
	}

	protected MarkupContainer newLinkComponent(String id, IModel<T> model) {
		return new AjaxFallbackLink<Void>(id) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isEnabled() {
				return StyledLinkLabel.this.isClickable();
			}

			@Override
			public void onClick(AjaxRequestTarget target) {
				StyledLinkLabel.this.onClick(target);
			}
		};
	}

	protected Component newLabelComponent(String id, IModel<T> model) {
		return new Label(id, newLabelModel(model));
	}

	protected IModel<String> newLabelModel(final IModel<T> model) {
		return new LoadableModel<String>() {

			@Override
			protected String load() {
				NodeDto dto = model.getObject();
				return dto.getDisplayName();
			}
			
		};
	}

	protected abstract String getStyleClass();

	protected abstract String getButtonStyleClass();

	protected boolean isClickable() {
		NodeDto t = getModelObject();
		return t.getType().equals(NodeType.FOLDER);
	}

	protected void onClick(AjaxRequestTarget target) {
	}

	private static class StyleBehavior extends Behavior {

		@Override
		public void onComponentTag(Component component, ComponentTag tag) {
			StyledLinkLabel<?> parent = (StyledLinkLabel<?>) component.getParent();

			String styleClass = parent.getStyleClass();
			if (styleClass != null) {
				tag.put("class", styleClass);
			}
		}
	}

	private static class ButtonStyleBehavior extends Behavior {

		@Override
		public void onComponentTag(Component component, ComponentTag tag) {
			StyledLinkLabel<?> parent = (StyledLinkLabel<?>) component.getParent();

			String styleClass = parent.getButtonStyleClass();
			if (styleClass != null) {
				tag.put("class", styleClass);
			}
		}
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);

		response.renderCSSReference(new PackageResourceReference(OrgStructPanel.class, "StyledLinkLabel.css"));
		response.renderJavaScriptReference(new PackageResourceReference(OrgStructPanel.class,
				"StyledLinkLabel.js"));
		response.renderOnLoadJavaScript("initMenuButtons()");
	}
	
	private void createMenu(String id, IModel<T> model) {
		WebMarkupContainer panel = new WebMarkupContainer("menuPanel");
        ListView<String> menu = new ListView<String>("menu", createMenuItemModel(model)) {

            @Override
            protected void populateItem(ListItem<String> item) {
                item.add(new Label("menuItem", item.getModel()));
            }
        };
        panel.add(menu);
        add(panel);
        panel.setMarkupId(id + "_panel");
    }
	
	protected IModel<List<String>> createMenuItemModel(final IModel<T> model) {
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
}