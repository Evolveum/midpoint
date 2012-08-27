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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;

/**
 * @author mserbak
 */
public abstract class StyledLinkLabel<T> extends Panel {
	private static final StyleBehavior STYLE_CLASS = new StyleBehavior();
	private static final ButtonStyleBehavior BUTTON_STYLE_CLASS = new ButtonStyleBehavior();

	private static final long serialVersionUID = 1L;

	public StyledLinkLabel(String id, IModel<T> model) {
		super(id, model);

		MarkupContainer link = newLinkComponent("link", model);
		link.add(STYLE_CLASS);
		add(link);

		link.add(newLabelComponent("label", model));

		WebMarkupContainer treeButton = new WebMarkupContainer("treeButton");
		add(treeButton);

		treeButton.add(BUTTON_STYLE_CLASS);
	}

	@SuppressWarnings("unchecked")
	public IModel<T> getModel() {
		return (IModel<T>) getDefaultModel();
	}

	public T getModelObject() {
		return getModel().getObject();
	}

	/**
	 * Hook method to create a new link component.
	 * 
	 * This default implementation returns an {@link AjaxFallbackLink} which
	 * invokes {@link #onClick(AjaxRequestTarget)} only if
	 * {@link #isClickable()} returns <code>true</code>.
	 * 
	 * @see #isClickable()
	 * @see #onClick(AjaxRequestTarget)
	 */
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

	/**
	 * Hook method to create a new label component.
	 * 
	 * @param id
	 * @param model
	 * @return created component
	 * 
	 * @see #newLabelModel(IModel)
	 */
	protected Component newLabelComponent(String id, IModel<T> model) {
		return new Label(id, newLabelModel(model));
	}

	/**
	 * Create the model for the label, defaults to the model itself.
	 * 
	 * @param model
	 * @return wrapping model
	 */
	protected IModel<?> newLabelModel(IModel<T> model) {
		return model;
	}

	/**
	 * Get a style class for the link.
	 */
	protected abstract String getStyleClass();

	/**
	 * Get a style class for the button.
	 */
	protected abstract String getButtonStyleClass();

	/**
	 * Clicking is disabled by default, override this method if you want your
	 * link to be enabled.
	 * 
	 * @see #newLinkComponent(String, IModel)
	 * @see #isClickable()
	 */
	protected boolean isClickable() {
		return false;
	}

	/**
	 * Hook method to be notified of a click on the link.
	 * 
	 * @param target
	 * 
	 * @see #newLinkComponent(String, IModel)
	 * @see #isClickable()
	 */
	protected void onClick(AjaxRequestTarget target) {
	}

	/**
	 * Behavior to add a style class attribute to a contained link.
	 */
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

	/**
	 * Behavior to add a style class attribute to a contained button.
	 */
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
		response.renderOnLoadJavaScript("initOrgStruct()");
	}
}