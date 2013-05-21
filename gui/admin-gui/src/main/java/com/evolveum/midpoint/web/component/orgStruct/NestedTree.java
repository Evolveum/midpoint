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
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;

import com.evolveum.midpoint.web.component.orgStruct.nested.BranchItem;
import com.evolveum.midpoint.web.component.orgStruct.nested.Subtree;

import wickettree.ITreeProvider;

/**
 * A tree with nested markup.
 * 
 * @author Sven Meier
 */
public abstract class NestedTree<T> extends AbstractTree<T> {

	private static final long serialVersionUID = 1L;

	public NestedTree(String id, ITreeProvider<T> provider) {
		this(id, provider, null);
	}

	public NestedTree(String id, ITreeProvider<T> provider, IModel<Set<T>> state) {
		super(id, provider, state);

		add(newSubtree("subtree", new RootsModel()));
	}

	/**
	 * Create a new subtree.
	 * 
	 * @param id
	 *            component id
	 * @param model
	 *            the model of the new subtree
	 */
	public Component newSubtree(String id, IModel<T> model) {
		return new Subtree<T>(id, this, model);
	}

	/**
	 * Overriden to update the affected {@link BranchItem} only.
	 */
	@Override
	public void updateBranch(T t, final AjaxRequestTarget target) {
		if (target != null) {
			final IModel<T> model = getProvider().model(t);
			visitChildren(BranchItem.class, new IVisitor<BranchItem<T>, Void>() {
				public void component(BranchItem<T> branch, IVisit<Void> visit) {
					if (model.equals(branch.getModel())) {
						target.add(branch);
						visit.stop();
					}
				}
			});
			model.detach();
		}
	}

	private class RootsModel extends AbstractReadOnlyModel<T> {
		private static final long serialVersionUID = 1L;

		@Override
		public T getObject() {
			return null;
		}
	}
}
