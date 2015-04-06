package com.evolveum.midpoint.web.page.admin.reports.component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.model.common.expression.ExpressionUtil;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.parser.QueryConvertor;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.DateInput;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.EditableLinkColumn;
import com.evolveum.midpoint.web.component.data.column.EditablePropertyColumn;
import com.evolveum.midpoint.web.component.form.DateFormGroup;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.form.TextAreaFormGroup;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.prism.PrismPropertyPanel;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.XmlGregorianCalendarModel;
import com.evolveum.midpoint.web.page.admin.reports.PageReports;
import com.evolveum.midpoint.web.page.admin.reports.dto.JasperReportParameterDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportDto;
import com.evolveum.midpoint.web.page.admin.users.component.AssignableRolePopupContent;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportParameterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

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
	}
	
	
	public RunReportPopupPanel(String id) {
		super(id);
	}
	
	
	@Override
	protected void initLayout() {
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
	
	private List<JasperReportParameterDto> getParameters(){
		TablePanel table = (TablePanel) get(ID_PARAMETERS_TABLE);
		List<JasperReportParameterDto> params = ((ListDataProvider) table.getDataTable().getDataProvider()).getAvailableData();
		return params;
	}
	
	private List<IColumn<JasperReportParameterDto, String>> initParameterColumns() {
        List<IColumn<JasperReportParameterDto, String>> columns = new ArrayList<>();
      

        //name editing column
        columns.add(new PropertyColumn(createStringResource("Parameter name"), "name"));
        columns.add(new PropertyColumn(createStringResource("Parameter class"), "typeAsString"));

        //class editing column
        columns.add(new EditablePropertyColumn<JasperReportParameterDto>(createStringResource("Parameter value"), "value") {
        	
        	
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
