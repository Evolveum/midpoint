/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.data.column;

import static com.evolveum.midpoint.gui.impl.util.DetailsPageUtil.dispatchToObjectDetailsPage;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.gui.impl.util.RelationUtil;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.data.column.CompositedIconColumn;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.org.PageOrg;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.cases.ApprovalUtils;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.schema.util.task.work.ResourceObjectSetUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.assignment.AssignmentsUtil;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.component.util.SelectableRow;
import com.evolveum.midpoint.web.page.admin.server.dto.ApprovalOutcomeIcon;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class ColumnUtils {
    private static final Trace LOGGER = TraceManager.getTrace(ColumnUtils.class);

    public static <T> List<IColumn<T, String>> createColumns(List<ColumnTypeDto<String>> columns) {
        List<IColumn<T, String>> tableColumns = new ArrayList<>();
        for (ColumnTypeDto<String> column : columns) {
            tableColumns.add(createPropertyColumn(column));
        }
        return tableColumns;
    }

    public static <T> PropertyColumn<T, String> createPropertyColumn(ColumnTypeDto<String> column) {
        if (column.isSortable()) {
            return createPropertyColumn(column.getColumnName(), column.getSortableColumn(),
                    column.getColumnValue(), column.isMultivalue(), column.isTranslated());
        }
        if (column.isTranslated()) {
            return new PolyStringPropertyColumn<>(createStringResource(column.getColumnName()),
                    column.getColumnValue());
        } else {
            return new PropertyColumn<>(createStringResource(column.getColumnName()),
                    column.getColumnValue());
        }
    }

    private static <T> PropertyColumn<T, String> createPropertyColumn(String name, String sortableProperty,
            final String expression, final boolean multivalue, boolean translated) {
        if (!multivalue && translated) {
            return new PolyStringPropertyColumn<>(createStringResource(name), sortableProperty,
                    expression);
        }
        return new PropertyColumn<>(createStringResource(name), sortableProperty, expression) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item item, String componentId, IModel rowModel) {
                if (multivalue) {
                    IModel<List<?>> values = new PropertyModel<>(rowModel, expression);
                    RepeatingView repeater = new RepeatingView(componentId);
                    for (final Object task : values.getObject()) {
                        repeater.add(new Label(repeater.newChildId(), task.toString()));
                    }
                    item.add(repeater);
                    return;
                }

                super.populateItem(item, componentId, rowModel);
            }
        };
    }

    public static <C extends ObjectType> List<IColumn<SelectableBean<C>, String>> getDefaultColumns(Class<? extends C> type, PageBase pageBase) {
        if (type == null) {
            return getDefaultUserColumns();
        }

        if (type.equals(UserType.class)) {
            return getDefaultUserColumns();
        } else if (RoleType.class.equals(type)) {
            return getDefaultRoleColumns();
        } else if (OrgType.class.equals(type)) {
            return getDefaultOrgColumns(pageBase);
        } else if (ServiceType.class.equals(type)) {
            return getDefaultServiceColumns();
        } else if (ArchetypeType.class.equals(type)) {
            return getDefaultArchetypeColumns();
        } else if (type.equals(TaskType.class)) {
            return getDefaultTaskColumns();
        } else if (type.equals(ResourceType.class)) {
            return getDefaultResourceColumns();
//        } else if (type.equals(AssignmentType.class)) {
//            return getDefaultAssignmentsColumns(pageBase);
        } else {
            return new ArrayList<>();
        }
    }

    public static <O extends ObjectType> IColumn<SelectableBean<O>, String> createIconColumn(PageBase pageBase) {

        return new CompositedIconColumn<>(createIconColumnHeaderModel()) {

            @Override
            protected CompositedIcon getCompositedIcon(IModel<SelectableBean<O>> rowModel) {
                if (rowModel == null || rowModel.getObject() == null || rowModel.getObject().getValue() == null) {
                    return new CompositedIconBuilder().build();
                }
                return WebComponentUtil.createCompositeIconForObject(rowModel.getObject().getValue(),
                        rowModel.getObject().getResult(), pageBase);
            }
        };
    }

    public static <T extends ObjectType> String getIconColumnValue(IModel<SelectableBean<T>> rowModel) {
        if (rowModel == null || rowModel.getObject() == null || rowModel.getObject().getValue() == null) {
            return "";
        }

        return getIconColumnValue(rowModel.getObject().getValue(), rowModel.getObject().getResult());

    }

    public static <T extends ObjectType> String getIconColumnValue(T object, OperationResult result) {
        Class<?> type = object.getClass();
        if (type.equals(ObjectType.class)) {
            return IconAndStylesUtil.createDefaultIcon(object.asPrismObject());
        } else if (type.equals(UserType.class)) {
            return IconAndStylesUtil.createUserIcon(object.asPrismContainer());
        } else if (RoleType.class.equals(type)) {
            return IconAndStylesUtil.createRoleIcon(object.asPrismContainer());
        } else if (OrgType.class.equals(type)) {
            return IconAndStylesUtil.createOrgIcon();
        } else if (ServiceType.class.equals(type)) {
            return IconAndStylesUtil.createServiceIcon();
        } else if (ShadowType.class.equals(type)) {
            return IconAndStylesUtil.createShadowIcon(object.asPrismContainer());
        } else if (type.equals(TaskType.class)) {
            return IconAndStylesUtil.createTaskIcon();
        } else if (type.equals(ResourceType.class)) {
            return IconAndStylesUtil.createResourceIcon(object.asPrismContainer());
        } else if (type.equals(AccessCertificationDefinitionType.class)) {
            return GuiStyleConstants.CLASS_OBJECT_CERT_DEF_ICON + " " + GuiStyleConstants.CLASS_ICON_STYLE_NORMAL;
        } else if (type.equals(CaseType.class)) {
            return GuiStyleConstants.EVO_CASE_OBJECT_ICON;
        } else if (type.equals(CaseWorkItemType.class)) {
            return GuiStyleConstants.CLASS_OBJECT_WORK_ITEM_ICON;
        }

        return "";
    }

    private static <T extends ObjectType> IModel<String> getIconColumnDataModel(IModel<SelectableBean<T>> rowModel) {
        Class<T> type = (Class<T>) rowModel.getObject().getValue().getClass();
        if (ShadowType.class.equals(type)) {
            T shadow = rowModel.getObject().getValue();
            if (shadow == null) {
                return null;
            }
            return ShadowUtil.isProtected(shadow.asPrismContainer()) ?
                    createStringResource("ThreeStateBooleanPanel.true") : createStringResource("ThreeStateBooleanPanel.false");

        }
        return Model.of();
    }

    public static <T extends ObjectType> String getIconColumnTitle(IModel<SelectableBean<T>> rowModel) {
        if (rowModel == null || rowModel.getObject() == null) {
            return null;
        }
        return getIconColumnTitle(rowModel.getObject().getValue(), rowModel.getObject().getResult());
    }

    public static <T extends ObjectType> String getIconColumnTitle(T object, OperationResult result) {
        if (object == null) {
            return null;
        }
        if (result != null && result.isFatalError()) {
            return result.getUserFriendlyMessage() != null ?
                    result.getUserFriendlyMessage().getFallbackMessage() : result.getMessage();
        }
        Class<T> type = (Class<T>) object.getClass();
        if (object == null && !ShadowType.class.equals(type)) {
            return null;
        } else if (type.equals(UserType.class)) {
            String iconClass = object != null ? IconAndStylesUtil.createUserIcon(object.asPrismContainer()) : null;
            String compareStringValue = GuiStyleConstants.CLASS_OBJECT_USER_ICON + " " + GuiStyleConstants.CLASS_ICON_STYLE;
            String compareStringValueNormal = GuiStyleConstants.CLASS_OBJECT_USER_ICON + " " + GuiStyleConstants.CLASS_ICON_STYLE_NORMAL;
            String titleValue = "";
            if (iconClass.equals(compareStringValueNormal)) {
                return "";
            }
            if (iconClass != null &&
                    iconClass.startsWith(compareStringValue) &&
                    iconClass.length() > compareStringValue.length()) {
                titleValue = iconClass.substring(compareStringValue.length());
            }
            return createStringResource("ColumnUtils.getUserIconColumn.createTitleModel." + titleValue) == null ?
                    "" : createStringResource("ColumnUtils.getUserIconColumn.createTitleModel." + titleValue).getString();
        } else {
            return object.asPrismContainer().getDefinition().getTypeName().getLocalPart();
        }
    }

    private static IModel<String> createIconColumnHeaderModel() {
        return new Model<>() {
            @Override
            public String getObject() {
                return "";
            }
        };
    }

    public static StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return new StringResourceModel(resourceKey).setModel(new Model<String>()).setDefaultValue(resourceKey)
                .setParameters(objects);
    }

    public static StringResourceModel createStringResource(String resourceKey, String defaultString, Object... objects) {
        return new StringResourceModel(resourceKey).setModel(new Model<String>()).setDefaultValue(defaultString)
                .setParameters(objects);
    }

    public static <T extends ObjectType> List<IColumn<SelectableBean<T>, String>> getDefaultUserColumns() {

        List<ColumnTypeDto<String>> columnsDefs = Arrays.asList(
                new ColumnTypeDto<>("UserType.givenName", UserType.F_GIVEN_NAME.getLocalPart(),
                        SelectableBeanImpl.F_VALUE + ".givenName", false, true),
                new ColumnTypeDto<>("UserType.familyName", UserType.F_FAMILY_NAME.getLocalPart(),
                        SelectableBeanImpl.F_VALUE + ".familyName", false, true),
                new ColumnTypeDto<>("UserType.fullName", UserType.F_FULL_NAME.getLocalPart(),
                        SelectableBeanImpl.F_VALUE + ".fullName", false, true),
                new ColumnTypeDto<>("UserType.emailAddress", UserType.F_EMAIL_ADDRESS.getLocalPart(),
                        SelectableBeanImpl.F_VALUE + ".emailAddress", false)

        );

        return new ArrayList<>(ColumnUtils.createColumns(columnsDefs));
    }

    public static <T extends ObjectType> List<IColumn<SelectableBean<T>, String>> getDefaultTaskColumns() {
        List<IColumn<SelectableBean<T>, String>> columns = new ArrayList<>();

        columns.add(
                new AbstractColumn<>(createStringResource("TaskType.kind")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<T>>> cellItem,
                            String componentId, IModel<SelectableBean<T>> rowModel) {
                        SelectableBean<TaskType> object = (SelectableBean<TaskType>) rowModel.getObject();

                        PrismProperty<ShadowKindType> pKind = null;
                        if (object.getValue() != null) {
                            pKind = findPropertyInResourceSet(object.getValue(), ResourceObjectSetType.F_KIND);
                        }

                        if (pKind != null) {
                            cellItem.add(new Label(componentId, WebComponentUtil
                                    .createLocalizedModelForEnum(pKind.getRealValue(), cellItem)));
                        } else {
                            cellItem.add(new Label(componentId));
                        }

                    }
                });

        columns.add(new AbstractColumn<>(
                createStringResource("TaskType.intent")) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<T>>> cellItem,
                    String componentId, IModel<SelectableBean<T>> rowModel) {
                SelectableBean<TaskType> object = (SelectableBean<TaskType>) rowModel.getObject();

                PrismProperty<String> pIntent = null;
                if (object.getValue() != null) {
                    pIntent = findPropertyInResourceSet(object.getValue(), ResourceObjectSetType.F_INTENT);
                }

                if (pIntent != null) {
                    cellItem.add(new Label(componentId, pIntent.getRealValue()));
                } else {
                    cellItem.add(new Label(componentId));
                }
            }
        });

        columns.add(new AbstractColumn<>(
                createStringResource("TaskType.objectClass")) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<T>>> cellItem,
                    String componentId, IModel<SelectableBean<T>> rowModel) {
                SelectableBean<TaskType> object = (SelectableBean<TaskType>) rowModel.getObject();

                PrismProperty<QName> pObjectClass = null;
                if (object.getValue() != null) {
                    pObjectClass = findPropertyInResourceSet(object.getValue(), ResourceObjectSetType.F_OBJECTCLASS);
                }

                if (pObjectClass != null) {
                    cellItem.add(new Label(componentId, pObjectClass.getRealValue().getLocalPart()));
                } else {
                    cellItem.add(new Label(componentId, ""));
                }

            }
        });

        List<ColumnTypeDto<String>> columnsDefs = Collections.singletonList(
                new ColumnTypeDto<>("TaskType.executionState", TaskType.F_EXECUTION_STATE.getLocalPart(),
                        SelectableBeanImpl.F_VALUE + ".executionState", false));
        columns.addAll(ColumnUtils.createColumns(columnsDefs));

        return columns;

    }

    private static PrismProperty findPropertyInResourceSet(TaskType value, ItemPath pathToProperty) {
        if (!WebComponentUtil.isResourceRelatedTask(value)) {
            return null;
        }
        @Nullable ResourceObjectSetType resourceSet = ResourceObjectSetUtil.fromTask(value);
        if (resourceSet != null) {
            return resourceSet.asPrismContainerValue().findProperty(pathToProperty);
        } else {
            return null;
        }
    }

    public static <T extends ObjectType> List<IColumn<SelectableBean<T>, String>> getDefaultRoleColumns() {
        List<IColumn<SelectableBean<T>, String>> columns = new ArrayList<>();

        columns.addAll((Collection) getDefaultAbstractRoleColumns(true));

        return columns;
    }

    public static <T extends ObjectType> List<IColumn<SelectableBean<T>, String>> getDefaultServiceColumns() {
        List<IColumn<SelectableBean<T>, String>> columns = new ArrayList<>();

        columns.addAll((Collection) getDefaultAbstractRoleColumns(true));

        return columns;
    }

    public static <T extends ObjectType> List<IColumn<SelectableBean<T>, String>> getDefaultOrgColumns(PageBase pageBase) {
        List<IColumn<SelectableBean<T>, String>> columns = new ArrayList<>();

        columns.addAll((Collection) getDefaultAbstractRoleColumns(false));

        columns.add(new LinkColumn<>(createStringResource("ObjectType.parentOrgRef")) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<T>>> cellItem, String componentId, IModel<SelectableBean<T>> rowModel) {
                createParentOrgColumn(cellItem, componentId, rowModel, pageBase);
            }

            @Override
            public void onClick(IModel<SelectableBean<T>> rowModel) {
                super.onClick(rowModel);
            }

            @Override
            public IModel<String> getDataModel(IModel<SelectableBean<T>> rowModel) {
                List<ObjectReferenceType> parentOrgRefs = getParentOrgRefs(rowModel);
                return () ->
                        parentOrgRefs.stream()
                                .map(parentRef -> WebModelServiceUtils.resolveReferenceName(parentRef, pageBase, true))
                                .collect(Collectors.joining(", "));
            }

        });

        columns.add((IColumn) getAbstractRoleColumnForProjection());
        return columns;
    }

    private static <T extends ObjectType> void createParentOrgColumn(Item<ICellPopulator<SelectableBean<T>>> cellItem, String componentId, IModel<SelectableBean<T>> rowModel, PageBase pageBase) {
        RepeatingView links = new RepeatingView(componentId);
        cellItem.add(links);

        List<ObjectReferenceType> parentOrgRefs = getParentOrgRefs(rowModel);
        for (ObjectReferenceType parentRef : parentOrgRefs) {
            LinkPanel parentOrgLinkPanel = createParentOrgLink(links.newChildId(), parentRef, pageBase);
            if (parentOrgLinkPanel == null) {
                continue;
            }
            links.add(parentOrgLinkPanel);
        }
    }

    private static <T extends ObjectType> List<ObjectReferenceType> getParentOrgRefs(IModel<SelectableBean<T>> rowModel) {
        if (rowModel == null) {
            return null;
        }

        SelectableBean<T> bean = rowModel.getObject();
        if (bean == null) {
            return null;
        }
        T object = bean.getValue();
        if (object == null) {
            return null;
        }
        return object.getParentOrgRef();
    }

    private static LinkPanel createParentOrgLink(String id, ObjectReferenceType parentRef, PageBase pageBase) {
        String parentOrgOid = parentRef.getOid();
        if (parentOrgOid == null) {
            return null;
        }
        Model name = Model.of(WebModelServiceUtils.resolveReferenceName(parentRef, pageBase, true));
        if (name.getObject() == null) {
            return null;
        }

        return new LinkPanel(id, name) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, parentOrgOid);
                pageBase.navigateToNext(PageOrg.class, parameters);
            }

            @Override
            public boolean isEnabled() {
                return name.getObject() != null;
            }
        };
    }

    public static <T extends ObjectType> List<IColumn<SelectableBean<T>, String>> getDefaultArchetypeColumns() {
        List<IColumn<SelectableBean<T>, String>> columns = new ArrayList<>();

        columns.addAll((Collection) getDefaultAbstractRoleColumns(false));

        return columns;
    }

    public static <O extends ObjectType> List<IColumn<SelectableBean<O>, String>> getDefaultObjectColumns() {
        List<ColumnTypeDto<String>> columnsDefs = Collections.singletonList(
                new ColumnTypeDto<>("ObjectType.description",
                        null,
                        SelectableBeanImpl.F_VALUE + ".description", false)
        );
        List<IColumn<SelectableBean<O>, String>> columns = createColumns(columnsDefs);
        return columns;
    }

    public static <T extends AbstractRoleType> List<IColumn<SelectableBean<T>, String>> getDefaultAbstractRoleColumns(boolean showAccounts) {
        String sortByDisplayName;
        String sortByIdentifier;
        sortByDisplayName = AbstractRoleType.F_DISPLAY_NAME.getLocalPart();
        sortByIdentifier = AbstractRoleType.F_IDENTIFIER.getLocalPart();
        List<ColumnTypeDto<String>> columnsDefs = Arrays.asList(
                new ColumnTypeDto<>("AbstractRoleType.displayName",
                        sortByDisplayName,
                        SelectableBeanImpl.F_VALUE + ".displayName", false, true),
                new ColumnTypeDto<>("AbstractRoleType.description",
                        null,
                        SelectableBeanImpl.F_VALUE + ".description", false),
                new ColumnTypeDto<>("AbstractRoleType.identifier", sortByIdentifier,
                        SelectableBeanImpl.F_VALUE + ".identifier", false)

        );
        List<IColumn<SelectableBean<T>, String>> columns = createColumns(columnsDefs);

        if (showAccounts) {
            columns.add(getAbstractRoleColumnForProjection());
        }
        return columns;

    }

    private static <T extends AbstractRoleType> IColumn<SelectableBean<T>, String> getAbstractRoleColumnForProjection() {
        IColumn<SelectableBean<T>, String> column = new AbstractExportableColumn<>(
                createStringResource("AbstractRole.projectionsColumn")) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<T>>> cellItem,
                    String componentId, IModel<SelectableBean<T>> model) {
                cellItem.add(new Label(componentId,
                        model.getObject().getValue() != null ?
                                model.getObject().getValue().getLinkRef().size() : null));
            }

            @Override
            public IModel<String> getDataModel(IModel<SelectableBean<T>> rowModel) {
                return Model.of(rowModel.getObject().getValue() != null ?
                        Integer.toString(rowModel.getObject().getValue().getLinkRef().size()) : "");
            }

        };

        return column;
    }

    public static <T extends ObjectType> List<IColumn<SelectableBean<T>, String>> getDefaultResourceColumns() {

        List<ColumnTypeDto<String>> columnsDefs = Collections.singletonList(
                new ColumnTypeDto<>("AbstractRoleType.description", null,
                        SelectableBeanImpl.F_VALUE + ".description", false)

        );

        return new ArrayList<>(ColumnUtils.createColumns(columnsDefs));
    }

    public static List<IColumn<PrismContainerValueWrapper<CaseWorkItemType>, String>> getDefaultWorkItemColumns(PageBase pageBase, boolean isFullView) {
        List<IColumn<PrismContainerValueWrapper<CaseWorkItemType>, String>> columns = new ArrayList<>();
        columns.add(new AbstractExportableColumn<>(
                createStringResource("WorkItemsPanel.stage")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<CaseWorkItemType>>> cellItem,
                    String componentId, IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                cellItem.add(new Label(componentId, ApprovalContextUtil.getWorkItemStageInfo(unwrapRowModel(rowModel))));
            }

            @Override
            public IModel<String> getDataModel(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                return Model.of(ApprovalContextUtil.getStageInfo(unwrapRowModel(rowModel)));
            }

        });
        columns.add(new AbstractExportableColumn<>(
                createStringResource("pageCases.table.state")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<CaseWorkItemType>>> cellItem,
                    String componentId, IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                CaseWorkItemType workItem = unwrapRowModel(rowModel);
                cellItem.add(new Label(componentId, workItem.getCloseTimestamp() != null
                        ? createStringResource("Case.state.closed")
                        : createStringResource("Case.state.open")));
            }

            @Override
            public IModel<String> getDataModel(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                CaseType parentCase = CaseTypeUtil.getCase(unwrapRowModel(rowModel));
                return Model.of(parentCase != null ? parentCase.getState() : "");
            }
        });

        columns.add(new AjaxLinkColumn<>(createStringResource("WorkItemsPanel.object")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                CaseWorkItemType caseWorkItemType = unwrapRowModel(rowModel);
                CaseType caseType = CaseTypeUtil.getCase(caseWorkItemType);
                return Model.of(WebComponentUtil.getReferencedObjectDisplayNameAndName(caseType.getObjectRef(), true, pageBase));
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                CaseWorkItemType caseWorkItemType = unwrapRowModel(rowModel);
                CaseType caseType = CaseTypeUtil.getCase(caseWorkItemType);

                dispatchToObjectDetailsPage(caseType.getObjectRef(), pageBase, false);
            }

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<CaseWorkItemType>>> cellItem, String componentId,
                    final IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                CaseWorkItemType caseWorkItemType = unwrapRowModel(rowModel);
                CaseType caseType = CaseTypeUtil.getCase(caseWorkItemType);
                AssignmentHolderType object = WebComponentUtil.getObjectFromAddDeltaForCase(caseType);
                if (object == null) {
                    super.populateItem(cellItem, componentId, rowModel);
                } else {
                    IModel model = createLinkModel(rowModel);
                    cellItem.add(new Label(componentId, model));
                }

                Component c = cellItem.get(componentId);

                String descriptionValue = "";
                ObjectReferenceType objectRef = caseType.getObjectRef();
                if (objectRef != null) {
                    PrismReferenceValue refVal = objectRef.asReferenceValue();
                    if (refVal.getObject() != null) {
                        descriptionValue = refVal.getObject().asObjectable().getDescription();
                    }
                }

                c.add(new AttributeAppender("title", descriptionValue));
            }

            @Override
            public boolean isEnabled(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                CaseWorkItemType caseWorkItemType = unwrapRowModel(rowModel);
                CaseType caseType = CaseTypeUtil.getCase(caseWorkItemType);
                return CollectionUtils.isNotEmpty(WebComponentUtil.loadReferencedObjectList(
                        Collections.singletonList(caseType.getObjectRef()), "loadCaseWorkItemObjectRef", pageBase));
            }
        });
        columns.add(new AjaxLinkColumn<>(createStringResource("WorkItemsPanel.target")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                CaseWorkItemType caseWorkItemType = unwrapRowModel(rowModel);
                CaseType caseType = CaseTypeUtil.getCase(caseWorkItemType);
                return Model.of(WebComponentUtil.getReferencedObjectDisplayNameAndName(caseType.getTargetRef(), false, pageBase));
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                CaseWorkItemType caseWorkItemType = unwrapRowModel(rowModel);
                CaseType caseType = CaseTypeUtil.getCase(caseWorkItemType);
                dispatchToObjectDetailsPage(caseType.getTargetRef(), pageBase, false);
            }

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<CaseWorkItemType>>> cellItem, String componentId,
                    final IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                super.populateItem(cellItem, componentId, rowModel);
                Component c = cellItem.get(componentId);

                CaseWorkItemType caseWorkItemType = unwrapRowModel(rowModel);
                CaseType caseType = CaseTypeUtil.getCase(caseWorkItemType);
                PrismReferenceValue refVal = caseType.getTargetRef() != null ? caseType.getTargetRef().asReferenceValue() : null;
                String descriptionValue = refVal != null && refVal.getObject() != null ?
                        refVal.getObject().asObjectable().getDescription() : "";

                c.add(new AttributeAppender("title", descriptionValue));
            }

            @Override
            public boolean isEnabled(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                CaseWorkItemType caseWorkItem = unwrapRowModel(rowModel);
                CaseType caseType = CaseTypeUtil.getCase(caseWorkItem);
                if (caseType == null) {
                    return false;
                }

                ObjectReferenceType ref = caseType.getTargetRef();
                return ref != null && ref.getOid() != null;
            }
        });
        if (isFullView) {
            columns.add(new AbstractExportableColumn<>(
                    createStringResource("WorkItemsPanel.actors")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<CaseWorkItemType>>> cellItem,
                        String componentId, IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                    CaseWorkItemType caseWorkItemType = unwrapRowModel(rowModel);
                    CaseType caseType = CaseTypeUtil.getCase(caseWorkItemType);
                    List<ObjectReferenceType> assigneeRefs = getActorsForWorkitem(caseWorkItemType, CaseTypeUtil.isClosed(caseType));
                    cellItem.add(getMultilineLinkPanel(componentId, assigneeRefs, pageBase));
                }

                @Override
                public IModel<String> getDataModel(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                    String assignee = WebComponentUtil.getReferencedObjectNames(unwrapRowModel(rowModel).getAssigneeRef(), false);
                    return Model.of(assignee != null ? assignee : WebComponentUtil.getReferencedObjectNames(unwrapRowModel(rowModel).getCandidateRef(), true));
                }

            });
        }
        columns.add(new AbstractExportableColumn<>(
                createStringResource("WorkItemsPanel.created")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<CaseWorkItemType>>> cellItem,
                    String componentId, IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                cellItem.add(new Label(componentId,
                        WebComponentUtil.getShortDateTimeFormattedValue(unwrapRowModel(rowModel).getCreateTimestamp(), pageBase)));
            }

            @Override
            public IModel<String> getDataModel(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                return Model.of(WebComponentUtil.getShortDateTimeFormattedValue(unwrapRowModel(rowModel).getCreateTimestamp(), pageBase));
            }
        });
        if (isFullView) {
            columns.add(new AbstractColumn<>(createStringResource("WorkItemsPanel.started")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<CaseWorkItemType>>> cellItem, String componentId,
                        final IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                    cellItem.add(new DateLabelComponent(componentId, new IModel<>() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public Date getObject() {
                            CaseWorkItemType workItem = rowModel.getObject().getRealValue();
                            CaseType caseType = CaseTypeUtil.getCase(workItem);
                            return XmlTypeConverter.toDate(CaseTypeUtil.getStartTimestamp(caseType));
                        }
                    }, WebComponentUtil.getShortDateTimeFormat(pageBase)));
                }
            });
            columns.add(new AbstractExportableColumn<>(
                    createStringResource("WorkItemsPanel.deadline")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<CaseWorkItemType>>> cellItem,
                        String componentId, IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                    cellItem.add(new Label(componentId,
                            WebComponentUtil.getShortDateTimeFormattedValue(unwrapRowModel(rowModel).getDeadline(), pageBase)));
                }

                @Override
                public IModel<String> getDataModel(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                    return Model.of(WebComponentUtil.getShortDateTimeFormattedValue(unwrapRowModel(rowModel).getDeadline(),
                            pageBase));
                }
            });
            columns.add(new AbstractExportableColumn<>(
                    createStringResource("WorkItemsPanel.escalationLevel")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<CaseWorkItemType>>> cellItem,
                        String componentId, IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                    cellItem.add(new Label(componentId, ApprovalContextUtil.getEscalationLevelInfo(unwrapRowModel(rowModel))));
                }

                @Override
                public IModel<String> getDataModel(IModel<PrismContainerValueWrapper<CaseWorkItemType>> rowModel) {
                    return Model.of(ApprovalContextUtil.getEscalationLevelInfo(unwrapRowModel(rowModel)));
                }
            });
        }
        return columns;
    }

    public static List<IColumn<SelectableBean<CaseType>, String>> getDefaultCaseColumns(PageBase pageBase, boolean isDashboard) {

        List<IColumn<SelectableBean<CaseType>, String>> columns = new ArrayList<>();

        IColumn column = new PropertyColumn(createStringResource("pageCases.table.description"), "value.description");
        columns.add(column);

        columns.add(new AjaxLinkColumn<>(createStringResource("pageCases.table.objectRef")) {
            private static final long serialVersionUID = 1L;

            @Override
            public IModel<String> getDataModel(IModel<SelectableBean<CaseType>> rowModel) {
                CaseType caseModelObject = rowModel.getObject().getValue();
                return Model.of(WebComponentUtil.getReferencedObjectDisplayNameAndName(caseModelObject.getObjectRef(), true, pageBase));
            }

            @Override
            protected IModel<String> createLinkModel(IModel<SelectableBean<CaseType>> rowModel) {
                CaseType caseType = rowModel.getObject().getValue();
                return Model.of(WebComponentUtil.getReferencedObjectDisplayNameAndName(caseType.getObjectRef(), true, pageBase));
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<CaseType>> rowModel) {
                CaseType caseType = rowModel.getObject().getValue();

                dispatchToObjectDetailsPage(caseType.getObjectRef(), pageBase, false);
            }

            @Override
            public boolean isEnabled(IModel<SelectableBean<CaseType>> rowModel) {
                CaseType caseType = rowModel.getObject().getValue();
                if (caseType.getObjectRef() == null) {
                    return false;
                }

                PrismObject object = caseType.getObjectRef().getObject();
                // Do not generate link if the object has not been created yet.
                // Check the version to see if it has not been created.
                return object != null && object.getVersion() != null;
            }
        });

        if (!isDashboard) {
            columns.add(createCaseActorsColumn(pageBase));
        }

        column = new AbstractColumn<SelectableBean<CaseType>, String>(
                createStringResource("pageCases.table.openTimestamp"),
                "metadata/createTimestamp") {

            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<CaseType>>> cellItem,
                    String componentId, final IModel<SelectableBean<CaseType>> rowModel) {

                cellItem.add(new Label(componentId, (IModel<String>) () -> createCaseOpenTimestampModel(rowModel, pageBase)));
            }

            @Override
            public String getCssClass() {
                return isDashboard ? "mp-w-sm-2 mp-w-lg-1" : super.getCssClass();
            }
        };
        columns.add(column);

        if (!isDashboard) {
            column = new PropertyColumn<SelectableBean<CaseType>, String>(createStringResource("pageCases.table.closeTimestamp"), CaseType.F_CLOSE_TIMESTAMP.getLocalPart(), "value.closeTimestamp") {
                @Override
                public void populateItem(Item<ICellPopulator<SelectableBean<CaseType>>> cellItem,
                        String componentId, final IModel<SelectableBean<CaseType>> rowModel) {

                    cellItem.add(new Label(componentId, (IModel<String>) () -> createCaseClosedTimestampLabel(rowModel, pageBase)));
                }

                @Override
                public String getCssClass() {
                    return isDashboard ? "mp-w-sm-2 mp-w-lg-1" : super.getCssClass();
                }
            };
            columns.add(column);
        }

        column = new CountIconColumn<SelectableBean<CaseType>>(createStringResource("CaseType.outcome")) {

            @Override
            protected Map<DisplayType, Integer> getIconDisplayType(IModel<SelectableBean<CaseType>> rowModel) {
                Map<DisplayType, Integer> map = new HashMap<>();
                CaseType caseType = rowModel.getObject().getValue();
                if (caseType == null) {
                    return null;
                }
                if (ObjectTypeUtil.hasArchetypeRef(caseType, SystemObjectsType.ARCHETYPE_OPERATION_REQUEST.value())) {
                    ObjectQuery queryFilter = pageBase.getPrismContext().queryFor(CaseType.class)
                            .item(CaseType.F_PARENT_REF)
                            .ref(caseType.getOid())
                            .build();
                    List<PrismObject<CaseType>> childs =
                            WebModelServiceUtils.searchObjects(CaseType.class, queryFilter, new OperationResult("search_case_child"), pageBase);

                    for (PrismObject<CaseType> child : childs) {
                        processCaseOutcome(child.asObjectable(), map, false);
                    }
                } else {
                    processCaseOutcome(caseType, map, true);
                }

                return map;
            }

            @Override
            public String getCssClass() {
                return "mp-w-lg-1";
            }
        };
        columns.add(column);

        column = new PropertyColumn<SelectableBean<CaseType>, String>(createStringResource("pageCases.table.state"), CaseType.F_STATE.getLocalPart(), "value.state") {
            @Override
            public String getCssClass() {
                return "mp-w-lg-1";
            }

            @Override
            public IModel<?> getDataModel(IModel<SelectableBean<CaseType>> rowModel) {
                IModel<String> dataModel = (IModel<String>) super.getDataModel(rowModel);
                String state = dataModel.getObject();
                if (StringUtils.isNotBlank(state)) {
                    String key = CaseType.COMPLEX_TYPE.getLocalPart() + "." + CaseType.F_STATE.getLocalPart() + "." + state;
                    return new StringResourceModel(key, pageBase).setModel(new Model<String>()).setDefaultValue(state);
                }
                return dataModel;
            }
        };
        columns.add(column);

        if (!isDashboard) {
            column = new AbstractExportableColumn<SelectableBean<CaseType>, String>(
                    createStringResource("pageCases.table.workitems")) {

                @Override
                public void populateItem(Item<ICellPopulator<SelectableBean<CaseType>>> cellItem,
                        String componentId, IModel<SelectableBean<CaseType>> model) {
                    cellItem.add(new Label(componentId,
                            model.getObject().getValue() != null && model.getObject().getValue().getWorkItem() != null ?
                                    model.getObject().getValue().getWorkItem().size() : null));
                }

                @Override
                public IModel<String> getDataModel(IModel<SelectableBean<CaseType>> rowModel) {
                    return Model.of(rowModel.getObject().getValue() != null && rowModel.getObject().getValue().getWorkItem() != null ?
                            Integer.toString(rowModel.getObject().getValue().getWorkItem().size()) : "");
                }

                @Override
                public String getCssClass() {
                    return "mp-w-lg-1";
                }

            };
            columns.add(column);
        }

        return columns;
    }

    private static String createCaseClosedTimestampLabel(IModel<SelectableBean<CaseType>> rowModel, PageBase pageBase) {
        CaseType object = rowModel.getObject().getValue();
        XMLGregorianCalendar closedCal = object != null ? object.getCloseTimestamp() : null;
        final Date closed;
        if (closedCal != null) {
            closed = closedCal.toGregorianCalendar().getTime();
//            cellItem.add(AttributeModifier.replace("title", WebComponentUtil.getLocalizedDate(closed, DateLabelComponent.LONG_MEDIUM_STYLE)));
//            cellItem.add(new TooltipBehavior());
        } else {
            closed = null;
        }
        return WebComponentUtil.getShortDateTimeFormattedValue(closed, pageBase);
    }

    private static String createCaseOpenTimestampModel(IModel<SelectableBean<CaseType>> rowModel, PageBase pageBase) {
        CaseType object = rowModel.getObject().getValue();
        MetadataType metadata = object != null ? object.getMetadata() : null;
        XMLGregorianCalendar createdCal = metadata != null ? metadata.getCreateTimestamp() : null;
        final Date created;
        if (createdCal != null) {
            created = createdCal.toGregorianCalendar().getTime();
        } else {
            created = null;
        }
        return WebComponentUtil.getShortDateTimeFormattedValue(created, pageBase);
    }

    private static void processCaseOutcome(CaseType caseType, Map<DisplayType, Integer> map, boolean useNullAsOne) {
        if (caseType == null) {
            return;
        }
        Integer one = null;
        if (!useNullAsOne) {
            one = 1;
        }
        if (CaseTypeUtil.isApprovalCase(caseType)) {
            ApprovalOutcomeIcon icon;
            String outcome = caseType.getOutcome();

            if (StringUtils.isEmpty(outcome)) {
                if (caseType.getCloseTimestamp() != null) {
                    return;
                } else {
                    icon = ApprovalOutcomeIcon.IN_PROGRESS;
                }
            } else {
                icon = WebComponentUtil.caseOutcomeUriToIcon(outcome);
            }

            putDisplayTypeToMapWithCount(map, one, GuiDisplayTypeUtil.createDisplayType(icon));
            return;
        }
        if (CaseTypeUtil.isManualProvisioningCase(caseType)) {

            if (StringUtils.isEmpty(caseType.getOutcome())) {
                if (caseType.getCloseTimestamp() != null) {
                    putDisplayTypeToMapWithCount(map, one, GuiDisplayTypeUtil.createDisplayType(OperationResultStatusPresentationProperties.UNKNOWN));
                } else {
                    putDisplayTypeToMapWithCount(map, one, GuiDisplayTypeUtil.createDisplayType(OperationResultStatusPresentationProperties.IN_PROGRESS));
                }
            } else {
                OperationResultStatusType result;
                try {
                    result = OperationResultStatusType.fromValue(caseType.getOutcome());
                } catch (IllegalArgumentException e) {
                    putDisplayTypeToMapWithCount(map, one,
                            GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.caseOutcomeUriToPresentation(caseType.getOutcome())));
                    return;
                }
                OperationResultStatusPresentationProperties resultStatus = OperationResultStatusPresentationProperties.parseOperationalResultStatus(result);
                putDisplayTypeToMapWithCount(map, one, GuiDisplayTypeUtil.createDisplayType(resultStatus));
            }
        }
    }

    private static void putDisplayTypeToMapWithCount(Map<DisplayType, Integer> map, Integer one, DisplayType caseDisplayType) {
        if (map.containsKey(caseDisplayType)) {
            map.merge(caseDisplayType, 1, Integer::sum);
        } else {
            map.put(caseDisplayType, one);
        }
    }

    public static AbstractColumn<SelectableBean<CaseType>, String> createCaseActorsColumn(PageBase pageBase) {
        return new AbstractColumn<>(createStringResource("pageCases.table.actors")) {
            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<CaseType>>> item, String componentId, IModel<SelectableBean<CaseType>> rowModel) {
                CaseType caseInstance = rowModel != null ? rowModel.getObject().getValue() : null;
                item.add(getMultilineLinkPanel(componentId, getActorsForCase(caseInstance), pageBase));
            }
        };
    }

    public static RepeatingView getMultilineLinkPanel(String componentId, List<ObjectReferenceType> referencesList, PageBase pageBase) {
        RepeatingView multilineLinkPanel = new RepeatingView(componentId);
        multilineLinkPanel.setOutputMarkupId(true);
        if (referencesList != null) {
            referencesList.forEach(reference -> {
                AjaxLinkPanel referenceAjaxLinkPanel = new AjaxLinkPanel(multilineLinkPanel.newChildId(),
                        Model.of(WebComponentUtil.getReferencedObjectDisplayNameAndName(reference, true, pageBase))) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        dispatchToObjectDetailsPage(reference, pageBase, false);
                    }

                    @Override
                    public boolean isEnabled() {
                        return CollectionUtils.isNotEmpty(WebComponentUtil.loadReferencedObjectList(
                                Collections.singletonList(reference), "loadCaseReferenceObject", pageBase));
                    }
                };
                referenceAjaxLinkPanel.setOutputMarkupId(true);
                multilineLinkPanel.add(referenceAjaxLinkPanel);
            });
        }
        return multilineLinkPanel;
    }

    public static <C extends Containerable> C unwrapRowModel(IModel<PrismContainerValueWrapper<C>> rowModel) {
        return rowModel.getObject().getRealValue();
    }

    public static <C extends Containerable, S extends SelectableRow<C>> C unwrapSelectableRowModel(IModel<S> rowModel) {
        if (rowModel == null) {
            return null;
        }
        S rowValue = rowModel.getObject();
        return unwrapRowRealValue(rowValue);
    }

    public static <T extends Serializable, S extends SelectableRow<T>> T unwrapRowRealValue(S rowValue) {
        if (rowValue == null) {
            return null;
        }
        if (rowValue instanceof SelectableBean) {
            return (T) ((SelectableBean<?>) rowValue).getValue();
        } else if (rowValue instanceof PrismValueWrapper) {
            return (T) ((PrismValueWrapper<?>) rowValue).getRealValue();
        }

        return null;
    }

    private static List<ObjectReferenceType> getActorsForCase(CaseType caseType) {
        List<ObjectReferenceType> actorsList = new ArrayList<>();
        if (caseType != null) {
            List<CaseWorkItemType> caseWorkItemTypes = caseType.getWorkItem();
            for (CaseWorkItemType caseWorkItem : caseWorkItemTypes) {
                actorsList.addAll(
                        getActorsForWorkitem(caseWorkItem, CaseTypeUtil.isClosed(caseType)));
            }
        }
        // Note that this makes the parent case object inconsistent. Hopefully it will be thrown away anyway.
        actorsList.forEach(a -> a.asReferenceValue().clearParent());
        return actorsList;
    }

    private static List<ObjectReferenceType> getActorsForWorkitem(CaseWorkItemType workItem, boolean isClosed) {
        if (isClosed) {
            ObjectReferenceType performerRef = workItem.getPerformerRef();
            return performerRef != null ? List.of(performerRef) : List.of();
        } else if (workItem.getAssigneeRef() != null && !workItem.getAssigneeRef().isEmpty()) {
            return workItem.getAssigneeRef();
        } else {
            return workItem.getCandidateRef();
        }
    }

    public static <S extends SelectableRow<AssignmentType>> List<IColumn<S, String>> getDefaultAssignmentsColumns(String realValuePath,
            PageBase pageBase) {
        return getDefaultAssignmentsColumns(null, realValuePath, false, pageBase);
    }

    //attempt to gather assignment columns creation in one place. not finished (e.g.  for construction, inducements...), not sure if needed at all
    public static <S extends SelectableRow<AssignmentType>> List<IColumn<S, String>> getDefaultAssignmentsColumns(
            QName assignmentTargetRefType, String realValuePath, boolean showAllColumns, PageBase pageBase) {

        List<ColumnTypeDto<String>> columnsDefs = Arrays.asList(
                new ColumnTypeDto<>("AssignmentDataTablePanel.activationColumnName",
                        realValuePath + ".activation.effectiveStatus", null)

        );

        List<IColumn<S, String>> assignmentColumns = new ArrayList<>(ColumnUtils.createColumns(columnsDefs));
        if (assignmentTargetRefType == null && !showAllColumns) {
            return assignmentColumns;
        }
        if (showAllColumns || QNameUtil.matchAny(assignmentTargetRefType, Arrays.asList(RoleType.COMPLEX_TYPE, OrgType.COMPLEX_TYPE,
                ServiceType.COMPLEX_TYPE))) {
            assignmentColumns.add(new AbstractColumn<>(
                    createStringResource("AbstractRoleAssignmentPanel.relationLabel")) {
                @Override
                public void populateItem(Item<ICellPopulator<S>> item, String componentId, IModel<S> assignmentModel) {
                    item.add(new Label(componentId, RelationUtil.getRelationLabelValue(unwrapSelectableRowModel(assignmentModel), pageBase)));
                }
            });

            assignmentColumns.add(new AbstractColumn<>(createStringResource("AbstractRoleAssignmentPanel.identifierLabel")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void populateItem(Item<ICellPopulator<S>> item, String componentId,
                        final IModel<S> rowModel) {
                    item.add(new Label(componentId, AssignmentsUtil.getIdentifierLabelModel(unwrapSelectableRowModel(rowModel), pageBase)));
                }
            });
        }
        if (showAllColumns || QNameUtil.match(assignmentTargetRefType, RoleType.COMPLEX_TYPE)) {
            assignmentColumns.add(new PropertyColumn<>(pageBase.createStringResource("AssignmentDataTablePanel.tenantColumnName"),
                    AssignmentType.F_TENANT_REF.getLocalPart()) {
                @Override
                public IModel<String> getDataModel(IModel<S> rowModel) {
                    AssignmentType assignment = unwrapSelectableRowModel(rowModel);
                    if (assignment == null) {
                        return Model.of("");
                    }
                    return Model.of(WebComponentUtil.getReferencedObjectDisplayNameAndName(assignment.getTenantRef(), true, pageBase));
                }
            });
            assignmentColumns.add(new PropertyColumn<>(pageBase.createStringResource("AssignmentDataTablePanel.organizationColumnName"),
                    AssignmentType.F_ORG_REF.getLocalPart()) {
                @Override
                public IModel<String> getDataModel(IModel<S> rowModel) {
                    AssignmentType assignment = unwrapSelectableRowModel(rowModel);
                    if (assignment == null) {
                        return Model.of("");
                    }
                    return Model.of(WebComponentUtil.getReferencedObjectDisplayNameAndName(assignment.getOrgRef(), true, pageBase));
                }
            });
        }
        if (showAllColumns || QNameUtil.match(assignmentTargetRefType, ResourceType.COMPLEX_TYPE)) {
            List<ColumnTypeDto<String>> constructionColumnsDefs = Arrays.asList(
                    new ColumnTypeDto<>("ConstructionType.kind",
                            realValuePath + "." + AssignmentType.F_CONSTRUCTION.getLocalPart() + "." + ConstructionType.F_KIND.getLocalPart(),
                            null),
                    new ColumnTypeDto<>("ConstructionType.intent",
                            realValuePath + "." + AssignmentType.F_CONSTRUCTION.getLocalPart() + "." + ConstructionType.F_INTENT.getLocalPart(),
                            null)
            );
            assignmentColumns.addAll(ColumnUtils.createColumns(constructionColumnsDefs));
        }
        return assignmentColumns;
    }

    public static <S extends SelectableRow<AssignmentType>> CompositedIconColumn<S> createAssignmentIconColumn(PageBase pageBase) {
        return new CompositedIconColumn<>(Model.of("")) {

            @Override
            protected CompositedIcon getCompositedIcon(IModel<S> rowModel) {
                AssignmentType assignment = unwrapSelectableRowModel(rowModel);
                PrismObject<? extends FocusType> object = AssignmentsUtil.loadTargetObject(assignment, pageBase);
                if (object != null) {
                    return WebComponentUtil.createCompositeIconForObject(object.asObjectable(),
                            new OperationResult("create_assignment_composited_icon"), pageBase);
                }
                String displayType = IconAndStylesUtil.createDefaultBlackIcon(AssignmentsUtil.getTargetType(assignment));
                CompositedIconBuilder iconBuilder = new CompositedIconBuilder();
                iconBuilder.setBasicIcon(displayType, IconCssStyle.IN_ROW_STYLE);
                return iconBuilder.build();
            }
        };
    }

    public static <S extends SelectableRow<AssignmentType>> String loadValuesForAssignmentNameColumn(IModel<S> rowModel, Collection<String> evaluatedExpressionValues,
            boolean useEvaluatedValues, PageBase pageBase) {
        if (useEvaluatedValues) {
            if (CollectionUtils.isEmpty(evaluatedExpressionValues)) {
                return "";
            }
            if (evaluatedExpressionValues.size() == 1) {
                return evaluatedExpressionValues.iterator().next();
            }
            return String.join(", ", evaluatedExpressionValues);
        }
        String name = AssignmentsUtil.getName(unwrapSelectableRowModel(rowModel), pageBase);
        LOGGER.trace("Name for AssignmentType: " + name);
        if (StringUtils.isBlank(name)) {
            return createStringResource("AssignmentPanel.noName").getString();
        }

        return name;
    }

}
