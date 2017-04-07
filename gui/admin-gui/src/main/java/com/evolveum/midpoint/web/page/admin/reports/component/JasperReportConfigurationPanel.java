package com.evolveum.midpoint.web.page.admin.reports.component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxPanel;
import com.evolveum.midpoint.web.component.data.column.EditableLinkColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.page.admin.reports.dto.JasperReportFieldDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.JasperReportParameterDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportDto;
import com.evolveum.midpoint.web.util.Base64Model;

public class JasperReportConfigurationPanel extends BasePanel<ReportDto> {

	private static final long serialVersionUID = 1L;
	
	private static final String ID_PARAMETERS_TABLE = "parametersTable";
	private static final String ID_FIELDS_TABLE = "fieldsTable";
	private static final String ID_BUTTON_ADD_PARAMETER = "addParameter";
	private static final String ID_BUTTON_ADD_FIELD = "addField";
	private static final String ID_QUERY = "query";
	private static final String ID_TEMPLATE = "template";
	private static final String ID_DELETE_PARAMETER = "deleteParameter";
	private static final String ID_DELETE_FIELD = "deleteField";

	public JasperReportConfigurationPanel(String id, IModel<ReportDto> model) {
		super(id, model);
		initLayout();
	}

	
	protected void initLayout() {
		AceEditorPanel queryPanel = new AceEditorPanel(ID_QUERY,
				createStringResource("JasperReportConfigurationPanel.reportQuery"),
				new PropertyModel<String>(getModel(), "jasperReportDto.query"));
		add(queryPanel);

		initParametersTable();
		initFiledsTable();

		IModel<String> data = new Base64Model(new PropertyModel<byte[]>(getModel(), "jasperReportDto.jasperReportXml"));
		AceEditorPanel templateEditor = new AceEditorPanel(ID_TEMPLATE,
				createStringResource("PageReport.jasperTemplate"), data, 300);
		add(templateEditor);
	}

	private void initParametersTable() {
		ISortableDataProvider<JasperReportParameterDto, String> provider = new ListDataProvider<JasperReportParameterDto>(this,
				new PropertyModel<List<JasperReportParameterDto>>(getModel(), "jasperReportDto.parameters"));
		BoxedTablePanel<JasperReportParameterDto> table = new BoxedTablePanel<>(ID_PARAMETERS_TABLE, provider, initParameterColumns(), null, 10);
//		table.setShowPaging(false);
		table.setOutputMarkupId(true);
		add(table);

		AjaxButton addParameter = new AjaxButton(ID_BUTTON_ADD_PARAMETER,
				createStringResource("JasperReportConfigurationPanel.addParameter")) {

			private static final long serialVersionUID = 1L;
			@Override
			public void onClick(AjaxRequestTarget target) {
				addParameterPerformed(target);
			}
		};
		add(addParameter);

		AjaxButton deleteParameter = new AjaxButton(ID_DELETE_PARAMETER,
				createStringResource("JasperReportConfigurationPanel.deleteParameter")) {

			private static final long serialVersionUID = 1L;
			@Override
			public void onClick(AjaxRequestTarget target) {
				deleteParameterPerformed(target);
			}
		};
		add(deleteParameter);

	}

	private void initFiledsTable() {
		ISortableDataProvider<JasperReportFieldDto, String> provider = new ListDataProvider<JasperReportFieldDto>(this,
				new PropertyModel<List<JasperReportFieldDto>>(getModel(), "jasperReportDto.fields"));
		BoxedTablePanel<JasperReportFieldDto> table = new BoxedTablePanel<JasperReportFieldDto>(ID_FIELDS_TABLE, provider, initFieldColumns(), null, 10);
//		table.setShowPaging(false);
		table.setOutputMarkupId(true);
		add(table);

		AjaxButton addParameter = new AjaxButton(ID_BUTTON_ADD_FIELD,
				createStringResource("JasperReportConfigurationPanel.addField")) {

			private static final long serialVersionUID = 1L;
			@Override
			public void onClick(AjaxRequestTarget target) {
				addFieldPerformed(target);
			}
		};
		add(addParameter);

		AjaxButton deleteParameter = new AjaxButton(ID_DELETE_FIELD,
				createStringResource("JasperReportConfigurationPanel.deleteField")) {

			private static final long serialVersionUID = 1L;
			@Override
			public void onClick(AjaxRequestTarget target) {
				deleteFieldPerformed(target);
			}
		};
		add(deleteParameter);
	}

	private void addParameterPerformed(AjaxRequestTarget target) {
		ReportDto dto = getModel().getObject();
		JasperReportParameterDto parameter = new JasperReportParameterDto();
		parameter.setEditing(true);
		dto.getJasperReportDto().getParameters().add(parameter);

		BoxedTablePanel<JasperReportParameterDto> parametersTable = getParametersTable();
		adjustParametersTablePage(parametersTable, dto);
		target.add(getParametersTable());
	}

	private void deleteParameterPerformed(AjaxRequestTarget target) {
		Iterator<JasperReportParameterDto> iterator = getModelObject().getJasperReportDto().getParameters().iterator();
		while (iterator.hasNext()) {
			JasperReportParameterDto item = iterator.next();
			if (item.isSelected()) {
				iterator.remove();
			}
		}
		target.add(getParametersTable());
	}

	private void addFieldPerformed(AjaxRequestTarget target) {
		ReportDto dto = getModel().getObject();
		JasperReportFieldDto parameter = new JasperReportFieldDto();
		parameter.setEditing(true);
		dto.getJasperReportDto().getFields().add(parameter);

		BoxedTablePanel<JasperReportFieldDto> fieldsTable = getFieldsTable();
		adjustFieldsTablePage(fieldsTable, dto);
		target.add(getFieldsTable());
	}

	private void deleteFieldPerformed(AjaxRequestTarget target) {
		Iterator<JasperReportFieldDto> iterator = getModelObject().getJasperReportDto().getFields().iterator();
		while (iterator.hasNext()) {
			JasperReportFieldDto item = iterator.next();
			if (item.isSelected()) {
				iterator.remove();
			}
		}
		target.add(getFieldsTable());
	}

	@SuppressWarnings("unchecked")
	private void adjustParametersTablePage(BoxedTablePanel<JasperReportParameterDto> parametersTable, ReportDto dto) {
		if (parametersTable != null && dto.getJasperReportDto().getParameters().size() % 10 == 1
				&& dto.getJasperReportDto().getParameters().size() != 1) {
			DataTable<JasperReportParameterDto, String> table = parametersTable.getDataTable();

			if (table != null) {
				table.setCurrentPage((long) (dto.getJasperReportDto().getParameters().size() / 10));
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void adjustFieldsTablePage(BoxedTablePanel<JasperReportFieldDto> parametersTable, ReportDto dto) {
		if (parametersTable != null && dto.getJasperReportDto().getFields().size() % 10 == 1
				&& dto.getJasperReportDto().getFields().size() != 1) {
			DataTable<JasperReportFieldDto, String> table = parametersTable.getDataTable();

			if (table != null) {
				table.setCurrentPage((long) (dto.getJasperReportDto().getFields().size() / 10));
			}
		}
	}

	private List<IColumn<JasperReportParameterDto, String>> initParameterColumns() {
		List<IColumn<JasperReportParameterDto, String>> columns = new ArrayList<>();
		IColumn<JasperReportParameterDto, String> column = new CheckBoxHeaderColumn<>();
		columns.add(column);

		// name editing column
		columns.add(buildEditableLinkColumn("JasperReportConfigurationPanel.parameterName", null, "name", true));

		// class editing column
		columns.add(
				buildEditableLinkColumn("JasperReportConfigurationPanel.parameterClass", null, "typeAsString", true));

		columns.add(new LinkColumn<JasperReportParameterDto>(createStringResource("JasperReportConfigurationPanel.properties")) {
			
			private static final long serialVersionUID = 1L;
			@Override
			public void onClick(AjaxRequestTarget target,
					IModel<JasperReportParameterDto> rowModel) {
				showPropertiesPopup(target, rowModel);
			}
			
			@Override
			protected IModel createLinkModel(IModel<JasperReportParameterDto> rowModel) {
				return createStringResource("JasperReportConfigurationPanel.configure");
			}

		}); 
		

		CheckBoxColumn<JasperReportParameterDto> forPrompting = new CheckBoxColumn<JasperReportParameterDto>(
				createStringResource("JasperReportConfigurationPanel.forPrompting"), "forPrompting") {

			private static final long serialVersionUID = 1L;
			@Override
			public void populateItem(Item<ICellPopulator<JasperReportParameterDto>> cellItem, String componentId,
					IModel<JasperReportParameterDto> rowModel) {
				CheckBoxPanel checkBox = new CheckBoxPanel(componentId,
						new PropertyModel<Boolean>(rowModel, getPropertyExpression()), new Model<>(true));
				cellItem.add(checkBox);
			}
		};

		columns.add(forPrompting);

		return columns;
	}
	
	private void showPropertiesPopup(AjaxRequestTarget target,
			IModel<JasperReportParameterDto> rowModel) {
		
		ParameterPropertiesPopupPanel propertiesPopup = new ParameterPropertiesPopupPanel(getPageBase().getMainPopupBodyId(), new PropertyModel<>(rowModel, "properties"));
		getPageBase().showMainPopup(propertiesPopup, target);
		
	}
	private EditableLinkColumn<JasperReportParameterDto> buildEditableLinkColumn(String resource, String resourceParam,
			String property, final Boolean mandatory) {
		return new EditableLinkColumn<JasperReportParameterDto>(createStringResource(resource, resourceParam),
				property) {

			private static final long serialVersionUID = 1L;
			@Override
			protected Component createInputPanel(String componentId, final IModel<JasperReportParameterDto> model) {
				return createTextPanel(componentId, model, getPropertyExpression(), mandatory);

			}

			@Override
			public void onClick(AjaxRequestTarget target, IModel<JasperReportParameterDto> rowModel) {
				parameterEditPerformed(target, rowModel);
			}
		};
	}

	private void parameterEditPerformed(AjaxRequestTarget target, IModel<JasperReportParameterDto> rowModel) {
		JasperReportParameterDto parameter = rowModel.getObject();
		parameter.setEditing(true);
		target.add(getParametersTable());
	}

	@SuppressWarnings("unchecked")
	private BoxedTablePanel<JasperReportParameterDto> getParametersTable() {
		return (BoxedTablePanel<JasperReportParameterDto>) get(ID_PARAMETERS_TABLE);
	}

	private List<IColumn<JasperReportFieldDto, String>> initFieldColumns() {
		List<IColumn<JasperReportFieldDto, String>> columns = new ArrayList<>();
		IColumn<JasperReportFieldDto, String> column = new CheckBoxHeaderColumn<>();
		columns.add(column);

		// name editing column
		columns.add(new EditableLinkColumn<JasperReportFieldDto>(
				createStringResource("JasperReportConfigurationPanel.fieldName"), "name") {

			private static final long serialVersionUID = 1L;
			@Override
			protected Component createInputPanel(String componentId, final IModel<JasperReportFieldDto> model) {
				return createTextPanel(componentId, model, getPropertyExpression(), true);

			}

			@Override
			public void onClick(AjaxRequestTarget target, IModel<JasperReportFieldDto> rowModel) {
				fieldEditPerformed(target, rowModel);
			}
		});

		// class editing column
		columns.add(new EditableLinkColumn<JasperReportFieldDto>(
				createStringResource("JasperReportConfigurationPanel.fieldClass"), "typeAsString") {

			private static final long serialVersionUID = 1L;
			@Override
			protected Component createInputPanel(String componentId, IModel<JasperReportFieldDto> model) {
				return createTextPanel(componentId, model, getPropertyExpression(), true);
			}

			@Override
			public void onClick(AjaxRequestTarget target, IModel<JasperReportFieldDto> rowModel) {
				fieldEditPerformed(target, rowModel);
			}
		});

		return columns;
	}

	private void fieldEditPerformed(AjaxRequestTarget target, IModel<JasperReportFieldDto> rowModel) {
		JasperReportFieldDto parameter = rowModel.getObject();
		parameter.setEditing(true);
		target.add(getFieldsTable());
	}

	@SuppressWarnings("unchecked")
	private BoxedTablePanel<JasperReportFieldDto> getFieldsTable() {
		return (BoxedTablePanel<JasperReportFieldDto>) get(ID_FIELDS_TABLE);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <J> Component createTextPanel(String componentId, final IModel<J> model, String expression,
			final Boolean mandatory) {
		TextPanel<String> textPanel = new TextPanel<String>(componentId, new PropertyModel<String>(model, expression));
		FormComponent input = textPanel.getBaseFormComponent();
		input.add(new AttributeAppender("style", "width: 100%"));
		input.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		input.add(new IValidator() {

			private static final long serialVersionUID = 1L;
			@Override
			public void validate(IValidatable validatable) {
				if (!mandatory) {
					return;
				}
				if (validatable.getValue() == null) {
					validatable.error(new ValidationError("JasperReportConfigurationPanel.errormsg"));
				}
			}

		});
		return textPanel;
	}

	private static class EmptyOnBlurAjaxFormUpdatingBehaviour extends AjaxFormComponentUpdatingBehavior {

		private static final long serialVersionUID = 1L;
		public EmptyOnBlurAjaxFormUpdatingBehaviour() {
			super("blur");
		}

		@Override
		protected void onUpdate(AjaxRequestTarget target) {
		}
	}
}
