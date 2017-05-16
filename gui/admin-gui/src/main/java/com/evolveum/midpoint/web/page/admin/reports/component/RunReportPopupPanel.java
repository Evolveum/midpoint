/*
 * Copyright (c) 2010-2017 Evolveum
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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
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
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.report.api.ReportConstants;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.input.DatePanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.LookupPropertyModel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.reports.dto.JasperReportParameterDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.JasperReportParameterPropertiesDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.JasperReportValueDto;
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

public class RunReportPopupPanel extends BasePanel<ReportDto> implements Popupable {

	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(RunReportPopupPanel.class);

    private static final String DOT_CLASS = RunReportPopupPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_RESOURCES = DOT_CLASS + "createResourceList";

    private static final String ID_MAIN_FORM = "mainForm";

    private static final String ID_RUN = "runReport";

    private static final Integer AUTO_COMPLETE_BOX_SIZE = 10;
    
    private static final String ID_VALUE_LIST = "valueList";
    private static final String ID_ADD_BUTTON = "add";
    private static final String ID_REMOVE_BUTTON = "remove";
    
    private static final String ID_PARAMETERS = "paramTable";

    private IModel<ReportDto> reportModel;
    
    public RunReportPopupPanel(String id, final ReportType reportType) {
        super(id);

        reportModel = new LoadableModel<ReportDto>(false) {

    		private static final long serialVersionUID = 1L;

			@Override
            protected ReportDto load() {
                return new ReportDto(reportType, true);
            }
        };

        initLayout();
    }

    protected void initLayout() {

    	Form<?> mainForm = new Form<>(ID_MAIN_FORM);
    	add(mainForm);

        
        ListView<JasperReportParameterDto> paramListView = new ListView<JasperReportParameterDto>(ID_PARAMETERS, new PropertyModel<>(reportModel, "jasperReportDto.parameters")) {

        	private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<JasperReportParameterDto> paramItem) {
				paramItem.add(createParameterPanel(paramItem.getModel()));
				
			}
		};
		paramListView.setOutputMarkupId(true);
		mainForm.add(paramListView);
		
        AjaxSubmitButton addButton = new AjaxSubmitButton(ID_RUN,
                createStringResource("runReportPopupContent.button.run")) {
        	private static final long serialVersionUID = 1L;

        	@Override
        	protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
        		runConfirmPerformed(target, reportModel);
        	}
                };
        mainForm.add(addButton);

    }
    
    private WebMarkupContainer createParameterPanel(final IModel<JasperReportParameterDto> parameterModel) {
    	WebMarkupContainer paramPanel = new WebMarkupContainer("paramPanel");
    	paramPanel.setOutputMarkupId(true);
    	String paramValue = new PropertyModel<String>(parameterModel, "name").getObject();
        StringResourceModel paramDisplay = PageBase.createStringResourceStatic(RunReportPopupPanel.this, "runReportPopupContent.param.name." + paramValue, new Object[]{});

        paramPanel.add(new Label("name", paramDisplay)); // use display name rather than property name

        String paramClass = new PropertyModel<String>(parameterModel, "nestedTypeAsString").getObject();
        if (StringUtils.isBlank(paramClass)) {
        	paramClass = new PropertyModel<String>(parameterModel, "typeAsString").getObject();
        }
        paramClass = paramClass == null ? "" : paramClass.substring(paramClass.lastIndexOf(".") + 1);
        paramPanel.add(new Label("type", paramClass));
        
        ListView<JasperReportValueDto> listView = new ListView<JasperReportValueDto>(ID_VALUE_LIST, new PropertyModel<>(parameterModel, "value")) {

    		private static final long serialVersionUID = 1L;

			@Override
    		protected void populateItem(ListItem<JasperReportValueDto> item) {
    			item.add(createInputMarkup(item.getModel(), parameterModel.getObject()));
    			
    		}

    	};
    	listView.setOutputMarkupId(true);
        
        paramPanel.add(listView);
		return paramPanel;
    }
 
	private WebMarkupContainer createInputMarkup(final IModel<JasperReportValueDto> valueModel, JasperReportParameterDto param) {
		param.setEditing(true);
		WebMarkupContainer paramValueMarkup = new WebMarkupContainer("valueMarkup");
		paramValueMarkup.setOutputMarkupId(true);
    
		InputPanel input = createTypedInputPanel("inputValue", valueModel, "value", param);
		paramValueMarkup.add(input);
    
		//buttons
		AjaxLink<String> addButton = new AjaxLink<String>(ID_ADD_BUTTON) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				addValue(paramValueMarkup, param, target);
			}
		};
		addButton.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return isAddButtonVisible(param);
			}
		});
		paramValueMarkup.add(addButton);

		AjaxLink<String> removeButton = new AjaxLink<String>(ID_REMOVE_BUTTON) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				removeValue(paramValueMarkup, param, valueModel.getObject(), target);
			}
		};
		removeButton.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return isRemoveButtonVisible();
			}
		});
		paramValueMarkup.add(removeButton);
		return paramValueMarkup;
	}
    
    private InputPanel createTypedInputPanel(String componentId, IModel<JasperReportValueDto> model, String expression, JasperReportParameterDto param) {
    	InputPanel panel;
    	Class<?> type;
        try {
        	if (param.isMultiValue()) {
        		type = param.getNestedType();
        	} else {
        		type = param.getType();
        	}
        } catch (ClassNotFoundException e) {
            getSession().error("Could not find parameter type definition. Check the configuration.");
            throw new RestartResponseException(getPageBase());
        }

        if (type.isEnum()) {
            panel = WebComponentUtil.createEnumPanel(type, componentId, new PropertyModel<>(model, expression), this);
        } else if (XMLGregorianCalendar.class.isAssignableFrom(type)) {
            panel = new DatePanel(componentId, new PropertyModel<>(model, expression));
        } else if (param.getProperties() != null && param.getProperties().getTargetType() != null) { // render autocomplete box
        	LookupTableType lookup = new LookupTableType();
            panel = new AutoCompleteTextPanel<String>(componentId, new LookupPropertyModel<>(model, expression, lookup, false), String.class) {
				@Override
				public Iterator<String> getIterator(String input) {
					return prepareAutoCompleteList(input, lookup, param).iterator();
				}
			};
		} else {
            panel = new TextPanel<>(componentId, new PropertyModel<>(model, expression), type);
        }
        List<FormComponent> components = panel.getFormComponents();
        for (FormComponent component : components) {
                component.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        }
        panel.setOutputMarkupId(true);
        return panel;
    }
    
    
    private void addValue(WebMarkupContainer paramValueMarkup, JasperReportParameterDto valueModel, AjaxRequestTarget target) {
    	valueModel.addValue();
    	//reload just current parameter container panel
    	target.add(paramValueMarkup.findParent(ListView.class).findParent(WebMarkupContainer.class));
    }
    
    private ListView getParametersView(){
		return (ListView) get(createComponentPath(ID_MAIN_FORM, ID_PARAMETERS));
    }

    private boolean isAddButtonVisible(JasperReportParameterDto valueDecs) {
    	try {
			return valueDecs.isMultiValue();
		} catch (ClassNotFoundException e) {
			return false;
		}
    }
    
	private void removeValue(WebMarkupContainer paramValueMarkup, JasperReportParameterDto valueModel,
			JasperReportValueDto object, AjaxRequestTarget target) {
		valueModel.removeValue(object);
		//reload just current parameter container panel
		target.add(paramValueMarkup.findParent(ListView.class).findParent(WebMarkupContainer.class));
	}
       
	private boolean isRemoveButtonVisible() {
		return true;
	}
   
    private <O extends ObjectType> List<LookupTableRowType> createLookupTableRows(JasperReportParameterDto param, String input) {
        ItemPath label = null;
        ItemPath key = null;
        List<LookupTableRowType> rows = new ArrayList<>();

        JasperReportParameterPropertiesDto properties = param.getProperties();

        if (properties == null) {
            return null;
        }

        String pLabel = properties.getLabel();
        if (pLabel != null) {
            label = new ItemPath(pLabel);
        }
        String pKey = properties.getKey();
        if (pKey != null) {
            key = new ItemPath(pKey);
        }

        String pTargetType = properties.getTargetType();
        Class<O> targetType = null;
        if (pTargetType != null) {
            try {
                targetType = (Class<O>) Class.forName(pTargetType);
            } catch (ClassNotFoundException e) {
                error("Error while creating lookup table for input parameter: " + param.getName() + ", " + e.getClass().getSimpleName() + " (" + e.getMessage() + ")");
                //e.printStackTrace();
            }
        }

        if (label != null && targetType != null && input != null) {
            OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCES);
            Task task = createSimpleTask(OPERATION_LOAD_RESOURCES);

            Collection<PrismObject<O>> objects;
            ObjectQuery query = QueryBuilder.queryFor(targetType, getPrismContext())
                    .item(new QName(SchemaConstants.NS_C, pLabel)).startsWith(input)
                        .matching(new QName(SchemaConstants.NS_MATCHING_RULE, "origIgnoreCase"))
                    .maxSize(AUTO_COMPLETE_BOX_SIZE)
                    .build();
            try {
                objects = getPageBase().getModelService().searchObjects(targetType, query, SelectorOptions.createCollection(GetOperationOptions.createNoFetch()), task, result);

                for (PrismObject<O> o : objects) {
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
            } catch (SchemaException | ObjectNotFoundException | SecurityViolationException | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
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

    private void runConfirmPerformed(AjaxRequestTarget target, IModel<ReportDto> model) {
        ReportDto reportDto = model.getObject();

        PrismContainerDefinition<ReportParameterType> paramContainerDef = getPrismContext().getSchemaRegistry().findContainerDefinitionByElementName(ReportConstants.REPORT_PARAMS_PROPERTY_NAME);
        PrismContainer<ReportParameterType> paramContainer;
        try {
        	paramContainer = paramContainerDef.instantiate();
            ReportParameterType reportParam = new ReportParameterType();
            PrismContainerValue<ReportParameterType> reportParamValue = reportParam.asPrismContainerValue();
            reportParamValue.revive(getPrismContext());
            paramContainer.add(reportParamValue);
            
            List<JasperReportParameterDto> params = getParametersView().getModelObject();
            for (JasperReportParameterDto paramDto : params) {
                if (paramDto.getValue() == null) {
                    continue;
                }
                List<JasperReportValueDto> values = paramDto.getValue();
                Class<?> paramClass = paramDto.getType();
                
                boolean multivalue = false;
                if (List.class.isAssignableFrom(paramClass)) {
                	paramClass = paramDto.getNestedType();
                	if (paramClass == null) {
                		getSession().error("Nested type for list must be defined!");
                        target.add(getPageBase().getFeedbackPanel());
                        return;
                	}
                }

                QName typeName = getPrismContext().getSchemaRegistry().determineTypeForClass(paramClass);
                PrismPropertyDefinitionImpl<?> def = new PrismPropertyDefinitionImpl<>(new QName(ReportConstants.NS_EXTENSION, paramDto.getName()), typeName, getPrismContext());
                def.setDynamic(true);
                def.setRuntimeSchema(true);
                def.setMaxOccurs(multivalue ? -1 : 1);			// TODO multivalue is always 'false' here ...
                
                PrismProperty prop = def.instantiate();
				for (JasperReportValueDto paramValue : values) {
					Object realValue = paramValue.getValue();
					if (realValue == null) {
						continue;
					}
					if (AuditEventType.class.isAssignableFrom(paramClass)) {
						paramClass = AuditEventTypeType.class;
						realValue = AuditEventType.fromAuditEventType((AuditEventType) realValue);
					} else if (AuditEventStage.class.isAssignableFrom(paramClass)) {
						paramClass = AuditEventStageType.class;
						realValue = AuditEventStage.fromAuditEventStage((AuditEventStage) realValue);
					}

					prop.addRealValue(realValue);
				}
				if (!prop.isEmpty()) {
					reportParamValue.add(prop);
				}
            }
        } catch (SchemaException | ClassNotFoundException e) {
            OperationResult result = new OperationResult("Parameters serialization");
            result.recordFatalError("Could not serialize parameters");
            getPageBase().showResult(result);
            return;
        }

        runConfirmPerformed(target, reportDto.getObject().asObjectable(), paramContainer);
    }
    
//    private boolean isMultiValue(JasperReportParameterDto param){
//    	if (param == null) {
//    		return false;
//    	}
//    	
//    	if (param.getProperties() == null) {
//    		return false;
//    	}
//    	
//    	return param.getProperties().getMultivalue();
//    	
//    }

    private PrismContext getPrismContext() {
        return getPageBase().getPrismContext();
    }

    protected void runConfirmPerformed(AjaxRequestTarget target, ReportType reportType2,
			PrismContainer<ReportParameterType> reportParam) {
    }

	@Override
	public int getWidth() {
		return 1150;
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

}
