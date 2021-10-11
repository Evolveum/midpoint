/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.factory;

import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteQNamePanel;
import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.prism.ConstructionValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;


@Component
public class ResourceAttributeRefPanelFactory extends AbstractGuiComponentFactory<ItemPathType> implements Serializable {

    private static final transient Trace LOGGER = TraceManager.getTrace(ResourceAttributeRefPanelFactory.class);

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<ItemPathType> panelCtx) {

        AutoCompleteQNamePanel<ItemName> autoCompleteTextPanel = new AutoCompleteQNamePanel(panelCtx.getComponentId(), new AttributeRefModel(panelCtx.getRealValueModel())) {

            @Override
            public Collection<ItemName> loadChoices() {
                return getChoicesList(panelCtx);
            }

        };

        autoCompleteTextPanel.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());

        return autoCompleteTextPanel;
    }

    @Override
    public <IW extends ItemWrapper> boolean match(IW wrapper) {
        ItemPath wrapperPath = wrapper.getPath().removeIds();
        return isAssignmentAttributeOrAssociation(wrapperPath) || isInducementAttributeOrAssociation(wrapperPath);
    }

    private boolean isAssignmentAttributeOrAssociation(ItemPath wrapperPath) {
        ItemPath assignmentAttributePath = ItemPath.create(AssignmentHolderType.F_ASSIGNMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_ATTRIBUTE, ResourceAttributeDefinitionType.F_REF);
        ItemPath assignmetnAssociationPath = ItemPath.create(AssignmentHolderType.F_ASSIGNMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_ASSOCIATION, ResourceAttributeDefinitionType.F_REF);

        return assignmentAttributePath.equivalent(wrapperPath) || assignmetnAssociationPath.equivalent(wrapperPath);
    }

    private boolean isInducementAttributeOrAssociation(ItemPath wrapperPath) {
        ItemPath inducementAttributePath = ItemPath.create(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_ATTRIBUTE, ResourceAttributeDefinitionType.F_REF);
        ItemPath inducementAssociationPath = ItemPath.create(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_ASSOCIATION, ResourceAttributeDefinitionType.F_REF);

        return inducementAttributePath.equivalent(wrapperPath) || inducementAssociationPath.equivalent(wrapperPath);
    }

    private List<ItemName> getChoicesList(PrismPropertyPanelContext<ItemPathType> ctx) {


        PrismPropertyWrapper wrapper = ctx.unwrapWrapperModel();
        //attribute/ref
        if (wrapper == null) {
            return Collections.emptyList();
        }

        //attribute value
        if (wrapper.getParent() == null) {
            return Collections.emptyList();
        }

        //attribute
        ItemWrapper attributeWrapper = wrapper.getParent().getParent();
        if (attributeWrapper == null) {
            return Collections.emptyList();
        }

        PrismContainerValueWrapper itemWrapper = attributeWrapper.getParent();

        if (itemWrapper == null) {
            return Collections.emptyList();
        }

        if (!(itemWrapper instanceof ConstructionValueWrapper)) {
            return Collections.emptyList();
        }

        ConstructionValueWrapper constructionWrapper = (ConstructionValueWrapper) itemWrapper;


        try {
            RefinedResourceSchema schema = constructionWrapper.getResourceSchema();
            if (schema == null) {
                return new ArrayList<>();
            }
            RefinedObjectClassDefinition rOcd = schema.getRefinedDefinition(constructionWrapper.getKind(), constructionWrapper.getIntent());
            if (rOcd == null) {
                return Collections.emptyList();
            }

            if (ConstructionType.F_ASSOCIATION.equivalent(attributeWrapper.getItemName())) {
                Collection<RefinedAssociationDefinition> associationDefs = rOcd.getAssociationDefinitions();
                return associationDefs.stream().map(association -> association.getName()).collect(Collectors.toList());
            }

            Collection<? extends  ResourceAttributeDefinition> attrDefs = rOcd.getAttributeDefinitions();
            return  attrDefs.stream().map(a -> a.getItemName()).collect(Collectors.toList());

        } catch (SchemaException e) {
            LOGGER.warn("Cannot get resource attribute definitions");
        }

        return Collections.emptyList();

    }

    @Override
    public Integer getOrder() {
        return 9999;
    }

    class AttributeRefModel implements IModel<ItemName> {

        private IModel<ItemPathType> itemPath;

        public AttributeRefModel(IModel<ItemPathType> itemPath) {
            this.itemPath = itemPath;
        }

        @Override
        public ItemName getObject() {
            ItemPathType itemPathType = itemPath.getObject();
            if (itemPathType == null) {
                return null;
            }
            ItemPath path = itemPathType.getItemPath();
           if (path.size() > 1) {
               return new ItemName("failure");
           }

           if (ItemPath.isName(path.first())) {
               return path.firstToName();
           }

           return null;
        }

        @Override
        public void setObject(ItemName object) {
            itemPath.setObject(new ItemPathType(object));
        }
    }

}
