/*
 * Copyright (C) 2018-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.security;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.model.api.authentication.*;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignmentTarget;
import com.evolveum.midpoint.model.impl.controller.CollectionProcessor;
import com.evolveum.midpoint.model.impl.lens.LoginAssignmentCollector;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResourceShadowCoordinates;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.merger.AdminGuiConfigurationMergeManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationTransformer;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.ProfileCompilerOptions;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;

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

    @Autowired private ModelSecurityPolicyFinder modelSecurityPolicyFinder;
    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private CollectionProcessor collectionProcessor;
    @Autowired PrismContext prismContext;
    @Autowired @Qualifier("modelObjectResolver") private ObjectResolver objectResolver;

    @Autowired private LoginAssignmentCollector assignmentCollector;

    @Autowired private SchemaService schemaService;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    @Autowired private GuiProfileCompilerRegistry guiProfileCompilerRegistry;

    @Autowired private AdminGuiConfigurationMergeManager adminGuiConfigurationMergeManager;

    private static final String STATISTIC_WIDGET_PANEL_TYPE = "statisticWidget";

    void compileFocusProfile(
            GuiProfiledPrincipal principal,
            PrismObject<SystemConfigurationType> systemConfiguration,
            AuthorizationTransformer authorizationTransformer,
            ProfileCompilerOptions options,
            Task task,
            OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ObjectNotFoundException {

        if (options == null) {
            options = ProfileCompilerOptions.create();
        }

        LOGGER.debug("Going to compile focus profile for {}", principal.getName());

        if (options.isTryReusingSecurityPolicy()) {
            MidPointPrincipal authPrincipal = AuthUtil.getMidpointPrincipal();
            if (authPrincipal != null) {
                principal.setApplicableSecurityPolicy(authPrincipal.getApplicableSecurityPolicy());
            }
        }

        if (options.isLocateSecurityPolicy()
                && (!options.isTryReusingSecurityPolicy() || principal.getApplicableSecurityPolicy() == null)) {
            principal.setApplicableSecurityPolicy(
                    modelSecurityPolicyFinder.locateSecurityPolicyForFocus(
                            principal.getFocus().asPrismObject(), systemConfiguration, task, result));
        }

        List<AdminGuiConfigurationType> adminGuiConfigurations = new ArrayList<>();
        Set<String> profileDependencies = new HashSet<>();

        profileDependencies.add(principal.getOid());
        if (systemConfiguration != null) {
            profileDependencies.add(systemConfiguration.getOid());
        }

        collect(adminGuiConfigurations, profileDependencies, principal, authorizationTransformer, options, task, result);

        if (!options.isCompileGuiAdminConfiguration()) {
            return;
        }

        CompiledGuiProfile compiledGuiProfile = compileFocusProfile(adminGuiConfigurations, systemConfiguration, principal, task, result);

        compiledGuiProfile.getObjectCollectionViews().forEach(compiledObjectCollectionView -> {
            String archetypeOid = compiledObjectCollectionView.getArchetypeOid();
            if (StringUtils.isNotEmpty(archetypeOid)) {
                profileDependencies.add(archetypeOid);
            }
        });

        setupFocusPhoto(principal, compiledGuiProfile, result);
        setupLocale(principal, compiledGuiProfile);
        compiledGuiProfile.setDependencies(profileDependencies);

        guiProfileCompilerRegistry.invokeCompiler(compiledGuiProfile);
        principal.setCompiledGuiProfile(compiledGuiProfile);
    }

    private void collect(
            List<AdminGuiConfigurationType> adminGuiConfigurations,
            Set<String> consideredOids,
            GuiProfiledPrincipal principal,
            AuthorizationTransformer authorizationTransformer,
            ProfileCompilerOptions options, Task task,
            OperationResult result) throws SchemaException, ConfigurationException {

        if (!options.isCollectAuthorization() && !options.isCompileGuiAdminConfiguration()) {
            return;
        }

        FocusType focus = principal.getFocus(); // [EP:APSO] DONE, focus is from repository

        //TODO why??
//        ObjectQuery query = PrismContext.get().queryFor(AssignmentType.class).ownerId(focus.getOid()).and().ownedBy(focus.getClass()).build();
//        SearchResultList<AssignmentType> assignments = repositoryService.searchContainers(AssignmentType.class, query, null, result);
//        focus.getAssignment().addAll(assignments.stream()
//                .map(originalAssignment -> (AssignmentType) originalAssignment.cloneWithoutId())
//                .toList());

        Collection<? extends EvaluatedAssignment> evaluatedAssignments = // [EP:APSO] DONE, see the called method
                assignmentCollector.collect(focus.asPrismObject(), task, result);

        MidpointAuthentication auth = AuthUtil.getMidpointAuthenticationNotRequired();
        AuthenticationChannel channel = auth != null ? auth.getAuthenticationChannel() : null;

        if (!options.isRunAsRunner() && channel != null) {
            @Nullable Authorization additionalAuth = channel.getAdditionalAuthority();
            if (additionalAuth != null) {
                addAuthorizationToPrincipal(principal, additionalAuth, authorizationTransformer);
            }
        }

        for (EvaluatedAssignment assignment : evaluatedAssignments) {
            if (assignment.isValid()) {
                if (options.isCompileGuiAdminConfiguration()) {
                    // TODO: Should we add also invalid assignments?
                    consideredOids.addAll(assignment.getAdminGuiDependencies());
                }

                if (options.isCollectAuthorization()) {
                    addAuthorizations(principal, channel, assignment.getAuthorizations(), authorizationTransformer, options);
                }
                if (options.isCompileGuiAdminConfiguration()) {
                    adminGuiConfigurations.addAll(assignment.getAdminGuiConfigurations());
                }
            }
            for (EvaluatedAssignmentTarget target : assignment.getRoles().getNonNegativeValues()) { // TODO see MID-6403
                if (target.isValid() && target.getAssignmentPath().containsDelegation()) {
                    principal.addDelegationTarget(
                            target.getTarget(),
                            target.getAssignmentPath().getOtherPrivilegesLimitation());
                }
            }
        }

        if (!options.isCompileGuiAdminConfiguration()) {
            return;
        }

        if (focus instanceof UserType user && user.getAdminGuiConfiguration() != null) {
            // config from the user object should go last (to be applied as the last one)
            adminGuiConfigurations.add(user.getAdminGuiConfiguration());
        } else if (focus instanceof AbstractRoleType role && role.getAdminGuiConfiguration() != null) {
            adminGuiConfigurations.add(role.getAdminGuiConfiguration());
        }
    }

    private void addAuthorizations(
            @NotNull MidPointPrincipal principal,
            @Nullable AuthenticationChannel channel,
            @NotNull Collection<Authorization> sourceCollection,
            @Nullable AuthorizationTransformer authorizationTransformer, ProfileCompilerOptions options) {
        for (Authorization autz : sourceCollection) {
            Authorization resolvedAutz = autz;

            if (!options.isRunAsRunner() && channel != null) {
                resolvedAutz = channel.resolveAuthorization(autz);
                if (resolvedAutz == null) {
                    continue;
                }
            }
            addAuthorizationToPrincipal(principal, resolvedAutz, authorizationTransformer);
        }
    }

    private void addAuthorizationToPrincipal(
            MidPointPrincipal principal, Authorization autz, AuthorizationTransformer authorizationTransformer) {
        if (authorizationTransformer == null) {
            principal.addAuthorization(autz.clone());
        } else {
            for (Authorization transformedAutz : emptyIfNull(authorizationTransformer.transform(autz))) {
                principal.addAuthorization(transformedAutz);
            }
        }
    }

    @NotNull
    public CompiledGuiProfile compileFocusProfile(@NotNull List<AdminGuiConfigurationType> adminGuiConfigurations,
            PrismObject<SystemConfigurationType> systemConfiguration, Task task, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ObjectNotFoundException {
        return compileFocusProfile(adminGuiConfigurations, systemConfiguration, null, task, result);
    }

    public CompiledGuiProfile compileFocusProfile(@NotNull List<AdminGuiConfigurationType> adminGuiConfigurations,
            PrismObject<SystemConfigurationType> systemConfiguration, GuiProfiledPrincipal principal, Task task, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ObjectNotFoundException {

        if (principal != null) {
            LOGGER.debug("Going to compile focus profile (inner) for {}", principal.getName());
        }
        AdminGuiConfigurationType globalAdminGuiConfig = null;
        if (systemConfiguration != null) {
            globalAdminGuiConfig = systemConfiguration.asObjectable().getAdminGuiConfiguration();
        }

        if (adminGuiConfigurations.isEmpty() && globalAdminGuiConfig == null) {
            return new CompiledGuiProfile();
        }

        CompiledGuiProfile composite = new CompiledGuiProfile();
        if (globalAdminGuiConfig != null) {
            applyAdminGuiConfiguration(composite, globalAdminGuiConfig.cloneWithoutId(), principal, task, result);
        }
        for (AdminGuiConfigurationType adminGuiConfiguration : adminGuiConfigurations) {
            applyAdminGuiConfiguration(composite, adminGuiConfiguration.cloneWithoutId(), principal, task, result);
        }

        mergeDeprecatedRoleManagement(composite, systemConfiguration.asObjectable().getRoleManagement());

        return composite;
    }

    private void setupFocusPhoto(GuiProfiledPrincipal principal, @NotNull CompiledGuiProfile compiledGuiProfile, OperationResult result) {
        FocusType focus = principal.getFocus();
        byte[] jpegPhoto = focus.getJpegPhoto();
        Item<PrismValue, ItemDefinition<?>> jpegPhotoItem = focus.asPrismObject().findItem(FocusType.F_JPEG_PHOTO);
        if (jpegPhotoItem != null && jpegPhotoItem.isIncomplete()) {
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
        String prefLang = FocusTypeUtil.languageOrLocale(focus);
        Locale locale = LocalizationUtil.toLocale(prefLang);
        compiledGuiProfile.setLocale(locale);
    }

    private void applyAdminGuiConfiguration(CompiledGuiProfile composite, AdminGuiConfigurationType adminGuiConfiguration, GuiProfiledPrincipal principal, Task task, OperationResult result)
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
        if (adminGuiConfiguration.getDisplayFormats() != null) {
            composite.setDisplayFormats(adminGuiConfiguration.getDisplayFormats().clone());
        }

        applyViews(composite, adminGuiConfiguration.getObjectCollectionViews(), task, result);

        if (adminGuiConfiguration.getObjectDetails() != null) {
            if (composite.getObjectDetails() == null) {
                composite.setObjectDetails(adminGuiConfiguration.getObjectDetails().clone());
            } else {
                for (GuiObjectDetailsPageType objectDetails : adminGuiConfiguration.getObjectDetails().getObjectDetailsPage()) {
                    joinObjectDetails(composite.getObjectDetails(), objectDetails);
                }
            }

            for (GuiShadowDetailsPageType shadowDetails : adminGuiConfiguration.getObjectDetails().getShadowDetailsPage()) {
                joinShadowDetails(composite.getObjectDetails(), shadowDetails);
            }

            Optional<GuiResourceDetailsPageType> detailForAllResources
                    = adminGuiConfiguration.getObjectDetails().getResourceDetailsPage().stream()
                    .filter(currentDetails -> currentDetails.getConnectorRef() == null)
                    .findFirst();
            for (GuiResourceDetailsPageType resourceDetails : adminGuiConfiguration.getObjectDetails().getResourceDetailsPage()) {
                joinResourceDetails(composite.getObjectDetails(), resourceDetails, detailForAllResources, result);
            }
        }

        if (!adminGuiConfiguration.getConfigurableUserDashboard().isEmpty()) {
            for (ConfigurableUserDashboardType configurableUserDashboard : adminGuiConfiguration.getConfigurableUserDashboard()) {
                applyConfigurableDashboard(composite, configurableUserDashboard, task, result);
            }
        }

        for (UserInterfaceFeatureType feature : adminGuiConfiguration.getFeature()) {
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
                    composite.setRoleManagement(new AdminGuiConfigurationRoleManagementType());
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
                    composite.setApprovals(new AdminGuiApprovalsConfigurationType());
                }
                composite.getApprovals().setExpandRolesOnPreview(
                        adminGuiConfiguration.getApprovals().isExpandRolesOnPreview());
            }
        }

        if (adminGuiConfiguration.getAccessRequest() != null) {
            mergeAccessRequestConfiguration(composite, adminGuiConfiguration.getAccessRequest());
        }

        if (adminGuiConfiguration.getHomePage() != null) {
            QName principalType = null;
            if (principal != null) {
                principalType = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(principal.getFocus().getClass()).getTypeName();
            }
            HomePageType configuredHomePage = getHomePageByFocusType(adminGuiConfiguration.getHomePage(), principalType);
            if (composite.getHomePage() == null) {
                composite.setHomePage(configuredHomePage);
            } else {
                composite.setHomePage(mergeHomePage(composite.getHomePage(), configuredHomePage));
            }
        }
        if (composite.getHomePage() != null && composite.getHomePage().getWidget() != null) {
            List<PreviewContainerPanelConfigurationType> sorted = new ArrayList<>(composite.getHomePage().getWidget());
            MiscSchemaUtil.sortFeaturesPanels(sorted);
            composite.getHomePage().getWidget().clear();
            composite.getHomePage().getWidget().addAll(sorted);
        }

        if (composite.getSelfProfilePage() == null && principal != null) {
            QName principalType = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(principal.getFocus().getClass()).getTypeName();
            composite.setSelfProfilePage(new GuiObjectDetailsPageType().type(principalType));
        }
        if (adminGuiConfiguration.getSelfProfilePage() != null) {
            composite.setSelfProfilePage(adminGuiConfigurationMergeManager.mergeObjectDetailsPageConfiguration(
                    adminGuiConfiguration.getSelfProfilePage(), composite.getSelfProfilePage()));
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
        if (feedbackMessagesHook.getDisableOperationResultDownload() != null) {
            composite.getFeedbackMessagesHook().setDisableOperationResultDownload(feedbackMessagesHook.getDisableOperationResultDownload());
        }
        if (feedbackMessagesHook.getShowOnlyUserFriendlyMessages() != null) {
            composite.getFeedbackMessagesHook().setShowOnlyUserFriendlyMessages(feedbackMessagesHook.getShowOnlyUserFriendlyMessages());
        }
        if (feedbackMessagesHook.isDisplayOnlyTopLevelOperationResult() != null) {
            composite.getFeedbackMessagesHook().setDisplayOnlyTopLevelOperationResult(feedbackMessagesHook.isDisplayOnlyTopLevelOperationResult());
        }
    }

    private HomePageType getHomePageByFocusType(List<HomePageType> homePageList, QName type) {
        if (CollectionUtils.isEmpty(homePageList)) {
            return null;
        }
        for (HomePageType homePageType : homePageList) {
            if (homePageType.getType() == null && QNameUtil.match(UserType.COMPLEX_TYPE, type)) {   //todo UserType is default for no-type-specified home page?
                return homePageType;
            }
            if (QNameUtil.match(homePageType.getType(), type)) {
                return homePageType;
            }
        }
        return null;
    }

    private HomePageType mergeHomePage(HomePageType compositeHomePage, HomePageType homePage) {
        if (homePage == null) {
            return compositeHomePage;
        }
        if (compositeHomePage == null) {
            return homePage;
        }
        if (compositeHomePage.getType() != null && homePage.getType() != null &&
                !QNameUtil.match(compositeHomePage.getType(), homePage.getType())) {
            return compositeHomePage;
        }
        if (StringUtils.isNotEmpty(compositeHomePage.getIdentifier()) && compositeHomePage.getIdentifier().equals(homePage.getIdentifier())) {
            return compositeHomePage;
        }
        if (compositeHomePage.getType() == null) {
            compositeHomePage.setType(homePage.getType());
        }
        mergeFeature(compositeHomePage, homePage, UserInterfaceElementVisibilityType.AUTOMATIC);

        if (CollectionUtils.isNotEmpty(homePage.getWidget())) {
            if (compositeHomePage.getWidget() == null) {
                compositeHomePage.createWidgetList();
            }
            List<PreviewContainerPanelConfigurationType> mergedWidgets =
                    adminGuiConfigurationMergeManager.mergePreviewContainerPanelConfigurationType(compositeHomePage.getWidget(), homePage.getWidget());
            compositeHomePage.getWidget().clear();
            compositeHomePage.getWidget().addAll(mergedWidgets);
        }
        return compositeHomePage;
    }

    private void mergeDeprecatedRoleManagement(CompiledGuiProfile composite, RoleManagementConfigurationType roleManagement) {
        if (roleManagement == null) {
            return;
        }

        AccessRequestType ar = composite.getAccessRequest();
        if (ar == null) {
            ar = new AccessRequestType();
            composite.setAccessRequest(ar);
        }

        if (ar.getDescription() == null) {
            ar.setDescription(roleManagement.getDescription());
        }

        if (ar.getDocumentation() == null) {
            ar.setDocumentation(roleManagement.getDocumentation());
        }

        if (ar.getRoleCatalog() == null) {
            ar.setRoleCatalog(new RoleCatalogType());
        }
    }

    private void mergeAccessRequestConfiguration(CompiledGuiProfile composite, AccessRequestType accessRequest) {
        if (composite.getAccessRequest() == null) {
            composite.setAccessRequest(accessRequest.clone());
        }

        AccessRequestType ar = composite.getAccessRequest();
        if (accessRequest.getTargetSelection() != null) {
            ar.setTargetSelection(accessRequest.getTargetSelection().clone());
        }

        if (accessRequest.getRelationSelection() != null) {
            ar.setRelationSelection(accessRequest.getRelationSelection().clone());
        }

        if (accessRequest.getRoleCatalog() != null) {
            ar.setRoleCatalog(accessRequest.getRoleCatalog().clone());
        }

        if (accessRequest.getCheckout() != null) {
            ar.setCheckout(accessRequest.getCheckout().clone());
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
        } catch (ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException |
                SecurityViolationException | ExpressionEvaluationException e) {
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
            composite.getShadowCollectionViews().add(existingView);
        }
        return existingView;
    }

    public void compileView(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType, Task task, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ObjectNotFoundException {
        collectionProcessor.compileView(existingView, objectListViewType, task, result);
    }

    private void joinShadowDetails(GuiObjectDetailsSetType objectDetailsSet, GuiShadowDetailsPageType newObjectDetails) {
        objectDetailsSet.getShadowDetailsPage().removeIf(currentDetails -> isTheSameShadowDiscriminatorType(currentDetails, newObjectDetails));
        objectDetailsSet.getShadowDetailsPage().add(newObjectDetails.clone());
    }

    private void joinResourceDetails(GuiObjectDetailsSetType objectDetailsSet, GuiResourceDetailsPageType newObjectDetails, Optional<GuiResourceDetailsPageType> detailForAllResources, OperationResult result) {
        objectDetailsSet.getResourceDetailsPage().removeIf(currentDetails -> isTheSameConnector(currentDetails, newObjectDetails, result));
        GuiResourceDetailsPageType added;
        if (!detailForAllResources.isEmpty() && newObjectDetails.getConnectorRef() != null) {
            GuiResourceDetailsPageType merged = adminGuiConfigurationMergeManager.mergeObjectDetailsPageConfiguration(
                    detailForAllResources.get(),
                    newObjectDetails);
            merged.setConnectorRef(newObjectDetails.getConnectorRef().clone());
            added = merged;
        } else {
            added = newObjectDetails.clone();
        }
        if (added.getConnectorRef() != null && StringUtils.isEmpty(added.getConnectorRef().getOid())) {
            @NotNull List<String> refsOid = resolveReferenceIfNeeded(added.getConnectorRef(), result);
            refsOid.forEach(oid -> {
                GuiResourceDetailsPageType clone = added.clone();
                clone.getConnectorRef().setOid(oid);
                objectDetailsSet.getResourceDetailsPage().add(clone);
            });
        } else {
            objectDetailsSet.getResourceDetailsPage().add(added);
        }
    }

    private void joinObjectDetails(GuiObjectDetailsSetType objectDetailsSet, GuiObjectDetailsPageType newObjectDetails) {
        AtomicBoolean merged = new AtomicBoolean(false);
        objectDetailsSet.getObjectDetailsPage().forEach(currentDetails -> {
            if (isTheSameObjectType(currentDetails, newObjectDetails)) {
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
        ResourceShadowCoordinates oldCoords =
                new ResourceShadowCoordinates(
                        oldConf.getResourceRef().getOid(), oldConf.getKind(), oldConf.getIntent());
        ResourceShadowCoordinates newCoords =
                new ResourceShadowCoordinates(
                        newConf.getResourceRef().getOid(), newConf.getKind(), newConf.getIntent());
        return oldCoords.equals(newCoords);
    }

    private boolean isTheSameConnector(GuiResourceDetailsPageType oldConf, GuiResourceDetailsPageType newConf, OperationResult result) {
        if (oldConf.getConnectorRef() == null || newConf.getConnectorRef() == null) {
            LOGGER.trace("Cannot join resource details configuration as defined in {} and {}. No connector defined", oldConf, newConf);
            return false;
        }
        List<String> oldConnectorOids = resolveReferenceIfNeeded(oldConf.getConnectorRef(), result);
        List<String> newConnectorOids = resolveReferenceIfNeeded(newConf.getConnectorRef(), result);
        if (oldConnectorOids.isEmpty() || newConnectorOids.isEmpty()) {
            return false;
        }
        return newConnectorOids.stream().anyMatch(newConnectorOid -> oldConnectorOids.contains(newConnectorOid));
    }

    private @NotNull List<String> resolveReferenceIfNeeded(ObjectReferenceType reference, OperationResult result) {
        if (reference.getOid() != null) {
            return List.of(reference.getOid());
        }
        if (reference.getFilter() == null) {
            LOGGER.debug("Neither filter, nor oid defined in the reference: {}", reference);
            return List.of();
        }

        if (reference.getResolutionTime() == EvaluationTimeType.RUN) {
            List<String> objects = ModelImplUtils.resolveObjectsFromRef(reference.asReferenceValue(), repositoryService,
                    EvaluationTimeType.RUN, "resolving connector reference", false, result);
            if (objects.size() == 1) {
                reference.asReferenceValue().setOid(objects.get(0));
            }
            return objects;
        }
        return List.of();
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
        if (compositeFeature == null) {
            compositeFeature = newFeature;
        }
        if (compositeFeature.getIdentifier() != null && !compositeFeature.getIdentifier().equals(newFeature.getIdentifier())) {
            return;
        }
        if (StringUtils.isNotEmpty(newFeature.getDescription())) {
            compositeFeature.setDescription(newFeature.getDescription());
        }
        if (StringUtils.isNotEmpty(newFeature.getDocumentation())) {
            compositeFeature.setDocumentation(newFeature.getDocumentation());
        }
        if (newFeature.getDisplay() != null) {
            if (compositeFeature.getDisplay() == null) {
                compositeFeature.setDisplay(newFeature.getDisplay());
            } else {
                MiscSchemaUtil.mergeDisplay(newFeature.getDisplay(), compositeFeature.getDisplay());
                compositeFeature.setDisplay(newFeature.getDisplay());
            }
        }

        UserInterfaceElementVisibilityType newCompositeVisibility = mergeVisibility(compositeFeature.getVisibility(), newFeature.getVisibility(), defaultVisibility);
        compositeFeature.setVisibility(newCompositeVisibility);

        if (newFeature.getApplicableForOperation() != null) {
            compositeFeature.setApplicableForOperation(newFeature.getApplicableForOperation());
        }
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
