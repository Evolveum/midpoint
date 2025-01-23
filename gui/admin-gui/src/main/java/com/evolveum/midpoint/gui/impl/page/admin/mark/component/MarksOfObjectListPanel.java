/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.mark.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumnPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismReferenceWrapperColumnPanel;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.input.LifecycleStatePanel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperModel;
import com.evolveum.midpoint.web.model.PrismReferenceWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import java.io.Serial;
import java.util.*;

public class MarksOfObjectListPanel<O extends ObjectType> extends MainObjectListPanel<MarkType> {

    private static final Trace LOGGER = TraceManager.getTrace(MarksOfObjectListPanel.class);

    private final IModel<PrismObjectWrapper<O>> objectModel;

    public MarksOfObjectListPanel(String id, IModel<PrismObjectWrapper<O>> objectModel) {
        this(id, objectModel, null);
    }

    public MarksOfObjectListPanel(String id, IModel<PrismObjectWrapper<O>> objectModel, ContainerPanelConfigurationType config) {
        super(id, MarkType.class, config);
        this.objectModel = objectModel;
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_MARKS_OF_OBJECT;
    }

    @Override
    protected ISelectableDataProvider<SelectableBean<MarkType>> createProvider() {
        return new SelectableBeanObjectDataProvider<>(
                getPageBase(), getSearchModel(), null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                if (objectModel == null || objectModel.getObject() == null) {
                    warn(getString("pageContentAccounts.message.noAccountSelected"));
                    return PrismContext.get().queryFor(MarkType.class).none().build();
                }

                Set<String> selectedMarks = new HashSet<>();

                O object = objectModel.getObject().getValue().getRealValue();
                selectedMarks.addAll(object.getEffectiveMarkRef().stream()
                        .filter(ref -> ref != null && StringUtils.isNotEmpty(ref.getOid()))
                        .map(AbstractReferencable::getOid)
                        .toList());

                selectedMarks.addAll(object.getPolicyStatement().stream()
                                .filter(ps -> ps.getMarkRef() != null)
                                .map(ps -> ps.getMarkRef())
                                .filter(ref -> ref.getOid() != null)
                                .map(AbstractReferencable::getOid)
                                .toList());

                if (selectedMarks.isEmpty()) {
                    LOGGER.trace("Selected object does not contain any mark.");
                    return PrismContext.get().queryFor(MarkType.class).none().build();
                }

                return PrismContext.get().queryFor(MarkType.class)
                        .item(MarkType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF)
                        .ref(SystemObjectsType.ARCHETYPE_OBJECT_MARK.value(), SystemObjectsType.ARCHETYPE_SHADOW_POLICY_MARK.value())
                        .and()
                        .id(selectedMarks.toArray(new String[0]))
                        .build();
            }

            @Override
            public SelectableBean<MarkType> createDataObjectWrapper(MarkType obj) {
                SelectableBean<MarkType> bean = super.createDataObjectWrapper(obj);
                bean.setCustomData(new EffectiveMarkDto<>(objectModel.getObject(), obj));
                return bean;
            }

            @Override
            protected List<SelectableBean<MarkType>> createDataObjectWrappers(Class<MarkType> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result) throws CommonException {
                List<SelectableBean<MarkType>> list = super.createDataObjectWrappers(type, query, options, task, result);
                PrismContainerWrapper<PolicyStatementType> container = objectModel.getObject().findContainer(ObjectType.F_POLICY_STATEMENT);
                container.getValues().stream()
                        .filter(value -> addStatementPolicyMarkValue(value))
                        .forEach(value -> {
                            SelectableBeanImpl<MarkType> selectableBean = new SelectableBeanImpl<>();
                            selectableBean.setCustomData(new EffectiveMarkDto<>(value));
                            selectableBean.setDetachCustomData(false);
                            list.add(selectableBean);
                        });
                return list;
            }

            @Override
            protected int internalSize() {
                int count = super.internalSize();

                if (objectModel == null || objectModel.getObject() == null) {
                    return count;
                }

                try {
                    PrismContainerWrapper<PolicyStatementType> container = objectModel.getObject().findContainer(ObjectType.F_POLICY_STATEMENT);
                    List<PrismContainerValueWrapper<PolicyStatementType>> emptyValues = container.getValues().stream()
                            .filter(value -> addStatementPolicyMarkValue(value))
                            .toList();
                    count = count + emptyValues.size();
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't find policy statement container in " + objectModel.getObject());
                }
                return count;
            }
        };
    }

    @Override
    protected IColumn<SelectableBean<MarkType>, String> createIconColumn() {
        return null;
    }

    private boolean addStatementPolicyMarkValue(PrismContainerValueWrapper<PolicyStatementType> value) {
        if (value == null || value.getRealValue() == null) {
            return false;
        }
        if (value.getRealValue().getMarkRef() == null
                || StringUtils.isEmpty(value.getRealValue().getMarkRef().getOid())) {
            return true;
        }

        return ValueStatus.ADDED == value.getStatus();
    }

    @Override
    protected List<IColumn<SelectableBean<MarkType>, String>> createDefaultColumns() {
        List<IColumn<SelectableBean<MarkType>, String>> columns = new ArrayList<>();

        columns.add(new AbstractExportableColumn<>(createStringResource("PolicyStatementType.type")) {
            @Override
            public IModel<?> getDataModel(IModel<SelectableBean<MarkType>> iModel) {
                return null;
            }

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<MarkType>>> item, String componentId,
                    final IModel<SelectableBean<MarkType>> rowModel) {
                if (!getEffectiveMarkDto(rowModel).isAddedByPolicyStatement()) {
                    item.add(new Label(componentId));
                } else {
                    item.add(new PrismPropertyWrapperColumnPanel<>(
                            componentId,
                            PrismPropertyWrapperModel.fromContainerValueWrapper(
                                    getModelPolicyStatement(rowModel),
                                    PolicyStatementType.F_TYPE),
                            AbstractItemWrapperColumn.ColumnType.VALUE) {
                        @Override
                        protected IModel<String> getCustomHeaderModel() {
                            return getDisplayModel();
                        }
                    });
                }
            }
        });

        columns.add(new AbstractExportableColumn<>(createStringResource("ObjectType.lifecycleState")) {
            @Override
            public IModel<?> getDataModel(IModel<SelectableBean<MarkType>> iModel) {
                return null;
            }

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<MarkType>>> item, String componentId,
                    final IModel<SelectableBean<MarkType>> rowModel) {
                if (!getEffectiveMarkDto(rowModel).isAddedByPolicyStatement()) {
                    item.add(new Label(componentId));
                } else {
                    item.add(new LifecycleStatePanel(
                            componentId,
                            PrismPropertyWrapperModel.fromContainerValueWrapper(
                                    getModelPolicyStatement(rowModel),
                                    PolicyStatementType.F_LIFECYCLE_STATE)));
                }
            }
        });

        columns.add(new AbstractExportableColumn<>(createStringResource("MarksOfObjectListPanel.appliedBy")) {
            @Override
            public IModel<?> getDataModel(IModel<SelectableBean<MarkType>> iModel) {
                return null;
            }

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<MarkType>>> item, String componentId,
                    final IModel<SelectableBean<MarkType>> rowModel) {
                EffectiveMarkDto<O> dto = getEffectiveMarkDto(rowModel);
                StringBuilder origin = new StringBuilder();
                if (dto.isAddedByPolicyStatement()) {
                    origin.append(LocalizationUtil.translate("MarksOfObjectListPanel.appliedBy.policyStatement"));
                }
                if (dto.isAddedByMarkingRule()) {
                    if (!origin.isEmpty()) {
                        origin.append("<br>");
                    }
                    if (dto.isTransitional()) {
                        origin.append(LocalizationUtil.translate("MarksOfObjectListPanel.appliedBy.markingRule.transitional"));
                    } else {
                        origin.append(LocalizationUtil.translate("MarksOfObjectListPanel.appliedBy.markingRule.permanent"));
                    }
                } else if (dto.isAddedByPolicyRule()) {
                    if (!origin.isEmpty()) {
                        origin.append("<br>");
                    }
                    if (dto.isTransitional()) {
                        origin.append(LocalizationUtil.translate("MarksOfObjectListPanel.appliedBy.policyRule.transitional"));
                    } else {
                        origin.append(LocalizationUtil.translate("MarksOfObjectListPanel.appliedBy.policyRule.permanent"));
                    }
                }

                Label label = new Label(componentId, origin.toString());
                label.setEscapeModelStrings(false);
                item.add(label);
            }

        });

        return columns;
    }

    @Override
    protected IColumn<SelectableBean<MarkType>, String> createNameColumn(IModel<String> displayModel, GuiObjectColumnType customColumn, ExpressionType expression) {
        return new ObjectNameColumn<>(createStringResource("MarksOfObjectListPanel.mark")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public boolean isClickable(IModel<SelectableBean<MarkType>> rowModel) {
                return false;
            }

            @Override
            protected Component createComponent(String componentId, IModel<String> labelModel, IModel<SelectableBean<MarkType>> rowModel) {
                EffectiveMarkDto<O> dto = getEffectiveMarkDto(rowModel);
                if (dto.isAddedByPolicyStatement()) {
                    return new PrismReferenceWrapperColumnPanel<>(
                            componentId,
                            PrismReferenceWrapperModel.fromContainerValueWrapper(
                                    getModelPolicyStatement(rowModel),
                                    PolicyStatementType.F_MARK_REF),
                            AbstractItemWrapperColumn.ColumnType.VALUE) {
                        @Override
                        protected ItemPanelSettings createPanelSettings() {
                            ItemPanelSettings settings = super.createPanelSettings();
                            settings.setMandatoryHandler(itemWrapper -> {
                                if (itemWrapper.getItemName().equals(PolicyStatementType.F_MARK_REF)) {
                                    return true;
                                }
                                return itemWrapper.isMandatory();
                            });
                            return settings;
                        }
                    };
                } else {
                    return super.createComponent(componentId, labelModel, rowModel);
                }
            }
        };
    }

    private IModel<PrismContainerValueWrapper<PolicyStatementType>> getModelPolicyStatement(IModel<SelectableBean<MarkType>> rowModel) {
        return new LoadableDetachableModel<>() {
            @Override
            protected PrismContainerValueWrapper<PolicyStatementType> load() {
                return getEffectiveMarkDto(rowModel).getPolicyStatement();
            }
        };
    }

    private EffectiveMarkDto<O> getEffectiveMarkDto(IModel<SelectableBean<MarkType>> rowModel) {
        return getEffectiveMarkDto(rowModel.getObject());
    }

    private EffectiveMarkDto<O> getEffectiveMarkDto(SelectableBean<MarkType> value) {
        EffectiveMarkDto<O> dto = ((EffectiveMarkDto<O>) value.getCustomData());
        if (dto == null || value.getValue() != null) {
            dto = new EffectiveMarkDto<>(objectModel.getObject(), value.getValue());
            value.setCustomData(dto);
        }
        return dto;
    }

    @Override
    protected boolean isDuplicationSupported() {
        return false;
    }

    @Override
    protected void customProcessNewRowItem(Item<SelectableBean<MarkType>> item, IModel<SelectableBean<MarkType>> model) {
        item.add(AttributeModifier.append("class", () -> {
            EffectiveMarkDto<O> dto = getEffectiveMarkDto(model);
            PrismValueWrapper value;
            if (dto.isAddedByPolicyStatement()) {
                value = dto.getPolicyStatement();
            } else {
                if (!dto.isTransitional()) {
                    return "table-secondary";
                }
                value = dto.getEffectiveMark();
            }

            if (value != null) {
                if (value.getStatus() == ValueStatus.ADDED) {
                    return "table-success";
                }
                if (value.getStatus() == ValueStatus.DELETED) {
                    return "table-danger";
                }
            }
            return null;
        }));
    }

    @Override
    protected boolean isObjectDetailsEnabled(IModel<SelectableBean<MarkType>> rowModel) {
        return false;
    }

    @Override
    protected IColumn<SelectableBean<MarkType>, String> createCheckboxColumn() {
        return new CheckBoxHeaderColumn<>() {
            @Override
            protected void processBehaviourOfCheckBox(IsolatedCheckBoxPanel check, IModel<SelectableBean<MarkType>> rowModel) {
                check.add(new EnableBehaviour(() -> {
                    EffectiveMarkDto<O> dto = getEffectiveMarkDto(rowModel);
                    return dto.isAddedByPolicyStatement() || dto.isTransitional();
                }));
            }
        };
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(createDeleteInlineMenu());
        return items;
    }

    public ButtonInlineMenuItem createDeleteInlineMenu() {
        ButtonInlineMenuItem menuItem = new ButtonInlineMenuItem(createStringResource("pageAdminFocus.button.delete")) {
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_DELETE_MENU_ITEM);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return createDeleteColumnAction();
            }
        };
        menuItem.setVisibilityChecker((rowModel, isHeader) -> {
            if (isHeader) {
                return true;
            }

            if (rowModel == null || rowModel.getObject() == null) {
                return true;
            }
            EffectiveMarkDto<O> dto = getEffectiveMarkDto((IModel<SelectableBean<MarkType>>) rowModel);
            return dto.isAddedByPolicyStatement() || dto.isTransitional();
        });
        return menuItem;
    }

    public ColumnMenuAction<SelectableBean<MarkType>> createDeleteColumnAction() {
        return new ColumnMenuAction<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (getRowModel() == null) {
                    deleteItemPerformed(target, getSelectedObjects());
                } else {
                    List<SelectableBean<MarkType>> toDelete = new ArrayList<>();
                    toDelete.add(getRowModel().getObject());
                    deleteItemPerformed(target, toDelete);
                }
            }
        };
    }

    public void deleteItemPerformed(AjaxRequestTarget target, List<SelectableBean<MarkType>> toDelete) {
        if (toDelete == null || toDelete.isEmpty()) {
            warn(createStringResource("MultivalueContainerListPanel.message.noItemsSelected").getString());
            target.add(getPageBase().getFeedbackPanel());
            return;
        }
        toDelete.forEach(value -> {
            EffectiveMarkDto<O> dto = getEffectiveMarkDto(value);
            PrismContainerValueWrapper<PolicyStatementType> policyStatement = dto.getPolicyStatement();

            if (policyStatement != null) {
                if (policyStatement.getStatus() == ValueStatus.ADDED) {
                    PrismContainerWrapper<PolicyStatementType> wrapper = policyStatement.getParent();
                    if (wrapper != null) {
                        try {
                            wrapper.remove(policyStatement, getPageBase());
                        } catch (SchemaException e) {
                            warn(createStringResource("MarksOfObjectListPanel.message.couldntRemove").getString());
                            target.add(getPageBase().getFeedbackPanel());
                            return;
                        }
                    }
                } else {
                    policyStatement.setStatus(ValueStatus.DELETED);
                }
                value.setSelected(false);
                return;
            }

            if (!dto.isTransitional()) {
                warn(createStringResource("MarksOfObjectListPanel.message.couldntRemove").getString());
                target.add(getPageBase().getFeedbackPanel());
                return;
            }

            PrismReferenceValueWrapperImpl<ObjectReferenceType> effectiveMarkWrapper = dto.getEffectiveMark();
            if (effectiveMarkWrapper.getStatus() == ValueStatus.ADDED) {
                PrismReferenceWrapper<ObjectReferenceType> wrapper = effectiveMarkWrapper.getParent();
                if (wrapper != null) {
                    wrapper.getValues().remove(value);
                }
            } else {
                effectiveMarkWrapper.setStatus(ValueStatus.DELETED);
            }
            value.setSelected(false);
        });
        refreshTable(target);
    }

    @Override
    protected boolean isNewObjectButtonEnabled() {
        return objectModel != null && objectModel.getObject() != null;
    }

    @Override
    protected void newObjectPerformed(AjaxRequestTarget target, AssignmentObjectRelation relation, CompiledObjectCollectionView collectionView) {
        try {
            PrismContainerWrapper<PolicyStatementType> container = objectModel.getObject().findContainer(ObjectType.F_POLICY_STATEMENT);
            PrismContainerValue<PolicyStatementType> newValue = container.getItem().getDefinition().instantiate().createNewValue();
            PrismContainerValueWrapper<PolicyStatementType> newItemWrapper = WebPrismUtil.createNewValueWrapper(container, newValue, getPageBase());
            container.getValues().add(newItemWrapper);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't delete policy statement container in " + objectModel.getObject());
        }
        refreshTable(target);
    }

    @Override
    protected boolean showNewObjectCreationPopup() {
        return false;
    }
}
