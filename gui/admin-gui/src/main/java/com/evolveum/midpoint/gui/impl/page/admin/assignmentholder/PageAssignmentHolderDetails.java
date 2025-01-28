/*
 * Copyright (C) 2021-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.assignmentholder;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsFragment;
import com.evolveum.midpoint.gui.impl.page.admin.TemplateChoicePanel;
import com.evolveum.midpoint.gui.impl.page.admin.component.AssignmentHolderOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.gui.impl.util.ObjectCollectionViewUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.util.SerializableConsumer;
import com.evolveum.midpoint.web.model.PrismContainerValueWrapperModel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;

public abstract class PageAssignmentHolderDetails<AH extends AssignmentHolderType, AHDM extends AssignmentHolderDetailsModel<AH>>
        extends AbstractPageObjectDetails<AH, AHDM> {

    private static final Trace LOGGER = TraceManager.getTrace(PageAssignmentHolderDetails.class);
    protected static final String ID_TEMPLATE_VIEW = "templateView";
    protected static final String ID_TEMPLATE = "template";
    private static final String ID_WIZARD_FRAGMENT = "wizardFragment";
    private static final String ID_WIZARD = "wizard";

    private List<Breadcrumb> wizardBreadcrumbs = new ArrayList<>();
    private final boolean showTemplate;

    public PageAssignmentHolderDetails() {
        this(null, null);
    }

    public PageAssignmentHolderDetails(PageParameters pageParameters) {
        this(pageParameters, null);
    }

    public PageAssignmentHolderDetails(PrismObject<AH> assignmentHolder) {
        this(null, assignmentHolder);
    }

    private PageAssignmentHolderDetails(PageParameters pageParameters, PrismObject<AH> assignmentHolder) {
        super(pageParameters, assignmentHolder);
        showTemplate = assignmentHolder == null;
    }

    @Override
    protected void initLayout() {
        if (isApplicableTemplate()) {
            if (isAdd() && existMoreApplicableTemplate()) {
                Fragment templateFragment = createTemplateFragment();
                add(templateFragment);
                return;
            } else {
                if (isAdd()) {
                    Collection<CompiledObjectCollectionView> allApplicableArchetypeViews = findAllApplicableArchetypeViews();
                    if (allApplicableArchetypeViews.size() == 1) {
                        CompiledObjectCollectionView view = allApplicableArchetypeViews.iterator().next();
                        if (!view.isDefaultView()) {
                            applyTemplate(allApplicableArchetypeViews.iterator().next());
                        }
                    }
                }
            }
        }
        super.initLayout();
    }


    protected DetailsFragment createDetailsFragment() {
        if (canShowWizard()) {
            setShowedByWizard(true);
            return createWizardFragment();
        }

        return super.createDetailsFragment();
    }

    /**
     * Return DetailsFragment that contains wizard.
     */
    protected DetailsFragment createWizardFragment() {
        return super.createDetailsFragment();
    }

    /**
     * Define whether wizard will be showed, for current object.
     */
    protected boolean canShowWizard() {
        return false;
    }

    protected Fragment createTemplateFragment() {
        return new TemplateFragment(ID_DETAILS_VIEW, ID_TEMPLATE_VIEW, PageAssignmentHolderDetails.this);
    }

    private boolean existMoreApplicableTemplate() {
        Collection<CompiledObjectCollectionView> applicableArchetypes = findAllApplicableArchetypeViews();
        return applicableArchetypes.size() > 1;
    }

    private boolean isShowTemplateChoices() {
        return showTemplate;
    }

    /**
     * Method for if page support selecting of template (archetype) for type which page works.
     */
    protected boolean isApplicableTemplate() {
        return isShowTemplateChoices();
    }

    private class TemplateFragment extends Fragment {

        public TemplateFragment(String id, String markupId, MarkupContainer markupProvider) {
            super(id, markupId, markupProvider);
            setOutputMarkupId(true);
        }

        @Override
        protected void onInitialize() {
            super.onInitialize();
            initTemplateLayout();
        }

        protected void initTemplateLayout() {
            add(createTemplatePanel(ID_TEMPLATE));
        }
    }

    protected WebMarkupContainer createTemplatePanel(String id) {
        return new TemplateChoicePanel(id) {

            @Override
            protected Collection<CompiledObjectCollectionView> findAllApplicableArchetypeViews() {
                return PageAssignmentHolderDetails.this.findAllApplicableArchetypeViews();
            }

            @Override
            protected QName getType() {
                return ObjectTypes.getObjectType(PageAssignmentHolderDetails.this.getType()).getTypeQName();
            }

            @Override
            protected void onTemplateChosePerformed(CompiledObjectCollectionView collectionViews, AjaxRequestTarget target) {
                getWizardBreadcrumbs().clear();
                applyTemplate(collectionViews);

                Fragment fragment = createDetailsFragment();
                fragment.setOutputMarkupId(true);
                PageAssignmentHolderDetails.this.replace(fragment);
                target.add(fragment);
                target.add(getTitleContainer());
            }
        };
    }

    private void applyTemplate(CompiledObjectCollectionView collectionViews) {
        PrismObject<AH> assignmentHolder = getObjectDetailsModels().getObjectWrapper().getObjectOld();

        if (assignmentHolder == null) {
            try {
                assignmentHolder = getPrismContext().createObject(PageAssignmentHolderDetails.this.getType());
            } catch (Throwable e) {
                LOGGER.error("Cannot create prism object for {}. Using object from page model.", PageAssignmentHolderDetails.this.getType());
                assignmentHolder = getObjectDetailsModels().getObjectWrapperModel().getObject().getObjectOld().clone();
            }
        }
        List<ObjectReferenceType> archetypeRef = PageAssignmentHolderDetails.this.getArchetypeReferencesList(collectionViews);
        if (archetypeRef != null) {
            AssignmentHolderType holder = assignmentHolder.asObjectable();
            archetypeRef.forEach(a -> holder.getAssignment().add(ObjectTypeUtil.createAssignmentTo(a)));

        }

        reloadObjectDetailsModel(assignmentHolder);
    }

    protected List<ObjectReferenceType> getArchetypeReferencesList(CompiledObjectCollectionView collectionViews) {
        return ObjectCollectionViewUtil.getArchetypeReferencesList(collectionViews);
    }

    protected Collection<CompiledObjectCollectionView> findAllApplicableArchetypeViews() {
        return getCompiledGuiProfile().findAllApplicableArchetypeViews(getType(), OperationTypeType.ADD);
    }

    @Override
    protected AssignmentHolderOperationalButtonsPanel<AH> createButtonsPanel(String id, LoadableModel<PrismObjectWrapper<AH>> wrapperModel) {
        return new AssignmentHolderOperationalButtonsPanel<>(id, wrapperModel) {

            @Override
            protected void refresh(AjaxRequestTarget target) {
                PageAssignmentHolderDetails.this.refresh(target);
            }

            @Override
            protected void submitPerformed(AjaxRequestTarget target) {
                PageAssignmentHolderDetails.this.savePerformed(target);
            }

            @Override
            protected void backPerformed(AjaxRequestTarget target) {
                super.backPerformed(target);
                onBackPerform(target);
            }

            @Override
            protected void addButtons(RepeatingView repeatingView) {
                addAdditionalButtons(repeatingView);
            }

            @Override
            protected void deleteConfirmPerformed(AjaxRequestTarget target) {
                super.deleteConfirmPerformed(target);
                PageAssignmentHolderDetails.this.afterDeletePerformed(target);
            }

            @Override
            protected boolean hasUnsavedChanges(AjaxRequestTarget target) {
                return PageAssignmentHolderDetails.this.hasUnsavedChanges(target);
            }
        };
    }

    protected void afterDeletePerformed(AjaxRequestTarget target) {
    }

    protected void addAdditionalButtons(RepeatingView repeatingView) {
    }

    protected AHDM createObjectDetailsModels(PrismObject<AH> object) {
        //noinspection unchecked
        return (AHDM) new AssignmentHolderDetailsModel<>(createPrismObjectModel(object), this);
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        final IModel<String> defaultTitleModel = super.createPageTitleModel();
        return new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                String objectCollectionName = getObjectCollectionName();
                if (objectCollectionName != null) {
                    if (getObjectDetailsModels() != null && getObjectDetailsModels().getObjectStatus() == ItemStatus.ADDED) {
                        return createStringResource("PageAdminObjectDetails.title.new", objectCollectionName).getString();
                    }

                    String name = null;
                    if (getModelWrapperObject() != null && getModelWrapperObject().getObject() != null) {
                        name = WebComponentUtil.getName(getModelWrapperObject().getObject());
                    }

                    return createStringResource("PageAdminObjectDetails.title.edit.readonly.${readOnly}", getModel(), objectCollectionName, name).getString();
                }

                return defaultTitleModel.getObject();

            }
        };
    }

    protected String getObjectCollectionName() {
        if (getModelWrapperObject() == null || getModelWrapperObject().getObject() == null) {
            return null;
        }

        PrismObject<AH> assignmentHolderObj = getObjectForResolvingArchetypePolicyDisplayType();
        DisplayType displayType = GuiDisplayTypeUtil.getArchetypePolicyDisplayType(assignmentHolderObj, PageAssignmentHolderDetails.this);
        if (displayType == null || displayType.getLabel() == null) {
            return null;
        }

        String archetypeLocalizedName = getLocalizationService()
                .translate(displayType.getLabel().toPolyString(), WebComponentUtil.getCurrentLocale(), true);
        if (StringUtils.isNotEmpty(archetypeLocalizedName)) {
            return archetypeLocalizedName;
        }

        return null;
    }

    protected PrismObject<AH> getObjectForResolvingArchetypePolicyDisplayType() {
        return getModelWrapperObject().getObject();
    }

    public List<Breadcrumb> getWizardBreadcrumbs() {
        return wizardBreadcrumbs;
    }

    public boolean isShowByWizard() {
        return isShowedByWizard();
    }

    protected <C extends Containerable, P extends AbstractWizardPanel<C, AHDM>> P showWizard(
            AjaxRequestTarget target,
            ItemPath pathToValue,
            Class<P> clazz) {
        return showWizard(null, target, pathToValue, clazz);
    }

    protected <C extends Containerable, P extends AbstractWizardPanel<C, AHDM>> P showWizard(
            AjaxRequestTarget target,
            ItemPath pathToValue,
            Class<P> clazz,
            IModel<String> exitLabel) {
        return showWizard(null, target, pathToValue, clazz, exitLabel);
    }

    protected <C extends Containerable, P extends AbstractWizardPanel<C, AHDM>> P showWizard(
            PrismContainerValue<C> newValue,
            AjaxRequestTarget target,
            ItemPath pathToValue,
            Class<P> clazz) {
        return showWizard(newValue, target, pathToValue, clazz, null);
    }

    protected <C extends Containerable, P extends AbstractWizardPanel<C, AHDM>> P showWizard(
            PrismContainerValue<C> newValue,
            AjaxRequestTarget target,
            ItemPath pathToValue,
            Class<P> clazz,
            IModel<String> exitLabel) {

        setShowedByWizard(true);
        getObjectDetailsModels().saveDeltas();
        getObjectDetailsModels().reloadPrismObjectModel();

        IModel<PrismContainerValueWrapper<C>> valueModel = null;

        if (newValue != null) {
            valueModel = new LoadableModel<>(false) {
                @Override
                protected PrismContainerValueWrapper<C> load() {
                    try {
                        PrismContainerWrapper<C> container =
                                getObjectDetailsModels().getObjectWrapper().findContainer(pathToValue);
                        PrismContainerValueWrapper<C> newWrapper = WebPrismUtil.createNewValueWrapper(
                                container,
                                newValue,
                                PageAssignmentHolderDetails.this,
                                getObjectDetailsModels().createWrapperContext());
                        container.getValues().add(newWrapper);
                        return newWrapper;
                    } catch (SchemaException e) {
                        LOGGER.error("Couldn't resolve value for path: " + pathToValue);
                    }
                    return null;
                }
            };
        } else if (pathToValue != null) {
            valueModel = new LoadableModel<>(false) {
                @Override
                protected PrismContainerValueWrapper<C> load() {
                    try {
                        if (!ItemPath.isId(pathToValue.last())) {
                            PrismContainerWrapper<C> container =
                                    getObjectDetailsModels().getObjectWrapper().findContainer(pathToValue);
                            if (container.isMultiValue()) {
                                PrismContainerValue<C> value = container.getItem().createNewValue();
                                PrismContainerValueWrapper<C> newWrapper = WebPrismUtil.createNewValueWrapper(
                                        container,
                                        value,
                                        PageAssignmentHolderDetails.this,
                                        getObjectDetailsModels().createWrapperContext());
                                container.getValues().add(newWrapper);
                                return newWrapper;
                            }
                        }
                        return getModelWrapperObject().findContainerValue(pathToValue);
                    } catch (SchemaException e) {
                        LOGGER.error("Couldn't resolve value for path: " + pathToValue);
                    }
                    return null;
                }
            };
        }

        getFeedbackPanel().setVisible(false);
        Fragment fragment = new Fragment(ID_DETAILS_VIEW, ID_WIZARD_FRAGMENT, PageAssignmentHolderDetails.this);
        fragment.setOutputMarkupId(true);
        addOrReplace(fragment);

        try {
            Constructor<P> constructor = clazz.getConstructor(String.class, WizardPanelHelper.class);

            WizardPanelHelper<C, AHDM> helper = createContainerWizardHelper(valueModel);
            helper.setExitLabel(exitLabel);

            P wizard = constructor.newInstance(ID_WIZARD, helper);
            wizard.setOutputMarkupId(true);
            fragment.add(wizard);
            target.add(fragment);
            return wizard;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            LOGGER.error("Couldn't create panel by constructor for class " + clazz.getSimpleName()
                    + " with parameters type: String, WizardPanelHelper", e);
        }
        return null;
    }

    protected <C extends Containerable, P extends AbstractWizardPanel<C, AHDM>> P showWizardWithoutSave(
            IModel<PrismContainerValueWrapper<C>> valueModel,
            AjaxRequestTarget target,
            Class<P> clazz) {

        setShowedByWizard(true);

        getFeedbackPanel().setVisible(false);
        Fragment fragment = new Fragment(ID_DETAILS_VIEW, ID_WIZARD_FRAGMENT, PageAssignmentHolderDetails.this);
        fragment.setOutputMarkupId(true);
        addOrReplace(fragment);

        try {
            Constructor<P> constructor = clazz.getConstructor(String.class, WizardPanelHelper.class);
            P wizard = constructor.newInstance(ID_WIZARD, createContainerWizardHelperWithoutSave(valueModel));
            wizard.setOutputMarkupId(true);
            fragment.add(wizard);
            target.add(fragment);
            return wizard;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            LOGGER.error("Couldn't create panel by constructor for class " + clazz.getSimpleName()
                    + " with parameters type: String, WizardPanelHelper", e);
        }
        return null;
    }

    private <C extends Containerable> WizardPanelHelper<C, AHDM> createContainerWizardHelper(
            IModel<PrismContainerValueWrapper<C>> valueModel) {
        return new WizardPanelHelper<>(getObjectDetailsModels(), valueModel) {

            @Override
            public void onExitPerformed(AjaxRequestTarget target) {
                SerializableConsumer<AjaxRequestTarget> consumer = consumerTarget -> {
                    setShowedByWizard(false);
                    PrismObject<AH> oldObject = getObjectDetailsModels().getObjectWrapper().getObjectOld();
                    getObjectDetailsModels().reset();
                    getObjectDetailsModels().reloadPrismObjectModel(oldObject);
                    backToDetailsFromWizard(consumerTarget);
                    getWizardBreadcrumbs().clear();
                };

                checkDeltasExitPerformed(consumer, target);

            }

            @Override
            public OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
                OperationResult result = new OperationResult(OPERATION_SAVE);
                saveOrPreviewPerformed(target, result, false);
                if (!result.isError()) {
                    if (!isEditObject()) {
                        removeLastBreadcrumb();
                        String oid = getPrismObject().getOid();
                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, oid);
                        Class<? extends PageBase> page = DetailsPageUtil.getObjectDetailsPage(getType());
                        navigateToNext(page, parameters);
                        WebComponentUtil.createToastForCreateObject(target, getType());
                    } else {
                        WebComponentUtil.createToastForUpdateObject(target, getType());
                    }
                }
                return result;
            }
        };
    }

    private <C extends Containerable> WizardPanelHelper<C, AHDM> createContainerWizardHelperWithoutSave(
            IModel<PrismContainerValueWrapper<C>> valueModel) {
        return new WizardPanelHelper<>(getObjectDetailsModels(), valueModel) {

            @Override
            public void onExitPerformed(AjaxRequestTarget target) {
                setShowedByWizard(false);
                backToDetailsFromWizard(target);
                getWizardBreadcrumbs().clear();
                WebComponentUtil.showToastForRecordedButUnsavedChanges(target, valueModel.getObject());
            }

            @Override
            public OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
                return new OperationResult(OPERATION_SAVE);
            }
        };
    }

    protected WizardPanelHelper<AH, AHDM> createObjectWizardPanelHelper() {
        return new WizardPanelHelper<>(getObjectDetailsModels()) {

            @Override
            public void onExitPerformed(AjaxRequestTarget target) {
                SerializableConsumer<AjaxRequestTarget> consumer =
                        consumerTarget -> exitFromWizard();
                checkDeltasExitPerformed(consumer, target);
            }

            @Override
            public IModel<PrismContainerValueWrapper<AH>> getDefaultValueModel() {
                return PrismContainerValueWrapperModel.fromContainerWrapper(
                        getDetailsModel().getObjectWrapperModel(), ItemPath.EMPTY_PATH);
            }

            @Override
            public OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
                boolean isCreated = getPrismObject() == null || getPrismObject().getOid() == null;
                OperationResult result = new OperationResult(OPERATION_SAVE);
                saveOrPreviewPerformed(target, result, false);
                if (!result.isError()) {
                    if (isCreated) {
                        WebComponentUtil.createToastForCreateObject(target, getType());
                    } else {
                        WebComponentUtil.createToastForUpdateObject(target, getType());
                    }
                }
                return result;
            }
        };
    }

    protected void exitFromWizard() {
        navigateToNext(DetailsPageUtil.getObjectListPage(getType()));
    }

    public void checkDeltasExitPerformed(SerializableConsumer<AjaxRequestTarget> consumer, AjaxRequestTarget target) {

        if (!hasUnsavedChangesInWizard(target)) {
            consumer.accept(target);
            return;
        }
        ConfirmationPanel confirmationPanel = new ConfirmationPanel(getMainPopupBodyId(),
                createStringResource("OperationalButtonsPanel.confirmBack")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                consumer.accept(target);
            }
        };

        showMainPopup(confirmationPanel, target);
    }

    private void backToDetailsFromWizard(AjaxRequestTarget target) {
        DetailsFragment detailsFragment = createDetailsFragment();
        PageAssignmentHolderDetails.this.addOrReplace(detailsFragment);
        target.add(detailsFragment);

        getFeedbackPanel().setVisible(true);
    }
}
