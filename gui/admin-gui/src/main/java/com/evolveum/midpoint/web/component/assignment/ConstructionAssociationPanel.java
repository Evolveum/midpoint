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
import com.evolveum.midpoint.prism.util.ItemPathUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.ValueChoosePanel;
import com.evolveum.midpoint.web.component.form.multivalue.GenericMultiValueLabelEditPanel;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueChoosePanel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * Created by honchar.
 */
public class ConstructionAssociationPanel<C extends Containerable, IW extends ItemWrapper> extends BasePanel<ContainerWrapper<ConstructionType>> {
    private static final long serialVersionUID = 1L;

    private static final String ID_ASSOCIATIONS = "associations";
    private static final String ID_ASSOCIATION_NAME = "associationName";
    private static final String ID_ASSOCIATION_REFERENCE_PANEL = "associationReferencePanel";

    private static final Trace LOGGER = TraceManager.getTrace(ConstructionAssociationPanel.class);
    private static final String DOT_CLASS = ConstructionAssociationPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";

    private static final String ID_LABEL_SIZE = "col-md-4";
    private static final String ID_INPUT_SIZE = "col-md-6";

    private LoadableDetachableModel<PrismObject<ResourceType>> resourceModel;
    private ContainerWrapper<ResourceObjectAssociationType> associationWrapper;
    private LoadableDetachableModel<List<RefinedAssociationDefinition>> refinedAssociationDefinitionsModel;


    public ConstructionAssociationPanel(String id, IModel<ContainerWrapper<ConstructionType>> constructionWrapperModel){
        super(id, constructionWrapperModel);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initModels();
        initLayout();
    }

    private void initModels(){
        refinedAssociationDefinitionsModel = new LoadableDetachableModel<List<RefinedAssociationDefinition>>() {
            @Override
            protected List<RefinedAssociationDefinition> load() {
                List<RefinedAssociationDefinition> associationDefinitions = new ArrayList<>();
                ConstructionType construction = getModelObject().getItem().getValue().asContainerable();
                ShadowKindType kind = construction.getKind();
                String intent = construction.getIntent();
                try {
                    ObjectReferenceType resourceRef = construction.getResourceRef();
                    Task loadResourceTask = getPageBase().createSimpleTask(OPERATION_LOAD_RESOURCE);
                    OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCE);
                    PrismObject<ResourceType> resource = WebModelServiceUtils.loadObject(resourceRef, getPageBase(), loadResourceTask, result);
                    result.computeStatusIfUnknown();
                    if (!result.isAcceptable()) {
                        LOGGER.error("Cannot find resource referenced from construction. {}", construction, result.getMessage());
                        result.recordPartialError("Could not find resource referenced from construction.");
                        return associationDefinitions;
                    }
                    RefinedResourceSchema refinedResourceSchema = RefinedResourceSchema.getRefinedSchema(resource);
                    RefinedObjectClassDefinition oc = refinedResourceSchema.getRefinedDefinition(kind, intent);
                    if (oc == null) {
                        LOGGER.debug("Association for {}/{} not supported by resource {}", kind, intent, resource);
                        return associationDefinitions;
                    }
                    associationDefinitions.addAll(oc.getAssociationDefinitions());

                    if (CollectionUtils.isEmpty(associationDefinitions)) {
                        LOGGER.debug("Association for {}/{} not supported by resource {}", kind, intent, resource);
                        return associationDefinitions;
                    }
                } catch (Exception ex){
                    LOGGER.error("Association for {}/{} not supported by resource {}", kind, intent, construction.getResourceRef(), ex.getLocalizedMessage());
                }
                return associationDefinitions;
            }
        };
    }

    private void initLayout(){
           ListView<RefinedAssociationDefinition> associationsPanel =
                new ListView<RefinedAssociationDefinition>(ID_ASSOCIATIONS, refinedAssociationDefinitionsModel){
                        @Override
                        protected void populateItem(ListItem<RefinedAssociationDefinition> item) {
                            getShadowReferenceValues(item.getModelObject());

                            GenericMultiValueLabelEditPanel associationReferencesPanel = new GenericMultiValueLabelEditPanel(ID_ASSOCIATION_REFERENCE_PANEL,
                                    Model.ofList(new ArrayList<>()), getAssociationDisplayNameModel(item.getModel()), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
                            associationReferencesPanel.setOutputMarkupId(true);
                            item.add(associationReferencesPanel);
                    }
                };

        associationsPanel.setOutputMarkupId(true);
        add(associationsPanel);
    }

    private IModel<String> getAssociationDisplayNameModel(IModel<RefinedAssociationDefinition> assocDef){
        StringBuilder sb = new StringBuilder();
        if (assocDef.getObject().getDisplayName() != null){
            sb.append(assocDef.getObject().getDisplayName()).append(", ");
        }
        if (assocDef.getObject().getResourceObjectAssociationType() != null && assocDef.getObject().getResourceObjectAssociationType().getRef() != null){
            sb.append("ref: ").append(assocDef.getObject().getResourceObjectAssociationType().getRef().getItemPath().toString());
        }
        return Model.of(sb.toString());
    }

    private List<ObjectReferenceType> getShadowReferenceValues(RefinedAssociationDefinition def){
        QName defName = def.getName();
        List<ObjectReferenceType> shadowsList = new ArrayList<>();
        ContainerWrapper associationWrapper = getModelObject().findContainerWrapper(getModelObject().getPath().append(ConstructionType.F_ASSOCIATION));
        associationWrapper.getValues().forEach(associationValueWrapper -> {
            PrismContainerValue contValue = ((ContainerValueWrapper)associationValueWrapper).getContainerValue();
            ResourceObjectAssociationType assoc = (ResourceObjectAssociationType) contValue.asContainerable();
            if (assoc == null || assoc.getOutbound() == null || assoc.getOutbound().getExpression() == null
                    || ExpressionUtil.getShadowRefValue(assoc.getOutbound().getExpression()) == null){
                return;
            }
            QName assocRef = ItemPathUtil.getOnlySegmentQName(assoc.getRef());
            if (defName != null && defName.equals(assocRef)){
                shadowsList.add(ExpressionUtil.getShadowRefValue(assoc.getOutbound().getExpression()));
            }
        });
            return new ArrayList<>();

    }
}
