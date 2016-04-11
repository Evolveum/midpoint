package com.evolveum.midpoint.gui.api.component;

import java.util.Collection;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public abstract class PopupObjectListPanel<T extends ObjectType> extends ObjectListPanel<T> {

	private boolean multiselect;

	public PopupObjectListPanel(String id, Class<T> type, boolean multiselect, PageBase parentPage) {
		super(id, type, null, parentPage);
		this.multiselect = multiselect;
	}

	@Override
	protected IColumn<SelectableBean<T>, String> createCheckboxColumn() {
		if (multiselect) {
			return new CheckBoxHeaderColumn<SelectableBean<T>>();
		}
		return null;
	}

	@Override
	protected IColumn<SelectableBean<T>, String> createNameColumn() {
		if (multiselect) {
			return new LinkColumn<SelectableBean<T>>(createStringResource("ObjectType.name"),
					ObjectType.F_NAME.getLocalPart(), SelectableBean.F_VALUE + ".name") {

				@Override
				public void onClick(AjaxRequestTarget target, IModel<SelectableBean<T>> rowModel) {
					T object = rowModel.getObject().getValue();
					onSelectPerformed(target, object);

				}
			};
		}

		else {
			return new PropertyColumn(createStringResource("userBrowserDialog.name"),
					ObjectType.F_NAME.getLocalPart(), SelectableBean.F_VALUE + ".name");
		}
	}

	@Override
	protected List<IColumn<SelectableBean<T>, String>> createColumns() {
		return ColumnUtils.getDefaultColumns(getType());
	}
	
	protected void onSelectPerformed(AjaxRequestTarget target, T object){
		
	}

}
