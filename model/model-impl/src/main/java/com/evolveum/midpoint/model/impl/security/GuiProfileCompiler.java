/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.AdminGuiConfigurationMergeManager;
import com.evolveum.midpoint.model.api.authentication.*;
import com.evolveum.midpoint.schema.*;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignmentTarget;
import com.evolveum.midpoint.model.api.util.DeputyUtils;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.impl.controller.CollectionProcessor;
import com.evolveum.midpoint.model.impl.lens.AssignmentCollector;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationTransformer;
import com.evolveum.midpoint.security.api.DelegatorWithOtherPrivilegesLimitations;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Compiles user interface profile for a particular user. The profile contains essential information needed to efficiently render
 * user interface pages for specified user.
 *
 * This methods in this component may be quite costly to invoke. Therefore it should NOT be invoked for every request.
 * The methods are supposed to be invoked once (or several times) during user's session. The result of this method should be
 * cached in web session (in principal).
 *
 * @author Radovan semancik
 */
@Component
public class GuiProfileCompiler {

    private static final Trace LOGGER = TraceManager.getTrace(GuiProfileCompiler.class);

    @Autowired private SecurityHelper securityHelper;
    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private RelationRegistry relationRegistry;
    @Autowired private PrismContext prismContext;
    @Autowired private CollectionProcessor collectionProcessor;
    @Autowired @Qualifier("modelObjectResolver") private ObjectResolver objectResolver;
    @Autowired private AdminGuiConfigurationMergeManager adminGuiConfigurationMergeManager;

    @Autowired private AssignmentCollector assignmentCollector;

    @Autowired private SchemaService schemaService;
    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired private GuiProfileCompilerRegistry guiProfileCompilerRegistry;

    public void compileFocusProfile(GuiProfiledPrincipal principal, PrismObject<SystemConfigurationType> systemConfiguration, AuthorizationTransformer authorizationTransformer, Task task, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ObjectNotFoundException {

        principal.setApplicableSecurityPolicy(securityHelper.locateSecurityPolicy(principal.getFocus().asPrismObject(), systemConfiguration, task, result));

        List<AdminGuiConfigurationType> adminGuiConfigurations = new ArrayList<>();
        collect(adminGuiConfigurations, principal, authorizationTransformer, task, result);

        CompiledGuiProfile compiledGuiProfile = compileFocusProfile(adminGuiConfigurations, systemConfiguration, task, result);
        if (compiledGuiProfile != null) {
            setupFocusPhoto(principal, compiledGuiProfile, result);
            setupLocale(principal, compiledGuiProfile);
        }
        guiProfileCompilerRegistry.invokeCompiler(compiledGuiProfile);
        principal.setCompiledGuiProfile(compiledGuiProfile);
    }

    private void collect(List<AdminGuiConfigurationType> adminGuiConfigurations, GuiProfiledPrincipal principal, AuthorizationTransformer authorizationTransformer, Task task, OperationResult result) throws SchemaException {
        FocusType focusType = principal.getFocus();

        Collection<? extends EvaluatedAssignment<? extends FocusType>> evaluatedAssignments = assignmentCollector.collect(focusType.asPrismObject(), true, task, result);
        Collection<Authorization> authorizations = principal.getAuthorities();
        for (EvaluatedAssignment<? extends FocusType> assignment : evaluatedAssignments) {
            if (assignment.isValid()) {
                addAuthorizations(authorizations, assignment.getAuthorizations(), authorizationTransformer);
                adminGuiConfigurations.addAll(assignment.getAdminGuiConfigurations());
            }
            for (EvaluatedAssignmentTarget target : assignment.getRoles().getNonNegativeValues()) { // MID-6403
                if (target.isValid() && target.getTarget().asObjectable() instanceof UserType
                        && DeputyUtils.isDelegationPath(target.getAssignmentPath(), relationRegistry)) {
                    List<OtherPrivilegesLimitationType> limitations = DeputyUtils.extractLimitations(target.getAssignmentPath());
                    principal.addDelegatorWithOtherPrivilegesLimitations(new DelegatorWithOtherPrivilegesLimitations(
                            (UserType) target.getTarget().asObjectable(), limitations));
                }
            }
        }

        if (focusType instanceof UserType && ((UserType)focusType).getAdminGuiConfiguration() != null) {
            // config from the user object should go last (to be applied as the last one)
            adminGuiConfigurations.add(((UserType)focusType).getAdminGuiConfiguration());
        }

    }

    private void addAuthorizations(Collection<Authorization> targetCollection, Collection<Authorization> sourceCollection, AuthorizationTransformer authorizationTransformer) {
        if (sourceCollection == null) {
            return;
        }
        for (Authorization autz: sourceCollection) {
            if (authorizationTransformer == null) {
                targetCollection.add(autz.clone());
            } else {
                Collection<Authorization> transformedAutzs = authorizationTransformer.transform(autz);
                if (transformedAutzs != null) {
                    targetCollection.addAll(transformedAutzs);
                }
            }
        }
    }

    public CompiledGuiProfile compileFocusProfile(@NotNull List<AdminGuiConfigurationType> adminGuiConfigurations,
            PrismObject<SystemConfigurationType> systemConfiguration, Task task, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ObjectNotFoundException {

        AdminGuiConfigurationType globalAdminGuiConfig = null;
        if (systemConfiguration != null) {
            globalAdminGuiConfig = systemConfiguration.asObjectable().getAdminGuiConfiguration();
        }
        // if there's no admin config at all, return null (to preserve original behavior)
        if (adminGuiConfigurations.isEmpty() && globalAdminGuiConfig == null) {
            return null;
        }

        CompiledGuiProfile composite = new CompiledGuiProfile();
        if (globalAdminGuiConfig != null) {
            applyAdminGuiConfiguration(composite, globalAdminGuiConfig.cloneWithoutId(), task, result);
        }
        for (AdminGuiConfigurationType adminGuiConfiguration: adminGuiConfigurations) {
            applyAdminGuiConfiguration(composite, adminGuiConfiguration.cloneWithoutId(), task, result);
        }

        return composite;
    }

    private void setupFocusPhoto(GuiProfiledPrincipal principal, @NotNull CompiledGuiProfile compiledGuiProfile, OperationResult result) {
        FocusType focus = principal.getFocus();
        byte[] jpegPhoto = focus.getJpegPhoto();
        if (jpegPhoto == null) {
            Collection<SelectorOptions<GetOperationOptions>> options = schemaService.getOperationOptionsBuilder()
                    // no read-only because the photo (byte[]) is provided to unknown actors
                    .item(FocusType.F_JPEG_PHOTO).retrieve()
                    .build();
            try {
                PrismObject<? extends FocusType> resolvedFocus = repositoryService.getObject(focus.getClass(), focus.getOid(), options, result);
                jpegPhoto = resolvedFocus.asObjectable().getJpegPhoto();
            } catch (ObjectNotFoundException | SchemaException e) {
                LOGGER.trace("Failed to load photo for {}, continue without it", focus, e);
            }
        }
        compiledGuiProfile.setJpegPhoto(jpegPhoto);
    }

    private void setupLocale(GuiProfiledPrincipal principal, @NotNull CompiledGuiProfile compiledGuiProfile) {
        FocusType focus = principal.getFocus();
        String prefLang = focus.getPreferredLanguage();
        if (StringUtils.isBlank(prefLang)) {
            prefLang = focus.getLocale();
        }
        Locale locale = null;
        try {
            locale = LocaleUtils.toLocale(prefLang);
        } catch (Exception ex) {
            LOGGER.debug("Error occurred while getting user locale, " + ex.getMessage());
        }
        compiledGuiProfile.setLocale(locale);
    }

    private void applyAdminGuiConfiguration(CompiledGuiProfile composite, AdminGuiConfigurationType adminGuiConfiguration, Task task, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ObjectNotFoundException {
        if (adminGuiConfiguration == null) {
            return;
        }
        adminGuiConfiguration.getAdditionalMenuLink().forEach(additionalMenuLink -> composite.getAdditionalMenuLink().add(additionalMenuLink.clone()));
        adminGuiConfiguration.getUserDashboardLink().forEach(userDashboardLink -> composite.getUserDashboardLink().add(userDashboardLink.clone()));
        if (adminGuiConfiguration.getDefaultTimezone() != null) {
            composite.setDefaultTimezone(adminGuiConfiguration.getDefaultTimezone());
        }
        if (adminGuiConfiguration.getPreferredDataLanguage() != null) {
            composite.setPreferredDataLanguage(adminGuiConfiguration.getPreferredDataLanguage());
        }
        if (adminGuiConfiguration.isEnableExperimentalFeatures() != null) {
            composite.setEnableExperimentalFeatures(adminGuiConfiguration.isEnableExperimentalFeatures());
        }
        if (adminGuiConfiguration.isUseNewDesign() != null) {
            composite.setUseNewDesign(adminGuiConfiguration.isUseNewDesign());
        }
        if (adminGuiConfiguration.getDefaultExportSettings() != null) {
            composite.setDefaultExportSettings(adminGuiConfiguration.getDefaultExportSettings().clone());
        }
        if (adminGuiConfiguration.getDisplayFormats() != null){
            composite.setDisplayFormats(adminGuiConfiguration.getDisplayFormats().clone());
        }

        applyViews(composite, adminGuiConfiguration.getObjectCollectionViews(), task, result);

        if (adminGuiConfiguration.getObjectForms() != null) {
            if (composite.getObjectForms() == null) {
                composite.setObjectForms(adminGuiConfiguration.getObjectForms().clone());
            } else {
                for (ObjectFormType objectForm: adminGuiConfiguration.getObjectForms().getObjectForm()) {
                    joinForms(composite.getObjectForms(), objectForm.clone());
                }
            }
        }
        if (adminGuiConfiguration.getObjectDetails() != null) {
            if (composite.getObjectDetails() == null) {
                composite.setObjectDetails(adminGuiConfiguration.getObjectDetails().clone());
            } else {
                for (GuiObjectDetailsPageType objectDetails: adminGuiConfiguration.getObjectDetails().getObjectDetailsPage()) {
                    joinObjectDetails(composite.getObjectDetails(), objectDetails);
                }
            }

            for (GuiShadowDetailsPageType shadowDetails : adminGuiConfiguration.getObjectDetails().getShadowDetailsPage()) {
                joinShadowDetails(composite.getObjectDetails(), shadowDetails);
            }
        }
        if (adminGuiConfiguration.getUserDashboard() != null) {
            if (composite.getUserDashboard() == null) {
                composite.setUserDashboard(adminGuiConfiguration.getUserDashboard().clone());
            } else {
                for (DashboardWidgetType widget: adminGuiConfiguration.getUserDashboard().getWidget()) {
                    mergeWidget(composite, widget);
                }
            }
        }

        if (!adminGuiConfiguration.getConfigurableUserDashboard().isEmpty()) {
            for (ConfigurableUserDashboardType configurableUserDashboard : adminGuiConfiguration.getConfigurableUserDashboard()) {
                applyConfigurableDashboard(composite, configurableUserDashboard, task, result);
            }
        }

        for (UserInterfaceFeatureType feature: adminGuiConfiguration.getFeature()) {
            mergeFeature(composite, feature.clone());
        }


        if (adminGuiConfiguration.getFeedbackMessagesHook() != null) {
            mergeFeedbackMessagesHook(composite, adminGuiConfiguration.getFeedbackMessagesHook());
        }

        if (adminGuiConfiguration.getRoleManagement() != null &&
                adminGuiConfiguration.getRoleManagement().getAssignmentApprovalRequestLimit() != null) {
            if (composite.getRoleManagement() != null && composite.getRoleManagement().getAssignmentApprovalRequestLimit() != null) {
                // the greater value wins (so it is possible to give an exception to selected users)
                Integer newValue = Math.max(
                        adminGuiConfiguration.getRoleManagement().getAssignmentApprovalRequestLimit(),
                        composite.getRoleManagement().getAssignmentApprovalRequestLimit());
                composite.getRoleManagement().setAssignmentApprovalRequestLimit(newValue);
            } else {
                if (composite.getRoleManagement() == null) {
                    composite.setRoleManagement(new AdminGuiConfigurationRoleManagementType(prismContext));
                }
                composite.getRoleManagement().setAssignmentApprovalRequestLimit(
                        adminGuiConfiguration.getRoleManagement().getAssignmentApprovalRequestLimit());
            }
        }

        if (adminGuiConfiguration.getApprovals() != null &&
                adminGuiConfiguration.getApprovals().isExpandRolesOnPreview() != null) {
            if (composite.getApprovals() != null && composite.getApprovals().isExpandRolesOnPreview() != null) {
                // the most permissive value wins (so it is possible to give an exception to selected users)
                boolean newValue = adminGuiConfiguration.getApprovals().isExpandRolesOnPreview() ||
                        composite.getApprovals().isExpandRolesOnPreview();
                composite.getApprovals().setExpandRolesOnPreview(newValue);
            } else {
                if (composite.getApprovals() == null) {
                    composite.setApprovals(new AdminGuiApprovalsConfigurationType(prismContext));
                }
                composite.getApprovals().setExpandRolesOnPreview(
                        adminGuiConfiguration.getApprovals().isExpandRolesOnPreview());
            }
        }
    }

    private void mergeFeedbackMessagesHook(CompiledGuiProfile composite, FeedbackMessagesHookType feedbackMessagesHook) {
        if (composite.getFeedbackMessagesHook() == null) {
            composite.setFeedbackMessagesHook(feedbackMessagesHook.clone());
            return;
        }
        if (feedbackMessagesHook.getOperationResultHook() != null) {
            composite.getFeedbackMessagesHook().setOperationResultHook(feedbackMessagesHook.getOperationResultHook());
        }
        if (feedbackMessagesHook.getStackTraceVisibility() != null) {
            composite.getFeedbackMessagesHook().setStackTraceVisibility(feedbackMessagesHook.getStackTraceVisibility());
        }
        if (feedbackMessagesHook.isDisableOperationResultDownload() != null) {
            composite.getFeedbackMessagesHook().setDisableOperationResultDownload(feedbackMessagesHook.isDisableOperationResultDownload());
        }
        if (feedbackMessagesHook.isShowOnlyUserFriendlyMessages() != null) {
            composite.getFeedbackMessagesHook().setShowOnlyUserFriendlyMessages(feedbackMessagesHook.isShowOnlyUserFriendlyMessages());
        }
        if (feedbackMessagesHook.isDisplayOnlyTopLevelOperationResult() != null) {
            composite.getFeedbackMessagesHook().setDisplayOnlyTopLevelOperationResult(feedbackMessagesHook.isDisplayOnlyTopLevelOperationResult());
        }
    }

    private void applyConfigurableDashboard(CompiledGuiProfile composit, ConfigurableUserDashboardType configurableUserDashboard, Task task, OperationResult result) {
        if (configurableUserDashboard == null) {
            return;
        }

        ObjectReferenceType configurableUserDashboardRef = configurableUserDashboard.getConfigurableDashboardRef();
        if (configurableUserDashboardRef == null) {
            LOGGER.trace("No configuration for flexible dashboards found. Skipping processing");
            return;
        }

        try {
            DashboardType dashboardType = objectResolver.resolve(configurableUserDashboardRef, DashboardType.class, null, " configurable dashboard ", task, result);
            CompiledDashboardType compiledDashboard = new CompiledDashboardType(dashboardType);

            // DisplayType
            if (configurableUserDashboard.getDisplay() == null) {
                configurableUserDashboard.setDisplay(new DisplayType());
            }
            MiscSchemaUtil.mergeDisplay(configurableUserDashboard.getDisplay(), dashboardType.getDisplay());
            compiledDashboard.setDisplayType(configurableUserDashboard.getDisplay());

            UserInterfaceElementVisibilityType visibility = configurableUserDashboard.getVisibility();
            if (visibility == null) {
                visibility = UserInterfaceElementVisibilityType.AUTOMATIC;
            }
            compiledDashboard.setVisibility(visibility);

            composit.getConfigurableDashboards().add(compiledDashboard);
        } catch (ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException e) {
            LOGGER.warn("Failed to resolve dashboard {}", configurableUserDashboard);
            // probably we should not fail here, just log warn and continue as if there is no dashboard specification
        }
    }

    private void applyViews(CompiledGuiProfile composite, GuiObjectListViewsType viewsType, Task task, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ObjectNotFoundException {
        if (viewsType == null) {
            return;
        }

        if (viewsType.getDefault() != null) {
            if (composite.getDefaultObjectCollectionView() == null) {
                composite.setDefaultObjectCollectionView(new CompiledObjectCollectionView());
            }
            compileView(composite.getDefaultObjectCollectionView(), viewsType.getDefault(), task, result);
        }

        for (GuiObjectListViewType objectCollectionView : viewsType.getObjectCollectionView()) {
            applyView(composite, objectCollectionView, task, result);
        }

        for (GuiShadowListViewType shadowCollectionView : viewsType.getShadowCollectionView()) {
            applyShadowView(composite, shadowCollectionView, task, result);
        }
    }

    private void applyView(CompiledGuiProfile composite, GuiObjectListViewType objectListViewType, Task task, OperationResult result) {
        try {
            CompiledObjectCollectionView existingView = findOrCreateMatchingView(composite, objectListViewType);
            compileView(existingView, objectListViewType, task, result);
        } catch (Throwable e) {
            // Do not let any error stop processing here. This code is used during user login. An error here can stop login procedure. We do not
            // want that. E.g. wrong adminGuiConfig may prohibit login on administrator, therefore ruining any chance of fixing the situation.
            // This is also handled somewhere up the call stack. But we want to handle it also here. Otherwise an error in one collection would
            // mean that entire configuration processing will be stopped. We do not want that. We want to skip processing of just that one wrong view.
            LOGGER.error("Error compiling user profile, view '{}': {}", collectionProcessor.determineViewIdentifier(objectListViewType), e.getMessage(), e);
        }
    }

    private void applyShadowView(CompiledGuiProfile composite, GuiShadowListViewType objectListViewType, Task task, OperationResult result) {
        try {
            CompiledShadowCollectionView existingView = findOrCreateMatchingShadowView(composite, objectListViewType);
            existingView.setResourceRef(objectListViewType.getResourceRef());
            existingView.setShadowKindType(objectListViewType.getKind());
            existingView.setIntent(objectListViewType.getIntent());
            compileView(existingView, objectListViewType, task, result);
        } catch (Throwable e) {
            // Do not let any error stop processing here. This code is used during user login. An error here can stop login procedure. We do not
            // want that. E.g. wrong adminGuiConfig may prohibit login on administrator, therefore ruining any chance of fixing the situation.
            // This is also handled somewhere up the call stack. But we want to handle it also here. Otherwise an error in one collection would
            // mean that entire configuration processing will be stopped. We do not want that. We want to skip processing of just that one wrong view.
            LOGGER.error("Error compiling user profile, view '{}': {}", collectionProcessor.determineViewIdentifier(objectListViewType), e.getMessage(), e);
        }
    }


    private CompiledObjectCollectionView findOrCreateMatchingView(CompiledGuiProfile composite, GuiObjectListViewType objectListViewType) {
        QName objectType = objectListViewType.getType();
        String viewIdentifier = collectionProcessor.determineViewIdentifier(objectListViewType);
        CompiledObjectCollectionView existingView = composite.findObjectCollectionView(objectType, viewIdentifier);
        if (existingView == null) {
            existingView = new CompiledObjectCollectionView(objectType, viewIdentifier);
            composite.getObjectCollectionViews().add(existingView);
        }
        return existingView;
    }

    private CompiledShadowCollectionView findOrCreateMatchingShadowView(CompiledGuiProfile composite, GuiShadowListViewType objectListViewType) {
        String viewIdentifier = collectionProcessor.determineViewIdentifier(objectListViewType);
        if (objectListViewType.getResourceRef() == null) {
            return new CompiledShadowCollectionView();
        }
        CompiledShadowCollectionView existingView = composite.findShadowCollectionView(objectListViewType.getResourceRef().getOid(), objectListViewType.getKind(), objectListViewType.getIntent());
        if (existingView == null) {
            existingView = new CompiledShadowCollectionView(viewIdentifier);
            composite.getObjectCollectionViews().add(existingView);
        }
        return existingView;
    }


    public void compileView(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType, Task task, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ObjectNotFoundException {
        collectionProcessor.compileView(existingView, objectListViewType, task, result);
    }

    private void joinForms(ObjectFormsType objectForms, ObjectFormType newForm) {
        objectForms.getObjectForm().removeIf(currentForm -> isTheSameObjectForm(currentForm, newForm));
        objectForms.getObjectForm().add(newForm.clone().id(null));
    }

    private void joinShadowDetails(GuiObjectDetailsSetType objectDetailsSet, GuiShadowDetailsPageType newObjectDetails) {
        objectDetailsSet.getShadowDetailsPage().removeIf(currentDetails -> isTheSameShadowDiscriminatorType(currentDetails, newObjectDetails));
        objectDetailsSet.getShadowDetailsPage().add(newObjectDetails.clone());
    }

    private void joinObjectDetails(GuiObjectDetailsSetType objectDetailsSet, GuiObjectDetailsPageType newObjectDetails) {
        AtomicBoolean merged = new AtomicBoolean(false);
        objectDetailsSet.getObjectDetailsPage().forEach(currentDetails -> {
            if(isTheSameObjectType(currentDetails, newObjectDetails)){
                objectDetailsSet.getObjectDetailsPage().remove(currentDetails);
                objectDetailsSet.getObjectDetailsPage().add(
                        adminGuiConfigurationMergeManager.mergeObjectDetailsPageConfiguration(currentDetails, newObjectDetails));
                merged.set(true);
            }
        });
        if (!merged.get()) {
            objectDetailsSet.getObjectDetailsPage().add(newObjectDetails.clone());
        }
    }

    private boolean isTheSameObjectType(AbstractObjectTypeConfigurationType oldConf, AbstractObjectTypeConfigurationType newConf) {
        return QNameUtil.match(oldConf.getType(), newConf.getType());
    }

    private boolean isTheSameShadowDiscriminatorType(GuiShadowDetailsPageType oldConf, GuiShadowDetailsPageType newConf) {
        if (oldConf.getResourceRef() == null || newConf.getResourceRef() == null) {
            LOGGER.warn("Cannot join shadow details configuration as defined in {} and {}. No resource defined", oldConf, newConf);
            return false;
        }
        ResourceShadowDiscriminator oldDiscriminator = new ResourceShadowDiscriminator(oldConf.getResourceRef().getOid(), oldConf.getKind(), oldConf.getIntent(), null, false);
        ResourceShadowDiscriminator newDiscriminator = new ResourceShadowDiscriminator(newConf.getResourceRef().getOid(), newConf.getKind(), newConf.getIntent(), null, false);
        return oldDiscriminator.equals(newDiscriminator);
    }

    private boolean isTheSameObjectForm(ObjectFormType oldForm, ObjectFormType newForm){
        if (!isTheSameObjectType(oldForm,newForm)) {
            return false;
        }
        if (oldForm.isIncludeDefaultForms() != null &&
                newForm.isIncludeDefaultForms() != null){
            return true;
        }
        if (oldForm.getFormSpecification() == null && newForm.getFormSpecification() == null) {
            String oldFormPanelUri = oldForm.getFormSpecification().getPanelUri();
            String newFormPanelUri = newForm.getFormSpecification().getPanelUri();
            if (oldFormPanelUri != null && oldFormPanelUri.equals(newFormPanelUri)) {
                return true;
            }

            String oldFormPanelClass = oldForm.getFormSpecification().getPanelClass();
            String newFormPanelClass = newForm.getFormSpecification().getPanelClass();
            if (oldFormPanelClass != null && oldFormPanelClass.equals(newFormPanelClass)) {
                return true;
            }

            String oldFormRefOid = oldForm.getFormSpecification().getFormRef() == null ?
                    null : oldForm.getFormSpecification().getFormRef().getOid();
            String newFormRefOid = newForm.getFormSpecification().getFormRef() == null ?
                    null : newForm.getFormSpecification().getFormRef().getOid();
            if (oldFormRefOid != null && oldFormRefOid.equals(newFormRefOid)) {
                return true;
            }
        }
        return false;
    }

    private void mergeWidget(CompiledGuiProfile composite, DashboardWidgetType newWidget) {
        String newWidgetIdentifier = newWidget.getIdentifier();
        DashboardWidgetType compositeWidget = composite.findUserDashboardWidget(newWidgetIdentifier);
        if (compositeWidget == null) {
            composite.getUserDashboard().getWidget().add(newWidget.clone());
        } else {
            mergeWidget(compositeWidget, newWidget);
        }
    }

    private void mergeWidget(DashboardWidgetType compositeWidget, DashboardWidgetType newWidget) {
        mergeFeature(compositeWidget, newWidget, UserInterfaceElementVisibilityType.VACANT);
        // merge other widget properties (in the future)
    }

    private void mergeFeature(CompiledGuiProfile composite, UserInterfaceFeatureType newFeature) {
        String newIdentifier = newFeature.getIdentifier();
        UserInterfaceFeatureType compositeFeature = composite.findFeature(newIdentifier);
        if (compositeFeature == null) {
            composite.getFeatures().add(newFeature.clone());
        } else {
            mergeFeature(compositeFeature, newFeature, UserInterfaceElementVisibilityType.AUTOMATIC);
        }
    }

    private <T extends UserInterfaceFeatureType> void mergeFeature(T compositeFeature, T newFeature, UserInterfaceElementVisibilityType defaultVisibility) {
        UserInterfaceElementVisibilityType newCompositeVisibility = mergeVisibility(compositeFeature.getVisibility(), newFeature.getVisibility(), defaultVisibility);
        compositeFeature.setVisibility(newCompositeVisibility);
    }

    private UserInterfaceElementVisibilityType mergeVisibility(
            UserInterfaceElementVisibilityType compositeVisibility, UserInterfaceElementVisibilityType newVisibility, UserInterfaceElementVisibilityType defaultVisibility) {
        if (compositeVisibility == null) {
            compositeVisibility = defaultVisibility;
        }
        if (newVisibility == null) {
            newVisibility = defaultVisibility;
        }
        if (compositeVisibility == UserInterfaceElementVisibilityType.HIDDEN || newVisibility == UserInterfaceElementVisibilityType.HIDDEN) {
            return UserInterfaceElementVisibilityType.HIDDEN;
        }
        if (compositeVisibility == UserInterfaceElementVisibilityType.VISIBLE || newVisibility == UserInterfaceElementVisibilityType.VISIBLE) {
            return UserInterfaceElementVisibilityType.VISIBLE;
        }
        if (compositeVisibility == UserInterfaceElementVisibilityType.AUTOMATIC || newVisibility == UserInterfaceElementVisibilityType.AUTOMATIC) {
            return UserInterfaceElementVisibilityType.AUTOMATIC;
        }
        return UserInterfaceElementVisibilityType.VACANT;
    }

    public CompiledGuiProfile getGlobalCompiledGuiProfile(Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(parentResult);
        if (systemConfiguration == null) {
            return null;
        }
        List<AdminGuiConfigurationType> adminGuiConfigurations = new ArrayList<>();
        CompiledGuiProfile compiledGuiProfile = compileFocusProfile(adminGuiConfigurations, systemConfiguration, task, parentResult);
        // TODO: cache compiled profile
        return compiledGuiProfile;
    }


}
