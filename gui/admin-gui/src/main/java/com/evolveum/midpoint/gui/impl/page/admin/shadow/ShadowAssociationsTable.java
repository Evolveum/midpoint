/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.shadow;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismContainerWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismContainerWrapperColumnPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.dialog.OnePanelPopupPanel;
import com.evolveum.midpoint.gui.impl.component.table.ListItemWithPanelForItemPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ShadowAssociationValueWrapper;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ShadowAssociationsTable extends ListItemWithPanelForItemPanel<ShadowAssociationValueWrapper> {

    private final IModel<PrismContainerValueWrapper<ShadowAssociationsType>> parentModel;
    private final IModel<ResourceType> resourceModel;

    public ShadowAssociationsTable(
            String id,
            @NotNull IModel<PrismContainerValueWrapper<ShadowAssociationsType>> parentModel, IModel<ResourceType> resourceModel) {
        super(id);
        this.parentModel = parentModel;
        this.resourceModel = resourceModel;
    }

    @Override
    protected ShadowAssociationValueWrapper getSelectedItem() {
        if (parentModel.getObject() != null) {
            for (PrismContainerWrapper<? extends Containerable> container : parentModel.getObject().getContainers()) {
                if (container instanceof ShadowAssociationValueWrapper associationWrapper
                        && associationWrapper.isSelected()) {
                    return associationWrapper;
                }
            }
        }
        return null;
    }

    @Override
    protected List<ShadowAssociationValueWrapper> createListOfItem(IModel<String> searchItemModel) {
        List<ShadowAssociationValueWrapper> shadowAssociationValueWrappers = new ArrayList<>();

        if (parentModel.getObject() != null) {
            for (PrismContainerWrapper<? extends Containerable> container : parentModel.getObject().getContainers()) {
                if (container instanceof ShadowAssociationValueWrapper associationWrapper) {
                    if (StringUtils.isNotBlank(searchItemModel.getObject())
                            && !(associationWrapper.getItemName().getLocalPart().contains(searchItemModel.getObject()))) {
                        continue;
                    }
                    shadowAssociationValueWrappers.add(associationWrapper);

                }
            }
        }
        return shadowAssociationValueWrappers;
    }

    @Override
    protected WebMarkupContainer createPanelForItem(
            String idPanelForItem, IModel<ShadowAssociationValueWrapper> selectedItemModel) {
        MultivalueContainerListPanel<ShadowAssociationValueType> table =
                new MultivalueContainerListPanel<>(idPanelForItem, ShadowAssociationValueType.class) {
                    @Override
                    protected UserProfileStorage.TableId getTableId() {
                        return null;
                    }

                    @Override
                    protected void editItemPerformed(
                            AjaxRequestTarget target,
                            IModel<PrismContainerValueWrapper<ShadowAssociationValueType>> rowModel,
                            List<PrismContainerValueWrapper<ShadowAssociationValueType>> listItems) {
                        if (isValidFormComponentsOfRow(rowModel, target)) {
                            editAssociation(target, rowModel, listItems, getTableComponent());
                        }
                    }

                    @Override
                    protected void newItemPerformed(PrismContainerValue<ShadowAssociationValueType> value, AjaxRequestTarget target, AssignmentObjectRelation relationSpec, boolean isDuplicate) {
                        PrismContainerWrapper<ShadowAssociationValueType> container = getContainerModel().getObject();
                        PrismContainerValue<ShadowAssociationValueType> newValue = value;
                        if (newValue == null) {
                            newValue = container.getItem().createNewValue();
                        }
                        PrismContainerValueWrapper<ShadowAssociationValueType> newObjectPolicyWrapper = createNewItemContainerValueWrapper(newValue, container, target);
                        editAssociation(target, null, Arrays.asList(newObjectPolicyWrapper), getTableComponent());
                    }

                    @Override
                    protected boolean isCreateNewObjectVisible() {
                        return getContainerModel().getObject() != null;
                    }

                    @Override
                    protected IModel<PrismContainerWrapper<ShadowAssociationValueType>> getContainerModel() {
                        return selectedItemModel::getObject;
                    }

                    @Override
                    protected boolean showTableAsCard() {
                        return false;
                    }

                    @Override
                    protected boolean isHeaderVisible() {
                        return false;
                    }

                    @Override
                    protected IColumn<PrismContainerValueWrapper<ShadowAssociationValueType>, String> createCheckboxColumn() {
                        return new CheckBoxHeaderColumn<>();
                    }

                    @Override
                    protected List<IColumn<PrismContainerValueWrapper<ShadowAssociationValueType>, String>> createDefaultColumns() {
                        ArrayList<IColumn<PrismContainerValueWrapper<ShadowAssociationValueType>, String>> columns = new ArrayList<>();

                        if (selectedItemModel.getObject() == null) {
                            return columns;
                        }

                        columns.add(new ShadowAssociationObjectsColumn(getContainerModel(), getPageBase()));

                        ResourceType resource = resourceModel.getObject();
                        if (resource != null && resource.getSchemaHandling() != null) {
                            resource.getSchemaHandling().getAssociationType()
                                    .forEach(associationType -> {
                                        if (associationType.getSubject() != null
                                                && associationType.getSubject().getAssociation() != null
                                                && associationType.getSubject().getAssociation().getRef() != null) {
                                            @NotNull ItemPath currentPath = ItemPath.create(getContainerModel().getObject().getItemName());
                                            if (currentPath.equivalent(associationType.getSubject().getAssociation().getRef().getItemPath())) {

                                                Map<ItemPathType, AbstractAttributeMappingsDefinitionType> attributes = new HashMap<>();
                                                associationType.getSubject().getAssociation().getInbound().forEach(inbound -> {
                                                    if (inbound.getExpression() == null) {
                                                        return;
                                                    }

                                                    try {
                                                        AssociationSynchronizationExpressionEvaluatorType evaluator =
                                                                ExpressionUtil.getAssociationSynchronizationExpressionValue(inbound.getExpression());
                                                        if (evaluator == null) {
                                                           return;
                                                        }
                                                        evaluator.getAttribute().forEach(attribute ->
                                                            attributes.put(attribute.getRef(), attribute)
                                                        );
                                                    } catch (SchemaException e) {
                                                        // ignore it
                                                    }
                                                });

                                                associationType.getSubject().getAssociation().getOutbound().forEach(outbound -> {
                                                    if (outbound.getExpression() == null) {
                                                        return;
                                                    }

                                                    try {
                                                        AssociationConstructionExpressionEvaluatorType evaluator =
                                                                ExpressionUtil.getAssociationConstructionExpressionValue(outbound.getExpression());
                                                        if (evaluator == null) {
                                                            return;
                                                        }
                                                        evaluator.getAttribute().forEach(attribute -> {
                                                            if (!attributes.containsKey(attribute.getRef())) {
                                                                attributes.put(attribute.getRef(), attribute);
                                                            }
                                                        });
                                                    } catch (SchemaException e) {
                                                        // ignore it
                                                    }
                                                });

                                                attributes.values().forEach(attribute -> {
                                                    if (attribute.getRef() != null) {
                                                        columns.add(
                                                                new PrismPropertyWrapperColumn<>(
                                                                        getContainerModel(),
                                                                        ShadowAssociationValueType.F_ATTRIBUTES
                                                                                .append(attribute.getRef().getItemPath().firstName()),
                                                                        AbstractItemWrapperColumn.ColumnType.STRING,
                                                                        getPageBase()));
                                                    }
                                                });
                                            }
                                        }
                                    });
                        }

                        return columns;
                    }

                    @Override
                    protected List<InlineMenuItem> createInlineMenu() {
                        return getDefaultMenuActions();
                    }

                    @Override
                    protected boolean isDuplicationSupported() {
                        return false;
                    }

                    @Override
                    protected boolean allowEditMultipleValuesAtOnce() {
                        return false;
                    }
                };
        table.add(AttributeAppender.append("class", "card"));
        table.add(AttributeAppender.append(
                "style", "border-top-left-radius: 0 !important; border-bottom-left-radius: 0 !important;"));
        return table;
    }

    private void editAssociation(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ShadowAssociationValueType>> rowModel, List<PrismContainerValueWrapper<ShadowAssociationValueType>> listItems, Component tableComponent) {
        if ((listItems != null && !listItems.isEmpty()) || rowModel != null) {
            IModel<PrismContainerValueWrapper<ShadowAssociationValueType>> valueModel;
            if (rowModel == null) {
                valueModel = () -> listItems.iterator().next();
            } else {
                valueModel = rowModel;
            }
            if (valueModel != null) {
                OnePanelPopupPanel popup = new OnePanelPopupPanel(
                        getPageBase().getMainPopupBodyId(),
                        createStringResource("ShadowAssociationsTable.modifyProperty")) {
                    @Override
                    protected WebMarkupContainer createPanel(String id) {
                        return new ShadowAssociationValuePanel(id, valueModel);
                    }

                    @Override
                    protected void processHide(AjaxRequestTarget target) {
                        target.add(tableComponent);
                        super.processHide(target);
                    }
                };
                popup.setOutputMarkupId(true);
                getPageBase().showMainPopup(popup, target);
            }
        } else {
            warn(createStringResource("MultivalueContainerListPanel.message.noItemsSelected").getString());
            target.add(getPageBase().getFeedbackPanel());
        }
    }

    @Override
    protected boolean isNewItemButtonVisible() {
        return false;
    }

    @Override
    protected IModel<String> createItemIcon(IModel<ShadowAssociationValueWrapper> model) {
        return Model.of();
    }

    @Override
    protected LoadableDetachableModel<String> createItemLabel(IModel<ShadowAssociationValueWrapper> model) {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                return model.getObject().getItemName().getLocalPart();
            }
        };
    }
}
