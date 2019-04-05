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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.IPageFactory;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
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
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuditSearchType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetDataFieldTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetPresentationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetSourceTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IntegerStatType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
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
	
	private static HashMap<String, Class<? extends WebPage>> linksRefCollections;
	private static HashMap<QName, Class<? extends WebPage>> linksRefObjects;
	
	static {
	    linksRefCollections = new HashMap<String, Class<? extends WebPage>>() {
			private static final long serialVersionUID = 1L;

			{
	            put(ResourceType.COMPLEX_TYPE.getLocalPart(), PageResources.class);
	            put(AuditEventRecordItemType.COMPLEX_TYPE.getLocalPart(), PageAuditLogViewer.class);
	            put(TaskType.COMPLEX_TYPE.getLocalPart(), PageTasks.class);
	        }
	    };
	}
	
	static {
	    linksRefObjects = new HashMap<QName, Class<? extends WebPage>>() {
			private static final long serialVersionUID = 1L;

			{
	            put(TaskType.COMPLEX_TYPE, PageTaskEdit.class);
	        }
	    };
	}
	
	private static PageBase pageBase;
	private DisplayType display;

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
		this.display = model.getObject().getDisplay();
		
		WebMarkupContainer infoBox = new WebMarkupContainer(ID_INFO_BOX);
		add(infoBox);
		
		Label number = new Label(ID_NUMBER, getNumberMessage()); //number message have to add before icon because is needed evaluate variation
		infoBox.add(number);

		IModel<DisplayType> displayModel = new IModel<DisplayType>() {
			
			@Override
			public DisplayType getObject() {
				return InfoBoxPanel.this.display;
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
	
	private DashboardWidgetSourceTypeType getSourceType(IModel<DashboardWidgetType> model) {
		if(isSourceTypeOfDataNull(model)) {
			return null;
		}
		return model.getObject().getData().getSourceType();
	}
	
	private IModel<?> getNumberMessage() {
		IModel<DashboardWidgetType> model = (IModel<DashboardWidgetType>)getDefaultModel();
		DashboardWidgetSourceTypeType sourceType = getSourceType(model);
		DashboardWidgetPresentationType presentation = model.getObject().getPresentation();
		switch (sourceType) {
		case OBJECT_COLLECTION:
			if(!isDataFieldsOfPresentationNullOrEmpty(presentation)) {
				return generateNumberMessageForCollection(presentation);
			}
			break;
		case AUDIT_SEARCH:
			if(!isDataFieldsOfPresentationNullOrEmpty(presentation)) {
				return generateNumberMessageForAuditSearch(presentation);
			}
			break;
		case OBJECT:
			if(!isDataFieldsOfPresentationNullOrEmpty(presentation)) {
				return generateNumberMessageForObject(presentation);
			}
			break;
		}
		return getPageBase().createStringResource(NUMBER_MESSAGE_UNKNOWN);
	}

	private boolean isDataFieldsOfPresentationNullOrEmpty(DashboardWidgetPresentationType presentation) {
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

	private IModel<String> generateNumberMessageForObject(DashboardWidgetPresentationType dashboardWidgetPresentationType) {
		ObjectType object = getObjectFromObjectRef();
		if(object == null) {
			return getPageBase().createStringResource(NUMBER_MESSAGE_UNKNOWN);
		}
		ExpressionVariables variables = new ExpressionVariables();
		variables.addVariableDefinition(ExpressionConstants.VAR_OBJECT, object.asPrismObject());
		return generateNumberMessage(dashboardWidgetPresentationType, variables);
	}

	private IModel<String> generateNumberMessageForAuditSearch(DashboardWidgetPresentationType dashboardWidgetPresentationType) {
		AuditSearchType auditSearch = getAuditSearchType();
		if(auditSearch == null) {
			LOGGER.error("AuditSearch of data is not defined");
			return getPageBase().createStringResource(NUMBER_MESSAGE_UNKNOWN);
		}
		if(auditSearch.getRecordQuery() == null) {
			LOGGER.error("RecordQuery of auditSearch is not defined");
			return getPageBase().createStringResource(NUMBER_MESSAGE_UNKNOWN);
		}
		
		String query = getQueryForCount(auditSearch.getRecordQuery());
		int value = (int) getPageBase().getAuditService().countObjects(
				query, new HashMap<String, Object>());
		Integer domainValue = null;
		if(auditSearch.getDomainQuery() == null) {
			LOGGER.error("DomainQuery of auditSearch is not defined");
		} else {
			query = getQueryForCount(auditSearch.getDomainQuery());
			domainValue = (int) getPageBase().getAuditService().countObjects(
					query, new HashMap<String, Object>());
		}
		IntegerStatType statType = generateIntegerStat(value, domainValue);
		ExpressionVariables variables = new ExpressionVariables();
		variables.addVariableDefinition(ExpressionConstants.VAR_INPUT, statType);
		return generateNumberMessage(dashboardWidgetPresentationType, variables);
	}
	
	private String getQueryForCount(String query) {
		query = "select count (*) " + query;
		query = query.split("order")[0];
		LOGGER.debug("Query for select: " + query);
		return query;
	}
	
	private IModel<String> generateNumberMessageForCollection(DashboardWidgetPresentationType dashboardWidgetPresentationType) {
		ObjectCollectionType valueCollection = getObjectCollectionType();
		IModel<DashboardWidgetType> model = (IModel<DashboardWidgetType>)getDefaultModel();
		if(valueCollection != null && valueCollection.getType() != null && valueCollection.getType().getLocalPart() != null) {
			int value = getObjectCount(valueCollection, true);
			
			evaluateVariation(model.getObject(), valueCollection);
			
			int domainValue;
			if( valueCollection.getDomain() != null && valueCollection.getDomain().getCollectionRef() != null) {
				ObjectReferenceType ref = valueCollection.getDomain().getCollectionRef();
				Task task = getPageBase().createSimpleTask("Search domain collection");
				ObjectCollectionType domainCollection = (ObjectCollectionType)WebModelServiceUtils.loadObject(ref, 
						getPageBase(), task, task.getResult()).getRealValue();
				domainValue = getObjectCount(domainCollection, true);
			} else {
				LOGGER.error("Domain or collectionRef in domain is null in collection " + valueCollection.toString());
				LOGGER.trace("Using filter for all object based on type");
				domainValue = getObjectCount(valueCollection, false);
			}
			IntegerStatType statType = generateIntegerStat(value, domainValue);
			ExpressionVariables variables = new ExpressionVariables();
			variables.addVariableDefinition(ExpressionConstants.VAR_INPUT, statType);
			return generateNumberMessage(dashboardWidgetPresentationType, variables);
		}  else {
			LOGGER.error("CollectionType from collectionRef is null in widget " + model.getObject().getIdentifier());
		}
		return getPageBase().createStringResource(NUMBER_MESSAGE_UNKNOWN);
	}
	
	private IntegerStatType generateIntegerStat(Integer value, Integer domainValue){
		IntegerStatType statType = new IntegerStatType();
		statType.setValue(value);
		statType.setDomain(domainValue);
		return statType;
	}
	
	private IModel<String> generateNumberMessage(DashboardWidgetPresentationType dashboardWidgetPresentationType, ExpressionVariables variables) {
		IModel<DashboardWidgetType> model = (IModel<DashboardWidgetType>)getDefaultModel();
		Map<DashboardWidgetDataFieldTypeType, String> numberMessagesParts = new HashMap<DashboardWidgetDataFieldTypeType, String>(); 
		model.getObject().getPresentation().getDataField().forEach(dataField -> {
			switch(dataField.getFieldType()) {
			
			case VALUE:
				Task task = getPageBase().createSimpleTask("Search domain collection");
				try {
				String valueMessage = getStringExpressionMessage(variables, task, task.getResult(),
						dataField.getExpression(), "Get value message");
				if(valueMessage == null) {
					valueMessage = getPageBase().createStringResource(NUMBER_MESSAGE_UNKNOWN).toString();
				}
				numberMessagesParts.put(DashboardWidgetDataFieldTypeType.VALUE, valueMessage);
				} catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
				}
				break;
				
			case UNIT:
				task = getPageBase().createSimpleTask("Get unit");
				String unit = getStringExpressionMessage(new ExpressionVariables(), task, task.getResult(), dataField.getExpression(), "Unit");
				numberMessagesParts.put(DashboardWidgetDataFieldTypeType.UNIT, unit);
				break;
			}
		});
		if(!numberMessagesParts.containsKey(DashboardWidgetDataFieldTypeType.VALUE)) {
			LOGGER.error("Value message is not generate from widget " + model.getObject().getIdentifier());
			return getPageBase().createStringResource(NUMBER_MESSAGE_UNKNOWN);
		}
		StringBuilder sb = new StringBuilder();
		sb.append(numberMessagesParts.get(DashboardWidgetDataFieldTypeType.VALUE));
		if(numberMessagesParts.containsKey(DashboardWidgetDataFieldTypeType.UNIT)) {
			sb.append(" ").append(numberMessagesParts.get(DashboardWidgetDataFieldTypeType.UNIT));
		}
		return getStringModel(sb.toString());
	}

	private void evaluateVariation(DashboardWidgetType widget, ObjectCollectionType collection) {
		ExpressionVariables variables = new ExpressionVariables();
		variables.addVariableDefinition(ObjectCollectionType.F_POLICY_SITUATION, collection.getPolicySituation());
		if(widget.getPresentation() != null) {
			if(widget.getPresentation().getVariation() != null) {
				widget.getPresentation().getVariation().forEach(variation -> {
					Task task = getPageBase().createSimpleTask("Evaluate variation");
					PrismPropertyValue<Boolean> usingVariation;
					try {
						usingVariation = ExpressionUtil.evaluateCondition(variables, variation.getCondition(), getPageBase().getExpressionFactory(),
								"Variation", task, task.getResult());
				
						if(usingVariation != null && usingVariation.getRealValue() != null
								&& usingVariation.getRealValue().equals(Boolean.TRUE)) {
							this.display = variation.getDisplay();
						}
					} catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException
							| ConfigurationException | SecurityViolationException e) {
						LOGGER.error("Couldn't evaluate condition " + variation.toString(), e);
					}
				});
			}  else {
				LOGGER.error("Variation of presentation is not found in widget " + widget.getIdentifier());
			}
		}  else {
			LOGGER.error("Presentation is not found in widget " + widget.getIdentifier());
		}
	}

	private int getObjectCount(ObjectCollectionType collection, boolean usingFilter) {
		Class<ObjectType> objectClass = (Class<ObjectType>) getPageBase().getPrismContext().getSchemaRegistry()
				.getCompileTimeClassForObjectType(collection.getType());
		SearchFilterType searchFilter = collection.getFilter();
		ObjectQuery query = getPageBase().getPrismContext().queryFactory().createQuery();
		if (searchFilter != null && usingFilter) {
			try {
				query.setFilter(getPageBase().getPrismContext().getQueryConverter().parseFilter(searchFilter, objectClass));
			} catch (SchemaException e) {
				LOGGER.error("Filter couldn't parse in collection " + collection.toString(), e);
			}
		}
		List<PrismObject<ObjectType>> values = WebModelServiceUtils.searchObjects(objectClass, 
				query, new OperationResult("Load value Objects"), getPageBase());
		return values.size();
	}

	protected void customInitLayout(WebMarkupContainer infoBox) {
		
	}

	private IModel<String> getStringModel(String value){
		return new IModel<String>() {

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
				return getPageBase().createWebPage(pageType, null);
			}  else {
				LOGGER.error("CollectionType from collectionRef is null in widget " + model.getObject().getIdentifier());
			}
			break;
		case AUDIT_SEARCH:
			Class<? extends WebPage> pageType = getLinksRefCollections().get(AuditEventRecordItemType.COMPLEX_TYPE.getLocalPart());
			return getPageBase().createWebPage(pageType, null);
		case OBJECT:
			ObjectType object = getObjectFromObjectRef();
			if(object == null) {
				return null;
			}
			QName typeName = WebComponentUtil.classToQName(getPageBase().getPrismContext(), object.getClass());
			pageType = getLinksRefObjects().get(typeName);
			PageParameters parameters = new PageParameters();
			parameters.add(OnePageParameterEncoder.PARAMETER, object.getOid());
			return getPageBase().createWebPage(pageType, parameters);
	}
	return null;
	}
	
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
	
	private AuditSearchType getAuditSearchType() {
		IModel<DashboardWidgetType> model = (IModel<DashboardWidgetType>)getDefaultModel();
		if(isDataNull(model)) {
			return null;
		}
		AuditSearchType auditSearch = model.getObject().getData().getAuditSearch();
		if(auditSearch == null) {
			LOGGER.error("AuditSearch of data is not found in widget " + model.getObject().getIdentifier());
		}
		return auditSearch;
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
	
	public static String getStringExpressionMessage(ExpressionVariables variables,
    		Task task, OperationResult result, ExpressionType expression, String shortDes) {
    	if (expression != null) {
        	Collection<String> contentTypeList = null;
        	try {
        		contentTypeList = ExpressionUtil.evaluateStringExpression(variables, getPageBase().getPrismContext(),
	        			expression, getPageBase().getExpressionFactory() , shortDes, task, result);
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
}
