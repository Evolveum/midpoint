package com.evolveum.midpoint.web.page.admin.shadows;

import java.util.*;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.component.PendingOperationPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.authentication.CompiledShadowCollectionView;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.ColumnTypeDto;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.data.column.ObjectLinkColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.DeleteConfirmationPanel;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public abstract class ShadowTablePanel extends MainObjectListPanel<ShadowType> {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowTablePanel.class);

    private static final String DOT_CLASS = ShadowTablePanel.class.getName() + ".";
    private static final String OPERATION_CHANGE_OWNER = DOT_CLASS + "changeOwner";
    private static final String OPERATION_LOAD_SHADOW_OWNER = DOT_CLASS + "loadOwner";
    private static final String OPERATION_UPDATE_STATUS = DOT_CLASS + "updateStatus";
    private static final String OPERATION_DELETE_OBJECT = DOT_CLASS + "deleteObject";
    private static final String OPERATION_IMPORT_OBJECT = DOT_CLASS + "importObject";
    private static final String OPERATION_MARK_PROTECTED = DOT_CLASS + "markProtectedShadow";


    public ShadowTablePanel(String id) {
        super(id, ShadowType.class);
    }

    public ShadowTablePanel(String id, Collection<SelectorOptions<GetOperationOptions>> options) {
        super(id, ShadowType.class, options);
    }

    public ShadowTablePanel(String id, Collection<SelectorOptions<GetOperationOptions>> options, ContainerPanelConfigurationType config) {
        super(id, ShadowType.class, options, config);
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
    protected void objectDetailsPerformed(AjaxRequestTarget target, ShadowType object) {
        shadowDetailsPerformed(target, WebComponentUtil.getName(object), object.getOid());
    }

    @Override
    protected boolean isCreateNewObjectEnabled() {
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

        items.add(new ButtonInlineMenuItem(createStringResource("pageContentAccounts.menu.importAccount"), true) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<ShadowType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target) {
                        if (getRowModel() == null) {
                            importResourceObject(null, target);
                        } else {
                            SelectableBeanImpl<ShadowType> shadow = getRowModel().getObject();
                            importResourceObject(shadow.getValue(), target);
                        }
                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_IMPORT_MENU_ITEM);
            }
        });

//        items.add(new InlineMenuItem());

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

        return items;
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

    private void shadowDetailsPerformed(AjaxRequestTarget target, String accountName, String accountOid) {
        if (StringUtils.isEmpty(accountOid)) {
            error(getString("pageContentAccounts.message.cantShowAccountDetails", accountName,
                    accountOid));
            target.add(getPageBase().getFeedbackPanel());
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
    protected void deleteResourceObjectPerformed(IModel<SelectableBean<ShadowType>> selected, AjaxRequestTarget target) {
        final List<SelectableBean<ShadowType>> selectedShadows = getSelectedShadowsList(selected);
        final OperationResult result = new OperationResult(OPERATION_DELETE_OBJECT);

        if (selectedShadows == null || selectedShadows.isEmpty()) {
            result.recordWarning(createStringResource("ResourceContentPanel.message.deleteResourceObjectPerformed.warning").getString());
            getPageBase().showResult(result);
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        ConfirmationPanel dialog = new DeleteConfirmationPanel(((PageBase) getPage()).getMainPopupBodyId(), createDeleteConfirmString(selectedShadows)) {
            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                deleteAccountConfirmedPerformed(target, result, selectedShadows);
            }
        };
        getPageBase().showMainPopup(dialog, target);

    }

    private void deleteAccountConfirmedPerformed(AjaxRequestTarget target, OperationResult result,
            List<SelectableBean<ShadowType>> selected) {
        Task task = getPageBase().createSimpleTask(OPERATION_DELETE_OBJECT);

        for (SelectableBean<ShadowType> shadow : selected) {
            try {
                ObjectDelta<ShadowType> deleteDelta = PrismContext.get().deltaFactory().object().createDeleteDelta(ShadowType.class,
                        shadow.getValue().getOid());

                getPageBase().getModelService().executeChanges(
                        MiscUtil.createCollection(deleteDelta), createModelExecuteOptions(), task, result);
            } catch (Throwable e) {
                result.recordPartialError("Could not delete " + shadow + ", reason: " + e.getMessage(), e);
                LOGGER.error("Could not delete {}, using option {}", shadow, null, e);
            }
        }

        result.computeStatusIfUnknown();
        getPageBase().showResult(result);
        refreshTable(target);
        target.add(getPageBase().getFeedbackPanel());

    }

    private IModel<String> createDeleteConfirmString(List<SelectableBean<ShadowType>> selectedShadow) {
        return () -> {
            GetOperationOptions rootOptions = SelectorOptions.findRootOptions(getOptions());
            String deleteIndication = "";
            if (rootOptions != null && BooleanUtils.isTrue(rootOptions.getNoFetch())) {
                deleteIndication = ".repo";
            }

            if (selectedShadow.size() == 1) {
                ShadowType first = selectedShadow.get(0).getValue();
                String name = WebComponentUtil.getName(first);
                return createStringResource("pageContentAccounts.message.deleteConfirmationSingle" + deleteIndication, name).getString();
            }
            return createStringResource("pageContentAccounts.message.deleteConfirmation" + deleteIndication, selectedShadow.size())
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

    private void markProtectedShadow(IModel<SelectableBean<ShadowType>> model, AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_MARK_PROTECTED);
        Task task = getPageBase().createSimpleTask(OPERATION_MARK_PROTECTED);

        var selected = getSelectedShadowsList(model);
        if (selected == null || selected.isEmpty()) {
            result.recordWarning(createStringResource("ResourceContentPanel.message.markShadowProtectedPerformed.warning").getString());
            getPageBase().showResult(result);
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        for (SelectableBean<ShadowType> shadow : selected) {
            try {
                var policyStat = new PolicyStatementType()
                        .markRef(SystemObjectsType.MARK_PROTECTED_SHADOW.value(), MarkType.COMPLEX_TYPE)
                        .type(PolicyStatementTypeType.APPLY);
                var delta = getPageBase().getPrismContext().deltaFactory().object()
                        .createModificationAddContainer(ShadowType.class,
                                shadow.getValue().getOid(),ShadowType.F_POLICY_STATEMENT,
                                policyStat);
                getPageBase().getModelService().executeChanges(
                        MiscUtil.createCollection(delta), null, task, result);
            } catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
                    | ExpressionEvaluationException | CommunicationException | ConfigurationException
                    | PolicyViolationException | SecurityViolationException e) {
                result.recordPartialError(
                        createStringResource(
                                        "ResourceContentPanel.message.markShadowProtectedPerformed.partialError", shadow)
                                .getString(),
                        e);
                LOGGER.error("Could not mark shadow {} as protected", shadow, e);
            }
        }

        result.computeStatusIfUnknown();
        getPageBase().showResult(result);
        refreshTable(target);
        target.add(getPageBase().getFeedbackPanel());
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

    protected ModelExecuteOptions createModelExecuteOptions() {
        return null;
    }

}
