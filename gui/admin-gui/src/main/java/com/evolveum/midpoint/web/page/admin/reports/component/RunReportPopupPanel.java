/*
 * Copyright (c) 2010-2016 Evolveum
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
package com.evolveum.midpoint.web.page.admin.reports.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.report.api.ReportConstants;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.EditablePropertyColumn;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.input.DatePanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.model.LookupPropertyModel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.reports.dto.JasperReportParameterDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReportDto;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportParameterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import net.sf.jasperreports.engine.JRPropertiesMap;

public class RunReportPopupPanel extends BasePanel<ReportDto> implements Popupable {

  private static final long serialVersionUID = 1L;
//	@SpringBean(name = "modelController")
//    private ModelService modelService;
//    @SpringBean(name = "taskManager")
//    private TaskManager taskManager;
    private static final Trace LOGGER = TraceManager.getTrace(RunReportPopupPanel.class);

    private static final String DOT_CLASS = RunReportPopupPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_RESOURCES = DOT_CLASS + "createResourceList";

    private static final String ID_MAIN_FORM = "mainForm";

    private static final String ID_NAME_LABEL = "label";
    private static final String ID_RUN = "runReport";

    private static final String ID_LABEL_SIZE = "col-md-4";
    private static final String ID_INPUT_SIZE = "col-md-8";

    private static final String ID_PARAMETERS_TABLE = "paramTable";
    private static final Integer AUTO_COMPLETE_BOX_SIZE = 10;

    private IModel<ReportDto> reportModel;
    private ReportType reportType;

//    public void setReportType(ReportType reportType) {
//        this.reportType = reportType;
//
//        if (getParametersTable() != null) {
//            replace(createTablePanel());
//        }
//    }

    public RunReportPopupPanel(String id, final ReportType reportType) {
        super(id);

        reportModel = new LoadableModel<ReportDto>(false) {

            @Override
            protected ReportDto load() {
                return new ReportDto(reportType, true);
            }
        };

        initLayout();
    }

//    @Override
    protected void initLayout() {

    	Form<?> mainForm = new Form(ID_MAIN_FORM);
    	add(mainForm);

    	BoxedTablePanel<JasperReportParameterDto> table = createTablePanel();
        mainForm.add(table);

        AjaxSubmitButton addButton = new AjaxSubmitButton(ID_RUN,
                createStringResource("runReportPopupContent.button.run")) {

        	@Override
        	protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
        		runConfirmPerformed(target, reportModel, getParameters());
        	}
//                    @Override
//                    public void onClick(AjaxRequestTarget target) {
//                        runConfirmPerformed(target, reportModel, getParameters());
//                    }
                };
        mainForm.add(addButton);

    }

    private BoxedTablePanel<JasperReportParameterDto> createTablePanel() {


        ISortableDataProvider<JasperReportParameterDto, String> provider = new ListDataProvider<>(this,
                new PropertyModel<List<JasperReportParameterDto>>(reportModel, "jasperReportDto.parameters"));
        BoxedTablePanel<JasperReportParameterDto> table = new BoxedTablePanel<>(ID_PARAMETERS_TABLE, provider, initParameterColumns());
        table.setOutputMarkupId(true);
        table.setShowPaging(true);
        return table;
    }

    private BoxedTablePanel<JasperReportParameterDto> getParametersTable() {
        return (BoxedTablePanel) get(createComponentPath(ID_MAIN_FORM, ID_PARAMETERS_TABLE));
    }

    private List<JasperReportParameterDto> getParameters() {
    	BoxedTablePanel<JasperReportParameterDto> table = getParametersTable();
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
                StringResourceModel paramDisplay = PageBase.createStringResourceStatic(RunReportPopupPanel.this, "runReportPopupContent.param.name." + paramValue, new Object[]{});
//                StringResourceModel paramDisplay = new StringResourceModel("runReportPopupContent.param.name." + paramValue, null, paramValue, new Object[]{});
                item.add(new Label(componentId, paramDisplay)); // use display name rather than property name
            }
        });

        //parameter class column
        columns.add(new AbstractColumn(createStringResource("runReportPopupContent.param.class")) {

            @Override
            public void populateItem(Item item, String componentId, IModel model) {
                //Do not show whole parameter class name as this is not very user friendly
                String paramClass = new PropertyModel<String>(model, "typeAsString").getObject();
                paramClass = (paramClass == null) ? "" : paramClass.substring(paramClass.lastIndexOf(".") + 1);
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

    private Component createTypedInputPanel(String componentId, IModel<JasperReportParameterDto> model, String expression) {
        final JasperReportParameterDto param = model.getObject();
        param.setEditing(true);

        IModel label = new PropertyModel<String>(model, "name");
        PropertyModel value = new PropertyModel<String>(model, expression);
        String tooltipKey = model.getObject().getTypeAsString();
        Class type = null;
        final LookupTableType lookup;

        try {
            type = param.getType();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            return null;
        }
        InputPanel panel = null;

        if (type.isEnum()) {
            panel = WebComponentUtil.createEnumPanel(type, componentId, new PropertyModel(model, expression), this);
        } else if (XMLGregorianCalendar.class.isAssignableFrom(type)) {
            panel = new DatePanel(componentId, new PropertyModel<XMLGregorianCalendar>(model, expression));
        } else if (param.getPropertyTargetType() != null) { // render autocomplete box
            lookup = new LookupTableType();
            panel = new AutoCompleteTextPanel<String>(componentId, new LookupPropertyModel<String>(model, expression,
                    lookup, false), String.class) {

                        @Override
                        public Iterator<String> getIterator(String input) {
                            return prepareAutoCompleteList(input, lookup, param).iterator();
                        }
                    };
        } else {
            panel = new TextPanel<String>(componentId, new PropertyModel<String>(model, expression));
        }
        List<FormComponent> components = panel.getFormComponents();
        for (FormComponent component : components) {
//            if (component instanceof DateInput) {
//                addFormUpdatingBehavior(component, "date", model);
//                addFormUpdatingBehavior(component, "hours", model);
//                addFormUpdatingBehavior(component, "minutes", model);
//                addFormUpdatingBehavior(component, "amOrPmChoice", model);
//            } else {
                component.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
//            }
        }
        return panel;

    }

    private <T extends ObjectType> List<LookupTableRowType> createLookupTableRows(JasperReportParameterDto param, String input) {
        ItemPath label = null;
        ItemPath key = null;
        List<LookupTableRowType> rows = new ArrayList();

        JRPropertiesMap properties = param.getProperties();

        if (properties == null) {
            return null;
        }

        String pLabel = properties.getProperty("label");
        if (pLabel != null) {
            label = new ItemPath(pLabel);
        }
        String pKey = properties.getProperty("key");
        if (pKey != null) {
            key = new ItemPath(pKey);
        }

        String pTargetType = properties.getProperty("targetType");
        Class<T> targetType = null;
        if (pTargetType != null) {
            try {
                targetType = (Class<T>) Class.forName(pTargetType);
            } catch (ClassNotFoundException e) {
                error("Error while creating lookup table for input parameter: " + param.getName() + ", " + e.getClass().getSimpleName() + " (" + e.getMessage() + ")");
                //e.printStackTrace();
            }
        }

        if (label != null && targetType != null && input != null) {
            OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCES);
            Task task = createSimpleTask(OPERATION_LOAD_RESOURCES);

            Collection<PrismObject<T>> objects;
            ObjectQuery query = QueryBuilder.queryFor(targetType, getPrismContext())
                    .item(new QName(SchemaConstants.NS_C, pLabel)).startsWith(input)
                        .matching(new QName(SchemaConstants.NS_MATCHING_RULE, "origIgnoreCase"))
                    .maxSize(AUTO_COMPLETE_BOX_SIZE)
                    .build();
            try {
                objects = getPageBase().getModelService().searchObjects(targetType, query, SelectorOptions.createCollection(GetOperationOptions.createNoFetch()), task, result);

                for (PrismObject<T> o : objects) {
                    Object realKeyValue = null;
                    PrismProperty labelItem = o.findProperty(label);

                    //TODO: e.g. support not only for property, but also ref, container..
                    if (labelItem == null || labelItem.isEmpty()) {
                        continue;
                    }
                    PrismProperty keyItem = o.findProperty(key);
                    if ("oid".equals(pKey)) {
                        realKeyValue = o.getOid();
                    }
                    if (realKeyValue == null && (keyItem == null || keyItem.isEmpty())) {
                        continue;
                    }

                    //TODO: support for single/multivalue value 
                    if (!labelItem.isSingleValue()) {
                        continue;
                    }

                    Object realLabelValue = labelItem.getRealValue();
                    realKeyValue = (realKeyValue == null) ? keyItem.getRealValue() : realKeyValue;

                    // TODO: take definition into account
                    QName typeName = labelItem.getDefinition().getTypeName();

                    LookupTableRowType row = new LookupTableRowType();

                    if (realKeyValue != null) {
                        row.setKey(convertObjectToPolyStringType(realKeyValue).getOrig());
                    } else {
                        throw new SchemaException("Cannot create lookup table with null key for label: " + realLabelValue);
                    }

                    row.setLabel(convertObjectToPolyStringType(realLabelValue));

                    rows.add(row);
                }

                return rows;
            } catch (SchemaException | ObjectNotFoundException | SecurityViolationException | CommunicationException | ConfigurationException e) {
                error("Error while creating lookup table for input parameter: " + param.getName() + ", " + e.getClass().getSimpleName() + " (" + e.getMessage() + ")");
                //e.printStackTrace();
            }

        }
        return rows;
    }

    private PolyStringType convertObjectToPolyStringType(Object o) {
        if (o instanceof PolyString) {
            return new PolyStringType((PolyString) o);
        } else if (o instanceof PolyStringType) {
            return (PolyStringType) o;
        } else if (o instanceof String) {
            return new PolyStringType((String) o);
        } else {
            return new PolyStringType(o.toString());
        }
    }

    private List<String> prepareAutoCompleteList(String input, LookupTableType lookupTable, JasperReportParameterDto param) {
        List<String> values = new ArrayList<>();

        if (lookupTable == null) {
            return values;
        }

        List<LookupTableRowType> rows = createLookupTableRows(param, input);

        if (rows == null) {
            return values;
        }
        if (lookupTable.getRow() != null) {
            lookupTable.getRow().addAll(rows);
        }

        for (LookupTableRowType row : rows) {
            values.add(WebComponentUtil.getOrigStringFromPoly(row.getLabel()));

            if (values.size() > AUTO_COMPLETE_BOX_SIZE) {
                break;
            }
        }

        return values;
    }

    private IModel<List<String>> createUserAttributeListModel(Class type) {
        final List<String> attrList = new ArrayList();
        PrismObjectDefinition<UserType> userDefinition = getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        List<? extends ItemDefinition> itemDefs = userDefinition.getDefinitions();

        for (ItemDefinition id : itemDefs) {
            if (id instanceof PrismPropertyDefinition
                    && ((((PrismPropertyDefinition) id).isIndexed() == null)
                    || (((PrismPropertyDefinition) id).isIndexed() == true))) {
                if (id.getTypeClass() != null && id.getTypeClass().equals(type)) {
                    attrList.add(id.getName().getLocalPart());
                }
            }
        }

        return new AbstractReadOnlyModel<List<String>>() {

            @Override
            public List<String> getObject() {
                return attrList;
            }

        };
    }

    private IModel<List<String>> createResourceListModel() {
        OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCES);
        Task task = createSimpleTask(OPERATION_LOAD_RESOURCES);
        List<PrismObject<ResourceType>> resources = null;
        final List<String> resourceList = new ArrayList();

        try {
            resources = getPageBase().getModelService().searchObjects(ResourceType.class, new ObjectQuery(), null, task, result);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
        }

        for (PrismObject<ResourceType> resource : resources) {
            resourceList.add(resource.getBusinessDisplayName());
        }

        return new AbstractReadOnlyModel<List<String>>() {

            @Override
            public List<String> getObject() {
                return resourceList;
            }
        };
    }

    public Task createSimpleTask(String operation, PrismObject<UserType> owner) {
        Task task = getPageBase().getTaskManager().createTaskInstance(operation);

        if (owner == null) {
            MidPointPrincipal user = SecurityUtils.getPrincipalUser();
            if (user == null) {
                return task;
            } else {
                owner = user.getUser().asPrismObject();
            }
        }

        task.setOwner(owner);
        task.setChannel(SchemaConstants.CHANNEL_GUI_USER_URI);

        return task;
    }

    public Task createSimpleTask(String operation) {
        MidPointPrincipal user = SecurityUtils.getPrincipalUser();
        return createSimpleTask(operation, user != null ? user.getUser().asPrismObject() : null);
    }

//    private void addFormUpdatingBehavior(FormComponent parent, String id, final IModel<JasperReportParameterDto> model) {
//        Component c = parent.get(id);
//        if (c == null) {
//            return;
//        }
//        c.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
//    }

    private void runConfirmPerformed(AjaxRequestTarget target, IModel<ReportDto> model, List<JasperReportParameterDto> params) {
        ReportDto reportDto = model.getObject();

//    	List<ReportParameterType> reportParams = new ArrayList<ReportParameterType>();
        PrismContainerDefinition<ReportParameterType> paramContainterDef = getPrismContext().getSchemaRegistry().findContainerDefinitionByElementName(ReportConstants.REPORT_PARAMS_PROPERTY_NAME);
        PrismContainer<ReportParameterType> paramContainer;
        try {

        	paramContainer = paramContainterDef.instantiate();
            ReportParameterType reportParam = new ReportParameterType();
            PrismContainerValue<ReportParameterType> reportParamValue = reportParam.asPrismContainerValue();
            reportParamValue.revive(getPrismContext());
            paramContainer.add(reportParamValue);
            Class attributeNameClass = null;
            for (JasperReportParameterDto paramDto : params) {
                if (paramDto.getValue() == null) {
                    continue;
                }
                QName typeName = null;
                Object realValue = paramDto.getValue();
                Class paramClass = paramDto.getType();
                /*
                 if ("attributeName".equals(paramDto.getName())) {                    
                 attributeNameClass = ((ItemDefinition) realValue).getTypeClass();
                 realValue = ((ItemDefinition) realValue).getName().getLocalPart();
                 }
                 if ("attributeValue".equals(paramDto.getName())) {
                 if (attributeNameClass.equals(String.class)) {
                 realValue = getPrismContext().serializeAnyData((String) realValue, 
                 new QName(ReportConstants.NS_EXTENSION, paramDto.getName()), PrismContext.LANG_XML);
                 } else if (attributeNameClass.equals(PolyString.class)) {
                 realValue = getPrismContext().serializeAnyData(new PolyStringType((String) realValue),
                 new QName(ReportConstants.NS_EXTENSION, paramDto.getName()), PrismContext.LANG_XML);
                 }
                 }
                 */
                if (AuditEventType.class.isAssignableFrom(paramClass)) {
                    paramClass = AuditEventTypeType.class;
                    realValue = AuditEventType.fromAuditEventType((AuditEventType) realValue);
                } else if (AuditEventStage.class.isAssignableFrom(paramClass)) {
                    paramClass = AuditEventStageType.class;
                    realValue = AuditEventStage.fromAuditEventStage((AuditEventStage) realValue);
                }
                typeName = getPrismContext().getSchemaRegistry().determineTypeForClass(paramClass);
                PrismPropertyDefinitionImpl def = new PrismPropertyDefinitionImpl<>(new QName(ReportConstants.NS_EXTENSION, paramDto.getName()), typeName, getPrismContext());
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

	@Override
	public int getWidth() {
		return 1100;
	}

	@Override
	public int getHeight() {
		return 560;
	}

	@Override
	public StringResourceModel getTitle() {
		return createStringResource("RunReportPopupPanel.title");
	}

	@Override
	public Component getComponent() {
		return this;
	}

//    private static class EmptyOnBlurAjaxFormUpdatingBehaviour extends AjaxFormComponentUpdatingBehavior {
//
//        public EmptyOnBlurAjaxFormUpdatingBehaviour() {
//            super("change");
//        }
//
//        @Override
//        protected void onUpdate(AjaxRequestTarget target) {
//            MarkupContainer parent = getFormComponent().getParent();
//            if (parent instanceof DateInput) {
//                DateInput i = (DateInput) parent;
//                i.updateDateTimeModel();
//            }
//        }
//    }

}
