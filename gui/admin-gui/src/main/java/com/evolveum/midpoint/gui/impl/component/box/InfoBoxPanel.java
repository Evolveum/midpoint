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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.component.IRequestablePage;

import com.evolveum.midpoint.gui.api.page.PageBase;
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
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
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
	
	private static HashMap<String, Class<? extends IRequestablePage>> linksRef;
	
	static {
	    linksRef = new HashMap<String, Class<? extends IRequestablePage>>() {
			private static final long serialVersionUID = 1L;

			{
	            put(ResourceType.COMPLEX_TYPE.getLocalPart(), PageResources.class);
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
	
	private IModel<?> getNumberMessage() {
		IModel<DashboardWidgetType> model = (IModel<DashboardWidgetType>)getDefaultModel();
		if(model.getObject().getData() != null) {
			if (model.getObject().getData().getSourceType() != null) {
				switch (model.getObject().getData().getSourceType()) {
				case OBJECT_COLLECTION:
					if(existDataFields(model.getObject().getPresentation())) {
						return getNumberMessageForCollection(model.getObject().getPresentation());
					}
				
				case AUDIT_SEARCH:
					if(existDataFields(model.getObject().getPresentation())) {
						return getNumberMessageForAuditSearch(model.getObject().getPresentation());
					}
				case OBJECT:
					return getNumberMessageForObject();
				}
			} else {
				LOGGER.warn("SourceType of data is not defined");
			} 
		} else {
			LOGGER.warn("Data of widget is not defined");
		}
		return getPageBase().createStringResource("InfoBoxPanel.message.unknown");
	}

	private boolean existDataFields(DashboardWidgetPresentationType presentation) {
		if(presentation != null) {
			if(presentation.getDataField() != null) {
				if(!presentation.getDataField().isEmpty()) {
					return true;
				} else {
					LOGGER.warn("DataField of presentation is empty");
				}
			} else {
				LOGGER.warn("DataField of presentation is not defined");
			}
		} else {
			LOGGER.warn("Presentation of widget is not defined");
		}
		
		return false;
	}

	private IModel<String> getNumberMessageForObject() {
		return getPageBase().createStringResource("InfoBoxPanel.message.unknown");
	}

	private IModel<String> getNumberMessageForAuditSearch(DashboardWidgetPresentationType dashboardWidgetPresentationType) {
		return getPageBase().createStringResource("InfoBoxPanel.message.unknown");
	}

	private IModel<String> getNumberMessageForCollection(DashboardWidgetPresentationType dashboardWidgetPresentationType) {
		IModel<DashboardWidgetType> model = (IModel<DashboardWidgetType>)getDefaultModel();
		Map<DashboardWidgetDataFieldTypeType, String> numberMessagesParts = new HashMap<DashboardWidgetDataFieldTypeType, String>(); 
		model.getObject().getPresentation().getDataField().forEach(dataField -> {
			switch(dataField.getFieldType()) {
			
			case VALUE:
				ObjectCollectionType valueCollection = getObjectCollectionType();
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
						LOGGER.warn("Domain or collectionRef in domain is null in collection " + valueCollection.toString());
						LOGGER.trace("Using filter for all object based on type");
						domainValue = getObjectCount(valueCollection, false);
					}
					String valueMessaget = getNumberMessageFromExpression(value, domainValue, dataField.getExpression());
					numberMessagesParts.put(DashboardWidgetDataFieldTypeType.VALUE, valueMessaget);
				}  else {
					LOGGER.warn("CollectionType from collectionRef is null in widget " + model.getObject().getIdentifier());
				}
				break;
				
			case UNIT:
				Task task = getPageBase().createSimpleTask("Get unit");
				String unit = getStringExpressionMessage(new ExpressionVariables(), task, task.getResult(), dataField.getExpression(), "Unit");
				numberMessagesParts.put(DashboardWidgetDataFieldTypeType.UNIT, unit);
				break;
			}
		});
		if(!numberMessagesParts.containsKey(DashboardWidgetDataFieldTypeType.VALUE)) {
			LOGGER.warn("Value message is not generate from widget " + model.getObject().getIdentifier());
			return getPageBase().createStringResource("InfoBoxPanel.message.unknown");
		}
		StringBuilder sb = new StringBuilder();
		sb.append(numberMessagesParts.get(DashboardWidgetDataFieldTypeType.VALUE));
		if(numberMessagesParts.containsKey(DashboardWidgetDataFieldTypeType.UNIT)) {
			sb.append(" ").append(numberMessagesParts.get(DashboardWidgetDataFieldTypeType.UNIT));
		}
		return getStringModel(sb.toString());
	}

	private void evaluateVariation(DashboardWidgetType widget, ObjectCollectionType collection) {
		IModel<DashboardWidgetType> model = (IModel<DashboardWidgetType>)getDefaultModel();
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
				LOGGER.warn("Variation of presentation is not found in widget " + widget.getIdentifier());
			}
		}  else {
			LOGGER.warn("Presentation is not found in widget " + widget.getIdentifier());
		}
	}

	private String getNumberMessageFromExpression(Integer value, Integer domainValue, ExpressionType expression) {
		Task task = getPageBase().createSimpleTask("Search domain collection");
		IntegerStatType statType = new IntegerStatType();
		statType.setValue(value);
		statType.setDomain(domainValue);
		ExpressionVariables variables = new ExpressionVariables();
		variables.addVariableDefinition(ExpressionConstants.VAR_INPUT, statType);
		String valueMessaget = getStringExpressionMessage(variables, task, task.getResult(), expression, "Proportional message");
		return valueMessaget;
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
	
	protected static HashMap<String, Class<? extends IRequestablePage>> getLinksRef() {
		return linksRef;
	}
	
	protected static PageBase getPageBase() {
		return pageBase;
	}
	
	protected Class<? extends IRequestablePage> getLinkRef() {
		IModel<DashboardWidgetType> model = (IModel<DashboardWidgetType>)getDefaultModel();
		ObjectCollectionType collection = getObjectCollectionType();
		if(collection != null && collection.getType() != null && collection.getType().getLocalPart() != null) {
			return getLinksRef().get(collection.getType().getLocalPart());
		}  else {
			LOGGER.warn("CollectionType from collectionRef is null in widget " + model.getObject().getIdentifier());
		}
		return null;
	}
	
	private ObjectCollectionType getObjectCollectionType() {
		IModel<DashboardWidgetType> model = (IModel<DashboardWidgetType>)getDefaultModel();
		if(model.getObject().getData() != null) {
				if( model.getObject().getData().getSourceType().equals(DashboardWidgetSourceTypeType.OBJECT_COLLECTION)){
					if( model.getObject().getData().getCollection() != null) {
						ObjectReferenceType ref = model.getObject().getData().getCollection().getCollectionRef();
						if (ref != null) {
							Task task = getPageBase().createSimpleTask("Search collection");
							ObjectCollectionType collection = (ObjectCollectionType)WebModelServiceUtils.loadObject(ref, 
									getPageBase(), task, task.getResult()).getRealValue();
								return collection;
						}  else {
							LOGGER.warn("CollectionRef of collection is not found in widget " + model.getObject().getIdentifier());
						}
					} else {
						LOGGER.warn("Collection of data is not found in widget " + model.getObject().getIdentifier());
					}
				}
		} else {
			LOGGER.warn("Data is not found in widget " + model.getObject().getIdentifier());
		}
		return null;
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
        		LOGGER.error("Couldn't evaluate ProportionalExpression " + expression.toString(), e);
        	}
            if (contentTypeList == null || contentTypeList.isEmpty()) {
               throw new IllegalStateException("Proportional expression returned nothing");
            }
            if (contentTypeList.size() > 1) {
                LOGGER.warn("Proportional expression returned more than 1 item.");
            }
            return contentTypeList.iterator().next();
        } else {
            return null;
        }
    }
}
