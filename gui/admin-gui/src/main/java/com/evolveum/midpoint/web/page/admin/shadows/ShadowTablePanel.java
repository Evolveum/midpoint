package com.evolveum.midpoint.web.page.admin.shadows;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.component.PendingOperationPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.PageSimulationResultObjects;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.SimulationPage;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.TitleWithMarks;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.authentication.CompiledShadowCollectionView;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectOperationPolicyTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.DeleteConfirmationPanel;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.xml.namespace.QName;
import java.util.*;

public abstract class ShadowTablePanel extends MainObjectListPanel<ShadowType> {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowTablePanel.class);

    private static final String DOT_CLASS = ShadowTablePanel.class.getName() + ".";
    private static final String OPERATION_CHANGE_OWNER = DOT_CLASS + "changeOwner";
    private static final String OPERATION_LOAD_SHADOW_OWNER = DOT_CLASS + "loadOwner";
    private static final String OPERATION_UPDATE_STATUS = DOT_CLASS + "updateStatus";
    private static final String OPERATION_DELETE_OBJECT = DOT_CLASS + "deleteObject";
    private static final String OPERATION_DELETE_OBJECTS = DOT_CLASS + "deleteObjects";
    private static final String OPERATION_IMPORT_OBJECT = DOT_CLASS + "importObject";
    private static final String OPERATION_IMPORT_PREVIEW_OBJECT = DOT_CLASS + "importPreviewObject";
    private static final String OPERATION_MARK_SHADOW = DOT_CLASS + "markShadow";

    public ShadowTablePanel(String id) {
        super(id, ShadowType.class);
    }

    public ShadowTablePanel(String id, ContainerPanelConfigurationType config) {
        super(id, ShadowType.class, config);
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        return createRowMenuItems();
    }

    @Override
    protected List<IColumn<SelectableBean<ShadowType>, String>> createDefaultColumns() {
        return initColumns();
    }

    @Override
    protected void objectDetailsPerformed(ShadowType object) {
        shadowDetailsPerformed(WebComponentUtil.getName(object), object.getOid());
    }

    @Override
    protected boolean isObjectDetailsEnabled(IModel<SelectableBean<ShadowType>> rowModel) {
        return isShadowDetailsEnabled(rowModel);
    }

    protected boolean isShadowDetailsEnabled(IModel<SelectableBean<ShadowType>> rowModel) {
        return true;
    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return false;
    }

    @Override
    public CompiledObjectCollectionView getObjectCollectionView() {
        CompiledShadowCollectionView compiledView = findContainerPanelConfig();
        if (compiledView != null) {
            return compiledView;
        }
        return super.getObjectCollectionView();
    }

    protected CompiledShadowCollectionView findContainerPanelConfig() {
        return null;
    }

    private List<InlineMenuItem> createRowMenuItems() {
        List<InlineMenuItem> items = new ArrayList<>();

        items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.enableAccount"), true) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<ShadowType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {
                        updateResourceObjectStatusPerformed(getRowModel(), target, true);
                    }
                };
            }
        });

        items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.disableAccount"), true) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<ShadowType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {
                        updateResourceObjectStatusPerformed(getRowModel(), target, false);
                    }
                };
            }
        });

        items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.deleteAccount"), true) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<ShadowType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {
                        deleteResourceObjectPerformed(getRowModel(), target);
                    }
                };
            }
        });

        items.add(new InlineMenuItem(createStringResource("ShadowTablePanel.menu.importPreviewAccount"), true) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<ShadowType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            return;
                        }

                        SelectableBeanImpl<ShadowType> shadow = getRowModel().getObject();
                        importPreviewResourceObject(shadow.getValue(), target);
                    }
                };
            }
        });

        items.add(new ButtonInlineMenuItem(createStringResource("pageContentAccounts.menu.importAccount"), true) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<ShadowType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {
                        ShadowType shadow = null;
                        IModel<SelectableBeanImpl<ShadowType>> model = getRowModel();
                        if (model != null) {
                            shadow = model.getObject().getValue();
                        }

                        importResourceObject(shadow, target);
                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_IMPORT_MENU_ITEM);
            }
        });

        items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.removeOwner"), true) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<ShadowType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {
                        changeOwner(getRowModel(), target, null, true);
                    }
                };
            }
        });

        items.add(new ButtonInlineMenuItem(createStringResource("pageContentAccounts.menu.changeOwner"), true) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<ShadowType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {
                        ObjectBrowserPanel<FocusType> browser = new ObjectBrowserPanel<>(
                                getPageBase().getMainPopupBodyId(), UserType.class,
                                WebComponentUtil.createFocusTypeList(), false, getPageBase()) {

                            @Override
                            protected void onSelectPerformed(AjaxRequestTarget target, FocusType focus) {
                                changeOwner(getRowModel(), target, focus, false);
                            }

                        };

                        getPageBase().showMainPopup(browser, target);

                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_RECONCILE_MENU_ITEM);
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }

        });

        items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.markProtected"), true) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<ShadowType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {
                        markProtectedShadow(getRowModel(), target);
                    }
                };
            }
        });

        items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.mark"), true) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<ShadowType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {

                        ObjectFilter marksFilter = PrismContext.get().queryFor(MarkType.class)
                                .item(MarkType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF)
                                .ref(SystemObjectsType.ARCHETYPE_OBJECT_MARK.value())
                                .buildFilter();

                        ObjectBrowserPanel<MarkType> browser = new ObjectBrowserPanel<>(
                                getPageBase().getMainPopupBodyId(), MarkType.class,
                                Collections.singletonList(MarkType.COMPLEX_TYPE), true, getPageBase(), marksFilter) {

                            protected void addPerformed(AjaxRequestTarget target, QName type, List<MarkType> selected) {
                                LOGGER.debug("Selected marks: {}", selected);

                                List<String> markOids = Lists.transform(selected, MarkType::getOid);
                                markShadows(getRowModel(), markOids, target);
                                super.addPerformed(target, type, selected);
                            }

                            public org.apache.wicket.model.StringResourceModel getTitle() {
                                return createStringResource("pageContentAccounts.menu.mark.select");
                            }
                        };

                        getPageBase().showMainPopup(browser, target);
                    }
                };
            }
        });

        items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.mark.remove"), true) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<ShadowType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {

                        var selected = getSelectedShadowsList(getRowModel());
                        var selectedMarks = collectExistingMarks(selected);

                        ObjectFilter marksFilter = PrismContext.get().queryFor(MarkType.class)
                                .item(MarkType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF)
                                .ref(SystemObjectsType.ARCHETYPE_OBJECT_MARK.value())
                                .and()
                                .id(selectedMarks)
                                .buildFilter();

                        ObjectBrowserPanel<MarkType> browser = new ObjectBrowserPanel<>(
                                getPageBase().getMainPopupBodyId(), MarkType.class,
                                Collections.singletonList(MarkType.COMPLEX_TYPE), true, getPageBase(), marksFilter) {

                            protected void addPerformed(AjaxRequestTarget target, QName type, List<MarkType> selected) {
                                LOGGER.warn("Selected marks: {}", selected);

                                List<String> markOids = Lists.transform(selected, MarkType::getOid);
                                removeShadowMarks(getRowModel(), markOids, target);
                                super.addPerformed(target, type, selected);
                            }

                            public org.apache.wicket.model.StringResourceModel getTitle() {
                                return createStringResource("pageContentAccounts.menu.mark.select.remove");
                            }

                            protected org.apache.wicket.model.StringResourceModel getAddButtonTitle() {
                                return createStringResource("pageContentAccounts.menu.mark.remove");
                            };
                        };

                        getPageBase().showMainPopup(browser, target);
                    }

                };
            }
        });

        return items;
    }

    @Override
    protected IColumn<SelectableBean<ShadowType>, String> createNameColumn(IModel<String> displayModel, GuiObjectColumnType customColumn, ExpressionType expression) {
        return new ContainerableNameColumn<>(displayModel, ObjectType.F_NAME.getLocalPart(), customColumn, expression, getPageBase()) {

            @Override
            protected IModel<String> getContainerName(SelectableBean<ShadowType> rowModel) {
                ShadowType value = rowModel.getValue();
                return Model.of(value == null ? "" : WebComponentUtil.getName(value, true));
            }

            @Override
            protected Component createComponent(String componentId, IModel<String> labelModel, IModel<SelectableBean<ShadowType>> rowModel) {
                IModel<String> marks = new LoadableDetachableModel<>() {

                    @Override
                    protected String load() {
                        ShadowType shadow = rowModel.getObject().getValue();
                        if (shadow == null) {
                            return null;
                        }

                        List<ObjectReferenceType> refs = shadow.getEffectiveMarkRef();
                        Object[] marks = refs.stream()
                                .map(ref -> WebModelServiceUtils.loadObject(ref, getPageBase()))
                                .filter(mark -> mark != null)
                                .map(mark -> WebComponentUtil.getDisplayNameOrName(mark))
                                .toArray();

                        return StringUtils.joinWith(", ", marks);
                    }
                };
                return new TitleWithMarks(componentId, labelModel, marks) {

                    @Override
                    protected void onTitleClicked(AjaxRequestTarget target) {
                        ShadowType object = rowModel.getObject().getValue();
                        if (object == null) {
                            return;
                        }
                        objectDetailsPerformed(object);
                    }

                    @Override
                    protected boolean isTitleLinkEnabled() {
                        return isObjectDetailsEnabled(rowModel);
                    }
                };
            }
        };
    }

    private List<IColumn<SelectableBean<ShadowType>, String>> initColumns() {

        List<ColumnTypeDto<String>> columnDefs = Arrays.asList(
                new ColumnTypeDto<>("ShadowType.synchronizationSituation",
                        SelectableBeanImpl.F_VALUE + ".synchronizationSituation",
                        ShadowType.F_SYNCHRONIZATION_SITUATION.getLocalPart()),
                new ColumnTypeDto<>("ShadowType.intent", SelectableBeanImpl.F_VALUE + ".intent",
                        ShadowType.F_INTENT.getLocalPart()));

        List<IColumn<SelectableBean<ShadowType>, String>> columns = new ArrayList<>();

        IColumn<SelectableBean<ShadowType>, String> identifiersColumn = new AbstractColumn<>(
                createStringResource("pageContentAccounts.identifiers")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ShadowType>>> cellItem,
                                     String componentId, IModel<SelectableBean<ShadowType>> rowModel) {

                SelectableBean<ShadowType> dto = rowModel.getObject();
                RepeatingView repeater = new RepeatingView(componentId);

                ShadowType value = dto.getValue();
                if (value != null) {
                    for (ResourceAttribute<?> attr : ShadowUtil.getAllIdentifiers(value)) {
                        repeater.add(new Label(repeater.newChildId(),
                                attr.getElementName().getLocalPart() + ": " + attr.getRealValue()));

                    }
                }
                cellItem.add(repeater);

            }
        };
        columns.add(identifiersColumn);

        columns.addAll(ColumnUtils.createColumns(columnDefs));

        ObjectLinkColumn<SelectableBean<ShadowType>> ownerColumn = new ObjectLinkColumn<>(
                createStringResource("pageContentAccounts.owner")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<FocusType> createLinkModel(final IModel<SelectableBean<ShadowType>> rowModel) {

                return new IModel<>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public FocusType getObject() {
                        return loadShadowOwner(rowModel);
                    }

                };
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<ShadowType>> rowModel,
                                ObjectType targetObjectType) {
                ownerDetailsPerformed((FocusType) targetObjectType);
            }

            @Override
            public boolean isEnabled(IModel<SelectableBean<ShadowType>> rowModel) {
                return ShadowTablePanel.this.isShadowDetailsEnabled(rowModel);
            }
        };
        columns.add(ownerColumn);

        columns.add(new AbstractColumn<>(
                createStringResource("PageAccounts.accounts.pendingOperations")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ShadowType>>> cellItem,
                                     String componentId, IModel<SelectableBean<ShadowType>> rowModel) {
                cellItem.add(new PendingOperationPanel(componentId,
                        new PropertyModel<>(rowModel, SelectableBeanImpl.F_VALUE + "." + ShadowType.F_PENDING_OPERATION.getLocalPart())));
            }
        });
        return columns;
    }

    private void shadowDetailsPerformed(String accountName, String accountOid) {
        if (StringUtils.isEmpty(accountOid)) {
            error(getString("pageContentAccounts.message.cantShowAccountDetails", accountName,
                    accountOid));
//            target.add(getPageBase().getFeedbackPanel()); //TODO when this can happen?
            return;
        }

        WebComponentUtil.dispatchToObjectDetailsPage(ShadowType.class, accountOid, this, false);
    }

    private <F extends FocusType> F loadShadowOwner(IModel<SelectableBean<ShadowType>> model) {
        ShadowType shadow = model.getObject().getValue();
        String shadowOid;
        if (shadow != null) {
            shadowOid = shadow.getOid();
        } else {
            return null;
        }

        return loadShadowOwner(shadowOid);
    }

    private <F extends FocusType> F loadShadowOwner(String shadowOid) {

        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_SHADOW_OWNER);
        OperationResult result = new OperationResult(OPERATION_LOAD_SHADOW_OWNER);

        try {
            PrismObject<? extends FocusType> prismOwner = getPageBase().getModelService()
                    .searchShadowOwner(shadowOid, null, task, result);

            if (prismOwner != null) {
                //noinspection unchecked
                return (F) prismOwner.asObjectable();
            }
        } catch (ObjectNotFoundException exception) {
            // owner was not found, it's possible, and it's ok on unlinked
            // accounts
        } catch (Exception ex) {
            result.recordFatalError(getString("PageAccounts.message.ownerNotFound", shadowOid), ex);
            LoggingUtils.logUnexpectedException(LOGGER,
                    "Could not load owner of account with oid: " + shadowOid, ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (WebComponentUtil.showResultInPage(result)) {
            getPageBase().showResult(result, false);
        }

        return null;
    }

    private void ownerDetailsPerformed(FocusType owner) {
        if (owner == null) {
            return;
        }
        WebComponentUtil.dispatchToObjectDetailsPage(owner.getClass(), owner.getOid(), this, true);
    }

    protected void importPreviewResourceObject(ShadowType selected, AjaxRequestTarget target) {
        PageBase page = getPageBase();
        page.showMainPopup(new ChooseTaskExecutionModePopup(getPageBase().getMainPopupBodyId()) {

            @Override
            protected void onSelectPerformed(AjaxRequestTarget target, TaskExecutionMode mode) {
                importPreviewResourceObjectConfirmed(mode, selected, target);
            }
        }, target);
    }

    protected void importPreviewResourceObjectConfirmed(TaskExecutionMode mode, ShadowType selected, AjaxRequestTarget target) {
        PageBase page = getPageBase();

        Task task = page.createSimpleTask(OPERATION_IMPORT_PREVIEW_OBJECT);
        OperationResult opResult = task.getResult();
        try {
            String resultOid = page.getModelInteractionService().executeWithSimulationResult(
                    mode,
                    null,
                    task,
                    opResult,
                    () -> {
                        page.getModelService().importFromResource(selected.getOid(), task, opResult);

                        return task.getSimulationTransaction().getResultOid();
                    });

            PageParameters params = new PageParameters();
            params.set(SimulationPage.PAGE_PARAMETER_RESULT_OID, resultOid);

            page.navigateToNext(PageSimulationResultObjects.class, params);
        } catch (CommonException ex) {
            opResult.computeStatusIfUnknown();
            opResult.recordFatalError("Couldn't simulate import shadow", ex);

            page.showResult(opResult);
            target.add(page.getFeedbackPanel());
        }
    }

    //operations
    protected void importResourceObject(ShadowType selected, AjaxRequestTarget target) {
        List<ShadowType> selectedShadows;
        if (selected != null) {
            selectedShadows = new ArrayList<>();
            selectedShadows.add(selected);
        } else {
            selectedShadows = getSelectedRealObjects();
        }

        OperationResult result = new OperationResult(OPERATION_IMPORT_OBJECT);
        Task task = getPageBase().createSimpleTask(OPERATION_IMPORT_OBJECT);

        if (selectedShadows == null || selectedShadows.isEmpty()) {
            result.recordWarning(createStringResource("ResourceContentPanel.message.importResourceObject.warning").getString());
            getPageBase().showResult(result);
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        for (ShadowType shadow : selectedShadows) {
            try {
                getPageBase().getModelService().importFromResource(shadow.getOid(), task, result);
            } catch (Exception e) {
                result.recordPartialError(createStringResource("ResourceContentPanel.message.importResourceObject.partialError", shadow).getString(), e);
                LOGGER.error("Could not import account {} ", shadow, e);
            }
        }

        result.computeStatusIfUnknown();
        getPageBase().showResult(result);
        refreshTable(target);
        target.add(getPageBase().getFeedbackPanel());
    }

    protected void updateResourceObjectStatusPerformed(IModel<SelectableBean<ShadowType>> selected, AjaxRequestTarget target,
                                                       boolean enabled) {
        List<SelectableBean<ShadowType>> selectedShadow = getSelectedShadowsList(selected);

        OperationResult result = new OperationResult(OPERATION_UPDATE_STATUS);
        Task task = getPageBase().createSimpleTask(OPERATION_UPDATE_STATUS);

        if (selectedShadow == null || selectedShadow.isEmpty()) {
            result.recordWarning(createStringResource("ResourceContentPanel.message.updateResourceObjectStatusPerformed.warning").getString());
            getPageBase().showResult(result);
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        for (SelectableBean<ShadowType> selectableShadow : selectedShadow) {
            ShadowType shadow = selectableShadow.getValue();
            ActivationStatusType status = enabled ? ActivationStatusType.ENABLED
                    : ActivationStatusType.DISABLED;
            try {
                ObjectDelta<ShadowType> deleteDelta = getPageBase().getPrismContext().deltaFactory().object().createModificationReplaceProperty(
                        ShadowType.class, shadow.getOid(),
                        SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                        status);
                getPageBase().getModelService().executeChanges(
                        MiscUtil.createCollection(deleteDelta), null, task, result);
            } catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
                     | ExpressionEvaluationException | CommunicationException | ConfigurationException
                     | PolicyViolationException | SecurityViolationException e) {
                result.recordPartialError(
                        createStringResource(
                                "ResourceContentPanel.message.updateResourceObjectStatusPerformed.partialError", status, shadow)
                                .getString(),
                        e);
                LOGGER.error("Could not update status (to {}) for {}, using option {}",
                        status, shadow, null, e);
            }
        }

        result.computeStatusIfUnknown();
        getPageBase().showResult(result);
        refreshTable(target);
        target.add(getPageBase().getFeedbackPanel());

    }

    // TODO: as a task?
    private void deleteResourceObjectPerformed(IModel<SelectableBean<ShadowType>> selected, AjaxRequestTarget target) {
        List<SelectableBean<ShadowType>> selectedShadows = getSelectedShadowsList(selected);
        OperationResult result = new OperationResult(OPERATION_DELETE_OBJECTS);

        if (selectedShadows == null || selectedShadows.isEmpty()) {
            result.recordWarning(
                    createStringResource("ResourceContentPanel.message.deleteResourceObjectPerformed.warning")
                            .getString());
            getPageBase().showResult(result);
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        ConfirmationPanel dialog = new DeleteConfirmationPanel(
                ((PageBase) getPage()).getMainPopupBodyId(), createDeleteConfirmString(selectedShadows)) {
            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                deleteAccountsConfirmedPerformed(target, selectedShadows, result);
            }
        };
        getPageBase().showMainPopup(dialog, target);

    }

    private void deleteAccountsConfirmedPerformed(
            AjaxRequestTarget target, List<SelectableBean<ShadowType>> selected, OperationResult parentResult) {
        Task task = getPageBase().createSimpleTask(OPERATION_DELETE_OBJECTS); // created here because of serializability issues

        for (SelectableBean<ShadowType> shadowBean : selected) {
            ShadowType shadow = shadowBean.getValue();
            ModelExecuteOptions options = createModelExecuteOptions();
            var result = parentResult.subresult(OPERATION_DELETE_OBJECT)
                    .addArbitraryObjectAsParam("object", shadow)
                    .build();
            try {
                // Preliminary solution for MID-8601. Here we assume the shadow marks are visible by GUI.
                var policy = getPageBase().getObjectOperationPolicyHelper().getEffectivePolicy(shadow, result);
                var severity = ObjectOperationPolicyTypeUtil.getDeletionRestrictionSeverity(policy);
                if (severity == null) { // i.e. permitted
                    ObjectDelta<ShadowType> deleteDelta =
                            PrismContext.get().deltaFactory().object().createDeleteDelta(
                                    ShadowType.class, shadow.getOid());
                    getPageBase().getModelService().executeChanges(
                            MiscUtil.createCollection(deleteDelta), options, task, result);
                } else {
                    result.setStatus(OperationResultStatus.forViolationSeverity(severity));
                    result.setUserFriendlyMessage(
                            new SingleLocalizableMessage(
                                    "ShadowTablePanel.message.deletionForbidden",
                                    new Object[]{shadow.getName()}));
                }
            } catch (Throwable e) {
                result.recordPartialError("Could not delete " + shadow + ", reason: " + e.getMessage(), e);
                LOGGER.error("Could not delete {}, using option {}", shadow, options, e);
            } finally {
                result.close();
            }
        }

        parentResult.computeStatusIfUnknown();
        getPageBase().showResult(parentResult);
        refreshTable(target);
        target.add(getPageBase().getFeedbackPanel());
    }

    private IModel<String> createDeleteConfirmString(List<SelectableBean<ShadowType>> selectedShadow) {
        return () -> {
//            GetOperationOptions rootOptions = SelectorOptions.findRootOptions(getOptions());
//            String deleteIndication = "";
//            if (rootOptions != null && BooleanUtils.isTrue(rootOptions.getNoFetch())) {
//                deleteIndication = ".repo";
//            }

            if (selectedShadow.size() == 1) {
                ShadowType first = selectedShadow.get(0).getValue();
                String name = WebComponentUtil.getName(first);
                return createStringResource("pageContentAccounts.message.deleteConfirmationSingle", name).getString();
            }
            return createStringResource("pageContentAccounts.message.deleteConfirmation", selectedShadow.size())
                    .getString();
        };
    }

    private void changeOwner(IModel<SelectableBean<ShadowType>> selected, AjaxRequestTarget target, FocusType ownerToChange,
                             boolean remove) {

        getPageBase().hideMainPopup(target);

        List<SelectableBean<ShadowType>> selectedShadow = getSelectedShadowsList(selected);
        if (remove) {
            for (SelectableBean<ShadowType> shadow : selectedShadow) {
                removeShadowOwner(shadow, target);
            }
            return;
        }

        if (!isSatisfyConstraints(selectedShadow)) {
            return;
        }

        SelectableBean<ShadowType> shadow = selectedShadow.iterator().next();
        removeShadowOwner(shadow, target);
        setNewShadowOwner(ownerToChange, shadow, target);

    }

    private void removeShadowOwner(SelectableBean<ShadowType> selectableShadow, AjaxRequestTarget target) {
        ShadowType shadow = selectableShadow.getValue();
        FocusType owner = loadShadowOwner(shadow.getOid());
        if (owner == null) {
            return;
        }

        ReferenceDelta delta = getPageBase().getPrismContext().deltaFactory().reference().createModificationDelete(FocusType.F_LINK_REF,
                getFocusDefinition(),
                ObjectTypeUtil.createObjectRef(shadow, PrismContext.get()).asReferenceValue());
        changeOwnerInternal(owner.getOid(), owner.getClass(), Collections.singletonList(delta), target);
    }

    private <F extends FocusType> void setNewShadowOwner(F ownerToChange, SelectableBean<ShadowType> selectableShadow, AjaxRequestTarget target) {
        ShadowType shadow = selectableShadow.getValue();
        ReferenceDelta delta = getPageBase().getPrismContext().deltaFactory().reference().createModificationAdd(FocusType.F_LINK_REF, getFocusDefinition(),
                ObjectTypeUtil.createObjectRef(shadow, PrismContext.get()).asReferenceValue());
        changeOwnerInternal(ownerToChange.getOid(), ownerToChange.getClass(), Collections.singletonList(delta), target);
    }

    private void removeShadowMarks(IModel<SelectableBean<ShadowType>> rowModel, List<String> markOids,
            AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_MARK_SHADOW);
        Task task = getPageBase().createSimpleTask(OPERATION_MARK_SHADOW);

        var selected = getSelectedShadowsList(rowModel);
        if (selected == null || selected.isEmpty()) {
            result.recordWarning(createStringResource("ResourceContentPanel.message.markShadowPerformed.warning").getString());
            getPageBase().showResult(result);
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        for (SelectableBean<ShadowType> shadow : selected) {
            List<PolicyStatementType> statements = new ArrayList<>();
            // We recreate statements (can not reuse them between multiple objects - we can create new or clone
            // but for each delta we need separate statement

            for (var statement : shadow.getValue().getPolicyStatement()) {
                if (!PolicyStatementTypeType.APPLY.equals(statement.getType())) {
                    continue;
                }
                if (markOids.contains(statement.getMarkRef().getOid())) {
                    statements.add(statement.clone());
                }
            }
            try {
                var delta = getPageBase().getPrismContext().deltaFactory().object()
                        .createModificationDeleteContainer(ShadowType.class,
                                shadow.getValue().getOid(), ShadowType.F_POLICY_STATEMENT,
                                statements.toArray(new PolicyStatementType[0]));
                getPageBase().getModelService().executeChanges(MiscUtil.createCollection(delta), null, task, result);
            } catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
                    | ExpressionEvaluationException | CommunicationException | ConfigurationException
                    | PolicyViolationException | SecurityViolationException e) {
                result.recordPartialError(
                        createStringResource(
                                "ResourceContentPanel.message.markShadowPerformed.partialError", shadow)
                                .getString(),
                        e);
                LOGGER.error("Could not mark shadow {} with marks {}", shadow, markOids, e);
            }
        }

        result.computeStatusIfUnknown();
        getPageBase().showResult(result);
        refreshTable(target);
        target.add(getPageBase().getFeedbackPanel());
    }


    private void markShadows(IModel<SelectableBean<ShadowType>> rowModel, List<String> markOids,
                             AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_MARK_SHADOW);
        Task task = getPageBase().createSimpleTask(OPERATION_MARK_SHADOW);

        var selected = getSelectedShadowsList(rowModel);
        if (selected == null || selected.isEmpty()) {
            result.recordWarning(createStringResource("ResourceContentPanel.message.markShadowPerformed.warning").getString());
            getPageBase().showResult(result);
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        for (SelectableBean<ShadowType> shadow : selected) {
            List<PolicyStatementType> statements = new ArrayList<>();
            // We recreate statements (can not reuse them between multiple objects - we can create new or clone
            // but for each delta we need separate statement
            for (String oid : markOids) {
                statements.add(new PolicyStatementType().markRef(oid, MarkType.COMPLEX_TYPE)
                        .type(PolicyStatementTypeType.APPLY));
            }
            try {
                var delta = getPageBase().getPrismContext().deltaFactory().object()
                        .createModificationAddContainer(ShadowType.class,
                                shadow.getValue().getOid(), ShadowType.F_POLICY_STATEMENT,
                                statements.toArray(new PolicyStatementType[0]));
                getPageBase().getModelService().executeChanges(MiscUtil.createCollection(delta), null, task, result);
            } catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
                     | ExpressionEvaluationException | CommunicationException | ConfigurationException
                     | PolicyViolationException | SecurityViolationException e) {
                result.recordPartialError(
                        createStringResource(
                                "ResourceContentPanel.message.markShadowPerformed.partialError", shadow)
                                .getString(),
                        e);
                LOGGER.error("Could not mark shadow {} with marks {}", shadow, markOids, e);
            }
        }

        result.computeStatusIfUnknown();
        getPageBase().showResult(result);
        refreshTable(target);
        target.add(getPageBase().getFeedbackPanel());
    }

    private void markProtectedShadow(IModel<SelectableBean<ShadowType>> model, AjaxRequestTarget target) {
        markShadows(model, Collections.singletonList(SystemObjectsType.MARK_PROTECTED.value()), target);
    }

    private boolean isSatisfyConstraints(List selected) {
        if (selected.size() > 1) {
            error("Could not link to more than one owner");
            return false;
        }

        if (selected.isEmpty()) {
            warn("Could not link to more than one owner");
            return false;
        }

        return true;
    }

    private void changeOwnerInternal(String ownerOid, Class<? extends FocusType> ownerType, Collection<? extends ItemDelta> modifications,
                                     AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_CHANGE_OWNER);
        Task task = getPageBase().createSimpleTask(OPERATION_CHANGE_OWNER);
        ObjectDelta<? extends ObjectType> objectDelta =
                getPageBase().getPrismContext().deltaFactory().object()
                        .createModifyDelta(ownerOid, modifications, ownerType);
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        deltas.add(objectDelta);
        try {
            getPageBase().getModelService().executeChanges(deltas, null, task, result);

        } catch (Throwable e) {
            result.recordFatalError("Cannot change owner.");
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot change owner, {}", e, e.getMessage());
        }

        result.computeStatusIfUnknown();

        getPageBase().showResult(result);
        target.add(getPageBase().getFeedbackPanel());
        refreshTable(target);
    }

    private List<SelectableBean<ShadowType>> getSelectedShadowsList(IModel<SelectableBean<ShadowType>> selectedShadow) {
        if (selectedShadow != null) {
            return Collections.singletonList(selectedShadow.getObject());
        }
        return getSelectedObjects();
    }

    private PrismObjectDefinition<FocusType> getFocusDefinition() {
        return getPageBase().getPrismContext().getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(FocusType.class);
    }


    private String[] collectExistingMarks(List<SelectableBean<ShadowType>> selected) {
        Set<String> marks = new HashSet<>();
        for (SelectableBean<ShadowType> shadow : selected) {
            for (var statement : shadow.getValue().getPolicyStatement()) {
                if (PolicyStatementTypeType.APPLY.equals(statement.getType())) {
                    marks.add(statement.getMarkRef().getOid());
                }
            }
        }
        return marks.toArray(new String[] {});
    }

    protected ModelExecuteOptions createModelExecuteOptions() {
        return null;
    }

}
