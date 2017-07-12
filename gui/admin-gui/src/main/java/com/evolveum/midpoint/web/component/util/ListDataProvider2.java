/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.component.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;

/**
 * @author lazyman
 */
public class ListDataProvider2<W extends Serializable, T extends Serializable>
		extends BaseSortableDataProvider<W> {

	private IModel<List<T>> model;

	public ListDataProvider2(Component Component, IModel<List<T>> model) {
		super(Component);

		Validate.notNull(model);
		this.model = model;
	}

	@Override
	public Iterator<W> internalIterator(long first, long count) {
		getAvailableData().clear();

		List<T> list = model.getObject();
		if (list != null) {
			for (long i = first; i < first + count; i++) {
				if (i < 0 || i >= list.size()) {
					throw new ArrayIndexOutOfBoundsException(
							"Trying to get item on index " + i + " but list size is " + list.size());
				}
				getAvailableData().add(createObjectWrapper(list.get(WebComponentUtil.safeLongToInteger(i))));
			}
		}

		return getAvailableData().iterator();
	}

	protected W createObjectWrapper(T object) {
		return (W) new SelectableBean<T>(object);
	}

	@Override
	protected int internalSize() {
		List<T> list = model.getObject();
		if (list == null) {
			return 0;
		}

		return list.size();
	}

	public List<W> getSelectedObjects() {
		List<W> allSelected = new ArrayList<W>();
		for (Serializable s : super.getAvailableData()) {
			if (s instanceof SelectableBean) {
				SelectableBean<W> selectable = (SelectableBean<W>) s;
				if (selectable.isSelected() && selectable.getValue() != null) {
					allSelected.add(selectable.getValue());
				}
			}
		}

		return allSelected;
	}

}
