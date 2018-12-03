/**
 * Copyright (c) 2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.impl.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignmentTarget;
import com.evolveum.midpoint.model.api.util.DeputyUtils;
import com.evolveum.midpoint.model.api.util.ModelUtils;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.lens.AssignmentEvaluator;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensContextPlaceholder;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.projector.MappingEvaluator;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.AdminGuiConfigTypeUtil;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationTransformer;
import com.evolveum.midpoint.security.api.DelegatorWithOtherPrivilegesLimitations;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractObjectTypeConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationRoleManagementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardLayoutType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectColumnType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectDetailsPageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectDetailsSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectListViewType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectListViewsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFormType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFormsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectPolicyConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtherPrivilegesLimitationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceElementVisibilityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceFeatureType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

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
public class UserProfileCompiler {
	
	private static final Trace LOGGER = TraceManager.getTrace(UserProfileCompiler.class);
	
	@Autowired private SecurityHelper securityHelper;
	@Autowired private SystemObjectCache systemObjectCache;
	@Autowired private RelationRegistry relationRegistry;
	@Autowired private PrismContext prismContext;
	@Autowired private MappingFactory mappingFactory;
	@Autowired private MappingEvaluator mappingEvaluator;
	@Autowired private ActivationComputer activationComputer;
	@Autowired private Clock clock;
	@Autowired @Qualifier("modelObjectResolver") private ObjectResolver objectResolver;
	
	@Autowired
	@Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;
	
	public void compileUserProfile(MidPointPrincipal principal, PrismObject<SystemConfigurationType> systemConfiguration, AuthorizationTransformer authorizationTransformer, Task task, OperationResult result) throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		UserType userType = principal.getUser();

		Collection<Authorization> authorizations = principal.getAuthorities();
		List<AdminGuiConfigurationType> adminGuiConfigurations = new ArrayList<>();

        principal.setApplicableSecurityPolicy(securityHelper.locateSecurityPolicy(userType.asPrismObject(), systemConfiguration, task, result));
        
		if (!userType.getAssignment().isEmpty()) {
			LensContext<UserType> lensContext = createAuthenticationLensContext(userType.asPrismObject(), systemConfiguration);
			AssignmentEvaluator.Builder<UserType> builder =
					new AssignmentEvaluator.Builder<UserType>()
							.repository(repositoryService)
							.focusOdo(new ObjectDeltaObject<>(userType.asPrismObject(), null, userType.asPrismObject()))
							.channel(null)
							.objectResolver(objectResolver)
							.systemObjectCache(systemObjectCache)
							.relationRegistry(relationRegistry)
							.prismContext(prismContext)
							.mappingFactory(mappingFactory)
							.mappingEvaluator(mappingEvaluator)
							.activationComputer(activationComputer)
							.now(clock.currentTimeXMLGregorianCalendar())
							// We do need only authorizations + gui config. Therefore we not need to evaluate
							// constructions and the like, so switching it off makes the evaluation run faster.
							// It also avoids nasty problems with resources being down,
							// resource schema not available, etc.
							.loginMode(true)
							// We do not have real lens context here. But the push methods in ModelExpressionThreadLocalHolder
							// will need something to push on the stack. So give them context placeholder.
							.lensContext(lensContext);

			AssignmentEvaluator<UserType> assignmentEvaluator = builder.build();

			Collection<AssignmentType> collectedAssignments = new HashSet<>();
			collectedAssignments.addAll(userType.getAssignment());
			
			try {
				Collection<AssignmentType> forcedAssignments = LensUtil.getForcedAssignments(lensContext.getFocusContext().getLifecycleModel(), 
						userType.getLifecycleState(), objectResolver, prismContext, task, result);
				if (forcedAssignments != null) {
					collectedAssignments.addAll(forcedAssignments);
				}
			} catch (ObjectNotFoundException | CommunicationException | ConfigurationException | SecurityViolationException
					| ExpressionEvaluationException e1) {
				LOGGER.error("Forced assignments defined for lifecycle {} won't be evaluated", userType.getLifecycleState(), e1);
			}
			
			try {
				RepositoryCache.enter();
				for (AssignmentType assignmentType: collectedAssignments) {
					try {
						ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi = new ItemDeltaItem<>();
						assignmentIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(assignmentType));
						assignmentIdi.recompute();
						EvaluatedAssignment<UserType> assignment = assignmentEvaluator.evaluate(assignmentIdi, PlusMinusZero.ZERO, false, userType, userType.toString(), task, result);
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
					} catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException | PolicyViolationException | SecurityViolationException | ConfigurationException | CommunicationException e) {
						LOGGER.error("Error while processing assignment of {}: {}; assignment: {}",
								userType, e.getMessage(), assignmentType, e);
					}
				}
			} finally {
				RepositoryCache.exit();
			}
		}
		if (userType.getAdminGuiConfiguration() != null) {
			// config from the user object should go last (to be applied as the last one)
			adminGuiConfigurations.add(userType.getAdminGuiConfiguration());
		}
        principal.setAdminGuiConfiguration(compileAdminGuiConfiguration(adminGuiConfigurations, systemConfiguration));
	}

	private LensContext<UserType> createAuthenticationLensContext(PrismObject<UserType> user, PrismObject<SystemConfigurationType> systemConfiguration) throws SchemaException {
		LensContext<UserType> lensContext = new LensContextPlaceholder<>(user, prismContext);
		if (systemConfiguration != null) {
			ObjectPolicyConfigurationType policyConfigurationType = determineObjectPolicyConfiguration(user, systemConfiguration);
			lensContext.getFocusContext().setObjectPolicyConfigurationType(policyConfigurationType);
		}
		return lensContext;
	}

	private ObjectPolicyConfigurationType determineObjectPolicyConfiguration(PrismObject<UserType> user, PrismObject<SystemConfigurationType> systemConfiguration) throws SchemaException {
		ObjectPolicyConfigurationType policyConfigurationType;
		try {
			policyConfigurationType = ModelUtils.determineObjectPolicyConfiguration(user, systemConfiguration.asObjectable());
		} catch (ConfigurationException e) {
			throw new SchemaException(e.getMessage(), e);
		}
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Selected policy configuration from subtypes {}:\n{}", 
					FocusTypeUtil.determineSubTypes(user), policyConfigurationType==null?null:policyConfigurationType.asPrismContainerValue().debugDump(1));
		}
		
		return policyConfigurationType;
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
	
	public AdminGuiConfigurationType compileAdminGuiConfiguration(@NotNull List<AdminGuiConfigurationType> adminGuiConfigurations,
			PrismObject<SystemConfigurationType> systemConfiguration) {

		// if there's no admin config at all, return null (to preserve original behavior)
		if (adminGuiConfigurations.isEmpty() &&
				(systemConfiguration == null || systemConfiguration.asObjectable().getAdminGuiConfiguration() == null)) {
			return null;
		}

		AdminGuiConfigurationType composite = new AdminGuiConfigurationType();
		if (systemConfiguration != null) {
			applyAdminGuiConfiguration(composite, systemConfiguration.asObjectable().getAdminGuiConfiguration());
		}
		for (AdminGuiConfigurationType adminGuiConfiguration: adminGuiConfigurations) {
			applyAdminGuiConfiguration(composite, adminGuiConfiguration);
		}
		return composite;
	}

	private void applyAdminGuiConfiguration(AdminGuiConfigurationType composite, AdminGuiConfigurationType adminGuiConfiguration) {
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
		if (adminGuiConfiguration.getObjectLists() != null) {
			if (composite.getObjectLists() == null) {
				composite.setObjectLists(adminGuiConfiguration.getObjectLists().clone());
			} else {
				for (GuiObjectListViewType objectList: adminGuiConfiguration.getObjectLists().getObjectList()) {
					mergeList(composite.getObjectLists(), objectList.clone());
				}
			}
		}
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
					mergeWidget(composite.getUserDashboard(), widget);
				}
			}
		}
		for (UserInterfaceFeatureType feature: adminGuiConfiguration.getFeature()) {
			mergeFeature(composite.getFeature(), feature.clone());
		}
		if (composite.getObjectLists() != null && composite.getObjectLists().getObjectList() != null){
			for (GuiObjectListViewType objectListType : composite.getObjectLists().getObjectList()){
				if (objectListType.getColumn() != null) {
//					objectListType.getColumn().clear();
//					objectListType.getColumn().addAll(orderCustomColumns(objectListType.getColumn()));
					List<GuiObjectColumnType> orderedList = orderCustomColumns(objectListType.getColumn());
					objectListType.getColumn().clear();
					objectListType.getColumn().addAll(orderedList);
				}
			}
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

	private void joinForms(ObjectFormsType objectForms, ObjectFormType newForm) {
		objectForms.getObjectForm().removeIf(currentForm -> isTheSameObjectForm(currentForm, newForm));
		objectForms.getObjectForm().add(newForm.clone());
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

	private void mergeList(GuiObjectListViewsType objectLists, GuiObjectListViewType newList) {
		// We support only the default object lists now, so simply replace the existing definition with the
		// latest definition. We will need a more sophisticated merging later.
		objectLists.getObjectList().removeIf(currentList -> currentList.getType().equals(newList.getType()));
		objectLists.getObjectList().add(newList.clone());
	}

	private void mergeWidget(DashboardLayoutType compositeDashboard, DashboardWidgetType newWidget) {
		String newWidgetIdentifier = newWidget.getIdentifier();
		DashboardWidgetType compositeWidget = AdminGuiConfigTypeUtil.findWidget(compositeDashboard, newWidgetIdentifier);
		if (compositeWidget == null) {
			compositeDashboard.getWidget().add(newWidget.clone());
		} else {
			mergeWidget(compositeWidget, newWidget);
		}
	}
	
	private void mergeWidget(DashboardWidgetType compositeWidget, DashboardWidgetType newWidget) {
		mergeFeature(compositeWidget, newWidget, UserInterfaceElementVisibilityType.VACANT);
		// merge other widget properties (in the future)
	}

	private void mergeFeature(List<UserInterfaceFeatureType> compositeFeatures, UserInterfaceFeatureType newFeature) {
		String newIdentifier = newFeature.getIdentifier();
		UserInterfaceFeatureType compositeFeature = AdminGuiConfigTypeUtil.findFeature(compositeFeatures, newIdentifier);
		if (compositeFeature == null) {
			compositeFeatures.add(newFeature.clone());
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

	/*
	the ordering algorithm is: the first level is occupied by
	the column which previousColumn == null || "" || notExistingColumnNameValue.
	Each next level contains columns which
	previousColumn == columnNameFromPreviousLevel
	 */
	private List<GuiObjectColumnType> orderCustomColumns(List<GuiObjectColumnType> customColumns){
		if (customColumns == null || customColumns.size() == 0){
			return new ArrayList<>();
		}
		List<GuiObjectColumnType> customColumnsList = new ArrayList<>(customColumns);
		List<String> previousColumnValues = new ArrayList<>();
		previousColumnValues.add(null);
		previousColumnValues.add("");

		Map<String, String> columnRefsMap = new HashMap<>();
		for (GuiObjectColumnType column : customColumns){
			columnRefsMap.put(column.getName(), column.getPreviousColumn() == null ? "" : column.getPreviousColumn());
		}

		List<String> temp = new ArrayList<> ();
		int index = 0;
		while (index < customColumns.size()){
			int sortFrom = index;
			for (int i = index; i < customColumnsList.size(); i++){
				GuiObjectColumnType column = customColumnsList.get(i);
				if (previousColumnValues.contains(column.getPreviousColumn()) ||
						!columnRefsMap.containsKey(column.getPreviousColumn())){
					Collections.swap(customColumnsList, index, i);
					index++;
					temp.add(column.getName());
				}
			}
			if (temp.size() == 0){
				temp.add(customColumnsList.get(index).getName());
				index++;
			}
			if (index - sortFrom > 1){
				customColumnsList.subList(sortFrom, index - 1)
						.sort((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()));
			}
			previousColumnValues.clear();
			previousColumnValues.addAll(temp);
			temp.clear();
		}
		return customColumnsList;
	}


}
