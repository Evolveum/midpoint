/**
 * Copyright (c) 2015-2018 Evolveum
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
package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.ValueChoosePanel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.orgs.OrgTreePanel;
import com.evolveum.midpoint.web.page.admin.users.dto.TreeStateSet;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import javax.xml.namespace.QName;
import java.time.Instant;
import java.util.*;

/**
 * Created by honchar.
 */
public class ConstructionDetailsPanel<C extends Containerable, IW extends ItemWrapper> extends BasePanel<ContainerValueWrapper<C>> {
    private static final long serialVersionUID = 1L;

    private static final String ID_KIND_FIELD = "kindField";
    private static final String ID_INTENT_FIELD = "intentField";
    private static final String ID_ASSOCIATION = "association";

    private static final Trace LOGGER = TraceManager.getTrace(ConstructionDetailsPanel.class);
    private static final String DOT_CLASS = ConstructionDetailsPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";

    private LoadableDetachableModel<PrismObject<ResourceType>> resourceModel;
    private ContainerWrapper<ResourceObjectAssociationType> associationWrapper;

    public ConstructionDetailsPanel(String id, IModel<ContainerValueWrapper<C>> constructionWrapperModel){
        super(id, constructionWrapperModel);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initModels();
        initLayout();
    }

    private void initModels(){
        resourceModel = new LoadableDetachableModel<PrismObject<ResourceType>>() {
            @Override
            protected PrismObject<ResourceType> load() {
                ConstructionType construction = (ConstructionType)getModelObject().getContainerValue().asContainerable();
                ObjectReferenceType resourceRef = construction.getResourceRef();
                Task loadResourceTask = getPageBase().createSimpleTask(OPERATION_LOAD_RESOURCE);
                OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCE);
                PrismObject<ResourceType> resource = WebModelServiceUtils.loadObject(resourceRef, getPageBase(), loadResourceTask, result);
                result.computeStatusIfUnknown();
                if (!result.isAcceptable()) {
                    LOGGER.error("Cannot find resource referenced from construction. {}", result.getMessage());
                    result.recordPartialError("Could not find resource referenced from construction.");
                    return null;
                }
                return resource;
            }
        };
    }

    private void initLayout(){
        DropDownChoicePanel kindDropDown = WebComponentUtil.createEnumPanel(ShadowKindType.class, ID_KIND_FIELD,
                WebComponentUtil.createPrismPropertySingleValueModel(getModel(), ConstructionType.F_KIND), ConstructionDetailsPanel.this);
        kindDropDown.setOutputMarkupId(true);
        kindDropDown.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(ConstructionDetailsPanel.this);
            }
        });
        add(kindDropDown);

        DropDownChoicePanel intentDropDown = new DropDownChoicePanel(ID_INTENT_FIELD,
        WebComponentUtil.createPrismPropertySingleValueModel(getModel(), ConstructionType.F_INTENT), getIntentAvailableValuesModel());
        intentDropDown.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(ConstructionDetailsPanel.this);
            }
        });
        intentDropDown.setOutputMarkupId(true);
        add(intentDropDown);

        ContainerWrapperFactory factory = new ContainerWrapperFactory(getPageBase());
        ContainerStatus containerStatus = ContainerStatus.MODIFYING;
        PrismContainer<ResourceObjectAssociationType> assocContainer = getModelObject().getContainerValue().findContainer(ConstructionType.F_ASSOCIATION);
            try {
                if (assocContainer == null){
                    assocContainer = getModelObject().getContainerValue().findOrCreateContainer(ConstructionType.F_ASSOCIATION);
                    containerStatus = ContainerStatus.ADDING;
                }
                associationWrapper =
                        factory.createAssociationWrapper(resourceModel.getObject(), (ShadowKindType)kindDropDown.getBaseFormComponent().getModelObject(),
                                (String)intentDropDown.getBaseFormComponent().getModelObject(),
                                assocContainer, getModelObject().getObjectStatus(), containerStatus, new ItemPath(ConstructionType.F_ASSOCIATION));

            } catch (SchemaException ex){
                LOGGER.error("Cannot create association container for construction {}. {}", resourceModel.getObject().getName().getOrig(), ex.getLocalizedMessage());
            }
//           ListView<ContainerValueWrapper<ResourceObjectAssociationType>> associationDetailsPanel =
//                new ListView<ContainerValueWrapper<ResourceObjectAssociationType>>(ID_ASSOCIATION_CONTAINER, getAssociationsModel()){
//                        @Override
//                        protected void populateItem(ListItem<ContainerValueWrapper<ResourceObjectAssociationType>> item) {
//                        item.add(new AssociationDetailsPanel(ID_ASSOCIATION, item.getModel(),
//                                (ConstructionType)ConstructionDetailsPanel.this.getModelObject().getContainerValue().asContainerable()));
//                    }
//                };

//        ValueChoosePanel associationDetailsPanel = new ValueChoosePanel<PrismReferenceValue, ObjectType>(ID_ASSOCIATION,
//                new PropertyModel<>(getModel(), "value")) {
//
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            protected ObjectFilter createCustomFilter() {
////                ItemWrapper wrapper = PrismValuePanel.this.getModel().getObject().getItem();
////                if (!(wrapper instanceof ReferenceWrapper)) {
////                    return null;
////                }
////                return ((ReferenceWrapper) wrapper).getFilter();
//                return null;
//            }
//
//            @Override
//            protected boolean isEditButtonEnabled() {
////                return PrismValuePanel.this.getModel().getObject().isEditEnabled();
//                return true;
//            }
//
//            @Override
//            public List<QName> getSupportedTypes() {
////                List<QName> targetTypeList = ((ReferenceWrapper) PrismValuePanel.this.getModel().getObject().getItem()).getTargetTypes();
////                if (targetTypeList == null || WebComponentUtil.isAllNulls(targetTypeList)) {
////                    return Arrays.asList(ObjectType.COMPLEX_TYPE);
////                }
//
//                return Arrays.asList(ShadowType.COMPLEX_TYPE);
//            }
//
//            @Override
//            protected Class getDefaultType(List<QName> supportedTypes){
////                if (AbstractRoleType.COMPLEX_TYPE.equals(((PrismReference)item).getDefinition().getTargetTypeName())){
////                    return RoleType.class;
////                } else {
////                    return super.getDefaultType(supportedTypes);
////                }
//                return ShadowType.class;
//            }
//
//        };
//
        PrismContainerPanel<ResourceObjectAssociationType> associationDetailsPanel = new PrismContainerPanel<ResourceObjectAssociationType>(ID_ASSOCIATION,
                new IModel<ContainerWrapper<ResourceObjectAssociationType>>(){
                    private static final long serialVersionUID = 1L;

                    @Override
                    public ContainerWrapper<ResourceObjectAssociationType> getObject(){
                        return associationWrapper;
                    }

                    @Override
                    public void setObject(ContainerWrapper<ResourceObjectAssociationType> obj){
                        String s = "";
                    }

                    @Override
                    public void detach(){

                    }
                }, true, null, null, getPageBase()){
            @Override
            protected void onModelChanged() {
                super.onModelChanged();

                String r= "";
            }
        };
//
//        associationDetailsPanel.add(new AjaxEventBehavior("change") {
//
//            @Override
//            protected void onEvent(AjaxRequestTarget target) {
//                target.add(ConstructionDetailsPanel.this);
//            }
//        });
        associationDetailsPanel.setOutputMarkupId(true);
        add(associationDetailsPanel);
    }

    private PropertyModel<List<ContainerValueWrapper<ResourceObjectAssociationType>>> getAssociationsModel(){
//        PropertyModel<List<IW>> propertiesModel = new PropertyModel<>(get, "properties");
        List<ItemWrapper> propertiesList = getModelObject().getItems();
        for (ItemWrapper property : propertiesList){
            if (property.getName().equals(ConstructionType.F_ASSOCIATION)){
                return new PropertyModel<>(property, "values");
            }
        }
        return null;
    }

    private IModel<List<String>> getIntentAvailableValuesModel(){
        return new LoadableModel<List<String>>(true){
            private static final long serialVersionUID = 1L;

            @Override
            protected List<String> load(){
                List<String> availableIntentValues = new ArrayList<>();
                if (resourceModel.getObject() == null){
                    return availableIntentValues;
                }
                try {
                    RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resourceModel.getObject());
                    if (refinedSchema != null){
                        ShadowKindType kind = (ShadowKindType) ConstructionDetailsPanel.this.getKindDropdownComponent().getBaseFormComponent().getModelObject();
                        List<? extends RefinedObjectClassDefinition> definitions = refinedSchema.getRefinedDefinitions(kind);
                        for (RefinedObjectClassDefinition def : definitions){
                            availableIntentValues.add(def.getIntent());
                        }
                    }
                } catch (SchemaException ex){
                    LOGGER.error("Cannot get refined resource schema for resource {}. {}", resourceModel.getObject().getName().getOrig(), ex.getLocalizedMessage());
                }

                return availableIntentValues;
            }
        };
    }

//    private List<ResourceObjectAssociationType> loadAssociationFromResourceSchema(){
//        List<ResourceObjectAssociationType> assocList =  new ArrayList<>();
//        if (resourceModel.getObject() == null){
//            return assocList;
//        }
//        try {
//            RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resourceModel.getObject());
//            if (refinedSchema != null){
//                ShadowKindType kind = (ShadowKindType) ConstructionDetailsPanel.this.getKindDropdownComponent().getBaseFormComponent().getModelObject();
//                String intent = (String) ConstructionDetailsPanel.this.getIntentDropdownComponent().getBaseFormComponent().getModelObject();
//                RefinedObjectClassDefinition oc = refinedSchema.getRefinedDefinition(kind, intent);
//                if (oc == null) {
//                    LOGGER.debug("Association for {}/{} not supported by resource {}", kind, intent, resourceModel.getObject());
//                    return assocList;
//                }
//                Collection<RefinedAssociationDefinition> refinedAssociationDefinitions = oc.getAssociationDefinitions();
//
//                if (CollectionUtils.isEmpty(refinedAssociationDefinitions)) {
//                    LOGGER.debug("Association for {}/{} not supported by resource {}", kind, intent, resourceModel.getObject());
//                    return assocList;
//                }
//
//                PrismContainer<ResourceObjectAssociationType> association = getModelObject().getContainerValue().findOrCreateContainer(ConstructionType.F_ASSOCIATION);
//                for (RefinedAssociationDefinition refinedAssocationDefinition: refinedAssociationDefinitions) {
//                    PrismReferenceDefinitionImpl shadowRefDef = new PrismReferenceDefinitionImpl(refinedAssocationDefinition.getName(), ObjectReferenceType.COMPLEX_TYPE, modelServiceLocator.getPrismContext());
//                    shadowRefDef.setMaxOccurs(-1);
//                    shadowRefDef.setTargetTypeName(ShadowType.COMPLEX_TYPE);
//                    PrismReference shadowAss = shadowRefDef.instantiate();
//                    ItemPath itemPath = null;
//                    for (PrismContainerValue<ResourceObjectAssociationType> associationValue : association.getValues()) {
//                            //for now Induced entitlements gui should support only targetRef expression value
//                            //that is why no need to look for another expression types within association
//                            ResourceObjectAssociationType resourceAssociation = (ResourceObjectAssociationType) associationValue.asContainerable();
//                            if (resourceAssociation.getRef() == null || resourceAssociation.getRef().getItemPath() == null){
//                                continue;
//                            }
//                            if (resourceAssociation.getRef().getItemPath().asSingleName().equals(refinedAssocationDefinition.getName())){
//                                itemPath = associationValue.getPath();
//                                MappingType outbound = ((ResourceObjectAssociationType)association.getValue().asContainerable()).getOutbound();
//                                if (outbound == null){
//                                    continue;
//                                }
//                                ExpressionType expression = outbound.getExpression();
//                                if (expression == null){
//                                    continue;
//                                }
//                                ObjectReferenceType shadowRef = ExpressionUtil.getShadowRefValue(expression);
//                                if (shadowRef != null) {
//                                    shadowAss.add(shadowRef.asReferenceValue().clone());
//                                }
//                            }
//                    }
//
//                    if (itemPath == null) {
//                        itemPath = new ItemPath(ShadowType.F_ASSOCIATION);
//                    }
//
////                    ReferenceWrapper associationValueWrapper = new ReferenceWrapper(shadowValueWrapper, shadowAss, isItemReadOnly(association.getDefinition(), shadowValueWrapper), shadowAss.isEmpty() ? ValueStatus.ADDED : ValueStatus.NOT_CHANGED, itemPath);
//                    String displayName = refinedAssocationDefinition.getDisplayName();
//                    if (displayName == null) {
//                        displayName = refinedAssocationDefinition.getName().getLocalPart();
//                    }
////                    associationValueWrapper.setDisplayName(displayName);
//                    S_FilterEntryOrEmpty atomicFilter = QueryBuilder.queryFor(ShadowType.class, modelServiceLocator.getPrismContext());
//                    List<ObjectFilter> orFilterClauses = new ArrayList<>();
//                    refinedAssocationDefinition.getIntents()
//                            .forEach(intent -> orFilterClauses.add(atomicFilter.item(ShadowType.F_INTENT).eq(intent).buildFilter()));
//                    OrFilter intentFilter = OrFilter.createOr(orFilterClauses);
//
//                    AndFilter filter = (AndFilter) atomicFilter.item(ShadowType.F_KIND).eq(refinedAssocationDefinition.getKind()).and()
//                            .item(ShadowType.F_RESOURCE_REF).ref(resource.getOid(), ResourceType.COMPLEX_TYPE).buildFilter();
//                    filter.addCondition(intentFilter);
////                    associationValueWrapper.setFilter(filter);
////
////                    for (ValueWrapper valueWrapper : associationValueWrapper.getValues()) {
////                        valueWrapper.setEditEnabled(isEmpty(valueWrapper));
////                    }
//                    associationValueWrapper.setTargetTypes(Collections.singletonList(ShadowType.COMPLEX_TYPE));
//                    associationValuesWrappers.add(associationValueWrapper);
//                }
//
//            }
//        } catch (SchemaException ex){
//            LOGGER.error("Cannot get refined resource schema for resource {}. {}", resourceModel.getObject().getName().getOrig(), ex.getLocalizedMessage());
//        }
//
//        return assocList;
//    }

    private DropDownChoicePanel getKindDropdownComponent(){
        return (DropDownChoicePanel) get(ID_KIND_FIELD);
    }

    private DropDownChoicePanel getIntentDropdownComponent(){
        return (DropDownChoicePanel) get(ID_INTENT_FIELD);
    }
}
