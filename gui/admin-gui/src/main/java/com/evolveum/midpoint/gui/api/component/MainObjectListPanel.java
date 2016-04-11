package com.evolveum.midpoint.gui.api.component;

import java.util.Collection;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.data.column.LinkIconPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.PageImportObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public abstract class MainObjectListPanel<T extends ObjectType> extends ObjectListPanel<T> {

	private static final long serialVersionUID = 1L;
	
	private static final String ID_REFRESH = "refresh";
	private static final String ID_NEW_OBJECT = "newObject";
	private static final String ID_IMPORT_OBJECT = "importObject";

	public MainObjectListPanel(String id, Class<T> type, Collection<SelectorOptions<GetOperationOptions>> options, PageBase parentPage) {
		super(id, type, options, parentPage);
		
		LinkIconPanel refreshIcon = new LinkIconPanel(ID_REFRESH, new Model<String>("fa fa-refresh")){
			
			@Override
			protected void onClickPerformed(AjaxRequestTarget target) {
				Table table = getTable();
				target.add((Component) table);
			}
		};
		add(refreshIcon);
		
LinkIconPanel newObjectIcon = new LinkIconPanel(ID_NEW_OBJECT, new Model<String>("fa fa-edit")){
			
			@Override
			protected void onClickPerformed(AjaxRequestTarget target) {
				newObjectPerformed(target);
			}
		};
		add(newObjectIcon);
		
LinkIconPanel importObject = new LinkIconPanel(ID_IMPORT_OBJECT, new Model<String>("fa fa-download")){
			
			@Override
			protected void onClickPerformed(AjaxRequestTarget target) {
				setResponsePage(PageImportObject.class);
			}
		};
		add(importObject);
	}

	@Override
	protected IColumn<SelectableBean<T>, String> createCheckboxColumn() {
		return new CheckBoxHeaderColumn<SelectableBean<T>>();
	}

	@Override
	protected IColumn<SelectableBean<T>, String> createNameColumn() {
		return new LinkColumn<SelectableBean<T>>(createStringResource("ObjectType.name"),
				ObjectType.F_NAME.getLocalPart(), SelectableBean.F_VALUE + ".name") {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target, IModel<SelectableBean<T>> rowModel) {
				T object = rowModel.getObject().getValue();
				MainObjectListPanel.this.objectDetailsPerformed(target, object);
			};

		};
	}
	
	protected abstract void objectDetailsPerformed(AjaxRequestTarget target, T object);

	protected abstract void newObjectPerformed(AjaxRequestTarget target);
	
		


}
