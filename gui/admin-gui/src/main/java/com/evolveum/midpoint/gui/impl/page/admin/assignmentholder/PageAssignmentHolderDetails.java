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

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsFragment;
import com.evolveum.midpoint.gui.impl.page.admin.TemplateChoicePanel;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;

import com.evolveum.midpoint.web.model.ContainerValueWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.web.session.ObjectDetailsStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.component.AssignmentHolderOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.util.ObjectCollectionViewUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public abstract class PageAssignmentHolderDetails<AH extends AssignmentHolderType, AHDM extends AssignmentHolderDetailsModel<AH>>
        extends AbstractPageObjectDetails<AH, AHDM> {

    private static final Trace LOGGER = TraceManager.getTrace(PageAssignmentHolderDetails.class);
    protected static final String ID_TEMPLATE_VIEW = "templateView";
    protected static final String ID_TEMPLATE = "template";
    private static final String ID_WIZARD_FRAGMENT = "wizardFragment";
    private static final String ID_WIZARD = "wizard";

    private List<Breadcrumb> wizardBreadcrumbs = new ArrayList<>();

    public PageAssignmentHolderDetails() {
        super();
    }

    public PageAssignmentHolderDetails(PageParameters pageParameters) {
        super(pageParameters);
    }

    public PageAssignmentHolderDetails(PrismObject<AH> assignmentHolder) {
        super(assignmentHolder);
    }

    @Override
    protected void initLayout() {
        if (isAdd() && isApplicableTemplate()) {
            Fragment templateFragment = createTemplateFragment();
            add(templateFragment);
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
            super.initLayout();
        }
    }

    protected Fragment createTemplateFragment() {
        return new TemplateFragment(ID_DETAILS_VIEW, ID_TEMPLATE_VIEW, PageAssignmentHolderDetails.this);
    }

    protected boolean isApplicableTemplate() {
        Collection<CompiledObjectCollectionView> applicableArchetypes = findAllApplicableArchetypeViews();
        return applicableArchetypes.size() > 1;
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
        PrismObject<AH> assignmentHolder;
        try {
            assignmentHolder = getPrismContext().createObject(PageAssignmentHolderDetails.this.getType());
        } catch (Throwable e) {
            LOGGER.error("Cannot create prism object for {}. Using object from page model.", PageAssignmentHolderDetails.this.getType());
            assignmentHolder = getObjectDetailsModels().getObjectWrapperModel().getObject().getObjectOld().clone();
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
            protected void savePerformed(AjaxRequestTarget target) {
                PageAssignmentHolderDetails.this.savePerformed(target);
            }

            @Override
            protected boolean hasUnsavedChanges(AjaxRequestTarget target) {
                return PageAssignmentHolderDetails.this.hasUnsavedChanges(target);
            }
        };
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

    private String getObjectCollectionName() {
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

    protected <C extends Containerable, P extends AbstractWizardPanel<C, AHDM>> P showWizard(
            AjaxRequestTarget target,
            ItemPath pathToValue,
            Class<P> clazz) {

        setShowedByWizard(true);
        getObjectDetailsModels().saveDeltas();
        PrismObject<AH> oldObject = getObjectDetailsModels().getObjectWrapper().getObjectOld();
        getObjectDetailsModels().reset();
        getObjectDetailsModels().reloadPrismObjectModel(oldObject);

        IModel<PrismContainerValueWrapper<C>> valueModel = null;

        if (pathToValue != null) {
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
            P wizard = constructor.newInstance(ID_WIZARD, createContainerWizardHelper(valueModel));
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
                setShowedByWizard(false);
                PrismObject<AH> oldObject = getObjectDetailsModels().getObjectWrapper().getObjectOld();
                getObjectDetailsModels().reset();
                getObjectDetailsModels().reloadPrismObjectModel(oldObject);
                backToDetailsFromWizard(target);
                getWizardBreadcrumbs().clear();
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
                        Class<? extends PageBase> page = WebComponentUtil.getObjectDetailsPage(getType());
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

    protected WizardPanelHelper<AH, AHDM> createObjectWizardPanelHelper() {
        return new WizardPanelHelper<>(getObjectDetailsModels()) {

            @Override
            public void onExitPerformed(AjaxRequestTarget target) {
                navigateToNext(WebComponentUtil.getObjectListPage(getType()));
            }

            @Override
            public IModel<PrismContainerValueWrapper<AH>> getValueModel() {
                return new ContainerValueWrapperFromObjectWrapperModel<>(
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

    private void backToDetailsFromWizard(AjaxRequestTarget target) {
        DetailsFragment detailsFragment = createDetailsFragment();
        PageAssignmentHolderDetails.this.addOrReplace(detailsFragment);
        target.add(detailsFragment);

        getFeedbackPanel().setVisible(true);
    }
}
