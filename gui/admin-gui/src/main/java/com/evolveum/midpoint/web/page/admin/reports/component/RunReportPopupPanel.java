package com.evolveum.midpoint.web.page.admin.reports.component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.cxf.aegis.type.XMLTypeCreator;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.report.api.ReportConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.DateInput;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.EditablePropertyColumn;
import com.evolveum.midpoint.web.component.input.DatePanel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.XmlGregorianCalendarModel;
import com.evolveum.midpoint.web.page.admin.reports.dto.JasperReportParameterDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportDto;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportParameterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.StringResourceModel;

public class RunReportPopupPanel extends SimplePanel<ReportDto> {
    
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
        
        if (getParametersTable() != null) {
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
    
    private TablePanel createTablePanel() {
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
    
    private TablePanel getParametersTable() {
        return (TablePanel) get(ID_PARAMETERS_TABLE);
    }
    
    private List<JasperReportParameterDto> getParameters() {
        TablePanel table = getParametersTable();
        List<JasperReportParameterDto> params = ((ListDataProvider) table.getDataTable().getDataProvider()).getAvailableData();
        return params;
    }
    
    private List<IColumn<JasperReportParameterDto, String>> initParameterColumns() {
        List<IColumn<JasperReportParameterDto, String>> columns = new ArrayList<>();

        //parameter name column
        columns.add(new AbstractColumn(createStringResource("runReportPopupContent.param.name")) {
            
            @Override
            public void populateItem(Item item, String componentId, IModel model) {
                String paramValue = new PropertyModel<String>(model, "name").getObject();
                StringResourceModel paramDisplay = new StringResourceModel("runReportPopupContent.param.name." + paramValue, null, paramValue, new Object[]{});
                item.add(new Label(componentId, paramDisplay)); // use display name rather than property name
            }
        });

        //parameter class column
        columns.add(new AbstractColumn(createStringResource("runReportPopupContent.param.class")) {
            
            @Override
            public void populateItem(Item item, String componentId, IModel model) {
                //Do not show whole parameter class name as this is not very user friendly
                String paramClass = new PropertyModel<String>(model, "typeAsString").getObject();
                paramClass = (paramClass == null) ? "" : paramClass.substring(paramClass.lastIndexOf(".")+1);
                item.add(new Label(componentId, paramClass));
            }
        });

        //parameter value editing column
        columns.add(new EditablePropertyColumn<JasperReportParameterDto>(createStringResource("runReportPopupContent.param.value"), "value") {
            
            @Override
            public void populateItem(Item<ICellPopulator<JasperReportParameterDto>> cellItem, String componentId,
                    final IModel<JasperReportParameterDto> rowModel) {
                Component component = createTypedInputPanel(componentId, rowModel, getPropertyExpression());
                cellItem.add(component);
//                    cellItem.setOutputMarkupId(true);
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
        InputPanel panel = null;
        if (type.isEnum()) {
            panel = WebMiscUtil.createEnumPanel(type, componentId, new PropertyModel(model, expression), this);
        } else if (XMLGregorianCalendar.class.isAssignableFrom(type)) {          
            panel = new DatePanel(componentId, new PropertyModel<XMLGregorianCalendar>(model, expression));
        } else {
            panel = new TextPanel<String>(componentId, new PropertyModel<String>(model, expression));
        }
        List<FormComponent> components = panel.getFormComponents();
        for (FormComponent component : components) {
            Component c = component.get("date");
            if (c != null) {
                c.add(new AjaxFormComponentUpdatingBehavior("change") {
                    
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        model.getObject().setValue(MiscUtil.asXMLGregorianCalendar((Date) getFormComponent().getConvertedInput()));
                        int i = 0;
                    }
                });
            }
            component.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        }
        return panel;
        
    }
    
    private void runConfirmPerformed(AjaxRequestTarget target, IModel<ReportDto> model, List<JasperReportParameterDto> params) {
        ReportDto reportDto = model.getObject();

//    	List<ReportParameterType> reportParams = new ArrayList<ReportParameterType>();
        PrismContainerDefinition<ReportParameterType> paramContainterDef = getPrismContext().getSchemaRegistry().findContainerDefinitionByElementName(ReportConstants.REPORT_PARAMS_PROPERTY_NAME);
        PrismContainer<ReportParameterType> paramContainer = paramContainterDef.instantiate();
        try {
            
            ReportParameterType reportParam = new ReportParameterType();
            PrismContainerValue<ReportParameterType> reportParamValue = reportParam.asPrismContainerValue();
            reportParamValue.revive(getPrismContext());
            paramContainer.add(reportParamValue);
            for (JasperReportParameterDto paramDto : params) {
                if (paramDto.getValue() == null) {
                    continue;
                }
                QName typeName = null;
                Object realValue = paramDto.getValue();
                Class paramClass = paramDto.getType();
                if (XmlTypeConverter.canConvert(paramClass)) {
                    typeName = XsdTypeMapper.toXsdType(paramClass);
                } else {
                    
                    if (AuditEventType.class.isAssignableFrom(paramClass)) {
                        paramClass = AuditEventTypeType.class;
                        realValue = AuditEventType.fromAuditEventType((AuditEventType) realValue);
                    } else if (AuditEventStage.class.isAssignableFrom(paramClass)) {
                        paramClass = AuditEventStageType.class;
                        realValue = AuditEventStage.fromAuditEventStage((AuditEventStage) realValue);
                    }
                    typeName = getPrismContext().getBeanConverter().determineTypeForClass(paramClass);
                }
                PrismPropertyDefinition def = new PrismPropertyDefinition<>(new QName(ReportConstants.NS_EXTENSION, paramDto.getName()), typeName, getPrismContext());
                def.setDynamic(true);
                def.setRuntimeSchema(true);
                PrismProperty prop = def.instantiate();
                prop.addRealValue(realValue);
                reportParamValue.add(prop);
                //setPropertyRealValue(new QName(ReportConstants.NS_EXTENSION, paramDto.getName()), paramDto.getValue(), getPrismContext());
            }
        } catch (SchemaException | ClassNotFoundException e) {
            OperationResult result = new OperationResult("Parameters serialization");
            result.recordFatalError("Could not serialize parameters");
            getPageBase().showResult(result);
            return;
        }
        
        runConfirmPerformed(target, reportDto.getObject().asObjectable(), paramContainer);
        
    }
    
    private PrismContext getPrismContext() {
        return getPageBase().getPrismContext();
    }
    
    protected void runConfirmPerformed(AjaxRequestTarget target, ReportType reportType2,
            PrismContainer<ReportParameterType> reportParam) {
    }
    
    private static class EmptyOnBlurAjaxFormUpdatingBehaviour extends AjaxFormComponentUpdatingBehavior {
        
        public EmptyOnBlurAjaxFormUpdatingBehaviour() {
            super("change");
        }
        
        @Override
        protected void onUpdate(AjaxRequestTarget target) {
        }
    }
    
}
