/**
 * Copyright (c) 2010-2019 Evolveum
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
package com.evolveum.midpoint.gui.impl.component.box;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.interaction.DashboardService;
import com.evolveum.midpoint.model.api.interaction.DashboardWidget;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.reports.PageAuditLogViewer;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditEventRecordProvider;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditSearchDto;
import com.evolveum.midpoint.web.page.admin.resources.PageResource;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.roles.PageRole;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.services.PageService;
import com.evolveum.midpoint.web.page.admin.services.PageServices;
import com.evolveum.midpoint.web.page.admin.users.PageOrgTree;
import com.evolveum.midpoint.web.page.admin.users.PageOrgUnit;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuditSearchType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetDataFieldTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetPresentationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetSourceTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IntegerStatType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * @author skublik
 */
public abstract class InfoBoxPanel extends Panel{
	private static final long serialVersionUID = 1L;
	
	private static final Trace LOGGER = TraceManager.getTrace(InfoBoxPanel.class);

	private static final String ID_INFO_BOX = "infoBox";
	private static final String ID_ICON = "icon";
	private static final String ID_MESSAGE = "message";
	private static final String ID_NUMBER = "number";
	
	private static final String DEFAULT_BACKGROUND_COLOR = "background-color:#00a65a;";
	private static final String DEFAULT_COLOR = "color: #fff !important;";
	private static final String DEFAULT_ICON = "fa fa-question";
	
	private static final String NUMBER_MESSAGE_UNKNOWN = "InfoBoxPanel.message.unknown";

	public static final String VAR_PROPORTIONAL = "proportional";
//	private static final String VAR_POLICY_SITUATIONS = "policySituations";
	
	private static HashMap<String, Class<? extends WebPage>> linksRefCollections;
	private static HashMap<QName, Class<? extends WebPage>> linksRefObjects;
	
	static {
	    linksRefCollections = new HashMap<String, Class<? extends WebPage>>() {
			private static final long serialVersionUID = 1L;

			{
	            put(ResourceType.COMPLEX_TYPE.getLocalPart(), PageResources.class);
	            put(AuditEventRecordItemType.COMPLEX_TYPE.getLocalPart(), PageAuditLogViewer.class);
	            put(TaskType.COMPLEX_TYPE.getLocalPart(), PageTasks.class);
	            put(UserType.COMPLEX_TYPE.getLocalPart(), PageUsers.class);
	            put(RoleType.COMPLEX_TYPE.getLocalPart(), PageRoles.class);
	            put(OrgType.COMPLEX_TYPE.getLocalPart(), PageOrgTree.class);
	            put(ServiceType.COMPLEX_TYPE.getLocalPart(), PageServices.class);
	        }
	    };
	    
	    linksRefObjects = new HashMap<QName, Class<? extends WebPage>>() {
			private static final long serialVersionUID = 1L;

			{
	            put(TaskType.COMPLEX_TYPE, PageTaskEdit.class);
	            put(UserType.COMPLEX_TYPE, PageUser.class);
	            put(RoleType.COMPLEX_TYPE, PageRole.class);
	            put(OrgType.COMPLEX_TYPE, PageOrgUnit.class);
	            put(ServiceType.COMPLEX_TYPE, PageService.class);
	            put(ResourceType.COMPLEX_TYPE, PageResource.class);
	        }
	    };
	}
	
	private static PageBase pageBase;
	private DisplayType display;
//	private DisplayType variationDisplay;

	public InfoBoxPanel(String id, IModel<DashboardWidgetType> model, PageBase pageBase) {
		super(id, model);
		Validate.notNull(model, "Model must not be null.");
		Validate.notNull(model.getObject(), "Model object must not be null.");
		add(AttributeModifier.append("class", "dashboard-info-box"));
		this.pageBase = pageBase;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		initLayout();
		
	}

	private void initLayout() {
		IModel<DashboardWidgetType> model = (IModel<DashboardWidgetType>)getDefaultModel();
		IModel<DashboardWidget> data = new IModel<DashboardWidget>() {
			private static final long serialVersionUID = 1L;

			@Override
			public DashboardWidget getObject() {
				Task task = getPageBase().createSimpleTask("Get DashboardWidget");
				try {
					DashboardWidget ret = getPageBase().getDashboardService().createWidgetData(model.getObject(), task, task.getResult());
					setDisplay(ret.getDisplay());
					return ret;
				} catch (SchemaException | CommunicationException | ConfigurationException | SecurityViolationException
						| ExpressionEvaluationException | ObjectNotFoundException e) {
					LOGGER.error("Couldn't get DashboardWidget with widget " + model.getObject().getIdentifier(), e);
				}
				return null;
			}
		};
		
		this.display = model.getObject().getDisplay();
		
		WebMarkupContainer infoBox = new WebMarkupContainer(ID_INFO_BOX);
		add(infoBox);
		
		Label number = new Label(ID_NUMBER, 
				data.getObject().getNumberMessage() == null ? 
						getPageBase().createStringResource(NUMBER_MESSAGE_UNKNOWN) : 
							getStringModel(data.getObject().getNumberMessage())); //number message have to add before icon because is needed evaluate variation
		infoBox.add(number);

		IModel<DisplayType> displayModel = new IModel<DisplayType>() {
			private static final long serialVersionUID = 1L;

			@Override
			public DisplayType getObject() {
				return display;
			}
		};
		
        Label message = null;
        if(displayModel.getObject() != null && displayModel.getObject().getLabel() != null) {
        	message = new Label(ID_MESSAGE, new PropertyModel<String>(displayModel, "label"));
		} else {
			message = new Label(ID_MESSAGE, new PropertyModel<String>(model, "identifier"));
		}
        infoBox.add(message);
       
        if(displayModel.getObject() != null && StringUtils.isNoneBlank(displayModel.getObject().getColor())) {
			String color = displayModel.getObject().getColor();
			infoBox.add(AttributeModifier.append("style", getStringModel("background-color:" + color + ";")));
		} else {
			infoBox.add(AttributeModifier.append("style", getStringModel(DEFAULT_BACKGROUND_COLOR)));
		}
		infoBox.add(AttributeModifier.append("style", getStringModel(DEFAULT_COLOR)));
        
        WebMarkupContainer infoBoxIcon = new WebMarkupContainer(ID_ICON);
		infoBox.add(infoBoxIcon);
		if(displayModel.getObject() != null && displayModel.getObject().getIcon() != null
				&& StringUtils.isNoneBlank(displayModel.getObject().getIcon().getCssClass())) {
			infoBoxIcon.add(AttributeModifier.append("class", new PropertyModel<String>(displayModel, "icon.cssClass")));
		} else {
			infoBoxIcon.add(AttributeModifier.append("class", getStringModel(DEFAULT_ICON)));
		}
        
        customInitLayout(infoBox);
	}
	
	public void setDisplay(DisplayType display) {
		this.display = display;
	}
	
//	private DisplayType combinateDisplay() {
//		DisplayType combinatedDisplay = new DisplayType();
//		if (variationDisplay == null) {
//			return display;
//		}
//		if(StringUtils.isBlank(variationDisplay.getColor())) {
//			combinatedDisplay.setColor(display.getColor());
//		} else {
//			combinatedDisplay.setColor(variationDisplay.getColor());
//		}
//		if(StringUtils.isBlank(variationDisplay.getCssClass())) {
//			combinatedDisplay.setCssClass(display.getCssClass());
//		} else {
//			combinatedDisplay.setCssClass(variationDisplay.getCssClass());
//		}
//		if(StringUtils.isBlank(variationDisplay.getCssStyle())) {
//			combinatedDisplay.setCssStyle(display.getCssStyle());
//		} else {
//			combinatedDisplay.setCssStyle(variationDisplay.getCssStyle());
//		}
//		if(variationDisplay.getHelp() == null) {
//			combinatedDisplay.setHelp(display.getHelp());
//		} else {
//			combinatedDisplay.setHelp(variationDisplay.getHelp());
//		}
//		if(variationDisplay.getLabel() == null) {
//			combinatedDisplay.setLabel(display.getLabel());
//		} else {
//			combinatedDisplay.setLabel(variationDisplay.getLabel());
//		}
//		if(variationDisplay.getPluralLabel() == null) {
//			combinatedDisplay.setPluralLabel(display.getPluralLabel());
//		} else {
//			combinatedDisplay.setPluralLabel(variationDisplay.getPluralLabel());
//		}
//		if(variationDisplay.getTooltip() == null) {
//			combinatedDisplay.setTooltip(display.getTooltip());
//		} else {
//			combinatedDisplay.setTooltip(variationDisplay.getTooltip());
//		}
//		if(variationDisplay.getIcon() == null) {
//			combinatedDisplay.setIcon(display.getIcon());
//		} else if(display.getIcon() != null){
//			IconType icon = new IconType();
//			if(StringUtils.isBlank(variationDisplay.getIcon().getCssClass())) {
//				icon.setCssClass(display.getIcon().getCssClass());
//			} else {
//				icon.setCssClass(variationDisplay.getIcon().getCssClass());
//			}
//			if(StringUtils.isBlank(variationDisplay.getIcon().getColor())) {
//				icon.setColor(display.getIcon().getColor());
//			} else {
//				icon.setColor(variationDisplay.getIcon().getColor());
//			}
//			if(StringUtils.isBlank(variationDisplay.getIcon().getImageUrl())) {
//				icon.setImageUrl(display.getIcon().getImageUrl());
//			} else {
//				icon.setImageUrl(variationDisplay.getIcon().getImageUrl());
//			}
//			combinatedDisplay.setIcon(icon);
//		}
//		
//		return combinatedDisplay;
//	}

	private DashboardWidgetSourceTypeType getSourceType(IModel<DashboardWidgetType> model) {
		if(isSourceTypeOfDataNull(model)) {
			return null;
		}
		return model.getObject().getData().getSourceType();
	}
	
//	private IModel<?> getNumberMessage() {
//		IModel<DashboardWidgetType> model = (IModel<DashboardWidgetType>)getDefaultModel();
//		DashboardWidgetSourceTypeType sourceType = getSourceType(model);
//		DashboardWidgetPresentationType presentation = model.getObject().getPresentation();
//		switch (sourceType) {
//		case OBJECT_COLLECTION:
//			if(!isDataFieldsOfPresentationNullOrEmpty(presentation)) {
//				return generateNumberMessageForCollection(presentation);
//			}
//			break;
//		case AUDIT_SEARCH:
//			if(!isDataFieldsOfPresentationNullOrEmpty(presentation)) {
//				return generateNumberMessageForAuditSearch(presentation);
//			}
//			break;
//		case OBJECT:
//			if(!isDataFieldsOfPresentationNullOrEmpty(presentation)) {
//				return generateNumberMessageForObject(presentation);
//			}
//			break;
//		}
//		return getPageBase().createStringResource(NUMBER_MESSAGE_UNKNOWN);
//	}

//	private boolean isDataFieldsOfPresentationNullOrEmpty(DashboardWidgetPresentationType presentation) {
//		if(presentation != null) {
//			if(presentation.getDataField() != null) {
//				if(!presentation.getDataField().isEmpty()) {
//					return false;
//				} else {
//					LOGGER.error("DataField of presentation is empty");
//				}
//			} else {
//				LOGGER.error("DataField of presentation is not defined");
//			}
//		} else {
//			LOGGER.error("Presentation of widget is not defined");
//		}
//		
//		return true;
//	}

//	private IModel<String> generateNumberMessageForObject(DashboardWidgetPresentationType dashboardWidgetPresentationType) {
//		ObjectType object = getObjectFromObjectRef();
//		if(object == null) {
//			return getPageBase().createStringResource(NUMBER_MESSAGE_UNKNOWN);
//		}
//		return generateNumberMessage(dashboardWidgetPresentationType, createVariables(object.asPrismObject(), null, null));
//	}

//	private IModel<String> generateNumberMessageForAuditSearch(DashboardWidgetPresentationType dashboardWidgetPresentationType) {
//		ObjectCollectionType collection = getObjectCollectionType();
//		if(collection == null) {
//			return getPageBase().createStringResource(NUMBER_MESSAGE_UNKNOWN);
//		}
//		AuditSearchType auditSearch = collection.getAuditSearch();
//		if(auditSearch == null) {
//			LOGGER.error("AuditSearch of ObjectCollection is not found in widget " +
//					((IModel<DashboardWidgetType>)getDefaultModel()).getObject().getIdentifier());
//			return getPageBase().createStringResource(NUMBER_MESSAGE_UNKNOWN);
//		}
//		if(auditSearch.getRecordQuery() == null) {
//			LOGGER.error("RecordQuery of auditSearch is not defined in widget " + 
//		((IModel<DashboardWidgetType>)getDefaultModel()).getObject().getIdentifier());
//			return getPageBase().createStringResource(NUMBER_MESSAGE_UNKNOWN);
//		}
//		
//		Map<String, Object> parameters = new HashMap<String, Object>();
//		String query = getQueryForCount(AuditEventRecordProvider.createQuery(collection,
//				parameters, false, getPageBase().getClock()));
//		LOGGER.debug("Parameters for select: " + parameters);
//		int value = (int) getPageBase().getAuditService().countObjects(
//				query, parameters);
//		Integer domainValue = null;
//		if(auditSearch.getDomainQuery() == null) {
//			LOGGER.error("DomainQuery of auditSearch is not defined");
//		} else {
//			parameters = new HashMap<String, Object>();
//			query = getQueryForCount(AuditEventRecordProvider.createQuery(collection,
//					parameters, true, getPageBase().getClock()));
//			LOGGER.debug("Parameters for select: " + parameters);
//			domainValue = (int) getPageBase().getAuditService().countObjects(
//					query, parameters);
//		}
//		LOGGER.debug("Value: {}, Domain value: {}", value, domainValue);
//		IntegerStatType statType = generateIntegerStat(value, domainValue);
//		return generateNumberMessage(dashboardWidgetPresentationType, createVariables(null, statType, null));
//	}
	
//	private String getQueryForCount(String query) {
//		query = "select count (*) " + query;
//		query = query.split("order")[0];
//		LOGGER.debug("Query for select: " + query);
//		return query;
//	}
	
//	private IModel<String> generateNumberMessageForCollection(DashboardWidgetPresentationType dashboardWidgetPresentationType) {
//		ObjectCollectionType valueCollection = getObjectCollectionType();
//		IModel<DashboardWidgetType> model = (IModel<DashboardWidgetType>)getDefaultModel();
//		if(valueCollection != null && valueCollection.getType() != null && valueCollection.getType().getLocalPart() != null) {
//			int value = getObjectCount(valueCollection, true);
//			
//			int domainValue;
//			if( valueCollection.getDomain() != null && valueCollection.getDomain().getCollectionRef() != null) {
//				ObjectReferenceType ref = valueCollection.getDomain().getCollectionRef();
//				Task task = getPageBase().createSimpleTask("Search domain collection");
//				ObjectCollectionType domainCollection = (ObjectCollectionType)WebModelServiceUtils.loadObject(ref, 
//						getPageBase(), task, task.getResult()).getRealValue();
//				domainValue = getObjectCount(domainCollection, true);
//			} else {
//				LOGGER.error("Domain or collectionRef in domain is null in collection " + valueCollection.toString());
//				LOGGER.trace("Using filter for all object based on type");
//				domainValue = getObjectCount(valueCollection, false);
//			}
//			IntegerStatType statType = generateIntegerStat(value, domainValue);
//			
//			Task task = getPageBase().createSimpleTask("Evaluate collection");
//			try {
//				CompiledObjectCollectionView compiledCollection = getPageBase().getModelInteractionService().compileObjectCollectionView(
//						valueCollection.asPrismObject(), null, task, task.getResult());
//				Collection<EvaluatedPolicyRule> evalPolicyRules = getPageBase().getModelInteractionService().evaluateCollectionPolicyRules(
//						valueCollection.asPrismObject(), compiledCollection, null, task, task.getResult());
//				Collection<String> policySituations = new ArrayList<String>();
//				for(EvaluatedPolicyRule evalPolicyRule : evalPolicyRules) {
//					if(!evalPolicyRule.getAllTriggers().isEmpty()) {
//						policySituations.add(evalPolicyRule.getPolicySituation());
//					}
//				}
//				return generateNumberMessage(dashboardWidgetPresentationType, createVariables(null, statType, policySituations));
//			} catch (Exception e) {
//				LOGGER.error(e.getMessage(), e);
//				return getPageBase().createStringResource(NUMBER_MESSAGE_UNKNOWN);
//			}
//			
//		}  else {
//			LOGGER.error("CollectionType from collectionRef is null in widget " + model.getObject().getIdentifier());
//		}
//		return getPageBase().createStringResource(NUMBER_MESSAGE_UNKNOWN);
//	}
	
//	private ExpressionVariables createVariables(PrismObject<? extends ObjectType> object, IntegerStatType statType, Collection<String> policySituations) {
//		ExpressionVariables variables = new ExpressionVariables();
//		if(statType != null || policySituations != null) {
//			VariablesMap variablesMap = new VariablesMap();
//			if(statType != null ) {
//				variablesMap.put(ExpressionConstants.VAR_INPUT, statType, statType.getClass());
//				variablesMap.put(VAR_PROPORTIONAL, statType, statType.getClass());
//			}
//			if(policySituations != null) {
//				variablesMap.put(VAR_POLICY_SITUATIONS, policySituations, EvaluatedPolicyRule.class);
//			}
//			variables.addVariableDefinitions(variablesMap );
//		}
//		if(object != null) {
//			variables.addVariableDefinition(ExpressionConstants.VAR_OBJECT, object, object.getDefinition());
//		}
//		
//		return variables;
//	}
	
//	private IntegerStatType generateIntegerStat(Integer value, Integer domainValue){
//		IntegerStatType statType = new IntegerStatType();
//		statType.setValue(value);
//		statType.setDomain(domainValue);
//		return statType;
//	}
	
//	private IModel<String> generateNumberMessage(DashboardWidgetPresentationType dashboardWidgetPresentationType, ExpressionVariables variables) {
//		IModel<DashboardWidgetType> model = (IModel<DashboardWidgetType>)getDefaultModel();
//		Map<DashboardWidgetDataFieldTypeType, String> numberMessagesParts = new HashMap<DashboardWidgetDataFieldTypeType, String>(); 
//		model.getObject().getPresentation().getDataField().forEach(dataField -> {
//			switch(dataField.getFieldType()) {
//			
//			case VALUE:
//				Task task = getPageBase().createSimpleTask("Search domain collection");
//				try {
//				String valueMessage = getStringExpressionMessage(variables, task, task.getResult(),
//						dataField.getExpression(), "Get value message");
//				if(valueMessage == null) {
//					valueMessage = getPageBase().createStringResource(NUMBER_MESSAGE_UNKNOWN).toString();
//				}
//				numberMessagesParts.put(DashboardWidgetDataFieldTypeType.VALUE, valueMessage);
//				} catch (Exception e) {
//					LOGGER.error(e.getMessage(), e);
//				}
//				break;
//				
//			case UNIT:
//				task = getPageBase().createSimpleTask("Get unit");
//				String unit = getStringExpressionMessage(new ExpressionVariables(), task, task.getResult(), dataField.getExpression(), "Unit");
//				numberMessagesParts.put(DashboardWidgetDataFieldTypeType.UNIT, unit);
//				break;
//			}
//		});
//		if(!numberMessagesParts.containsKey(DashboardWidgetDataFieldTypeType.VALUE)) {
//			LOGGER.error("Value message is not generate from widget " + model.getObject().getIdentifier());
//			return getPageBase().createStringResource(NUMBER_MESSAGE_UNKNOWN);
//		}
//		StringBuilder sb = new StringBuilder();
//		sb.append(numberMessagesParts.get(DashboardWidgetDataFieldTypeType.VALUE));
//		if(numberMessagesParts.containsKey(DashboardWidgetDataFieldTypeType.UNIT)) {
//			sb.append(" ").append(numberMessagesParts.get(DashboardWidgetDataFieldTypeType.UNIT));
//		}
//		
//		try {
//			evaluateVariation(model.getObject(), variables);
//		} catch (Exception e) {
//			LOGGER.error(e.getMessage(), e);
//		}
//		
//		return getStringModel(sb.toString());
//	}

//	private void evaluateVariation(DashboardWidgetType widget, ExpressionVariables variables) {
//		
//		if(widget.getPresentation() != null) {
//			if(widget.getPresentation().getVariation() != null) {
//				widget.getPresentation().getVariation().forEach(variation -> {
//					Task task = getPageBase().createSimpleTask("Evaluate variation");
//					PrismPropertyValue<Boolean> usingVariation;
//					try {
//						usingVariation = ExpressionUtil.evaluateCondition(variables, variation.getCondition(), null, getPageBase().getExpressionFactory(),
//								"Variation", task, task.getResult());
//				
//						if(usingVariation != null && usingVariation.getRealValue() != null
//								&& usingVariation.getRealValue().equals(Boolean.TRUE)) {
//							this.variationDisplay = variation.getDisplay();
//						}
//					} catch (Exception e) {
//						LOGGER.error("Couldn't evaluate condition " + variation.toString(), e);
//					}
//				});
//			}  else {
//				LOGGER.error("Variation of presentation is not found in widget " + widget.getIdentifier());
//			}
//		}  else {
//			LOGGER.error("Presentation is not found in widget " + widget.getIdentifier());
//		}
//	}

//	private int getObjectCount(ObjectCollectionType collection, boolean usingFilter) {
//		Class<ObjectType> objectClass = (Class<ObjectType>) getPageBase().getPrismContext().getSchemaRegistry()
//				.getCompileTimeClassForObjectType(collection.getType());
//		SearchFilterType searchFilter = collection.getFilter();
//		ObjectQuery query = getPageBase().getPrismContext().queryFactory().createQuery();
//		if (searchFilter != null && usingFilter) {
//			try {
//				query.setFilter(getPageBase().getPrismContext().getQueryConverter().parseFilter(searchFilter, objectClass));
//			} catch (Exception e) {
//				LOGGER.error("Filter couldn't parse in collection " + collection.toString(), e);
//			}
//		}
//		List<PrismObject<ObjectType>> values = WebModelServiceUtils.searchObjects(objectClass, 
//				query, new OperationResult("Load value Objects"), getPageBase());
//		return values.size();
//	}

	protected void customInitLayout(WebMarkupContainer infoBox) {
		
	}

	private IModel<String> getStringModel(String value){
		return new IModel<String>() {
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject() {
				return value;
			}
		};
	}
	
	protected static HashMap<String, Class<? extends WebPage>> getLinksRefCollections() {
		return linksRefCollections;
	}
	
	protected static HashMap<QName, Class<? extends WebPage>> getLinksRefObjects() {
		return linksRefObjects;
	}
	
	protected static PageBase getPageBase() {
		return pageBase;
	}
	
	protected WebPage getLinkRef() {
		IModel<DashboardWidgetType> model = (IModel<DashboardWidgetType>)getDefaultModel();
		DashboardWidgetSourceTypeType sourceType = getSourceType(model);
		switch (sourceType) {
		case OBJECT_COLLECTION:
			ObjectCollectionType collection = getObjectCollectionType();
			if(collection != null && collection.getType() != null && collection.getType().getLocalPart() != null) {
		        Class<? extends WebPage> pageType = getLinksRefCollections().get(collection.getType().getLocalPart());
		        if(pageType == null) {
					return null;
				}
				return getPageBase().createWebPage(pageType, null);
			}  else {
				LOGGER.error("CollectionType from collectionRef is null in widget " + model.getObject().getIdentifier());
			}
			break;
		case AUDIT_SEARCH:
			collection = getObjectCollectionType();
			if(collection != null && collection.getAuditSearch() != null && collection.getAuditSearch().getRecordQuery() != null) {
				Class<? extends WebPage> pageType = getLinksRefCollections().get(AuditEventRecordItemType.COMPLEX_TYPE.getLocalPart());
				if(pageType == null) {
					return null;
				}
				AuditSearchDto searchDto = new AuditSearchDto();
				searchDto.setCollection(collection);
				getPageBase().getSessionStorage().getAuditLog().setSearchDto(searchDto);
				return getPageBase().createWebPage(pageType, null);
			}  else {
				LOGGER.error("CollectionType from collectionRef is null in widget " + model.getObject().getIdentifier());
			}
			break;
		case OBJECT:
			ObjectType object = getObjectFromObjectRef();
			if(object == null) {
				return null;
			}
			QName typeName = WebComponentUtil.classToQName(getPageBase().getPrismContext(), object.getClass());
			Class<? extends WebPage> pageType = getLinksRefObjects().get(typeName);
			if(pageType == null) {
				return null;
			}
			PageParameters parameters = new PageParameters();
			parameters.add(OnePageParameterEncoder.PARAMETER, object.getOid());
			return getPageBase().createWebPage(pageType, parameters);
		}
	return null;
	}
	
	protected boolean existLinkRef() {
		IModel<DashboardWidgetType> model = (IModel<DashboardWidgetType>)getDefaultModel();
		DashboardWidgetSourceTypeType sourceType = getSourceType(model);
		switch (sourceType) {
		case OBJECT_COLLECTION:
			ObjectCollectionType collection = getObjectCollectionType();
			if(collection != null && collection.getType() != null && collection.getType().getLocalPart() != null) {
				return getLinksRefCollections().containsKey(collection.getType().getLocalPart());
			}  else {
				return false;
			}
		case AUDIT_SEARCH:
			collection = getObjectCollectionType();
			if(collection != null && collection.getAuditSearch() != null && collection.getAuditSearch().getRecordQuery() != null) {
				return getLinksRefCollections().containsKey(AuditEventRecordItemType.COMPLEX_TYPE.getLocalPart());
			}  else {
				return false;
			}
		case OBJECT:
			ObjectType object = getObjectFromObjectRef();
			if(object == null) {
				return false;
			}
			QName typeName = WebComponentUtil.classToQName(getPageBase().getPrismContext(), object.getClass());
			return getLinksRefObjects().containsKey(typeName);
		}
	return false;
	}
	
//	private String addTimestampToQueryForAuditSearch(String origQuery, AuditSearchType auditSearch) {
//		String [] partsOfQuery = origQuery.split("where");
//		Duration interval = auditSearch.getInterval();
//		if(interval.getSign() == 1) {
//			interval.negate();
//		}
//		Date date = new Date(clock.currentTimeMillis());
//		interval.addTo(date);
//		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
//		String dateString = format.format(date);
//		String query = partsOfQuery[0] + " where " + TIMESTAMP_VALUE_NAME + " >= '" + dateString + "' ";
//		if(partsOfQuery.length > 1) {
//			query+= "and" +partsOfQuery[1]; 
//		}
//		return query;
//	}
	
	private boolean isDataNull(IModel<DashboardWidgetType> model) {
		if(model.getObject().getData() == null) {
			LOGGER.error("Data is not found in widget " + model.getObject().getIdentifier());
			return true;
		}
		return false;
	}
	
	private boolean isSourceTypeOfDataNull(IModel<DashboardWidgetType> model) {
		if(isDataNull(model)) {
			return true;
		}
		if(model.getObject().getData().getSourceType() == null) {
			LOGGER.error("SourceType of data is not found in widget " + model.getObject().getIdentifier());
			return true;
		}
		return false;
	}
	
	private boolean isCollectionOfDataNull(IModel<DashboardWidgetType> model) {
		if(isDataNull(model)) {
			return true;
		}
		if(model.getObject().getData().getCollection() == null) {
			LOGGER.error("Collection of data is not found in widget " + model.getObject().getIdentifier());
			return true;
		}
		return false;
	}
	
	private boolean isCollectionRefOfCollectionNull(IModel<DashboardWidgetType> model) {
		if(isDataNull(model)) {
			return true;
		}
		if(isCollectionOfDataNull(model)) {
			return true;
		}
		ObjectReferenceType ref = model.getObject().getData().getCollection().getCollectionRef();
		if(ref == null) {
			LOGGER.error("CollectionRef of collection is not found in widget " + model.getObject().getIdentifier());
			return true;
		}
		return false;
	}
	
	private ObjectCollectionType getObjectCollectionType() {
		IModel<DashboardWidgetType> model = (IModel<DashboardWidgetType>)getDefaultModel();
		if(isCollectionRefOfCollectionNull(model)) {
			return null;
		}
		ObjectReferenceType ref = model.getObject().getData().getCollection().getCollectionRef();
		Task task = getPageBase().createSimpleTask("Search collection");
		ObjectCollectionType collection = (ObjectCollectionType)WebModelServiceUtils.loadObject(ref, 
				getPageBase(), task, task.getResult()).getRealValue();
		return collection;
	}
	
	private ObjectType getObjectFromObjectRef() {
		IModel<DashboardWidgetType> model = (IModel<DashboardWidgetType>)getDefaultModel();
		if(isDataNull(model)) {
			return null;
		}
		ObjectReferenceType ref = model.getObject().getData().getObjectRef();
		if(ref == null) {
			LOGGER.error("ObjectRef of data is not found in widget " + model.getObject().getIdentifier());
			return null;
		}
		Task task = getPageBase().createSimpleTask("Search domain collection");
		ObjectType object = WebModelServiceUtils.loadObject(ref, 
				getPageBase(), task, task.getResult()).getRealValue();
		if(object == null) {
			LOGGER.error("Object from ObjectRef " + ref + " is null in widget " + model.getObject().getIdentifier());
		}
		return object;
	}
	
//	private static String getStringExpressionMessage(ExpressionVariables variables,
//    		Task task, OperationResult result, ExpressionType expression, String shortDes) {
//    	if (expression != null) {
//        	Collection<String> contentTypeList = null;
//        	try {
//        		contentTypeList = ExpressionUtil.evaluateStringExpression(variables, getPageBase().getPrismContext(),
//	        			expression, null, getPageBase().getExpressionFactory(), shortDes, task, result);
//        	} catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException
//        			| ConfigurationException | SecurityViolationException e) {
//        		LOGGER.error("Couldn't evaluate Expression " + expression.toString(), e);
//        	}
//            if (contentTypeList == null || contentTypeList.isEmpty()) {
//            	LOGGER.error("Expression " + expression + " returned nothing");
//            	return null;
//            }
//            if (contentTypeList.size() > 1) {
//                LOGGER.error("Expression returned more than 1 item. First item is used.");
//            }
//            return contentTypeList.iterator().next();
//        } else {
//            return null;
//        }
//    }
}
