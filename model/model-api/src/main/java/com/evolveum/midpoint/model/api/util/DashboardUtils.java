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
package com.evolveum.midpoint.model.api.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.Duration;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.DashboardWidget;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuditSearchType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetDataFieldTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetPresentationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetSourceTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetVariationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IntegerStatType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * @author skublik
 */
public class DashboardUtils {
	
	private static final Trace LOGGER = TraceManager.getTrace(DashboardUtils.class);

	public static final String VAR_PROPORTIONAL = "proportional";
	private static final String VAR_POLICY_SITUATIONS = "policySituations";
	
	public static final String PARAMETER_FROM = "from";
	private static final String AUDIT_RECORDS_ORDER_BY = " order by aer.timestamp desc";
	private static final String TIMESTAMP_VALUE_NAME = "aer.timestamp";
	
//	@Autowired private static TaskManager taskManager;
//    @Autowired private static AuditService auditService;
//    @Autowired private static PrismContext prismContext;
//    @Autowired private static Clock clock;
//    @Autowired private static ModelInteractionService modelInteractionService;
//    @Autowired private static ModelService modelService;
//    @Autowired private static ExpressionFactory expressionFactory;

	public static DashboardWidget createWidgetData(DashboardWidgetType widget, ModelService modelService,
			TaskManager taskManager, ExpressionFactory expressionFactory, AuditService auditService,
			Clock clock, ModelInteractionService modelInteractionService) {
		
		Validate.notNull(widget, "Widget is null");
		Validate.notNull(modelService, "ModelService is null");
		Validate.notNull(taskManager, "TaskManager is null");
		Validate.notNull(expressionFactory, "ExpressionFactory is null");
		Validate.notNull(auditService, "AuditService is null");
		Validate.notNull(clock, "Clock is null");
		Validate.notNull(modelInteractionService, "ModelInteractionService is null");
		
		DashboardWidget data = new DashboardWidget();
		getNumberMessage(widget, modelService, 
				modelService.getPrismContext(), taskManager, expressionFactory, 
				auditService, clock, modelInteractionService, data);
		data.setWidget(widget);
		if(data.getDisplay() == null) {
			data.setDisplay(widget.getDisplay());
		}
		LOGGER.debug("Widget Data: {}", data);
		return data;
	}
	
	private static DisplayType combinateDisplay(DisplayType display, DisplayType variationDisplay) {
		DisplayType combinatedDisplay = new DisplayType();
		if (variationDisplay == null) {
			return display;
		}
		if(display == null) {
			return variationDisplay;
		}
		if(StringUtils.isBlank(variationDisplay.getColor())) {
			combinatedDisplay.setColor(display.getColor());
		} else {
			combinatedDisplay.setColor(variationDisplay.getColor());
		}
		if(StringUtils.isBlank(variationDisplay.getCssClass())) {
			combinatedDisplay.setCssClass(display.getCssClass());
		} else {
			combinatedDisplay.setCssClass(variationDisplay.getCssClass());
		}
		if(StringUtils.isBlank(variationDisplay.getCssStyle())) {
			combinatedDisplay.setCssStyle(display.getCssStyle());
		} else {
			combinatedDisplay.setCssStyle(variationDisplay.getCssStyle());
		}
		if(variationDisplay.getHelp() == null) {
			combinatedDisplay.setHelp(display.getHelp());
		} else {
			combinatedDisplay.setHelp(variationDisplay.getHelp());
		}
		if(variationDisplay.getLabel() == null) {
			combinatedDisplay.setLabel(display.getLabel());
		} else {
			combinatedDisplay.setLabel(variationDisplay.getLabel());
		}
		if(variationDisplay.getPluralLabel() == null) {
			combinatedDisplay.setPluralLabel(display.getPluralLabel());
		} else {
			combinatedDisplay.setPluralLabel(variationDisplay.getPluralLabel());
		}
		if(variationDisplay.getTooltip() == null) {
			combinatedDisplay.setTooltip(display.getTooltip());
		} else {
			combinatedDisplay.setTooltip(variationDisplay.getTooltip());
		}
		if(variationDisplay.getIcon() == null) {
			combinatedDisplay.setIcon(display.getIcon());
		} else if(display.getIcon() != null){
			IconType icon = new IconType();
			if(StringUtils.isBlank(variationDisplay.getIcon().getCssClass())) {
				icon.setCssClass(display.getIcon().getCssClass());
			} else {
				icon.setCssClass(variationDisplay.getIcon().getCssClass());
			}
			if(StringUtils.isBlank(variationDisplay.getIcon().getColor())) {
				icon.setColor(display.getIcon().getColor());
			} else {
				icon.setColor(variationDisplay.getIcon().getColor());
			}
			if(StringUtils.isBlank(variationDisplay.getIcon().getImageUrl())) {
				icon.setImageUrl(display.getIcon().getImageUrl());
			} else {
				icon.setImageUrl(variationDisplay.getIcon().getImageUrl());
			}
			combinatedDisplay.setIcon(icon);
		}
		
		return combinatedDisplay;
	}

	public static DashboardWidgetSourceTypeType getSourceType(DashboardWidgetType widget) {
		if(isSourceTypeOfDataNull(widget)) {
			return null;
		}
		return widget.getData().getSourceType();
	}
	
	private static String getNumberMessage(DashboardWidgetType widget, ModelService modelService, 
			PrismContext prismContext, TaskManager taskManager, ExpressionFactory expressionFactory, 
			AuditService auditService, Clock clock, ModelInteractionService modelInteractionService, DashboardWidget data) {
		DashboardWidgetSourceTypeType sourceType = getSourceType(widget);
		DashboardWidgetPresentationType presentation = widget.getPresentation();
		switch (sourceType) {
		case OBJECT_COLLECTION:
			if(!isDataFieldsOfPresentationNullOrEmpty(presentation)) {
				return generateNumberMessageForCollection(widget, modelInteractionService, taskManager, 
						expressionFactory, prismContext, modelService, data);
			}
			break;
		case AUDIT_SEARCH:
			if(!isDataFieldsOfPresentationNullOrEmpty(presentation)) {
				return generateNumberMessageForAuditSearch(widget, auditService, taskManager, 
						expressionFactory, prismContext, clock, modelService, data);
			}
			break;
		case OBJECT:
			if(!isDataFieldsOfPresentationNullOrEmpty(presentation)) {
				return generateNumberMessageForObject(widget, modelService, prismContext, 
						taskManager, expressionFactory, data);
			}
			break;
		}
		return null;
	}

	public static boolean isDataFieldsOfPresentationNullOrEmpty(DashboardWidgetPresentationType presentation) {
		if(presentation != null) {
			if(presentation.getDataField() != null) {
				if(!presentation.getDataField().isEmpty()) {
					return false;
				} else {
					LOGGER.error("DataField of presentation is empty");
				}
			} else {
				LOGGER.error("DataField of presentation is not defined");
			}
		} else {
			LOGGER.error("Presentation of widget is not defined");
		}
		
		return true;
	}

	private static String generateNumberMessageForObject(DashboardWidgetType widget, ModelService modelService,
			PrismContext prismContext, TaskManager taskManager, ExpressionFactory expressionFactory, DashboardWidget data) {
		ObjectType object = getObjectFromObjectRef(widget, modelService, prismContext, taskManager);
		if(object == null) {
			return null;
		}
		return generateNumberMessage(widget, createVariables(object.asPrismObject(), null, null), taskManager,
				expressionFactory, prismContext, data);
	}

	private static String generateNumberMessageForAuditSearch(DashboardWidgetType widget, AuditService auditService,
			TaskManager taskManager, ExpressionFactory expressionFactory, PrismContext prismContext, Clock clock,
			ModelService modelService, DashboardWidget data) {
		ObjectCollectionType collection = getObjectCollectionType(widget, taskManager, prismContext, modelService);
		if(collection == null) {
			return null;
		}
		AuditSearchType auditSearch = collection.getAuditSearch();
		if(auditSearch == null) {
			LOGGER.error("AuditSearch of ObjectCollection is not found in widget " +
					widget.getIdentifier());
			return null;
		}
		if(auditSearch.getRecordQuery() == null) {
			LOGGER.error("RecordQuery of auditSearch is not defined in widget " + 
					widget.getIdentifier());
			return null;
		}
		
		Map<String, Object> parameters = new HashMap<String, Object>();
		String query = getQueryForCount(createQuery(collection,
				parameters, false, clock));
		LOGGER.debug("Parameters for select: " + parameters);
		int value = (int) auditService.countObjects(
				query, parameters);
		Integer domainValue = null;
		if(auditSearch.getDomainQuery() == null) {
			LOGGER.error("DomainQuery of auditSearch is not defined");
		} else {
			parameters = new HashMap<String, Object>();
			query = getQueryForCount(createQuery(collection,
					parameters, true, clock));
			LOGGER.debug("Parameters for select: " + parameters);
			domainValue = (int) auditService.countObjects(
					query, parameters);
		}
		LOGGER.debug("Value: {}, Domain value: {}", value, domainValue);
		IntegerStatType statType = generateIntegerStat(value, domainValue);
		return generateNumberMessage(widget, createVariables(null, statType, null), taskManager, expressionFactory,
				prismContext, data);
	}
	
	private static String getQueryForCount(String query) {
		query = "select count (*) " + query;
		query = query.split("order")[0];
		LOGGER.debug("Query for select: " + query);
		return query;
	}
	
	public static String getQueryForListRecords(String query) {
		query = query + AUDIT_RECORDS_ORDER_BY;
		LOGGER.debug("Query for select: " + query);
		return query;
	}
	
	private static String generateNumberMessageForCollection(DashboardWidgetType widget,
			ModelInteractionService modelInteractionService, TaskManager taskManager,
			ExpressionFactory expressionFactory, PrismContext prismContext, ModelService modelService, DashboardWidget data) {
		ObjectCollectionType valueCollection = getObjectCollectionType(widget, taskManager, prismContext,
				modelService);
		if(valueCollection != null && valueCollection.getType() != null && 
				valueCollection.getType().getLocalPart() != null) {
			int value = getObjectCount(valueCollection, true, prismContext, taskManager, modelService);
			
			int domainValue;
			if( valueCollection.getDomain() != null && valueCollection.getDomain().getCollectionRef() != null) {
//				ObjectCollectionType domainCollection = (ObjectCollectionType) getObjectCollectionType(widget,
//						taskManager, prismContext, modelService);
				ObjectReferenceType ref = valueCollection.getDomain().getCollectionRef();
				ObjectCollectionType domainCollection = (ObjectCollectionType)getObjectTypeFromObjectRef(ref, taskManager, prismContext, modelService);
				if(domainCollection == null) {
					return null;
				}
//				ObjectCollectionType domainCollection = (ObjectCollectionType)WebModelServiceUtils.loadObject(ref, 
//						getPageBase(), task, task.getResult()).getRealValue();
				domainValue = getObjectCount(domainCollection, true, prismContext, taskManager, modelService);
			} else {
				LOGGER.error("Domain or collectionRef in domain is null in collection " + valueCollection.toString());
				LOGGER.trace("Using filter for all object based on type");
				domainValue = getObjectCount(valueCollection, false, prismContext, taskManager, modelService);
			}
			IntegerStatType statType = generateIntegerStat(value, domainValue);
			
			Task task = taskManager.createTaskInstance("Evaluate collection");
			try {
				CompiledObjectCollectionView compiledCollection = modelInteractionService.compileObjectCollectionView(
						valueCollection.asPrismObject(), null, task, task.getResult());
				Collection<EvaluatedPolicyRule> evalPolicyRules = modelInteractionService.evaluateCollectionPolicyRules(
						valueCollection.asPrismObject(), compiledCollection, null, task, task.getResult());
				Collection<String> policySituations = new ArrayList<String>();
				for(EvaluatedPolicyRule evalPolicyRule : evalPolicyRules) {
					if(!evalPolicyRule.getAllTriggers().isEmpty()) {
						policySituations.add(evalPolicyRule.getPolicySituation());
					}
				}
				return generateNumberMessage(widget, createVariables(null, statType, policySituations),
						taskManager, expressionFactory, prismContext, data);
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
				return null;
			}
			
		}  else {
			LOGGER.error("CollectionType from collectionRef is null in widget " + widget.getIdentifier());
		}
		return null;
	}
	
	private static ExpressionVariables createVariables(PrismObject<? extends ObjectType> object,
			IntegerStatType statType, Collection<String> policySituations) {
		ExpressionVariables variables = new ExpressionVariables();
		if(statType != null || policySituations != null) {
			VariablesMap variablesMap = new VariablesMap();
			if(statType != null ) {
				variablesMap.put(ExpressionConstants.VAR_INPUT, statType, statType.getClass());
				variablesMap.put(VAR_PROPORTIONAL, statType, statType.getClass());
			}
			if(policySituations != null) {
				variablesMap.put(VAR_POLICY_SITUATIONS, policySituations, EvaluatedPolicyRule.class);
			}
			variables.addVariableDefinitions(variablesMap );
		}
		if(object != null) {
			variables.addVariableDefinition(ExpressionConstants.VAR_OBJECT, object, object.getDefinition());
		}
		
		return variables;
	}
	
	private static IntegerStatType generateIntegerStat(Integer value, Integer domainValue){
		IntegerStatType statType = new IntegerStatType();
		statType.setValue(value);
		statType.setDomain(domainValue);
		return statType;
	}
	
	private static String generateNumberMessage(DashboardWidgetType widget, ExpressionVariables variables,
			TaskManager taskManager, ExpressionFactory expressionFactory, PrismContext prismContext, DashboardWidget data) {
		Map<DashboardWidgetDataFieldTypeType, String> numberMessagesParts = new HashMap<DashboardWidgetDataFieldTypeType, String>(); 
		widget.getPresentation().getDataField().forEach(dataField -> {
			switch(dataField.getFieldType()) {
			
			case VALUE:
				Task task = taskManager.createTaskInstance("Search domain collection");
				try {
				String valueMessage = getStringExpressionMessage(variables, task, task.getResult(),
						dataField.getExpression(), "Get value message", prismContext, expressionFactory);
				if(valueMessage != null) {
					numberMessagesParts.put(DashboardWidgetDataFieldTypeType.VALUE, valueMessage);
				}
				} catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
				}
				break;
				
			case UNIT:
				task = taskManager.createTaskInstance("Get unit");
				String unit = getStringExpressionMessage(new ExpressionVariables(), task, 
						task.getResult(), dataField.getExpression(), "Unit",
						prismContext, expressionFactory);
				numberMessagesParts.put(DashboardWidgetDataFieldTypeType.UNIT, unit);
				break;
			}
		});
		if(!numberMessagesParts.containsKey(DashboardWidgetDataFieldTypeType.VALUE)) {
			LOGGER.error("Value message is not generate from widget " + widget.getIdentifier());
			return null;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(numberMessagesParts.get(DashboardWidgetDataFieldTypeType.VALUE));
		if(numberMessagesParts.containsKey(DashboardWidgetDataFieldTypeType.UNIT)) {
			sb.append(" ").append(numberMessagesParts.get(DashboardWidgetDataFieldTypeType.UNIT));
		}
		
		try {
			evaluateVariation(widget, variables, taskManager, expressionFactory, data);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
		data.setNumberMessage(sb.toString());
		return sb.toString();
	}

	private static void evaluateVariation(DashboardWidgetType widget, ExpressionVariables variables, TaskManager taskManager,
			ExpressionFactory expressionFactory, DashboardWidget data) {
		
		if(widget.getPresentation() != null) {
			if(widget.getPresentation().getVariation() != null) {
				for(DashboardWidgetVariationType variation : widget.getPresentation().getVariation()) {
					Task task = taskManager.createTaskInstance("Evaluate variation");
					PrismPropertyValue<Boolean> usingVariation;
					try {
						usingVariation = ExpressionUtil.evaluateCondition(variables, variation.getCondition(), null,
								expressionFactory,
								"Variation", task, task.getResult());
				
						if(usingVariation != null && usingVariation.getRealValue() != null
								&& usingVariation.getRealValue().equals(Boolean.TRUE)) {
							data.setDisplay(combinateDisplay(widget.getDisplay(), variation.getDisplay()));
						} else {
							data.setDisplay(widget.getDisplay());
						}
					} catch (Exception e) {
						LOGGER.error("Couldn't evaluate condition " + variation.toString(), e);
					}
				}
			}  else {
				LOGGER.error("Variation of presentation is not found in widget " + widget.getIdentifier());
			}
		}  else {
			LOGGER.error("Presentation is not found in widget " + widget.getIdentifier());
		}
	}

	private static int getObjectCount(ObjectCollectionType collection, boolean usingFilter, PrismContext prismContext, 
			TaskManager taskManager, ModelService modelService) {
		List<PrismObject<ObjectType>> values = searchObjectFromCollection(collection, usingFilter,
				prismContext, taskManager, modelService);
		if(values != null) {
			LOGGER.debug("Return count: {}", values.size());
			return values.size();
		}
		return 0; 
	}
	
	public static List<PrismObject<ObjectType>> searchObjectFromCollection(ObjectCollectionType collection, boolean usingFilter, PrismContext prismContext, 
			TaskManager taskManager, ModelService modelService){
		Class<ObjectType> type = (Class<ObjectType>) prismContext.getSchemaRegistry()
				.getCompileTimeClassForObjectType(collection.getType());
		SearchFilterType searchFilter = collection.getFilter();
		ObjectQuery query = prismContext.queryFactory().createQuery();
		if (searchFilter != null && usingFilter) {
			try {
				query.setFilter(prismContext.getQueryConverter().parseFilter(searchFilter, type));
			} catch (Exception e) {
				LOGGER.error("Filter couldn't parse in collection " + collection.toString(), e);
			}
		}
		Task task =taskManager.createTaskInstance("Load value Objects");
		List<PrismObject<ObjectType>> values;
		try {
			values = modelService.searchObjects(type, query, null, task, task.getResult());
			return values;
		} catch (Exception e) {
			LOGGER.error("Couldn't search objects", e);
		}
		return null; 
	}

	private static boolean isDataNull(DashboardWidgetType widget) {
		if(widget.getData() == null) {
			LOGGER.error("Data is not found in widget " + widget.getIdentifier());
			return true;
		}
		return false;
	}
	
	private static boolean isSourceTypeOfDataNull(DashboardWidgetType widget) {
		if(isDataNull(widget)) {
			return true;
		}
		if(widget.getData().getSourceType() == null) {
			LOGGER.error("SourceType of data is not found in widget " + widget.getIdentifier());
			return true;
		}
		return false;
	}
	
	private static boolean isCollectionOfDataNull(DashboardWidgetType widget) {
		if(isDataNull(widget)) {
			return true;
		}
		if(widget.getData().getCollection() == null) {
			LOGGER.error("Collection of data is not found in widget " + widget.getIdentifier());
			return true;
		}
		return false;
	}
	
	private static boolean isCollectionRefOfCollectionNull(DashboardWidgetType widget) {
		if(isDataNull(widget)) {
			return true;
		}
		if(isCollectionOfDataNull(widget)) {
			return true;
		}
		ObjectReferenceType ref = widget.getData().getCollection().getCollectionRef();
		if(ref == null) {
			LOGGER.error("CollectionRef of collection is not found in widget " + widget.getIdentifier());
			return true;
		}
		return false;
	}
	
	public static ObjectCollectionType getObjectCollectionType(DashboardWidgetType widget, TaskManager taskManager,
			PrismContext prismContext, ModelService modelService) {
		if(isCollectionRefOfCollectionNull(widget)) {
			return null;
		}
		ObjectReferenceType ref = widget.getData().getCollection().getCollectionRef();
		ObjectCollectionType collection = (ObjectCollectionType) getObjectTypeFromObjectRef(ref, taskManager, prismContext, 
				modelService);
		return collection;
	}
	
	private static ObjectType getObjectFromObjectRef(DashboardWidgetType widget, ModelService modelService, 
			PrismContext prismContext, TaskManager taskManager) {
		if(isDataNull(widget)) {
			return null;
		}
		ObjectReferenceType ref = widget.getData().getObjectRef();
		if(ref == null) {
			LOGGER.error("ObjectRef of data is not found in widget " + widget.getIdentifier());
			return null;
		}
		ObjectType object = getObjectTypeFromObjectRef(ref, taskManager, prismContext, modelService);
		if(object == null) {
			LOGGER.error("Object from ObjectRef " + ref + " is null in widget " + widget.getIdentifier());
		}
		return object;
	}
	
	public static ObjectType getObjectTypeFromObjectRef(ObjectReferenceType ref, TaskManager taskManager, 
			PrismContext prismContext, ModelService modelService) {
		Task task = taskManager.createTaskInstance("Get object");
		Class<ObjectType> type = prismContext.getSchemaRegistry().determineClassForType(ref.getType());
		PrismObject<ObjectType> object;
		
		try {
			object = modelService.getObject(type,
					ref.getOid(), null, task, task.getResult());
			return object.asObjectable();
		} catch (Exception e) {
			LOGGER.error("Couldn't get object from objectRef " + ref, e);
		}
		return null;
	}
	
	private static String getStringExpressionMessage(ExpressionVariables variables,
    		Task task, OperationResult result, ExpressionType expression, String shortDes, PrismContext prismContext, 
    		ExpressionFactory expressionFactory) {
    	if (expression != null) {
        	Collection<String> contentTypeList = null;
        	try {
        		contentTypeList = ExpressionUtil.evaluateStringExpression(variables, prismContext,
	        			expression, null, expressionFactory, shortDes, task, result);
        	} catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException
        			| ConfigurationException | SecurityViolationException e) {
        		LOGGER.error("Couldn't evaluate Expression " + expression.toString(), e);
        	}
            if (contentTypeList == null || contentTypeList.isEmpty()) {
            	LOGGER.error("Expression " + expression + " returned nothing");
            	return null;
            }
            if (contentTypeList.size() > 1) {
                LOGGER.error("Expression returned more than 1 item. First item is used.");
            }
            return contentTypeList.iterator().next();
        } else {
            return null;
        }
    }
	
	public static String createQuery(ObjectCollectionType collectionForQuery, Map<String, Object> parameters,
			boolean forDomain, Clock clock) {
		if(collectionForQuery == null) {
			return null;
		}
		AuditSearchType auditSearch = collectionForQuery.getAuditSearch();
		if(auditSearch != null || StringUtils.isNotBlank(auditSearch.getRecordQuery())) {
			Duration interval = auditSearch.getInterval();
			if(interval == null) {
				return auditSearch.getRecordQuery();
			}
			String origQuery = auditSearch.getRecordQuery();
			if(forDomain) {
				origQuery = auditSearch.getDomainQuery();
				if(origQuery == null) {
					return null;
				}
			}
			String [] partsOfQuery = origQuery.split("where");
			if(interval.getSign() == 1) {
				interval = interval.negate();
			}
			Date date = new Date(clock.currentTimeMillis());
			interval.addTo(date);
			String query = partsOfQuery[0] + "where " + TIMESTAMP_VALUE_NAME + " >= " + ":from" + " ";
			parameters.put(PARAMETER_FROM, date);
			if(partsOfQuery.length > 1) {
				query+= "and" +partsOfQuery[1]; 
			}
			return query;
		}
		return null;
	}
	
}
