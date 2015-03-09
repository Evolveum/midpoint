package com.evolveum.midpoint.web.page.admin.reports.component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;

import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.EditableLinkColumn;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.Validatable;
import com.evolveum.midpoint.web.page.admin.configuration.dto.InputStringValidator;
import com.evolveum.midpoint.web.page.admin.reports.dto.JasperReportDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.JasperReportFieldDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.JasperReportParameterDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportDto;
import com.evolveum.midpoint.web.util.Base64Model;

public class JasperReportConfigurationPanel extends SimplePanel<ReportDto> {

	private static final String ID_PARAMETERS_TABLE = "parametersTable";
	private static final String ID_PARAMETERS_TITLE = "parametersTitle";
	private static final String ID_FIELDS_TABLE = "fieldsTable";
	private static final String ID_FIELDS_TITLE = "fieldsTitle";
	private static final String ID_BUTTON_ADD_PARAMETER = "addParameter";
	private static final String ID_BUTTON_ADD_FIELD = "addField";
	
	public JasperReportConfigurationPanel(String id, IModel<ReportDto> model) {
		super(id, model);
	}
	
	@Override
	protected void initLayout() {
		IModel<ReportDto> reportModel = getModel();
		ReportDto report = reportModel.getObject();
		
		JasperReportDto jasperReport = report.getJasperReportDto();
			 IModel<String> title = JasperReportConfigurationPanel.this.createStringResource("Report query");
             
             AceEditorPanel queryPanel = new AceEditorPanel("query", title, new PropertyModel(getModel(), "jasperReportDto.query"));
             add(queryPanel);
             
            initParametersTable();
            initFiledsTable();
                          
             
             title = JasperReportConfigurationPanel.this.createStringResource("PageReport.jasperTemplate");
             IModel<String> data = new Base64Model(new PropertyModel(getModel(), "jasperReportDto.jasperReportXml"));
             AceEditorPanel templateEditor = new AceEditorPanel("template", title, data);
             add(templateEditor);
			
		
	}
	
	private void initParametersTable(){
		 Label parametersTitle = new Label(ID_PARAMETERS_TITLE, JasperReportConfigurationPanel.this.createStringResource("Report parameters"));
         add(parametersTitle);
         ISortableDataProvider<JasperReportParameterDto, String> provider = new ListDataProvider(this,
                 new PropertyModel<List<JasperReportParameterDto>>(getModel(), "jasperReportDto.parameters"));
         TablePanel table = new TablePanel<>(ID_PARAMETERS_TABLE, provider, initParameterColumns());
         table.setOutputMarkupId(true);
         table.setShowPaging(true);
         add(table);

         AjaxButton addParameter = new AjaxButton(ID_BUTTON_ADD_PARAMETER,
                 createStringResource("add parameter")) {

             @Override
             public void onClick(AjaxRequestTarget target) {
                 addParameterPerformed(target);
             }
         };
         add(addParameter);
         
         AjaxButton deleteParameter = new AjaxButton("deleteParameter",
                 createStringResource("delete parameter")) {

             @Override
             public void onClick(AjaxRequestTarget target) {
                 deleteParameterPerformed(target);
             }
         };
         add(deleteParameter);

	}
	
	private void initFiledsTable(){
		Label filedTitle = new Label(ID_FIELDS_TITLE, JasperReportConfigurationPanel.this.createStringResource("Report fields"));
        add(filedTitle);
        ISortableDataProvider<JasperReportFieldDto, String> provider = new ListDataProvider(this,
                new PropertyModel<List<JasperReportParameterDto>>(getModel(), "jasperReportDto.fields"));
        TablePanel table = new TablePanel<>(ID_FIELDS_TABLE, provider, initParameterColumns());
        table.setOutputMarkupId(true);
        table.setShowPaging(true);
        add(table);

        AjaxButton addParameter = new AjaxButton(ID_BUTTON_ADD_FIELD,
                createStringResource("add field")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addFieldPerformed(target);
            }
        };
        add(addParameter);
        
        AjaxButton deleteParameter = new AjaxButton("deleteField",
                createStringResource("delete field")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteFieldPerformed(target);
            }
        };
        add(deleteParameter);
	}
	 private void addParameterPerformed(AjaxRequestTarget target){
	        ReportDto dto = getModel().getObject();
	        JasperReportParameterDto parameter = new JasperReportParameterDto();
	        parameter.setEditing(true);
	        dto.getJasperReportDto().getParameters().add(parameter);

	        TablePanel parametersTable = getParametersTable();
	        adjustParametersTablePage(parametersTable, dto);
	        target.add(getParametersTable());
	    }
	 
	 private void deleteParameterPerformed(AjaxRequestTarget target) {
	        Iterator<JasperReportParameterDto> iterator = getModel().getObject().getJasperReportDto().getParameters().iterator();
	        while (iterator.hasNext()) {
	        	JasperReportParameterDto item = iterator.next();
	            if (item.isSelected()) {
	                iterator.remove();
	            }
	        }
	        target.add(getParametersTable());
	    }
	 
	 private void addFieldPerformed(AjaxRequestTarget target){
	        ReportDto dto = getModel().getObject();
	        JasperReportFieldDto parameter = new JasperReportFieldDto();
	        parameter.setEditing(true);
	        dto.getJasperReportDto().getFields().add(parameter);

	        TablePanel fieldsTable = getFieldsTable();
	        adjustFieldsTablePage(fieldsTable, dto);
	        target.add(getFieldsTable());
	    }
	 
	 private void deleteFieldPerformed(AjaxRequestTarget target) {
	        Iterator<JasperReportFieldDto> iterator = getModel().getObject().getJasperReportDto().getFields().iterator();
	        while (iterator.hasNext()) {
	        	JasperReportFieldDto item = iterator.next();
	            if (item.isSelected()) {
	                iterator.remove();
	            }
	        }
	        target.add(getFieldsTable());
	    }
	 private void adjustParametersTablePage(TablePanel parametersTable, ReportDto dto){
	        if(parametersTable != null && dto.getJasperReportDto().getParameters().size() % 10 == 1 && dto.getJasperReportDto().getParameters().size() != 1){
	            DataTable table = parametersTable.getDataTable();

	            if(table != null){
	                table.setCurrentPage((long)(dto.getJasperReportDto().getParameters().size()/10));
	            }
	        }
	    }
	 private void adjustFieldsTablePage(TablePanel parametersTable, ReportDto dto){
	        if(parametersTable != null && dto.getJasperReportDto().getFields().size() % 10 == 1 && dto.getJasperReportDto().getFields().size() != 1){
	            DataTable table = parametersTable.getDataTable();

	            if(table != null){
	                table.setCurrentPage((long)(dto.getJasperReportDto().getFields().size()/10));
	            }
	        }
	    }

	private List<IColumn<JasperReportParameterDto, String>> initParameterColumns() {
        List<IColumn<JasperReportParameterDto, String>> columns = new ArrayList<>();
        IColumn column = new CheckBoxHeaderColumn<JasperReportParameterDto>();
        columns.add(column);

        //name editing column
        columns.add(new EditableLinkColumn<JasperReportParameterDto>(
                createStringResource("Parameter name"), "name") {

            @Override
            protected Component createInputPanel(String componentId, final IModel<JasperReportParameterDto> model) {
            	return createTextPanel(componentId, model, getPropertyExpression());
            		
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<JasperReportParameterDto> rowModel) {
            	parameterEditPerformed(target, rowModel);
            }
        });

        //class editing column
        columns.add(new EditableLinkColumn<JasperReportParameterDto>(createStringResource("Parameter class"), "typeAsString") {

            @Override
            protected Component createInputPanel(String componentId, IModel<JasperReportParameterDto> model) {
            	return createTextPanel(componentId, model, getPropertyExpression());
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<JasperReportParameterDto> rowModel) {
            	parameterEditPerformed(target, rowModel);
            }
        });

        return columns;
    }
	 private void parameterEditPerformed(AjaxRequestTarget target, IModel<JasperReportParameterDto> rowModel) {
		 JasperReportParameterDto parameter = rowModel.getObject();
		 parameter.setEditing(true);
	        target.add(getParametersTable());
	    }
	 
	
	 private TablePanel getParametersTable() {
	        return (TablePanel) get(ID_PARAMETERS_TABLE);
	    }

		private List<IColumn<JasperReportFieldDto, String>> initFiledColumns() {
	        List<IColumn<JasperReportFieldDto, String>> columns = new ArrayList<>();
	        IColumn column = new CheckBoxHeaderColumn<JasperReportFieldDto>();
	        columns.add(column);

	        //name editing column
	        columns.add(new EditableLinkColumn<JasperReportFieldDto>(
	                createStringResource("Field name"), "name") {

	            @Override
	            protected Component createInputPanel(String componentId, final IModel<JasperReportFieldDto> model) {
	            	return createTextPanel(componentId, model, getPropertyExpression());
	            		
	            }

	            @Override
	            public void onClick(AjaxRequestTarget target, IModel<JasperReportFieldDto> rowModel) {
	            	fieldEditPerformed(target, rowModel);
	            }
	        });

	        //class editing column
	        columns.add(new EditableLinkColumn<JasperReportFieldDto>(createStringResource("Field class"), "typeAsString") {

	            @Override
	            protected Component createInputPanel(String componentId, IModel<JasperReportFieldDto> model) {
	            	return createTextPanel(componentId, model, getPropertyExpression());
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
		 
		 
	private Component createTextPanel(String componentId, final IModel model, String expression){
		TextPanel textPanel = new TextPanel<>(componentId, new PropertyModel<String>(model, expression));
        FormComponent input = textPanel.getBaseFormComponent();
        input.add(new AttributeAppender("style", "width: 100%"));
        input.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        input.add(new AbstractValidator(){

			@Override
			protected void onValidate(IValidatable validatable) {
				if(validatable.getValue() == null){
					error(validatable, "Empty values not allowed");
				}
				
			}
			
			@Override
			public boolean validateOnNullValue() {
				if (model.getObject() instanceof Validatable){
					return !((Validatable)model.getObject()).isEmpty();
				}
				
				return true;
			       	
			}
        });
        return textPanel;
	}
	
	
	 
	 private static class EmptyOnBlurAjaxFormUpdatingBehaviour extends AjaxFormComponentUpdatingBehavior {

	        public EmptyOnBlurAjaxFormUpdatingBehaviour() {
	            super("onBlur");
	        }

	        @Override
	        protected void onUpdate(AjaxRequestTarget target) {
	        }
	    }

	

}
