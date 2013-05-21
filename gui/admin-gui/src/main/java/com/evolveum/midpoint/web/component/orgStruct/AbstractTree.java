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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.DefaultItemReuseStrategy;
import org.apache.wicket.markup.repeater.IItemReuseStrategy;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;

import wickettree.ITreeProvider;
import wickettree.util.ProviderSubset;

/**
 * Abstract base class for {@link NestedTree} and {@link TableTree}. Uses its
 * model for storing the {@link State} of its nodes.
 * 
 * Note that a tree has no notion of a <em>selection</em>. Handling state of
 * nodes besides expanse/collapse is irrelevant to a tree implementation.
 * 
 * @see #newContentComponent(String, IModel)
 * 
 * @author Sven Meier
 */
public abstract class AbstractTree<T> extends Panel {
	private static final long serialVersionUID = 1L;

	private ITreeProvider<T> provider;

	private IItemReuseStrategy itemReuseStrategy;

	protected AbstractTree(String id, ITreeProvider<T> provider) {
		this(id, provider, null);
	}

	protected AbstractTree(String id, ITreeProvider<T> provider, IModel<Set<T>> state) {
		super(id, state);

		if (provider == null) {
			throw new IllegalArgumentException("argument [provider] cannot be null");
		}
		this.provider = provider;

		// see #updateBranch(Object, AjaxRequestTarget)
		setOutputMarkupId(true);
	}

	/**
	 * Sets the item reuse strategy. This strategy controls the creation of
	 * {@link Item}s.
	 * 
	 * @see IItemReuseStrategy
	 * 
	 * @param strategy
	 *            item reuse strategy
	 * @return this for chaining
	 */
	public AbstractTree<T> setItemReuseStrategy(IItemReuseStrategy strategy) {
		this.itemReuseStrategy = strategy;

		return this;
	}

	/**
	 * @return currently set item reuse strategy. Defaults to
	 *         <code>DefaultItemReuseStrategy</code> if none was set.
	 * 
	 * @see DefaultItemReuseStrategy
	 */
	public IItemReuseStrategy getItemReuseStrategy() {
		if (itemReuseStrategy == null) {
			return DefaultItemReuseStrategy.getInstance();
		}
		return itemReuseStrategy;
	}

	/**
	 * Get the provider of the tree nodes.
	 * 
	 * @return provider
	 */
	public ITreeProvider<T> getProvider() {
		return provider;
	}

	/**
	 * Delegate to {@link #newModel()} if none is inited in super
	 * implementation.
	 */
	@Override
	protected IModel<?> initModel() {
		IModel<?> model = super.initModel();

		if (model == null) {
			model = newModel();
		}

		return model;
	}

	/**
	 * Factory method for a model, by default creates a model containing a
	 * {@link ProviderSubset}.
	 */
	protected IModel<Set<T>> newModel() {
		return new ProviderSubset<T>(provider).createModel();
	}

	@SuppressWarnings("unchecked")
	public IModel<Set<T>> getModel() {
		return (IModel<Set<T>>) getDefaultModel();
	}

	public Set<T> getModelObject() {
		return getModel().getObject();
	}

	public AbstractTree<T> setModel(IModel<Set<T>> state) {
		setDefaultModel(state);

		return this;
	}

	public AbstractTree<T> setModelObject(Set<T> state) {
		setDefaultModelObject(state);

		return this;
	}

	/**
	 * Expand the given node, tries to update the affected branch if the change
	 * happens on an {@link AjaxRequestTarget}.
	 * 
	 * @see #getModelObject()
	 * @see Set#add(Object)
	 * @see #updateBranch(Object, AjaxRequestTarget)
	 */
	public void expand(T t) {
		getModelObject().add(t);

		updateBranch(t, getRequestCycle().find(AjaxRequestTarget.class));
	}

	/**
	 * Collapse the given node, tries to update the affected branch if the
	 * change happens on an {@link AjaxRequestTarget}.
	 * 
	 * @see #getModelObject()
	 * @see Set#remove(Object)
	 * @see #updateBranch(Object, AjaxRequestTarget)
	 */
	public void collapse(T t) {
		getModelObject().remove(t);

		updateBranch(t, getRequestCycle().find(AjaxRequestTarget.class));
	}

	/**
	 * Get the given node's {@link State}.
	 * 
	 * @see #getModelObject()
	 * @see Set#contains(Object)
	 */
	public State getState(T t) {
		if (getModelObject().contains(t)) {
			return State.EXPANDED;
		} else {
			return State.COLLAPSED;
		}
	}

	/**
	 * Overriden to detach the {@link ITreeProvider}.
	 */
	@Override
	protected void onDetach() {
		provider.detach();

		super.onDetach();
	}

	/**
	 * Create a new component for a node.
	 */
	public Component newNodeComponent(String id, final IModel<T> model) {

		return new TreeNode<T>(id, this, model) {

			@Override
			protected Component createContent(String id, IModel<T> model) {
				return AbstractTree.this.newContentComponent(id, model);
			}
			
			
		};

	}

	/**
	 * Create a new component for the content of a node.
	 */
	protected abstract Component newContentComponent(String id, IModel<T> model);

	/**
	 * Convenience method to update a single branch on an
	 * {@link AjaxRequestTarget}. Does nothing if the given node is currently
	 * not visible or target is <code>null</code>.
	 * 
	 * This default implementation adds this whole component for rendering.
	 * 
	 * @param t
	 * @param target
	 */
	public void updateBranch(T t, final AjaxRequestTarget target) {
		if (target != null) {
			target.add(this);
		}
	}

	/**
	 * Convenience method to update a single node on an
	 * {@link AjaxRequestTarget}. Does nothing if the given node is currently
	 * not visible or target is <code>null</code>.
	 * 
	 * @param t
	 * @param target
	 */
	public void updateNode(T t, final AjaxRequestTarget target) {
		if (target != null) {
			final IModel<T> model = getProvider().model(t);
			visitChildren(TreeNode.class, new IVisitor<TreeNode<T>, Void>() {

				@Override
				public void component(TreeNode<T> node, IVisit<Void> visit) {
					if (model.equals(node.getModel())) {
						target.add(node);
						visit.stop();
					}
					visit.dontGoDeeper();

				}
			});
			model.detach();
		}
	}

	public static enum State {
		COLLAPSED, EXPANDED
	}
}