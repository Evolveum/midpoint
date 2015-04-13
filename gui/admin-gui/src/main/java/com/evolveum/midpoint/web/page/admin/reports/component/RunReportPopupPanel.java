package com.evolveum.midpoint.web.page.admin.reports.component;

import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.DateInput;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.EditablePropertyColumn;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.XmlGregorianCalendarModel;
import com.evolveum.midpoint.web.page.admin.reports.dto.JasperReportParameterDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportDto;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportParameterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

public class RunReportPopupPanel extends SimplePanel<ReportDto>{
	
	private static final String ID_MAIN_FORM = "mainForm";
	
	private static final String ID_NAME_LABEL = "label";
	private static final String ID_RUN = "runReport";
	
	 private static final String ID_LABEL_SIZE = "col-md-4";
	    private static final String ID_INPUT_SIZE = "col-md-8";
	
	private static final String ID_PARAMETERS_TABLE = "paramTable";
	
	private IModel<ReportDto> reportModel;
	private ReportType reportType;
	
	public void setReportType(ReportType reportType) {
		this.reportType = reportType;
		
		if (getParametersTable() != null){
			replace(createTablePanel());
		}
	}
	
	
	public RunReportPopupPanel(String id) {
		super(id);
	}
	
	
	@Override
	protected void initLayout() {
		

	   TablePanel table = createTablePanel();
       add(table);
        
        AjaxButton addButton = new AjaxButton(ID_RUN,
                createStringResource("runReportPopupContent.button.run")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                runConfirmPerformed(target, reportModel, getParameters());
            }
        };
        add(addButton);

	}
	
	private TablePanel createTablePanel(){
		reportModel = new LoadableModel<ReportDto>() {
			@Override
			protected ReportDto load() {
				return new ReportDto(reportType, true);
			}
		};
		
		 ISortableDataProvider<JasperReportParameterDto, String> provider = new ListDataProvider<JasperReportParameterDto>(this,
	                new PropertyModel<List<JasperReportParameterDto>>(reportModel, "jasperReportDto.parameters"));
	        TablePanel table = new TablePanel<>(ID_PARAMETERS_TABLE, provider, initParameterColumns());
	        table.setOutputMarkupId(true);
	        table.setShowPaging(true);
	        return table;
	}
	
	private TablePanel getParametersTable(){
		return (TablePanel) get(ID_PARAMETERS_TABLE);
	}
	
	private List<JasperReportParameterDto> getParameters(){
		TablePanel table = getParametersTable();
		List<JasperReportParameterDto> params = ((ListDataProvider) table.getDataTable().getDataProvider()).getAvailableData();
		return params;
	}
	
	private List<IColumn<JasperReportParameterDto, String>> initParameterColumns() {
        List<IColumn<JasperReportParameterDto, String>> columns = new ArrayList<>();
      

        //name editing column
        columns.add(new PropertyColumn(createStringResource("runReportPopupContent.param.name"), "name"));
        columns.add(new PropertyColumn(createStringResource("runReportPopupContent.param.class"), "typeAsString"));

        //class editing column
        columns.add(new EditablePropertyColumn<JasperReportParameterDto>(createStringResource("runReportPopupContent.param.value"), "value") {
        	
        	
        	@Override
            public void populateItem(Item<ICellPopulator<JasperReportParameterDto>> cellItem, String componentId,
                    final IModel<JasperReportParameterDto> rowModel) {
        			Component component = createTypedInputPanel(componentId, rowModel, getPropertyExpression());
                    cellItem.add(component);
                    cellItem.setOutputMarkupId(true);
            }
        	
        });

        return columns;
    }
	
	private Component createTypedInputPanel(String componentId, final IModel<JasperReportParameterDto> model, String expression) {
		JasperReportParameterDto param = model.getObject();
		param.setEditing(true);
		
		IModel label = new PropertyModel<String>(model, "name");
		PropertyModel value = new PropertyModel<String>(model, expression);
		String tooltipKey = model.getObject().getTypeAsString();
		Class type = null;
		
		try {
			type = param.getType();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			return null;
		}
		if (type.isEnum()){
			DropDownChoicePanel panel = WebMiscUtil.createEnumPanel(type, componentId, new PropertyModel(model, expression), this);
			FormComponent component = panel.getBaseFormComponent();
			component.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
			return panel;
		} else if (XMLGregorianCalendar.class.isAssignableFrom(type)){
			return new DateInput(componentId, new XmlGregorianCalendarModel(new PropertyModel<XMLGregorianCalendar>(model, expression)));
		} 
			TextPanel panel = new TextPanel<String>(componentId, new PropertyModel<String>(model, expression));
		
		FormComponent component = panel.getBaseFormComponent();
		component.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
		return panel;
		
	}

	private void runConfirmPerformed(AjaxRequestTarget target, IModel<ReportDto> model, List<JasperReportParameterDto> params){		
		ReportDto reportDto = model.getObject();
    	
    	List<ReportParameterType> reportParams = new ArrayList<ReportParameterType>();
		try {
    	for (JasperReportParameterDto paramDto : params){
    		ReportParameterType parameterType = new ReportParameterType();
    		parameterType.setName(paramDto.getName());
    		parameterType.setType(paramDto.getTypeAsString());
    		String 	value = null;
    		if (XmlTypeConverter.canConvert(paramDto.getType())){
    			value = XmlTypeConverter.toXmlTextContent(paramDto.getValue(), null);
    		} else{
    			value = getPrismContext().serializeAnyData(paramDto.getValue(), SchemaConstants.C_REPORT_PARAM_VALUE, PrismContext.LANG_XML);
    		}
    		parameterType.setValue(value);
    		reportParams.add(parameterType);
    	}
		} catch (SchemaException | ClassNotFoundException e) {
			OperationResult result = new OperationResult("Parameters serialization");
			result.recordFatalError("Could not serialize parameters");
			getPageBase().showResult(result);
			return;
		}
    	
    	runConfirmPerformed(target, reportDto.getObject().asObjectable(), reportParams);
    	
    	
	}
	
	private PrismContext getPrismContext(){
		return getPageBase().getPrismContext();
	}
	
	protected void runConfirmPerformed(AjaxRequestTarget target, ReportType reportType2,
			List<ReportParameterType> paramsMap) {}
	private static class EmptyOnBlurAjaxFormUpdatingBehaviour extends AjaxFormComponentUpdatingBehavior {

        public EmptyOnBlurAjaxFormUpdatingBehaviour() {
            super("onBlur");
        }

        @Override
        protected void onUpdate(AjaxRequestTarget target) {
        }
    }

}
