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

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.web.component.util.SerializableFunction;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

/**
 * @author mederly
 */
public class GenericColumn<T, S> extends AbstractColumn<T, S> implements IExportableColumn<T, S>
{
	private static final long serialVersionUID = 1L;

	private final SerializableFunction<IModel<T>, IModel<?>> dataModelProvider;

	public GenericColumn(IModel<String> displayModel, S sortProperty, SerializableFunction<IModel<T>, IModel<?>> dataModelProvider) {
		super(displayModel, sortProperty);
		this.dataModelProvider = dataModelProvider;
	}

	public GenericColumn(IModel<String> displayModel, SerializableFunction<IModel<T>, IModel<?>> dataModelProvider) {
		this(displayModel, null, dataModelProvider);
	}

	@Override
	public void populateItem(Item<ICellPopulator<T>> item, String componentId, IModel<T> rowModel) {
		item.add(new Label(componentId, getDataModel(rowModel)));
	}

	@Override
	public IModel<?> getDataModel(IModel<T> rowModel) {
		return dataModelProvider.apply(rowModel);
	}
}
