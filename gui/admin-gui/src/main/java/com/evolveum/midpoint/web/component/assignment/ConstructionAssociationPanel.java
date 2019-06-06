/**
 * Copyright (c) 2015-2018 Evolveum
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.factory.WrapperContext;
import com.evolveum.midpoint.gui.impl.prism.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.util.ItemPathTypeUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.multivalue.GenericMultiValueLabelEditPanel;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.*;

import javax.xml.namespace.QName;
import java.util.*;


/**
 * Created by honchar.
 */
public class ConstructionAssociationPanel extends BasePanel<PrismContainerWrapper<ConstructionType>> {
    private static final long serialVersionUID = 1L;

    private static final String ID_ASSOCIATIONS = "associations";
    private static final String ID_ASSOCIATION_NAME = "associationName";
    private static final String ID_ASSOCIATION_REFERENCE_PANEL = "associationReferencePanel";

    private static final Trace LOGGER = TraceManager.getTrace(ConstructionAssociationPanel.class);
    private static final String DOT_CLASS = ConstructionAssociationPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";
    private static final String OPERATION_LOAD_SHADOW_DISPLAY_NAME = DOT_CLASS + "loadShadowReferenceDisplayName";

    private static final String ID_LABEL_SIZE = "col-md-4";
    private static final String ID_INPUT_SIZE = "col-md-6";

    private LoadableDetachableModel<PrismObject<ResourceType>> resourceModel;
    private LoadableDetachableModel<List<RefinedAssociationDefinition>> refinedAssociationDefinitionsModel;


    public ConstructionAssociationPanel(String id, IModel<PrismContainerWrapper<ConstructionType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initModels();
        initLayout();
    }

    private void initModels() {
        resourceModel = new LoadableDetachableModel<PrismObject<ResourceType>>() {
            @Override
            protected PrismObject<ResourceType> load() {
                ConstructionType construction = getModelObject().getItem().getRealValue();
                ObjectReferenceType resourceRef = construction.getResourceRef();
                Task loadResourceTask = getPageBase().createSimpleTask(OPERATION_LOAD_RESOURCE);
                OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCE);
                PrismObject<ResourceType> resource = WebModelServiceUtils.loadObject(resourceRef, getPageBase(), loadResourceTask, result);
                result.computeStatusIfUnknown();
                if (!result.isAcceptable()) {
                    LOGGER.error("Cannot find resource referenced from construction. {}", construction, result.getMessage());
                    result.recordPartialError("Could not find resource referenced from construction.");
                    return null;
                }
                return resource;
            }
        };
        refinedAssociationDefinitionsModel = new LoadableDetachableModel<List<RefinedAssociationDefinition>>() {
            @Override
            protected List<RefinedAssociationDefinition> load() {
                ConstructionType construction = getModelObject().getItem().getRealValue();
                if (construction == null){
                    return new ArrayList<>();
                }
                return WebComponentUtil.getRefinedAssociationDefinition(resourceModel.getObject().asObjectable(), construction.getKind(),
                        construction.getIntent());
            }
        };
    }

    protected void initLayout() {
        ListView<RefinedAssociationDefinition> associationsPanel =
                new ListView<RefinedAssociationDefinition>(ID_ASSOCIATIONS, refinedAssociationDefinitionsModel) {
                    @Override
                    protected void populateItem(ListItem<RefinedAssociationDefinition> item) {
                        GenericMultiValueLabelEditPanel associationReferencesPanel = new GenericMultiValueLabelEditPanel<ObjectReferenceType>(ID_ASSOCIATION_REFERENCE_PANEL,
                                getShadowReferencesModel(item.getModelObject()),
                                Model.of(WebComponentUtil.getAssociationDisplayName(item.getModelObject())),
                                ID_LABEL_SIZE, ID_INPUT_SIZE, true) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected boolean isEditButtonEnabled() {
                                return false;
                            }

                            @Override
                            protected void addValuePerformed(AjaxRequestTarget target) {
                                addNewShadowRefValuePerformed(target, item.getModelObject());
                            }

                            protected void addFirstPerformed(AjaxRequestTarget target){
                                addNewShadowRefValuePerformed(target, item.getModelObject());
                            }

                            @Override
                            protected IModel<String> createTextModel(final IModel<ObjectReferenceType> model) {
                                return new IModel<String>() {
                                    private static final long serialVersionUID = 1L;
                                    @Override
                                    public String getObject() {
                                        ObjectReferenceType obj = model.getObject();
                                        if (obj == null){
                                            return "";
                                        }
                                        return WebComponentUtil.getDisplayNameOrName(obj, getPageBase(), OPERATION_LOAD_SHADOW_DISPLAY_NAME);
                                    }
                                };
                            }

                            @Override
                            protected void removeValuePerformed(AjaxRequestTarget target, ListItem<ObjectReferenceType> item) {
                                try {
                                    ObjectReferenceType removedShadowRef = item.getModelObject();
                                    PrismContainerWrapper<ConstructionType> constructionContainerWrapper = ConstructionAssociationPanel.this.getModelObject();
                                    PrismContainerWrapper<ResourceObjectAssociationType> associationWrapper = constructionContainerWrapper
                                            .findContainer(ConstructionType.F_ASSOCIATION);
                                    associationWrapper.getValues().forEach(associationValueWrapper -> {
                                        if (ValueStatus.DELETED.equals(associationValueWrapper.getStatus())) {
                                            return;
                                        }
                                        ResourceObjectAssociationType associationValue = associationValueWrapper.getRealValue();
//                                        ResourceObjectAssociationType assoc = (ResourceObjectAssociationType) associationValue.asContainerable();
                                        if (associationValue == null || associationValue.getOutbound() == null || associationValue.getOutbound().getExpression() == null ||
                                                ExpressionUtil.getShadowRefValue(associationValue.getOutbound().getExpression(),
                                                        ConstructionAssociationPanel.this.getPageBase().getPrismContext()) == null) {
                                            return;
                                        }
                                        List<ObjectReferenceType> shadowRefList = ExpressionUtil.getShadowRefValue(associationValue.getOutbound().getExpression(),
                                                ConstructionAssociationPanel.this.getPageBase().getPrismContext());
                                        shadowRefList.forEach(shadowRef -> {
                                            if (shadowRef.equals(removedShadowRef)) {
                                                associationValueWrapper.setStatus(ValueStatus.DELETED);
                                            }
                                        });
                                    });
                                } catch (SchemaException ex){
                                    LOGGER.error("Couldn't remove association value, ", ex.getLocalizedMessage());
                                }
                                super.removeValuePerformed(target, item);
                            }


                        };
                        associationReferencesPanel.setOutputMarkupId(true);
                        item.add(associationReferencesPanel);
                    }
                };

        associationsPanel.setOutputMarkupId(true);
        add(associationsPanel);
    }

    private IModel<List<ObjectReferenceType>> getShadowReferencesModel(RefinedAssociationDefinition def) {
        return new LoadableModel<List<ObjectReferenceType>>() {
            private static final long serialVersionUID = 1L;

            @Override
            public List<ObjectReferenceType> load() {
                try {
                    QName defName = def.getName();
                    List<ObjectReferenceType> shadowsList = new ArrayList<>();
                    PrismContainerWrapper<ResourceObjectAssociationType> associationWrapper =
                            getModelObject().findContainer(ConstructionType.F_ASSOCIATION);
                    associationWrapper.getValues().forEach(associationValueWrapper -> {
                        if (ValueStatus.DELETED.equals(((PrismContainerValueWrapper) associationValueWrapper).getStatus())) {
                            return;
                        }
                        PrismContainerValue associationValue = (PrismContainerValue)((PrismContainerValueWrapper) associationValueWrapper).getNewValue();
                        ResourceObjectAssociationType assoc = (ResourceObjectAssociationType) associationValue.asContainerable();
                        if (assoc == null || assoc.getOutbound() == null || assoc.getOutbound().getExpression() == null
                                || (ExpressionUtil.getShadowRefValue(assoc.getOutbound().getExpression(),
                                ConstructionAssociationPanel.this.getPageBase().getPrismContext()) == null
                                && !ValueStatus.ADDED.equals(((PrismContainerValueWrapper) associationValueWrapper).getStatus()))) {
                            return;
                        }
                        QName assocRef = ItemPathTypeUtil.asSingleNameOrFailNullSafe(assoc.getRef());
                        if ((defName != null && defName.equals(assocRef))
                                || (assocRef == null && ValueStatus.ADDED.equals(((PrismContainerValueWrapper) associationValueWrapper).getStatus()))) {
                            shadowsList.addAll(ExpressionUtil.getShadowRefValue(assoc.getOutbound().getExpression(),
                                    ConstructionAssociationPanel.this.getPageBase().getPrismContext()));
                        }
                    });
                    return shadowsList;
                } catch (SchemaException ex){

                }
                return null;
            }
        };
    }

    private List<ObjectReferenceType> getAssociationsShadowRefs(boolean compareName, QName name) {
        List<ObjectReferenceType> shadowsList = new ArrayList<>();
        try {
            PrismContainerWrapper associationWrapper = getModelObject().findContainer(ConstructionType.F_ASSOCIATION);
            associationWrapper.getValues().forEach(associationValueWrapper -> {
                if (ValueStatus.DELETED.equals(((PrismContainerValueWrapper) associationValueWrapper).getStatus())) {
                    return;
                }
                PrismContainerValue associationValue = (PrismContainerValue) ((PrismContainerValueWrapper) associationValueWrapper).getNewValue();
                ResourceObjectAssociationType assoc = (ResourceObjectAssociationType) associationValue.asContainerable();
                if (assoc == null || assoc.getOutbound() == null || assoc.getOutbound().getExpression() == null
                        || ExpressionUtil.getShadowRefValue(assoc.getOutbound().getExpression(), getPageBase().getPrismContext()) == null) {
                    return;
                }
                if (compareName) {
                    QName assocRef = ItemPathTypeUtil.asSingleNameOrFailNullSafe(assoc.getRef());
                    if (name != null && name.equals(assocRef)) {
                        shadowsList.addAll(ExpressionUtil.getShadowRefValue(assoc.getOutbound().getExpression(), getPageBase().getPrismContext()));
                    }
                } else {
                    shadowsList.addAll(ExpressionUtil.getShadowRefValue(assoc.getOutbound().getExpression(), getPageBase().getPrismContext()));
                }
            });
        } catch (SchemaException ex){

        }
        return shadowsList;

    }

    private void addNewShadowRefValuePerformed(AjaxRequestTarget target, RefinedAssociationDefinition def){
        ObjectFilter filter = WebComponentUtil.createAssociationShadowRefFilter(def,
                getPageBase().getPrismContext(), resourceModel.getObject().getOid());
        ObjectBrowserPanel<ShadowType> objectBrowserPanel = new ObjectBrowserPanel<ShadowType>(
                getPageBase().getMainPopupBodyId(), ShadowType.class, Arrays.asList(ShadowType.COMPLEX_TYPE),
                false, getPageBase(),
                filter) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSelectPerformed(AjaxRequestTarget target, ShadowType object) {
                getPageBase().hideMainPopup(target);
                try {
                    PrismContainerWrapper<ConstructionType> constructionContainerWrapper = ConstructionAssociationPanel.this.getModelObject();
                    PrismContainerWrapper<ResourceObjectAssociationType> associationWrapper = constructionContainerWrapper.findContainer(ConstructionType.F_ASSOCIATION);
                    PrismContainerValue newAssociation = associationWrapper.getItem().createNewValue();
                    ItemName associationRefPath = def.getName();
                    ((ResourceObjectAssociationType) newAssociation.asContainerable())
                            .setRef(new ItemPathType(associationRefPath));
                    ExpressionType newAssociationExpression = ((ResourceObjectAssociationType) newAssociation.asContainerable()).beginOutbound().beginExpression();
                    ExpressionUtil.addShadowRefEvaluatorValue(newAssociationExpression, object.getOid(),
                            getPageBase().getPrismContext());
//                    ContainerWrapperFactory factory = new ContainerWrapperFactory(getPageBase());
//                    Task task = getPageBase().createAnonymousTask("Adding new shadow");
//                    getPageBase().createValueWrapper(associationWrapper, newAssociation, ValueStatus.ADDED, null);
//                    ContainerValueWrapper<ResourceObjectAssociationType> valueWrapper =
//                            factory.createContainerValueWrapper(associationWrapper, newAssociation,
//                                    associationWrapper.getObjectStatus(), ValueStatus.ADDED, associationWrapper.getPath(), task);

                    Task task = ConstructionAssociationPanel.this.getPageBase().createSimpleTask("Create new association value wrapper");
                    WrapperContext context = new WrapperContext(task, task.getResult());
                    associationWrapper.getValues().add(getPageBase().createValueWrapper(associationWrapper, newAssociation, ValueStatus.ADDED, context));
                } catch (SchemaException ex){
                    LOGGER.error("Couldn't find association container, ", ex.getLocalizedMessage());
                }
                target.add(ConstructionAssociationPanel.this);
            }

        };

        getPageBase().showMainPopup(objectBrowserPanel, target);

    }
}
