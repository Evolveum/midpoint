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
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxPanel;
import com.evolveum.midpoint.web.component.data.column.EditableLinkColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.reports.dto.JasperReportFieldDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.JasperReportParameterDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.JasperReportParameterPropertiesDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportDto;
import com.evolveum.midpoint.web.util.Base64Model;

public class JasperReportConfigurationPanel extends BasePanel<ReportDto> {

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
				new PropertyModel(getModel(), "jasperReportDto.query"));
		add(queryPanel);

		initParametersTable();
		initFiledsTable();

		IModel<String> data = new Base64Model(new PropertyModel(getModel(), "jasperReportDto.jasperReportXml"));
		AceEditorPanel templateEditor = new AceEditorPanel(ID_TEMPLATE,
				createStringResource("PageReport.jasperTemplate"), data, 300);
		add(templateEditor);
	}

	private void initParametersTable() {
		ISortableDataProvider<JasperReportParameterDto, String> provider = new ListDataProvider(this,
				new PropertyModel<List<JasperReportParameterDto>>(getModel(), "jasperReportDto.parameters"));
		TablePanel table = new TablePanel<>(ID_PARAMETERS_TABLE, provider, initParameterColumns());
		table.setShowPaging(false);
		table.setOutputMarkupId(true);
		add(table);

		AjaxButton addParameter = new AjaxButton(ID_BUTTON_ADD_PARAMETER,
				createStringResource("JasperReportConfigurationPanel.addParameter")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				addParameterPerformed(target);
			}
		};
		add(addParameter);

		AjaxButton deleteParameter = new AjaxButton(ID_DELETE_PARAMETER,
				createStringResource("JasperReportConfigurationPanel.deleteParameter")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				deleteParameterPerformed(target);
			}
		};
		add(deleteParameter);

	}

	private void initFiledsTable() {
		ISortableDataProvider<JasperReportFieldDto, String> provider = new ListDataProvider(this,
				new PropertyModel<List<JasperReportFieldDto>>(getModel(), "jasperReportDto.fields"));
		TablePanel table = new TablePanel<>(ID_FIELDS_TABLE, provider, initFieldColumns());
		table.setShowPaging(false);
		table.setOutputMarkupId(true);
		add(table);

		AjaxButton addParameter = new AjaxButton(ID_BUTTON_ADD_FIELD,
				createStringResource("JasperReportConfigurationPanel.addField")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				addFieldPerformed(target);
			}
		};
		add(addParameter);

		AjaxButton deleteParameter = new AjaxButton(ID_DELETE_FIELD,
				createStringResource("JasperReportConfigurationPanel.deleteField")) {

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

		TablePanel parametersTable = getParametersTable();
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

		TablePanel fieldsTable = getFieldsTable();
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

	private void adjustParametersTablePage(TablePanel parametersTable, ReportDto dto) {
		if (parametersTable != null && dto.getJasperReportDto().getParameters().size() % 10 == 1
				&& dto.getJasperReportDto().getParameters().size() != 1) {
			DataTable table = parametersTable.getDataTable();

			if (table != null) {
				table.setCurrentPage((long) (dto.getJasperReportDto().getParameters().size() / 10));
			}
		}
	}

	private void adjustFieldsTablePage(TablePanel parametersTable, ReportDto dto) {
		if (parametersTable != null && dto.getJasperReportDto().getFields().size() % 10 == 1
				&& dto.getJasperReportDto().getFields().size() != 1) {
			DataTable table = parametersTable.getDataTable();

			if (table != null) {
				table.setCurrentPage((long) (dto.getJasperReportDto().getFields().size() / 10));
			}
		}
	}

	private List<IColumn<JasperReportParameterDto, String>> initParameterColumns() {
		List<IColumn<JasperReportParameterDto, String>> columns = new ArrayList<>();
		IColumn column = new CheckBoxHeaderColumn<>();
		columns.add(column);

		// name editing column
		columns.add(buildEditableLinkColumn("JasperReportConfigurationPanel.parameterName", null, "name", true));

		// class editing column
		columns.add(
				buildEditableLinkColumn("JasperReportConfigurationPanel.parameterClass", null, "typeAsString", true));

		columns.add(new LinkColumn<JasperReportParameterDto>(createStringResource("JasperReportConfigurationPanel.properties")) {
			
			@Override
			public void onClick(AjaxRequestTarget target,
					IModel<JasperReportParameterDto> rowModel) {
				showPropertiesPopup(target, rowModel);
			}
			
			@Override
			protected IModel createLinkModel(IModel<JasperReportParameterDto> rowModel) {
				return createStringResource("JasperReportConfigurationPanel.properties");
			}

		}); 
		

		CheckBoxColumn forPrompting = new CheckBoxColumn<JasperReportParameterDto>(
				createStringResource("JasperReportConfigurationPanel.forPrompting"), "forPrompting") {

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
		
		ParameterPropertiesPopupPanel propertiesPopup = new ParameterPropertiesPopupPanel(getPageBase().getMainPopupBodyId(), new PropertyModel<>(rowModel, "properties")) {
			
			@Override
			protected void updateProperties(JasperReportParameterPropertiesDto properties, AjaxRequestTarget target) {
//				getModel().getObject().get
			}
			
		};
		getPageBase().showMainPopup(propertiesPopup, target);
		
	}
	
//	void popup(){
//		// property:key editing column
//				columns.add(buildEditableLinkColumn("JasperReportConfigurationPanel.parameterProperty", "key", "value.key",
//						false));
//
//				// property:label editing column
//				columns.add(buildEditableLinkColumn("JasperReportConfigurationPanel.parameterProperty", "label",
//						"value.label", false));
//
//				// property:targetType editing column
//				columns.add(buildEditableLinkColumn("JasperReportConfigurationPanel.parameterProperty", "targetType",
//						"value.targetType", false));
//	}

	private EditableLinkColumn<JasperReportParameterDto> buildEditableLinkColumn(String resource, String resourceParam,
			String property, final Boolean mandatory) {
		return new EditableLinkColumn<JasperReportParameterDto>(createStringResource(resource, resourceParam),
				property) {

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

	private TablePanel getParametersTable() {
		return (TablePanel) get(ID_PARAMETERS_TABLE);
	}

	private List<IColumn<JasperReportFieldDto, String>> initFieldColumns() {
		List<IColumn<JasperReportFieldDto, String>> columns = new ArrayList<>();
		IColumn column = new CheckBoxHeaderColumn<>();
		columns.add(column);

		// name editing column
		columns.add(new EditableLinkColumn<JasperReportFieldDto>(
				createStringResource("JasperReportConfigurationPanel.fieldName"), "name") {

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

	private TablePanel getFieldsTable() {
		return (TablePanel) get(ID_FIELDS_TABLE);
	}

	private Component createTextPanel(String componentId, final IModel model, String expression,
			final Boolean mandatory) {
		TextPanel textPanel = new TextPanel<>(componentId, new PropertyModel<String>(model, expression));
		FormComponent input = textPanel.getBaseFormComponent();
		input.add(new AttributeAppender("style", "width: 100%"));
		input.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		input.add(new IValidator() {

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

		public EmptyOnBlurAjaxFormUpdatingBehaviour() {
			super("blur");
		}

		@Override
		protected void onUpdate(AjaxRequestTarget target) {
		}
	}
}
