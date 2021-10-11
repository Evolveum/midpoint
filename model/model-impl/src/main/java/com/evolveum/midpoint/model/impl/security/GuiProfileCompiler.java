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

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignmentTarget;
import com.evolveum.midpoint.model.api.util.DeputyUtils;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.impl.controller.CollectionProcessor;
import com.evolveum.midpoint.model.impl.lens.AssignmentCollector;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationTransformer;
import com.evolveum.midpoint.security.api.DelegatorWithOtherPrivilegesLimitations;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

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

    @Autowired private AssignmentCollector assignmentCollector;



    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    public void compileUserProfile(GuiProfiledPrincipal principal, PrismObject<SystemConfigurationType> systemConfiguration, AuthorizationTransformer authorizationTransformer, Task task, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ObjectNotFoundException {

        principal.setApplicableSecurityPolicy(securityHelper.locateSecurityPolicy(principal.getFocus().asPrismObject(), systemConfiguration, task, result));

        List<AdminGuiConfigurationType> adminGuiConfigurations = new ArrayList<>();
        collect(adminGuiConfigurations, principal, authorizationTransformer, task, result);

        CompiledGuiProfile compiledGuiProfile = compileUserProfile(adminGuiConfigurations, systemConfiguration, task, result);
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
            for (EvaluatedAssignmentTarget target : assignment.getRoles().getNonNegativeValues()) {
                if (target.isValid() && target.getTarget() != null && target.getTarget().asObjectable() instanceof UserType
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
                targetCollection.add(autz);
            } else {
                Collection<Authorization> transformedAutzs = authorizationTransformer.transform(autz);
                if (transformedAutzs != null) {
                    targetCollection.addAll(transformedAutzs);
                }
            }
        }
    }

    public CompiledGuiProfile compileUserProfile(@NotNull List<AdminGuiConfigurationType> adminGuiConfigurations,
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
            applyAdminGuiConfiguration(composite, globalAdminGuiConfig, task, result);
        }
        for (AdminGuiConfigurationType adminGuiConfiguration: adminGuiConfigurations) {
            applyAdminGuiConfiguration(composite, adminGuiConfiguration, task, result);
        }
        return composite;
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
        if (adminGuiConfiguration.getDefaultExportSettings() != null) {
            composite.setDefaultExportSettings(adminGuiConfiguration.getDefaultExportSettings().clone());
        }
        if (adminGuiConfiguration.getDisplayFormats() != null){
            composite.setDisplayFormats(adminGuiConfiguration.getDisplayFormats().clone());
        }

        applyViews(composite, adminGuiConfiguration.getObjectLists(), task, result); // Compatibility, deprecated
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
        for (UserInterfaceFeatureType feature: adminGuiConfiguration.getFeature()) {
            mergeFeature(composite, feature.clone());
        }


        if (adminGuiConfiguration.getFeedbackMessagesHook() != null) {
            composite.setFeedbackMessagesHook(adminGuiConfiguration.getFeedbackMessagesHook().clone());
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

        for (GuiObjectListViewType objectCollectionView : viewsType.getObjectList()) { // Compatibility, legacy
            applyView(composite, objectCollectionView, task, result);
        }

        for (GuiObjectListViewType objectCollectionView : viewsType.getObjectCollectionView()) {
            applyView(composite, objectCollectionView, task, result);
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
            LOGGER.error("Error compiling user profile, view '{}': {}", determineViewIdentifier(objectListViewType), e.getMessage(), e);
        }
    }


    private CompiledObjectCollectionView findOrCreateMatchingView(CompiledGuiProfile composite, GuiObjectListViewType objectListViewType) {
        QName objectType = objectListViewType.getType();
        String viewIdentifier = determineViewIdentifier(objectListViewType);
        CompiledObjectCollectionView existingView = composite.findObjectCollectionView(objectType, viewIdentifier);
        if (existingView == null) {
            existingView = new CompiledObjectCollectionView(objectType, viewIdentifier);
            composite.getObjectCollectionViews().add(existingView);
        }
        return existingView;
    }

    private String determineViewIdentifier(GuiObjectListViewType objectListViewType) {
        String viewIdentifier = objectListViewType.getIdentifier();
        if (viewIdentifier != null) {
            return viewIdentifier;
        }
        String viewName = objectListViewType.getName();
        if (viewName != null) {
            // legacy, deprecated
            return viewName;
        }
        CollectionRefSpecificationType collection = objectListViewType.getCollection();
        if (collection == null) {
            return objectListViewType.getType().getLocalPart();
        }
        ObjectReferenceType collectionRef = collection.getCollectionRef();
        if (collectionRef == null) {
            return objectListViewType.getType().getLocalPart();
        }
        return collectionRef.getOid();
    }

    private void compileView(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType, Task task, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ObjectNotFoundException {
        compileActions(existingView, objectListViewType);
        compileAdditionalPanels(existingView, objectListViewType);
        compileColumns(existingView, objectListViewType);
        compileDisplay(existingView, objectListViewType);
        compileDistinct(existingView, objectListViewType);
        compileSorting(existingView, objectListViewType);
        compileCounting(existingView, objectListViewType);
        compileDisplayOrder(existingView, objectListViewType);
        compileSearchBox(existingView, objectListViewType);
        compileCollection(existingView, objectListViewType, task, result);
        compileRefreshInterval(existingView, objectListViewType);
    }

    private void compileActions(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType) {
        List<GuiActionType> newActions = objectListViewType.getAction();
        for (GuiActionType newAction: newActions) {
            // TODO: check for action duplication/override
            existingView.getActions().add(newAction); // No need to clone, CompiledObjectCollectionView is not prism
        }

    }

    private void compileAdditionalPanels(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType) {
        GuiObjectListViewAdditionalPanelsType newAdditionalPanels = objectListViewType.getAdditionalPanels();
        if (newAdditionalPanels == null) {
            return;
        }
        // TODO: later: merge additional panel definitions
        existingView.setAdditionalPanels(newAdditionalPanels);
    }

    private void compileCollection(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType, Task task, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ObjectNotFoundException {
        CollectionRefSpecificationType collectionSpec = objectListViewType.getCollection();
        if (collectionSpec == null) {
            ObjectReferenceType collectionRef = objectListViewType.getCollectionRef();
            if (collectionRef == null) {
                return;
            }
            // Legacy, deprecated
            collectionSpec = new CollectionRefSpecificationType();
            collectionSpec.setCollectionRef(collectionRef.clone());
        }
        if (existingView.getCollection() != null) {
            LOGGER.debug("Redefining collection in view {}", existingView.getViewIdentifier());
        }
        existingView.setCollection(collectionSpec);

        compileCollection(existingView, collectionSpec, task, result);
    }

    private void compileCollection(CompiledObjectCollectionView existingView, CollectionRefSpecificationType collectionSpec, Task task, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ObjectNotFoundException {

        QName targetObjectType = existingView.getObjectType();
        Class<? extends ObjectType> targetTypeClass = ObjectType.class;
        if (targetObjectType != null) {
            targetTypeClass = ObjectTypes.getObjectTypeFromTypeQName(targetObjectType).getClassDefinition();
        }
        collectionProcessor.compileObjectCollectionView(existingView, collectionSpec, targetTypeClass, task, result);
    }

    private void compileColumns(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType) {
        List<GuiObjectColumnType> newColumns = objectListViewType.getColumn();
        if (newColumns == null || newColumns.isEmpty()) {
            return;
        }
        // Not very efficient algorithm. But must do for now.
        List<GuiObjectColumnType> existingColumns = existingView.getColumns();
        existingColumns.addAll(newColumns);
        List<GuiObjectColumnType> orderedList = MiscSchemaUtil.orderCustomColumns(existingColumns);
        existingColumns.clear();
        existingColumns.addAll(orderedList);
    }

    private void compileDisplay(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType) {
        DisplayType newDisplay = objectListViewType.getDisplay();
        if (newDisplay == null) {
            return;
        }
        if (existingView.getDisplay() == null) {
            existingView.setDisplay(newDisplay);
        }
        MiscSchemaUtil.mergeDisplay(existingView.getDisplay(), newDisplay);
    }

    private void compileDistinct(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType) {
        DistinctSearchOptionType newDistinct = objectListViewType.getDistinct();
        if (newDistinct == null) {
            return;
        }
        existingView.setDistinct(newDistinct);
    }

    private void compileSorting(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType) {
        Boolean newDisableSorting = objectListViewType.isDisableSorting();
        if (newDisableSorting != null) {
            existingView.setDisableSorting(newDisableSorting);
        }
    }

    private void compileRefreshInterval(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType) {
        Integer refreshInterval = objectListViewType.getRefreshInterval();
        if (refreshInterval != null) {
            existingView.setRefreshInterval(refreshInterval);
        }
    }

    private void compileCounting(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType) {
        Boolean newDisableCounting = objectListViewType.isDisableCounting();
        if (newDisableCounting != null) {
            existingView.setDisableCounting(newDisableCounting);
        }
    }

    private void compileDisplayOrder(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType){
        Integer newDisplayOrder = objectListViewType.getDisplayOrder();
        if (newDisplayOrder != null){
            existingView.setDisplayOrder(newDisplayOrder);
        }
    }

    private void compileSearchBox(CompiledObjectCollectionView existingView, GuiObjectListViewType objectListViewType) {
        SearchBoxConfigurationType newSearchBoxConfig = objectListViewType.getSearchBoxConfiguration();
        if (newSearchBoxConfig == null) {
            return;
        }
        // TODO: merge
        existingView.setSearchBoxConfiguration(newSearchBoxConfig);
    }

    private void joinForms(ObjectFormsType objectForms, ObjectFormType newForm) {
        objectForms.getObjectForm().removeIf(currentForm -> isTheSameObjectForm(currentForm, newForm));
        objectForms.getObjectForm().add(newForm.clone().id(null));
    }

    private void joinObjectDetails(GuiObjectDetailsSetType objectDetailsSet, GuiObjectDetailsPageType newObjectDetails) {
        objectDetailsSet.getObjectDetailsPage().removeIf(currentDetails -> isTheSameObjectType(currentDetails, newObjectDetails));
        objectDetailsSet.getObjectDetailsPage().add(newObjectDetails.clone());
    }

    private boolean isTheSameObjectType(AbstractObjectTypeConfigurationType oldConf, AbstractObjectTypeConfigurationType newConf) {
        return QNameUtil.match(oldConf.getType(), newConf.getType());
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
        CompiledGuiProfile compiledGuiProfile = compileUserProfile(adminGuiConfigurations, systemConfiguration, task, parentResult);
        // TODO: cache compiled profile
        return compiledGuiProfile;
    }


}
