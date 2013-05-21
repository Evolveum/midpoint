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
package com.evolveum.midpoint.web.component.orgStruct.nested;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

/**
 * A branch is a container for a single node and its children inside a
 * {@link Subtree}.
 * 
 * @see Subtree#newBranchItem(String, int, IModel)
 * 
 * @author Sven Meier
 */
public final class BranchItem<T> extends Item<T> {

	private static final long serialVersionUID = 1L;

	public BranchItem(String id, int index, IModel<T> model) {
		super(id, index, model);

		setOutputMarkupId(true);
	}

	@Override
	protected void onComponentTag(ComponentTag tag) {
		super.onComponentTag(tag);

		if (isLast()) {
			tag.put("class", "tree-branch tree-branch-last");
		} else {
			tag.put("class", "tree-branch tree-branch-mid");
		}
	}

	private boolean isLast() {
		return getIndex() == getParent().size() - 1;
	}
}