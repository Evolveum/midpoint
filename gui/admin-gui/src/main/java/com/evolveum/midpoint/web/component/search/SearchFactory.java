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

package com.evolveum.midpoint.web.component.search;

import java.lang.reflect.Modifier;
import java.util.*;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FullTextSearchConfigurationUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AreaCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectListViewType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectListViewsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectPolicyConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author Viliam Repan (lazyman)
 */
public class SearchFactory {

    private static final String DOT_CLASS = SearchFactory.class.getName() + ".";
    private static final String LOAD_OBJECT_DEFINITION = DOT_CLASS + "loadObjectDefinition";
    private static final String LOAD_SYSTEM_CONFIGURATION = DOT_CLASS + "loadSystemConfiguration";
    private static final String LOAD_ADMIN_GUI_CONFIGURATION = DOT_CLASS + "loadAdminGuiConfiguration";

    private static final Map<Class, List<ItemPath>> SEARCHABLE_OBJECTS = new HashMap<>();

    static {
        SEARCHABLE_OBJECTS.put(ObjectType.class, Arrays.asList(
                ItemPath.create(ObjectType.F_NAME),
                ItemPath.create(ObjectType.F_LIFECYCLE_STATE),
                ItemPath.create(ObjectType.F_SUBTYPE)));
        SEARCHABLE_OBJECTS.put(FocusType.class, Arrays.asList(
                ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
                ItemPath.create(FocusType.F_ROLE_MEMBERSHIP_REF),
                ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS)));
        SEARCHABLE_OBJECTS.put(UserType.class, Arrays.asList(
                ItemPath.create(UserType.F_TITLE),
                ItemPath.create(UserType.F_GIVEN_NAME),
                ItemPath.create(UserType.F_FAMILY_NAME),
                ItemPath.create(UserType.F_FULL_NAME),
                ItemPath.create(UserType.F_ADDITIONAL_NAME),
                ItemPath.create(UserType.F_COST_CENTER),
                ItemPath.create(UserType.F_EMAIL_ADDRESS),
                ItemPath.create(UserType.F_TELEPHONE_NUMBER),
                ItemPath.create(UserType.F_EMPLOYEE_NUMBER),
                ItemPath.create(UserType.F_EMPLOYEE_TYPE),
                ItemPath.create(UserType.F_ORGANIZATIONAL_UNIT),
                ItemPath.create(UserType.F_COST_CENTER),
                ItemPath.create(UserType.F_LOCALITY)));
        SEARCHABLE_OBJECTS.put(RoleType.class, Arrays.asList(
                ItemPath.create(RoleType.F_NAME),
                ItemPath.create(RoleType.F_DISPLAY_NAME),
                ItemPath.create(RoleType.F_ROLE_TYPE)));
        SEARCHABLE_OBJECTS.put(ServiceType.class, Arrays.asList(
                ItemPath.create(ServiceType.F_NAME),
                ItemPath.create(ServiceType.F_SERVICE_TYPE),
                ItemPath.create(ServiceType.F_URL)));
        SEARCHABLE_OBJECTS.put(ConnectorHostType.class, Arrays.asList(
                ItemPath.create(ConnectorHostType.F_HOSTNAME)
        ));
        SEARCHABLE_OBJECTS.put(ConnectorType.class, Arrays.asList(
                ItemPath.create(ConnectorType.F_CONNECTOR_BUNDLE),
                ItemPath.create(ConnectorType.F_CONNECTOR_VERSION),
                ItemPath.create(ConnectorType.F_CONNECTOR_TYPE)
        ));
        SEARCHABLE_OBJECTS.put(AbstractRoleType.class, Arrays.asList(
        		ItemPath.create(AbstractRoleType.F_IDENTIFIER),
                ItemPath.create(AbstractRoleType.F_REQUESTABLE)
        ));
        SEARCHABLE_OBJECTS.put(OrgType.class, Arrays.asList(
                ItemPath.create(OrgType.F_DISPLAY_NAME),
                ItemPath.create(OrgType.F_COST_CENTER),
                ItemPath.create(OrgType.F_ORG_TYPE),
                ItemPath.create(OrgType.F_TENANT),
                ItemPath.create(OrgType.F_LOCALITY)
        ));
        SEARCHABLE_OBJECTS.put(GenericObjectType.class, Arrays.asList(
                ItemPath.create(GenericObjectType.F_OBJECT_TYPE)
        ));
        SEARCHABLE_OBJECTS.put(NodeType.class, Arrays.asList(
                ItemPath.create(NodeType.F_NODE_IDENTIFIER)
        ));
        SEARCHABLE_OBJECTS.put(ReportType.class, Arrays.asList(
                ItemPath.create(ReportType.F_PARENT)
        ));
        SEARCHABLE_OBJECTS.put(ShadowType.class, Arrays.asList(
//                ItemPath.create(ShadowType.F_OBJECT_CLASS),
                ItemPath.create(ShadowType.F_DEAD),
                ItemPath.create(ShadowType.F_INTENT),
                ItemPath.create(ShadowType.F_EXISTS),
                ItemPath.create(ShadowType.F_SYNCHRONIZATION_SITUATION),
                ItemPath.create(ShadowType.F_FAILED_OPERATION_TYPE)
        ));
        SEARCHABLE_OBJECTS.put(TaskType.class, Arrays.asList(
                ItemPath.create(TaskType.F_TASK_IDENTIFIER),
                ItemPath.create(TaskType.F_NODE),
                ItemPath.create(TaskType.F_CATEGORY),
                ItemPath.create(TaskType.F_RESULT_STATUS)
        ));
        
        SEARCHABLE_OBJECTS.put(AssignmentType.class, Arrays.asList(
                ItemPath.create(AssignmentType.F_TARGET_REF),
                ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF),
                ItemPath.create(AssignmentType.F_TENANT_REF),
                ItemPath.create(AssignmentType.F_ORG_REF)
        ));
        
        SEARCHABLE_OBJECTS.put(ObjectPolicyConfigurationType.class, Arrays.asList(
                ItemPath.create(ObjectPolicyConfigurationType.F_SUBTYPE),
                ItemPath.create(ObjectPolicyConfigurationType.F_OBJECT_TEMPLATE_REF)
        ));
    }

    public static <T extends ObjectType> Search createSearchForShadow(
            ResourceShadowDiscriminator discriminator, ModelServiceLocator modelServiceLocator) {
        return createSearch(ShadowType.class, discriminator, modelServiceLocator, true);
    }
    
    public static <C extends Containerable> Search createContainerSearch(Class<C> type, ModelServiceLocator modelServiceLocator) {
    	
    	PrismContainerDefinition<C> containerDef = modelServiceLocator.getPrismContext().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(type);
    	List<SearchItemDefinition> availableDefs = getAvailableDefinitions(containerDef, true);
    	
    	Search search = new Search(type, availableDefs);
    	return search;
    }
    
//    public static <C extends Containerable> Search createContainerSearch(Class<C> type, List<SearchItemDefinition> availableDefs, ModelServiceLocator modelServiceLocator) {
//    	
////    	PrismContainerDefinition<C> containerDef = modelServiceLocator.getPrismContext().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(type);
////    	List<SearchItemDefinition> availableDefs = getAvailableDefinitions(containerDef, true);
//    	
//    	Search search = new Search(type, availableDefs);
//    	return search;
//    }
    
        public static <T extends ObjectType> Search createSearch(Class<T> type, ModelServiceLocator modelServiceLocator) {
        return createSearch(type, null, modelServiceLocator, true);
    }

    public static <T extends ObjectType> Search createSearch(
            Class<T> type, ResourceShadowDiscriminator discriminator,
            ModelServiceLocator modelServiceLocator, boolean useDefsFromSuperclass) {

        PrismObjectDefinition objectDef = findObjectDefinition(type, discriminator, modelServiceLocator);

        List<SearchItemDefinition> availableDefs = getAvailableDefinitions(objectDef, useDefsFromSuperclass);
        boolean isFullTextSearchEnabled = isFullTextSearchEnabled(modelServiceLocator, type);

        Search search = new Search(type, availableDefs, isFullTextSearchEnabled,
                getDefaultSearchType(modelServiceLocator, type));

        SchemaRegistry registry = modelServiceLocator.getPrismContext().getSchemaRegistry();
        PrismObjectDefinition objDef = registry.findObjectDefinitionByCompileTimeClass(ObjectType.class);
        PrismPropertyDefinition def = objDef.findPropertyDefinition(ObjectType.F_NAME);

        SearchItem item = search.addItem(def);
        if (item != null) {
            item.setFixed(true);
        }

        return search;
    }

    private static <T extends ObjectType> PrismObjectDefinition findObjectDefinition(
            Class<T> type, ResourceShadowDiscriminator discriminator,
            ModelServiceLocator modelServiceLocator) {

        Task task = modelServiceLocator.createSimpleTask(LOAD_OBJECT_DEFINITION);
        OperationResult result = task.getResult();
        try {
            if (Modifier.isAbstract(type.getModifiers())) {
                SchemaRegistry registry = modelServiceLocator.getPrismContext().getSchemaRegistry();
                return registry.findObjectDefinitionByCompileTimeClass(type);
            }
            PrismObject empty = modelServiceLocator.getPrismContext().createObject(type);

            if (ShadowType.class.equals(type)) {
                return modelServiceLocator.getModelInteractionService().getEditShadowDefinition(discriminator,
                        AuthorizationPhaseType.REQUEST, task, result);
            } else {
                return modelServiceLocator.getModelInteractionService().getEditObjectDefinition(
                        empty, AuthorizationPhaseType.REQUEST, task, result);
            }
        } catch (SchemaException | ConfigurationException | ObjectNotFoundException | ExpressionEvaluationException | CommunicationException | SecurityViolationException ex) {
            result.recordFatalError(ex.getMessage());
            throw new SystemException(ex);
        }
    }

    private static <C extends Containerable> List<SearchItemDefinition> getAvailableDefinitions(
            PrismContainerDefinition<C> objectDef, boolean useDefsFromSuperclass) {
//        Map<ItemPath, ItemDefinition> map = new HashMap<>();
    	List<SearchItemDefinition> definitions = new ArrayList<>();

        if (objectDef == null) {
        	return definitions;
        }
        
        definitions.addAll(createExtensionDefinitionList(objectDef));

        Class<C> typeClass = objectDef.getCompileTimeClass();
        while (typeClass != null && !com.evolveum.prism.xml.ns._public.types_3.ObjectType.class.equals(typeClass)) {
            List<ItemPath> paths = SEARCHABLE_OBJECTS.get(typeClass);
            if (paths != null) {
                for (ItemPath path : paths) {
                    ItemDefinition def = objectDef.findItemDefinition(path);
                    if (def != null) {
                    	SearchItemDefinition searchItemDef = new SearchItemDefinition(path, def, null);
                    	definitions.add(searchItemDef);
                    }
                }
            }

            if (!useDefsFromSuperclass) {
                break;
            }

            typeClass = (Class<C>) typeClass.getSuperclass();
        }

        return definitions;
    }

    private static <T extends ObjectType> boolean isFullTextSearchEnabled(ModelServiceLocator modelServiceLocator, Class<T> type) {
        OperationResult result = new OperationResult(LOAD_SYSTEM_CONFIGURATION);
        try {
            return FullTextSearchConfigurationUtil.isEnabledFor(modelServiceLocator.getModelInteractionService().getSystemConfiguration(result)
                    .getFullTextSearch(), type);
        } catch (SchemaException | ObjectNotFoundException ex) {
	        throw new SystemException(ex);
        }
    }

    private static <T extends ObjectType> SearchBoxModeType getDefaultSearchType(ModelServiceLocator modelServiceLocator, Class<T> type) {
        OperationResult result = new OperationResult(LOAD_ADMIN_GUI_CONFIGURATION);
        try {
            AdminGuiConfigurationType guiConfig = modelServiceLocator.getModelInteractionService().getAdminGuiConfiguration(null, result);
            GuiObjectListViewsType objectLists = guiConfig.getObjectLists();
            if (objectLists != null && objectLists.getObjectList() != null){
                for (GuiObjectListViewType objectList : objectLists.getObjectList()){
                    if (objectList.getType() != null
                            && type.getSimpleName().equals(objectList.getType().getLocalPart())
                            && objectList.getSearchBoxConfiguration() != null) {
                        SearchBoxConfigurationType searchBoxConfig = objectList.getSearchBoxConfiguration();
                        return searchBoxConfig.getDefaultMode();
                    }
                }
            }
            return null;
        } catch (SchemaException | ObjectNotFoundException ex) {
            throw new SystemException(ex);
        }
    }

    public static <C extends Containerable> List<SearchItemDefinition> createExtensionDefinitionList(
            PrismContainerDefinition<C> objectDef) {

//        Map<ItemPath, ItemDefinition> map = new HashMap<>();
    	List<SearchItemDefinition> searchItemDefinitions = new ArrayList<>();

        ItemPath extensionPath = ObjectType.F_EXTENSION;

        PrismContainerDefinition ext = objectDef.findContainerDefinition(ObjectType.F_EXTENSION);
        if (ext == null) {
        	return searchItemDefinitions;
        }
        
        for (ItemDefinition def : (List<ItemDefinition>) ext.getDefinitions()) {
        	ItemPath itemPath = ItemPath.create(extensionPath, def.getName());
        	
        	if (!isIndexed(def)) {
            	continue;
            }
        	
        	if (def instanceof PrismPropertyDefinition) {
        		addSearchPropertyDef(objectDef, itemPath, searchItemDefinitions);
        	}
        	
        	if (def instanceof PrismReferenceDefinition) {
        		addSearchRefDef(objectDef, itemPath, searchItemDefinitions, null, null);
        	}
        	
            if (!(def instanceof PrismPropertyDefinition)
                    && !(def instanceof PrismReferenceDefinition)) {
                continue;
            }

        }

        return searchItemDefinitions;
    }
    
    public static <C extends Containerable> void addSearchRefDef(PrismContainerDefinition<C> containerDef, ItemPath path, List<SearchItemDefinition> defs, AreaCategoryType category, PageBase pageBase) {
		PrismReferenceDefinition refDef = containerDef.findReferenceDefinition(path);
		if (refDef == null) {
			return;
		}
		if (pageBase == null) {
			defs.add(new SearchItemDefinition(path, refDef, Collections.singletonList(WebComponentUtil.getDefaultRelationOrFail())));
			return;
		}
		defs.add(new SearchItemDefinition(path, refDef, WebComponentUtil.getCategoryRelationChoices(category, pageBase)));
	}
	
	public static <C extends Containerable> void addSearchPropertyDef(PrismContainerDefinition<C> containerDef, ItemPath path, List<SearchItemDefinition> defs) {
		PrismPropertyDefinition propDef = containerDef.findPropertyDefinition(path);
		if (propDef == null) {
			return;
		}
		defs.add(new SearchItemDefinition(path, propDef, null));
	}
    
    private static boolean isIndexed(ItemDefinition def) {
    	if (!(def instanceof PrismPropertyDefinition)) {
    		return true;
    	}
    	
    	PrismPropertyDefinition propertyDef = (PrismPropertyDefinition) def;
    	Boolean indexed = propertyDef.isIndexed();
    	if (indexed == null) {
    		return true;
    	}
    	
    	return indexed.booleanValue();
    }
}
