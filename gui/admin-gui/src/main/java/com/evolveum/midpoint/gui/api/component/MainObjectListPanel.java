/*
 * Copyright (C) 2016-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.AjaxCompositedIconButton;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.util.ObjectCollectionViewUtil;
import com.evolveum.midpoint.gui.impl.util.TableUtil;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.CompositedIconButtonDto;
import com.evolveum.midpoint.web.component.MultiFunctinalButtonDto;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.ObjectNameColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.PageImportObject;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author katkav
 */
public abstract class MainObjectListPanel<O extends ObjectType> extends ObjectListPanel<O> {
    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(MainObjectListPanel.class);

    private static final String DOT_CLASS = MainObjectListPanel.class.getName() + ".";
    private static final String OPERATION_DELETE_OBJECTS = DOT_CLASS + "deleteObjects";
    private static final String OPERATION_DELETE_OBJECT = DOT_CLASS + "deleteObject";

    private final LoadableModel<ExecuteChangeOptionsDto> executeOptionsModel;

    public MainObjectListPanel(String id, Class<O> type) {
        super(id, type);
        executeOptionsModel = new LoadableModel<>(false) {

            @Override
            protected ExecuteChangeOptionsDto load() {
                return ExecuteChangeOptionsDto.createFromSystemConfiguration();
            }
        };
    }

    public MainObjectListPanel(String id, Class<O> type, ContainerPanelConfigurationType config) {
        super(id, type, config);
        executeOptionsModel = new LoadableModel<>(false) {

            @Override
            protected ExecuteChangeOptionsDto load() {
                return ExecuteChangeOptionsDto.createFromSystemConfiguration();
            }
        };
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        setAdditionalBoxCssClasses(IconAndStylesUtil.getBoxCssClasses(WebComponentUtil.classToQName(getPrismContext(), getType())));
    }

    @Override
    protected IColumn<SelectableBean<O>, String> createCheckboxColumn() {
        return new CheckBoxHeaderColumn<>();
    }

    protected void newObjectPerformed(AjaxRequestTarget target, AssignmentObjectRelation relation, CompiledObjectCollectionView collectionView) {
        if (collectionView == null) {
            collectionView = getObjectCollectionView();
        }
        try {
            DetailsPageUtil.initNewObjectWithReference(getPageBase(),
                    relation != null && CollectionUtils.isNotEmpty(relation.getObjectTypes()) ?
                            relation.getObjectTypes().get(0) : WebComponentUtil.classToQName(getPrismContext(), getType()),
                    getNewObjectReferencesList(collectionView, relation));
        } catch (SchemaException ex) {
            getPageBase().getFeedbackMessages().error(MainObjectListPanel.this, ex.getUserFriendlyMessage());
            target.add(getPageBase().getFeedbackPanel());
        }
    }

        @Override
    protected IColumn<SelectableBean<O>, String> createNameColumn(IModel<String> displayModel, GuiObjectColumnType customColumn, ExpressionType expression) {
        return new ObjectNameColumn<>(displayModel == null ? createStringResource("ObjectType.name") : displayModel,
                getSortProperty(customColumn, expression), customColumn, expression, getPageBase()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(IModel<SelectableBean<O>> rowModel) {
                O object = rowModel.getObject().getValue();
                MainObjectListPanel.this.objectDetailsPerformed(object);
            }

            @Override
            public boolean isClickable(IModel<SelectableBean<O>> rowModel) {
                return MainObjectListPanel.this.isObjectDetailsEnabled(rowModel);
            }
        };
    }

    protected boolean isObjectDetailsEnabled(IModel<SelectableBean<O>> rowModel) {
        return true;
    }

    protected List<ObjectReferenceType> getNewObjectReferencesList(CompiledObjectCollectionView collectionView, AssignmentObjectRelation relation) {
        return ObjectCollectionViewUtil.getArchetypeReferencesList(collectionView);
    }

    private CompositedIcon createCompositedIcon(CompiledObjectCollectionView collectionView) {
        DisplayType display = GuiDisplayTypeUtil.getNewObjectDisplayTypeFromCollectionView(collectionView, getPageBase());
        CompositedIconBuilder builder = new CompositedIconBuilder();

        PolyStringType tooltip = display != null ? display.getTooltip() : null;

        builder.setBasicIcon(GuiDisplayTypeUtil.getIconCssClass(display), IconCssStyle.IN_ROW_STYLE)
                .appendColorHtmlValue(GuiDisplayTypeUtil.getIconColor(display))
                .setTitle(LocalizationUtil.translatePolyString(tooltip));

        return builder.build();
    }

    @Override
    protected ISelectableDataProvider<SelectableBean<O>> createProvider() {
        return createSelectableBeanObjectDataProvider(null, null); // default
    }

    @Override
    protected List<Component> createToolbarButtonsList(String buttonId) {
        List<Component> buttonsList = new ArrayList<>();

        buttonsList.add(createNewObjectButton(buttonId));
        buttonsList.add(createImportObjectButton(buttonId));
        buttonsList.add(createDownloadButton(buttonId));
        buttonsList.add(createCreateReportButton(buttonId));
        buttonsList.add(createRefreshButton(buttonId));
        buttonsList.add(createPlayPauseButton(buttonId));

        return buttonsList;
    }

    private Component createNewObjectButton(String buttonId) {
        DisplayType newObjectButtonDisplayType = getNewObjectButtonStandardDisplayType();
        CompositedIconBuilder builder = new CompositedIconBuilder();
        builder.setBasicIcon(GuiDisplayTypeUtil.getIconCssClass(newObjectButtonDisplayType), IconCssStyle.IN_ROW_STYLE)
                .appendColorHtmlValue(GuiDisplayTypeUtil.getIconColor(newObjectButtonDisplayType));
        CompiledObjectCollectionView view = getObjectCollectionView();
        if (isCollectionViewPanelForCompiledView() && GuiDisplayTypeUtil.existsIconDisplay(view)
                && GuiDisplayTypeUtil.containsDifferentIcon(newObjectButtonDisplayType, GuiStyleConstants.CLASS_ADD_NEW_OBJECT)) {
            IconType plusIcon = new IconType();
            plusIcon.setCssClass(GuiStyleConstants.CLASS_ADD_NEW_OBJECT);
            plusIcon.setColor("green");
            builder.appendLayerIcon(plusIcon, LayeredIconCssStyle.BOTTOM_RIGHT_MAX_ICON_STYLE);
        }
        String iconTitle = GuiDisplayTypeUtil.getDisplayTypeTitle(newObjectButtonDisplayType);
        AjaxCompositedIconButton createNewObjectButton = new AjaxCompositedIconButton(buttonId, builder.build(),
                createStringResource(StringUtils.isEmpty(iconTitle) ? "MainObjectListPanel.newObject" : iconTitle)) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (isCollectionViewPanelForCompiledView()) {
                    CompiledObjectCollectionView collectionView = getObjectCollectionView();
                    //HACK TODO clenup and think about generic mechanism for this
                    if (isCollectionViewWithoutMorePossibleNewType(collectionView)) {
                        newObjectPerformed(target, null, collectionView);
                        return;
                    }
                }

                if (!showNewObjectCreationPopup()) {
                    newObjectPerformed(target, null, getObjectCollectionView());
                    return;
                }

                NewObjectCreationPopup buttonsPanel = new NewObjectCreationPopup(getPageBase().getMainPopupBodyId(), new PropertyModel<>(loadButtonDescriptions(), MultiFunctinalButtonDto.F_ADDITIONAL_BUTTONS)) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected void buttonClickPerformed(AjaxRequestTarget target, AssignmentObjectRelation relationSpec, CompiledObjectCollectionView collectionViews, Class<? extends WebPage> page) {
                        getPageBase().hideMainPopup(target);
                        MainObjectListPanel.this.newObjectPerformed(target, relationSpec, collectionViews);
                    }

                };

                getPageBase().showMainPopup(buttonsPanel, target);
            }
        };
        createNewObjectButton.add(new VisibleBehaviour(this::isCreateNewObjectVisible));
        createNewObjectButton.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        return createNewObjectButton;
    }

    protected boolean isCollectionViewWithoutMorePossibleNewType(CompiledObjectCollectionView collectionView) {
        return true;
    }

    protected boolean showNewObjectCreationPopup() {
        return getNewObjectInfluencesList().size() > 1;
    }

    protected boolean isViewForObjectCollectionType(CompiledObjectCollectionView collectionView, String oid, QName type) {
        if (collectionView == null) {
            return false;
        }

        CollectionRefSpecificationType collectionRefSpecificationType = collectionView.getCollection();
        if (collectionRefSpecificationType == null) {
            return false;
        }

        ObjectReferenceType collectionRef = collectionRefSpecificationType.getCollectionRef();
        if (collectionRef == null) {
            return false;
        }

        if (!QNameUtil.match(collectionRef.getType(), type)) {
            return false;
        }

        return collectionRef.getOid().equals(oid);
    }

    protected LoadableModel<MultiFunctinalButtonDto> loadButtonDescriptions() {
        return new LoadableModel<>(false) {

            @Override
            protected MultiFunctinalButtonDto load() {
                MultiFunctinalButtonDto multifunctionalButton = new MultiFunctinalButtonDto();

                List<CompositedIconButtonDto> additionalButtons = new ArrayList<>();

                Collection<CompiledObjectCollectionView> compiledObjectCollectionViews = getNewObjectInfluencesList();

                if (CollectionUtils.isNotEmpty(compiledObjectCollectionViews)) {
                    compiledObjectCollectionViews.forEach(collection -> {
                        CompositedIconButtonDto buttonDesc = new CompositedIconButtonDto();
                        buttonDesc.setCompositedIcon(createCompositedIcon(collection));
                        buttonDesc.setOrCreateDefaultAdditionalButtonDisplayType(collection.getDisplay());
                        buttonDesc.setCollectionView(collection);
                        additionalButtons.add(buttonDesc);
                    });
                }

                multifunctionalButton.setAdditionalButtons(additionalButtons);

                return multifunctionalButton;
            }
        };

    }

    private DisplayType getNewObjectButtonStandardDisplayType() {
        if (isCollectionViewPanelForCompiledView()) {
            CompiledObjectCollectionView view = getObjectCollectionView();
            return GuiDisplayTypeUtil.getNewObjectDisplayTypeFromCollectionView(view, getPageBase());
        }

        String title = getTitleForNewObjectButton();
        return GuiDisplayTypeUtil.createDisplayType(GuiStyleConstants.CLASS_ADD_NEW_OBJECT, "green", title);
    }

    protected String getTitleForNewObjectButton() {
        return createStringResource("MainObjectListPanel.newObject").getString()
                + " "
                + createStringResource("ObjectTypeLowercase." + getType().getSimpleName()).getString();
    }

    private AjaxIconButton createImportObjectButton(String buttonId) {
        AjaxIconButton importObject = new AjaxIconButton(buttonId, new Model<>(GuiStyleConstants.CLASS_UPLOAD),
                createStringResource("MainObjectListPanel.import")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ((PageBase) getPage()).navigateToNext(PageImportObject.class);
            }
        };
        importObject.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        importObject.add(new VisibleBehaviour(this::isImportObjectButtonVisible));
        return importObject;
    }

    protected boolean isImportObjectButtonVisible() {
        try {
            return ((PageBase) getPage()).isAuthorized(ModelAuthorizationAction.IMPORT_OBJECTS.getUrl())
                    && WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_CONFIGURATION_ALL_URL,
                    AuthorizationConstants.AUTZ_UI_CONFIGURATION_IMPORT_URL);
        } catch (Exception ex) {
            LOGGER.error("Failed to check authorization for IMPORT action for " + getType().getSimpleName()
                    + " object, ", ex);
        }
        return false;
    }

    private AjaxCompositedIconButton createCreateReportButton(String buttonId) {
        final CompositedIconBuilder builder = new CompositedIconBuilder();
        builder.setBasicIcon(IconAndStylesUtil.createReportIcon(), IconCssStyle.IN_ROW_STYLE);
        IconType plusIcon = new IconType();
        plusIcon.setCssClass(GuiStyleConstants.CLASS_ADD_NEW_OBJECT);
        plusIcon.setColor("green");
        builder.appendLayerIcon(plusIcon, LayeredIconCssStyle.BOTTOM_RIGHT_STYLE);
        AjaxCompositedIconButton createReport = new AjaxCompositedIconButton(buttonId, builder.build(),
                getPageBase().createStringResource("MainObjectListPanel.createReport")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                createReportPerformed(target);
            }
        };
        createReport.add(AttributeAppender.append("class", "mr-2 btn btn-default btn-sm"));
        createReport.add(new VisibleBehaviour(() -> WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_CREATE_REPORT_BUTTON_URI)));
        return createReport;
    }

    private AjaxIconButton createRefreshButton(String buttonId) {
        AjaxIconButton refreshIcon = new AjaxIconButton(buttonId, new Model<>(GuiStyleConstants.CLASS_RECONCILE),
                createStringResource("MainObjectListPanel.refresh")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                clearCache();
                refreshTable(target);

                target.add(getTable());
            }
        };
        refreshIcon.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        return refreshIcon;
    }

    private AjaxIconButton createPlayPauseButton(String buttonId) {
        AjaxIconButton playPauseIcon = new AjaxIconButton(buttonId, getRefreshPausePlayButtonModel(),
                getRefreshPausePlayButtonTitleModel()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onClickPlayPauseButton(target, !isRefreshEnabled());
            }
        };
        playPauseIcon.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        return playPauseIcon;
    }

    private void onClickPlayPauseButton(AjaxRequestTarget target, boolean refreshEnabled) {
        clearCache();
        setManualRefreshEnabled(refreshEnabled);
        target.add(getTable());
    }

    public void startRefreshing(AjaxRequestTarget target) {
        onClickPlayPauseButton(target, true);
    }

    private IModel<String> getRefreshPausePlayButtonModel() {
        return () -> {
            if (isRefreshEnabled()) {
                return GuiStyleConstants.CLASS_PAUSE;
            }

            return GuiStyleConstants.CLASS_PLAY;
        };
    }

    private IModel<String> getRefreshPausePlayButtonTitleModel() {
        return () -> {
            if (isRefreshEnabled()) {
                return createStringResource("MainObjectListPanel.refresh.pause").getString();
            }
            return createStringResource("MainObjectListPanel.refresh.start").getString();
        };
    }
    protected boolean isCreateNewObjectVisible() {
        return !isCollectionViewPanel() || getObjectCollectionView().isApplicableForOperation(OperationTypeType.ADD) ||
                CollectionUtils.isNotEmpty(getNewObjectInfluencesList());
    }

    @NotNull protected List<CompiledObjectCollectionView> getNewObjectInfluencesList() {
        if (isCollectionViewPanelForCompiledView()) {
            return new ArrayList<>();
        }
        return getAllApplicableArchetypeViews();
    }

    public void deleteConfirmedPerformed(AjaxRequestTarget target, IModel<SelectableBean<O>> objectToDelete) {
        List<SelectableBean<O>> objects = isAnythingSelected(objectToDelete);

        if (objects.isEmpty()) {
            return;
        }

        OperationResult result = new OperationResult(objects.size() == 1 ? OPERATION_DELETE_OBJECT : OPERATION_DELETE_OBJECTS);
        for (SelectableBean<O> object : objects) {
            OperationResult subResult = result.createSubresult(OPERATION_DELETE_OBJECT);
            try {
                Task task = getPageBase().createSimpleTask(OPERATION_DELETE_OBJECT);

                ObjectDelta delta = getPrismContext().deltaFactory().object().create(object.getValue().getClass(), ChangeType.DELETE);
                delta.setOid(object.getValue().getOid());

                ExecuteChangeOptionsDto executeOptions = getExecuteOptions();
                ModelExecuteOptions options = executeOptions.createOptions(getPrismContext());
                LOGGER.debug("Using options {}.", executeOptions);
                getPageBase().getModelService()
                        .executeChanges(MiscUtil.createCollection(delta), options, task, subResult);
                subResult.computeStatus();
            } catch (Exception ex) {
                subResult.recomputeStatus();
                subResult.recordFatalError(getString("PageUsers.message.delete.fatalError"), ex);
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete user", ex);
            }
        }
        result.computeStatusComposite();
        clearCache();

        getPageBase().showResult(result);
        target.add(getFeedbackPanel());
        refreshTable(target);
        clearCache();
    }

    public ExecuteChangeOptionsDto getExecuteOptions() {
        return executeOptionsModel.getObject();
    }

    /**
     * This method check selection in table. If selectedObject != null than it
     * returns only this object.
     */
    public List<SelectableBean<O>> isAnythingSelected(IModel<SelectableBean<O>> selectedObject) {
        List<SelectableBean<O>> selectedObjects;
        if (selectedObject != null) {
            selectedObjects = new ArrayList<>();
            selectedObjects.add(selectedObject.getObject());
        } else {
            selectedObjects = TableUtil.getSelectedModels(getTable().getDataTable());
        }
        return selectedObjects;
    }

    protected String getNothingSelectedMessage() {
        return null;
    }

    public IModel<String> getConfirmationMessageModel(ColumnMenuAction action, String actionName) {
        if (action.getRowModel() == null) {
            return createStringResource(getConfirmMessageKeyForMultiObject(),
                    actionName, getSelectedObjectsCount());
        } else {
            return createStringResource(getConfirmMessageKeyForSingleObject(),
                    actionName, ((ObjectType) ((SelectableBean<?>) action.getRowModel().getObject()).getValue()).getName());
        }
    }

    protected String getConfirmMessageKeyForMultiObject() {
        throw new UnsupportedOperationException("getConfirmMessageKeyForMultiObject() not implemented for " + getClass());
    }

    protected String getConfirmMessageKeyForSingleObject() {
        throw new UnsupportedOperationException("getConfirmMessageKeyForSingleObject() not implemented for " + getClass());
    }

    public InlineMenuItem createDeleteInlineMenu() {
        return new InlineMenuItem(createStringResource("MainObjectListPanel.menu.delete")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<O>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        deleteConfirmedPerformed(target, getRowModel());
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("MainObjectListPanel.message.deleteAction").getString();
                return MainObjectListPanel.this.getConfirmationMessageModel((ColumnMenuAction) getAction(), actionName);
            }
        };
    }

    protected void objectDetailsPerformed(O object) {
        if (DetailsPageUtil.hasDetailsPage(object.getClass())) {
            DetailsPageUtil.dispatchToObjectDetailsPage(object.getClass(), object.getOid(), this, true);
        } else {
            error("Could not find proper response page");
            throw new RestartResponseException(getPageBase());
        }
    }

}
