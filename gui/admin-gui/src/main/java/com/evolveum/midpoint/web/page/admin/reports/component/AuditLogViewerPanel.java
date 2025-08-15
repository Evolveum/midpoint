/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.component;

import static com.evolveum.midpoint.gui.impl.util.DetailsPageUtil.dispatchToObjectDetailsPage;

import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.gui.api.component.button.CsvDownloadButtonPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.GuiChannel;
import com.evolveum.midpoint.gui.impl.component.AjaxCompositedIconButton;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.icon.CompositedIconWithLabelColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanContainerDataProvider;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchContext;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.PropertySearchItemWrapper;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.AuditSelectableLinkColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Created by honchar
 */
public class AuditLogViewerPanel extends ContainerableListPanel<AuditEventRecordType, SelectableBean<AuditEventRecordType>> {

    @Serial private static final long serialVersionUID = 1L;

    static final Trace LOGGER = TraceManager.getTrace(AuditLogViewerPanel.class);

    private static final String CHANGE_ITEM_VARIABLE = "changedItem";

    public AuditLogViewerPanel(String id) {
        super(id, AuditEventRecordType.class);
    }

    public AuditLogViewerPanel(String id, ContainerPanelConfigurationType configuration) {
        super(id, AuditEventRecordType.class, configuration);
    }

    @Override
    protected SearchContext createAdditionalSearchContext() {
        SearchContext ctx = new SearchContext();
        ctx.setHistory(isObjectHistoryPanel());
        return ctx;
    }

    @Override
    protected IColumn<SelectableBean<AuditEventRecordType>, String> createNameColumn(IModel<String> displayModel, GuiObjectColumnType customColumn, ExpressionType expression) {
        if (displayModel == null || customColumn == null) {
            return null;
        }
        return new AuditSelectableLinkColumn(displayModel, null, customColumn, expression, getPageBase());
    }

    @Override
    protected IColumn<SelectableBean<AuditEventRecordType>, String> createCheckboxColumn() {
        return null;
    }

    @Override
    protected IColumn<SelectableBean<AuditEventRecordType>, String> createIconColumn() {
        return null;
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PAGE_AUDIT_LOG_VIEWER;
    }

    @Override
    protected String getStorageKey() {
        String collectionNameValue = null;
        if (isCollectionViewPanelForCompiledView()) {
            StringValue collectionName = WebComponentUtil.getCollectionNameParameterValue(getPageBase());
            collectionNameValue = collectionName != null ? collectionName.toString() : "";
        }
        return getAuditStorageKey(collectionNameValue);
    }

    @Override
    protected ISelectableDataProvider<SelectableBean<AuditEventRecordType>> createProvider() {
        PageStorage pageStorage = getPageStorage();
        SelectableBeanContainerDataProvider<AuditEventRecordType> provider = new SelectableBeanContainerDataProvider<AuditEventRecordType>(
                AuditLogViewerPanel.this, getSearchModel(), null, false) {

            @Override
            protected Integer countObjects(Class<AuditEventRecordType> type, ObjectQuery query,
                    Collection<SelectorOptions<GetOperationOptions>> currentOptions, Task task, OperationResult result)
                    throws CommonException {
                return getPageBase().getModelAuditService().countObjects(query, currentOptions, task, result);
            }

            @Override
            protected List<AuditEventRecordType> searchObjects(Class<AuditEventRecordType> type, ObjectQuery query,
                    Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result)
                    throws CommonException {
                return getPageBase().getModelAuditService().searchObjects(query, options, task, result);
            }

            @NotNull
            @Override
            protected List<ObjectOrdering> createObjectOrderings(SortParam<String> sortParam) {
                if (sortParam != null && sortParam.getProperty() != null) {
                    OrderDirection order = sortParam.isAscending() ? OrderDirection.ASCENDING : OrderDirection.DESCENDING;
                    return Collections.singletonList(
                            getPrismContext().queryFactory().createOrdering(
                                    ItemPath.create(new QName(AuditEventRecordType.COMPLEX_TYPE.getNamespaceURI(), sortParam.getProperty())), order));
                } else {
                    return Collections.emptyList();
                }
            }

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                return AuditLogViewerPanel.this.getCustomizeContentQuery();
            }
        };
        provider.setSort(AuditEventRecordType.F_TIMESTAMP.getLocalPart(), SortOrder.DESCENDING);
        return provider;
    }

    @Override
    public List<AuditEventRecordType> getSelectedRealObjects() {
        return getSelectedObjects().stream().map(SelectableBean::getValue).collect(Collectors.toList());
    }

    @Override
    protected List<Component> createToolbarButtonsList(String idButton) {
        List<Component> buttonsList = new ArrayList<>();
        CsvDownloadButtonPanel exportDataLink = new CsvDownloadButtonPanel(idButton) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected String getFilename() {
                return "AuditLogViewer_" + createStringResource("MainObjectListPanel.exportFileName").getString();
            }

            @Override
            protected DataTable<?, ?> getDataTable() {
                return getTable().getDataTable();
            }
        };
        exportDataLink.add(new VisibleBehaviour(() -> WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_CSV_EXPORT_ACTION_URI)));
        buttonsList.add(exportDataLink);

        AjaxCompositedIconButton createReport = new AjaxCompositedIconButton(idButton, WebComponentUtil.createCreateReportIcon(),
                getPageBase().createStringResource("MainObjectListPanel.createReport")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                createReportPerformed(target);
            }
        };
        createReport.add(AttributeAppender.append("class", "mr-2 btn btn-default btn-sm"));
        buttonsList.add(createReport);
        return buttonsList;
    }

    protected String getAuditStorageKey(String collectionNameValue) {
        if (StringUtils.isNotEmpty(collectionNameValue)) {
            return SessionStorage.KEY_AUDIT_LOG + "." + collectionNameValue;
        }
        return SessionStorage.KEY_AUDIT_LOG;
    }

    protected ObjectQuery getCustomizeContentQuery() {
        return null;
    }

    private IColumn<SelectableBean<AuditEventRecordType>, String> createInitiatorRefColumn() {
        return new LinkColumn<>(createStringResource("AuditEventRecordType.initiatorRef"),
                SelectableBeanImpl.F_VALUE + "." + AuditEventRecordType.F_INITIATOR_REF.getLocalPart()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
                return () -> {
                    AuditEventRecordType auditEventRecordType = unwrapModel(rowModel);
                    if (auditEventRecordType == null) {
                        return null;
                    }
                    return WebModelServiceUtils.resolveReferenceName(auditEventRecordType.getInitiatorRef(), getPageBase(), true);
                };
            }

            @Override
            public void onClick(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
                AuditEventRecordType auditEventRecordType = unwrapModel(rowModel);
                dispatchToObjectDetailsPage(auditEventRecordType.getInitiatorRef(), getPageBase(), false);
            }

            @Override
            public boolean isEnabled(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
                return unwrapModel(rowModel) != null;
            }
        };
    }

    private IColumn<SelectableBean<AuditEventRecordType>, String> createEventStageColumn() {
        if (isObjectHistoryPanel()) {
            return null;
        }

        return new PropertyColumn<>(
                createStringResource("PageAuditLogViewer.eventStageLabel"), AuditEventRecordType.F_EVENT_STAGE.getLocalPart(),
                SelectableBeanImpl.F_VALUE + "." + AuditEventRecordType.F_EVENT_STAGE.getLocalPart()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public IModel<String> getDataModel(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
                AuditEventRecordType record = unwrapModel(rowModel);
                if (record == null) {
                    return new Model<>();
                }
                return WebComponentUtil.createLocalizedModelForEnum(record.getEventStage(),
                        AuditLogViewerPanel.this);
            }
        };
    }

    private IColumn<SelectableBean<AuditEventRecordType>, String> createEventTypeColumn() {
        return new CompositedIconWithLabelColumn<>(createStringResource("PageAuditLogViewer.eventTypeLabel")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected CompositedIcon getCompositedIcon(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
                AuditEventRecordType record = unwrapModel(rowModel);
                if (record == null) {
                    return null;
                }
                AuditEventType eventType = AuditEventType.fromSchemaValue(record.getEventType());
                ObjectReferenceType targetRef = record.getTargetRef();
                String defaultIcon = null;
                String rightBottomIcon = null;
                String iconColor = null;
                if (targetRef != null && targetRef.getType() != null) {
                    defaultIcon = IconAndStylesUtil.createDefaultBlackIcon(targetRef.getType());
                }
                if (eventType != null && eventType.getDisplay() != null && eventType.getDisplay().getIcon() != null) {
                    if (defaultIcon == null) {
                        defaultIcon = eventType.getDisplay().getIcon().getCssClass();
                    } else {
                        rightBottomIcon = eventType.getDisplay().getIcon().getCssClass();
                    }
                    iconColor = GuiDisplayTypeUtil.removeStringAfterSemicolon(eventType.getDisplay().getIcon().getColor());
                }
                CompositedIconBuilder iconBuilder = new CompositedIconBuilder();
                if (defaultIcon != null) {
                    iconBuilder.setBasicIcon(defaultIcon, IconCssStyle.IN_ROW_STYLE);
                    if (rightBottomIcon != null) {
                        iconBuilder.appendLayerIcon(new IconType().cssClass(rightBottomIcon).color(iconColor), IconCssStyle.BOTTOM_RIGHT_STYLE);
                    }
                    if (iconColor != null) {
                        iconBuilder.appendColorHtmlValue(iconColor);
                    }
                }
                return iconBuilder.build();
            }

            @Override
            public IModel<DisplayType> getLabelDisplayModel(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
                AuditEventRecordType record = unwrapModel(rowModel);
                if (record == null) {
                    return Model.of(new DisplayType());
                }
                AuditEventType eventType = AuditEventType.fromSchemaValue(record.getEventType());
                String label =
                        WebComponentUtil.createLocalizedModelForEnum(record.getEventType(), AuditLogViewerPanel.this).getObject();
                String color = eventType != null && eventType.getDisplay() != null && eventType.getDisplay().getIcon() != null ?
                        eventType.getDisplay().getIcon().getColor() : null;
                return Model.of(new DisplayType().label(label).color(GuiDisplayTypeUtil.removeStringAfterSemicolon(color)));
            }

            @Override
            public IModel<String> getDataModel(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
                AuditEventRecordType record = unwrapModel(rowModel);
                if (record == null) {
                    return Model.of("");
                }
                String value = WebComponentUtil.createLocalizedModelForEnum(record.getEventType(),
                        AuditLogViewerPanel.this).getObject();
                return Model.of(value);
            }
        };
    }

    private IColumn<SelectableBean<AuditEventRecordType>, String> createTargetOwnerRefColumn() {
        if (isObjectHistoryPanel()) {
            return null;
        }

        return new LinkColumn<>(createStringResource("AuditEventRecordType.targetOwnerRef"),
                SelectableBeanImpl.F_VALUE + "." + AuditEventRecordType.F_TARGET_OWNER_REF.getLocalPart()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
                return () -> {
                    AuditEventRecordType auditEventRecordType = unwrapModel(rowModel);
                    if (auditEventRecordType == null) {
                        return null;
                    }
                    return WebModelServiceUtils.resolveReferenceName(auditEventRecordType.getTargetOwnerRef(), getPageBase(), true);
                };
            }

            @Override
            public void onClick(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
                AuditEventRecordType auditEventRecordType = unwrapModel(rowModel);
                dispatchToObjectDetailsPage(auditEventRecordType.getTargetOwnerRef(), getPageBase(), false);
            }

            @Override
            public boolean isEnabled(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
                return unwrapModel(rowModel) != null;
            }
        };
    }

    private IColumn<SelectableBean<AuditEventRecordType>, String> createTargetRefColumn() {
        if (isObjectHistoryPanel()) {
            return null;
        }

        return new LinkColumn<>(createStringResource("AuditEventRecordType.targetRef"),
                SelectableBeanImpl.F_VALUE + "." + AuditEventRecordType.F_TARGET_REF.getLocalPart()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
                return () -> {
                    AuditEventRecordType auditEventRecordType = unwrapModel(rowModel);
                    if (auditEventRecordType == null) {
                        return null;
                    }
                    return WebModelServiceUtils.resolveReferenceName(auditEventRecordType.getTargetRef(), getPageBase(), true);
                };
            }

            @Override
            public boolean isEnabled(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
                return unwrapModel(rowModel) != null && !AuditEventTypeType.DELETE_OBJECT.equals(unwrapModel(rowModel).getEventType());
            }

            @Override
            public void onClick(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
                AuditEventRecordType auditEventRecordType = unwrapModel(rowModel);
                dispatchToObjectDetailsPage(auditEventRecordType.getTargetRef(), getPageBase(), false);
            }
        };
    }

    private IColumn<SelectableBean<AuditEventRecordType>, String> createChannelColumn() {
        return new PropertyColumn<>(
                createStringResource("AuditEventRecordType.channel"), AuditEventRecordType.F_CHANNEL.getLocalPart(),
                SelectableBeanImpl.F_VALUE + "." + AuditEventRecordType.F_CHANNEL.getLocalPart()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<AuditEventRecordType>>> item, String componentId,
                    IModel<SelectableBean<AuditEventRecordType>> rowModel) {
                IModel<String> channelModel = () -> {
                    AuditEventRecordType auditEventRecordType = unwrapModel(rowModel);
                    if (auditEventRecordType == null) {
                        return ""; //TODO might we return null?
                    }
                    String channel = auditEventRecordType.getChannel();
                    for (GuiChannel chan : GuiChannel.values()) {
                        if (chan.getUri().equals(channel)) {
                            return getPageBase().createStringResource(chan).getString();
                        }
                    }
                    return "";
                };
                item.add(new Label(componentId, channelModel));
                item.add(new AttributeModifier("style", new Model<>("width: 10%;")));
            }
        };
    }

    private IColumn<SelectableBean<AuditEventRecordType>, String> createOutcomeColumn() {
        return new PropertyColumn<>(
                createStringResource("PageAuditLogViewer.outcomeLabel"), AuditEventRecordType.F_OUTCOME.getLocalPart(),
                SelectableBeanImpl.F_VALUE + "." + AuditEventRecordType.F_OUTCOME.getLocalPart()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public IModel<String> getDataModel(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
                AuditEventRecordType record = unwrapModel(rowModel);
                if (record == null) {
                    return null;
                }
                return WebComponentUtil.createLocalizedModelForEnum(record.getOutcome(), AuditLogViewerPanel.this);
            }
        };
    }

    @Override
    protected IColumn<SelectableBean<AuditEventRecordType>, String> createCustomExportableColumn(
            IModel<String> displayModel, GuiObjectColumnType guiObjectColumn, SerializableSupplier<VariablesMap> variablesSupplier, ExpressionType expression) {

        ItemPath path = WebComponentUtil.getPath(guiObjectColumn);

        if (AuditEventRecordType.F_INITIATOR_REF.equivalent(path)) {
            return createInitiatorRefColumn();
        }

        if (AuditEventRecordType.F_EVENT_STAGE.equivalent(path)) {
            return createEventStageColumn();
        }

        if (AuditEventRecordType.F_EVENT_TYPE.equivalent(path)) {
            return createEventTypeColumn();
        }

        if (AuditEventRecordType.F_TARGET_REF.equivalent(path)) {
            return createTargetRefColumn();
        }

        if (AuditEventRecordType.F_TARGET_OWNER_REF.equivalent(path)) {
            return createTargetOwnerRefColumn();
        }

        if (AuditEventRecordType.F_CHANNEL.equivalent(path)) {
            return createChannelColumn();
        }

        if (AuditEventRecordType.F_OUTCOME.equivalent(path)) {
            return createOutcomeColumn();
        }

        SerializableSupplier<VariablesMap> customVariables = () -> {
            VariablesMap variablesMap = new VariablesMap();

            if (variablesSupplier != null) {
                VariablesMap map = variablesSupplier.get();
                if (map != null) {
                    variablesMap.putAll(map);
                }
            }

            variablesMap.put(CHANGE_ITEM_VARIABLE, getChangedItemPath(), ItemPathType.class);

            return variablesMap;
        };

        if (AuditEventRecordType.F_DELTA.equivalent(path)) {
            return createDeltaColumn(displayModel, guiObjectColumn, customVariables, expression);
        }

        return super.createCustomExportableColumn(displayModel, guiObjectColumn, customVariables, expression);
    }

    private ItemPathType getChangedItemPath() {
        Search search = getSearchModel().getObject();
        if (search == null) {
            return null;
        }

        // noinspection unchecked
        PropertySearchItemWrapper<ItemPathType> wrapper = search.findPropertySearchItem(AuditEventRecordType.F_CHANGED_ITEM);
        if (wrapper == null || wrapper.getValue() == null) {
            return null;
        }

        return wrapper.getValue().getValue();
    }

    @Override
    protected AuditLogViewerContext getColumnTypeConfigContext() {
        return new AuditLogViewerContext(() -> getSearchModel().getObject());
    }

    private IColumn<SelectableBean<AuditEventRecordType>, String> createDeltaColumn(
            IModel<String> displayModel, GuiObjectColumnType guiObjectColumn, SerializableSupplier<VariablesMap> variablesSupplier, ExpressionType expression) {

        boolean changedItemVisible = getColumnTypeConfigContext().isChangedItemSearchItemVisible();

        boolean conditionallyVisible =
                changedItemVisible
                        && !columnVisibilityMatches(guiObjectColumn, UserInterfaceElementVisibilityType.VISIBLE, UserInterfaceElementVisibilityType.AUTOMATIC);

        if (conditionallyVisible || getColumnVisibility(guiObjectColumn) == UserInterfaceElementVisibilityType.VISIBLE) {
            return new DeltaColumn(displayModel, guiObjectColumn, getSearchModel(), variablesSupplier, expression, getPageBase());
        }

        return null;
    }

    private UserInterfaceElementVisibilityType getColumnVisibility(GuiObjectColumnType column) {
        return column != null ? column.getVisibility() : null;
    }

    private boolean columnVisibilityMatches(GuiObjectColumnType column, UserInterfaceElementVisibilityType... visibilities) {
        UserInterfaceElementVisibilityType real = column != null ? column.getVisibility() : null;
        if (real == null) {
            return false;
        }

        return Arrays.stream(visibilities).anyMatch(v -> v == real);
    }

    @Override
    public void refreshTable(AjaxRequestTarget target) {
        resetTableColumns();

        super.refreshTable(target);
    }

    protected AuditEventRecordType unwrapModel(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
        if (rowModel == null || rowModel.getObject() == null) {
            return null;
        }
        return rowModel.getObject().getValue();
    }

    protected boolean isObjectHistoryPanel() {
        return false;
    }
}
