/**
 * Copyright (c) 2016 Evolveum
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

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.web.page.error.PageOperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.model.Model;

/**
 * @author semancik
 *
 */
public class ObjectNameColumn<O extends ObjectType> extends AbstractColumn<SelectableBean<O>, String>
		implements IExportableColumn<SelectableBean<O>, String>{
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(ObjectNameColumn.class);

	public ObjectNameColumn(IModel<String> displayModel) {
		super(displayModel, ObjectType.F_NAME.getLocalPart());
	}

	public ObjectNameColumn(IModel<String> displayModel, String itemPath) {
		super(displayModel, itemPath);
	}

	@Override
	public void populateItem(final Item<ICellPopulator<SelectableBean<O>>> cellItem, String componentId,
			final IModel<SelectableBean<O>> rowModel) {

		IModel<String> labelModel = new AbstractReadOnlyModel<String>() {
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject() {
				SelectableBean<O> selectableBean = rowModel.getObject();
				O value = selectableBean.getValue();
				if (value == null) {
					OperationResult result = selectableBean.getResult();
					OperationResultStatusPresentationProperties props = OperationResultStatusPresentationProperties.parseOperationalResultStatus(result.getStatus());
					return cellItem.getString(props.getStatusLabelKey());
				} else {
					String name = WebComponentUtil.getName(value);
					if (selectableBean.getResult() != null){
						StringBuilder complexName = new StringBuilder();
						complexName.append(name);
						complexName.append(" (");
						complexName.append(selectableBean.getResult().getStatus());
						complexName.append(")");
						return complexName.toString();
					}
						return name;


				}
			}
		};

		if (isClickable(rowModel)) {		// beware: rowModel is very probably resolved at this moment; but it seems to cause no problems
			cellItem.add(new LinkPanel(componentId, labelModel) {
				private static final long serialVersionUID = 1L;

				@Override
				public void onClick(AjaxRequestTarget target) {
					SelectableBean<O> selectableBean = rowModel.getObject();
					O value = selectableBean.getValue();
					if (value == null) {
						OperationResult result = selectableBean.getResult();
						throw new RestartResponseException(new PageOperationResult(result));
					} else {
						if (selectableBean.getResult() != null) {
							throw new RestartResponseException(new PageOperationResult(selectableBean.getResult()));
						} else {
							ObjectNameColumn.this.onClick(target, rowModel);
						}
					}
				}
			});
		} else {
			cellItem.add(new Label(componentId, labelModel));
		}
	}

	public boolean isClickable(IModel<SelectableBean<O>> rowModel) {
        return true;
    }

    public void onClick(AjaxRequestTarget target, IModel<SelectableBean<O>> rowModel) {
    }

	@Override
    public IModel<String> getDataModel(IModel<SelectableBean<O>> rowModel) {
		SelectableBean<O> selectableBean = rowModel.getObject();
		O value = selectableBean.getValue();
		return Model.of(value == null ? "" : WebComponentUtil.getName(value));
	}

}
