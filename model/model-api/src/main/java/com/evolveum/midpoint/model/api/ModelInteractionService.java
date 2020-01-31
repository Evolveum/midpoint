/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.TerminateSessionEvent;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.authentication.CompiledUserProfile;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.util.MergeDeltas;
import com.evolveum.midpoint.model.api.visualizer.Scene;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.enforcer.api.ItemSecurityConstraints;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ExecuteCredentialResetRequestType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ExecuteCredentialResetResponseType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.PolicyItemsDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.UserSessionManagementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A service provided by the IDM Model that allows to improve the (user) interaction with the model.
 * It is supposed to provide services such as preview of changes, diagnostics and other informational
 * services. It should only provide access to read-only data or provide a temporary (throw-away) previews
 * of data. It should not change the state of IDM repository, resources or tasks.
 *
 * EXPERIMENTAL/UNSTABLE: This is likely to change at any moment without a notice. Depend on this interface on your own risk.
 *
 * @author Radovan Semancik
 *
 */
@Experimental
public interface ModelInteractionService {

    String CLASS_NAME_WITH_DOT = ModelInteractionService.class.getName() + ".";
    String PREVIEW_CHANGES = CLASS_NAME_WITH_DOT + "previewChanges";
    String GET_EDIT_OBJECT_DEFINITION = CLASS_NAME_WITH_DOT + "getEditObjectDefinition";
    String GET_EDIT_SHADOW_DEFINITION = CLASS_NAME_WITH_DOT + "getEditShadowDefinition";
    String GET_ALLOWED_REQUEST_ASSIGNMENT_ITEMS = CLASS_NAME_WITH_DOT + "getAllowedRequestAssignmentItems";
    String GET_ASSIGNABLE_ROLE_SPECIFICATION = CLASS_NAME_WITH_DOT + "getAssignableRoleSpecification";
    String GET_CREDENTIALS_POLICY = CLASS_NAME_WITH_DOT + "getCredentialsPolicy";
    String GET_AUTHENTICATIONS_POLICY = CLASS_NAME_WITH_DOT + "getAuthenticationsPolicy";
    String GET_REGISTRATIONS_POLICY = CLASS_NAME_WITH_DOT + "getRegistrationsPolicy";
    String GET_SECURITY_POLICY = CLASS_NAME_WITH_DOT + "resolveSecurityPolicy";
    String CHECK_PASSWORD = CLASS_NAME_WITH_DOT + "checkPassword";
    String GET_CONNECTOR_OPERATIONAL_STATUS = CLASS_NAME_WITH_DOT + "getConnectorOperationalStatus";
    String MERGE_OBJECTS_PREVIEW_DELTA = CLASS_NAME_WITH_DOT + "mergeObjectsPreviewDelta";
    String MERGE_OBJECTS_PREVIEW_OBJECT = CLASS_NAME_WITH_DOT + "mergeObjectsPreviewObject";
    String GET_DEPUTY_ASSIGNEES = CLASS_NAME_WITH_DOT + "getDeputyAssignees";
    String SUBMIT_TASK_FROM_TEMPLATE = CLASS_NAME_WITH_DOT + "submitTaskFromTemplate";

    /**
     * Computes the most likely changes triggered by the provided delta. The delta may be any change of any object, e.g.
     * add of a user or change of a shadow. The resulting context will sort that out to "focus" and "projection" as needed.
     * The supplied delta will be used as a primary change. The resulting context will reflect both this primary change and
     * any resulting secondary changes.
     *
     * The changes are only computed, NOT EXECUTED. It also does not change any state of any repository object or task. Therefore
     * this method is safe to use anytime. However it is reading the data from the repository and possibly also from the resources
     * therefore there is still potential for communication (and other) errors and invocation of this method may not be cheap.
     * However, as no operations are really executed there may be issues with resource dependencies. E.g. identifier that are generated
     * by the resource are not taken into account while recomputing the values. This may also cause errors if some expressions depend
     * on the generated values.
     */
    <F extends ObjectType> ModelContext<F> previewChanges(
            Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options, Task task, OperationResult result)
            throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException;

    <F extends ObjectType> ModelContext<F> previewChanges(
            Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options, Task task, Collection<ProgressListener> listeners, OperationResult result)
            throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException;

    <F extends ObjectType> ModelContext<F> unwrapModelContext(LensContextType wrappedContext, Task task, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException;

    /**
     * <p>
     * Returns a schema that reflects editability of the object in terms of midPoint schema limitations and security. This method
     * merges together all the applicable limitations that midPoint knows of (schema, security, other constratints). It may be required
     * to pre-populate new object before calling this method, e.g. to put the object in a correct org in case that delegated administration
     * is used.
     * </p>
     * <p>
     * If null is returned then the access to the entire object is denied. It cannot be created or edited at all.
     * </p>
     * <p>
     * The returned definition contains all parts of static schema and run-time extensions. It does not contain parts of resource
     * "refined" schemas. Therefore for shadows it is only applicable to static parts of the shadow (not attributes).
     * </p>
     * <p>
     * This is <b>not</b> security-sensitive function. It provides data about security constraints but it does <b>not</b> enforce it and
     * it does not modify anything or reveal any data. The purpose of this method is to enable convenient display of GUI form fields,
     * e.g. to hide non-accessible fields from the form. The actual enforcement of the security is executed regardless of this
     * method.
     * </p>
     *
     * @param object object to edit
     * @return schema with correctly set constraint parts or null
     */
    <O extends ObjectType> PrismObjectDefinition<O> getEditObjectDefinition(PrismObject<O> object, AuthorizationPhaseType phase, Task task, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, SecurityViolationException;

    PrismObjectDefinition<ShadowType> getEditShadowDefinition(ResourceShadowDiscriminator discr, AuthorizationPhaseType phase, Task task, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, SecurityViolationException;

    RefinedObjectClassDefinition getEditObjectClassDefinition(PrismObject<ShadowType> shadow, PrismObject<ResourceType> resource, AuthorizationPhaseType phase, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException;

    /**
     * <p>
     * Returns a collection of all authorization actions known to the system. The format of returned data is designed for displaying
     * purposes.
     * </p>
     * <p>
     * Note: this method returns only the list of authorization actions that are known to the IDM Model component and the components
     * below. It does <b>not</b> return a GUI-specific authorization actions.
     * </p>
     *
     * @return
     */
    Collection<? extends DisplayableValue<String>> getActionUrls();

    /**
     * Returns an object that defines which roles can be assigned by the currently logged-in user.
     *
     * @param assignmentHolder Object of the operation. The object (usually user) to whom the roles should be assigned.
     * @param assignmentOrder order=0 means assignment, order>0 means inducement
     */
    <H extends AssignmentHolderType, R extends AbstractRoleType> RoleSelectionSpecification getAssignableRoleSpecification(PrismObject<H> assignmentHolder, Class<R> targetType, int assignmentOrder, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException, SecurityViolationException;

    /**
     * Returns filter for lookup of donors or power of attorney. The donors are the users that have granted
     * the power of attorney to the currently logged-in user.
     *
     * TODO: authorization limitations
     *
     * @param searchResultType type of the expected search results
     * @param origFilter original filter (e.g. taken from GUI search bar)
     * @param targetAuthorizationAction Authorization action that the attorney is trying to execute
     *                 on behalf of donor. Only donors for which the use of this authorization was
     *                 not limited will be returned (that does not necessarily mean that the donor
     *                 is able to execute this action, it may be limited by donor's authorizations).
     *                 If the parameter is null then all donors are returned.
     * @param task task
     * @param parentResult operation result
     * @return original filter with AND clause limiting the search.
     */
    <T extends ObjectType> ObjectFilter getDonorFilter(Class<T> searchResultType, ObjectFilter origFilter, String targetAuthorizationAction, Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException;

    /**
     * TODO
     * Question: does object make any sense here? E.g. when searching role members, the role OID should be determined from the query.
     *
     * @param includeSpecial include special authorizations, such as "self". If set to false those authorizations
     *                       will be ignored. This is a good way to avoid interference of "self" when checking for
     *                       authorizations such as ability to display role members.
     */
    <T extends ObjectType, O extends ObjectType> boolean canSearch(Class<T> resultType, Class<O> objectType, String objectOid, boolean includeSpecial, ObjectQuery query, Task task, OperationResult result)  throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException ;

    /**
     * Returns decisions for individual items for "assign" authorization. This is usually applicable to assignment parameters.
     * The decisions are evaluated using the security context of a currently logged-in user.
     *
     * @param object object of the operation (user)
     * @param target target of the operation (role, org, service that is being assigned)
     */
    <O extends ObjectType,R extends AbstractRoleType> ItemSecurityConstraints getAllowedRequestAssignmentItems(PrismObject<O> object, PrismObject<R> target, Task task, OperationResult result) throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

    SecurityPolicyType getSecurityPolicy(PrismObject<UserType> user, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

    /**
     * Returns an authentications policies as defined in the system configuration security policy. This method is designed to be used
     * during registration process or reset password process.
     * security questions, etc).
     *
     *
     * @param task
     * @param parentResult
     * @return applicable credentials policy or null
     * @throws ObjectNotFoundException No system configuration or other major system inconsistency
     * @throws SchemaException Wrong schema or content of security policy
     */
    AuthenticationsPolicyType getAuthenticationPolicy(PrismObject<UserType> user, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

    /**
     * Returns a policy for registration, e.g. type of the supported registrations (self, social,...)
     *
     * @param user user for who the policy should apply
     * @param task
     * @param parentResult
     * @return applicable credentials policy or null
     * @throws ObjectNotFoundException No system configuration or other major system inconsistency
     * @throws SchemaException Wrong schema or content of security policy
     * @deprecated
     */
    RegistrationsPolicyType getRegistrationPolicy(PrismObject<UserType> user, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

    /**
     * Returns a policy for registration, e.g. type of the supported registrations (self, social,...)
     *
     * @param user user for who the policy should apply
     * @param task
     * @param parentResult
     * @return applicable credentials policy or null
     * @throws ObjectNotFoundException No system configuration or other major system inconsistency
     * @throws SchemaException Wrong schema or content of security policy
     */
    RegistrationsPolicyType getFlowPolicy(PrismObject<UserType> user, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

        /**
     * Returns a credential policy that applies to the specified user. This method is designed to be used
     * during credential reset so the GUI has enough information to set up the credential (e.g. password policies,
     * security questions, etc).
     *
     * @param user user for who the policy should apply
     * @param task
     * @param parentResult
     * @return applicable credentials policy or null
     * @throws ObjectNotFoundException No system configuration or other major system inconsistency
     * @throws SchemaException Wrong schema or content of security policy
     */
    CredentialsPolicyType getCredentialsPolicy(PrismObject<UserType> user, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

    /**
     * Returns currently applicable user profile, compiled for efficient use in the user interface.
     * Use profile contains configuration, customization and user preferences for the user interface.
     * Note: This operation bypasses the authorizations. It will always return the value regardless whether
     * the current user is authorized to read the underlying objects or not. However, it will always return only
     * values applicable for current user, therefore the authorization might be considered to be implicit in this case.
     */
    @NotNull
    CompiledUserProfile getCompiledUserProfile(Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

    /**
     * @return list of logged in users with at least 1 active session (clusterwide)
     */
    List<UserSessionManagementType> getLoggedInPrincipals(Task task, OperationResult result);

    /**
     * Terminates specified sessions (clusterwide).
     */
    void terminateSessions(TerminateSessionEvent terminateSessionEvent, Task task, OperationResult result);

    SystemConfigurationType getSystemConfiguration(OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

    DeploymentInformationType getDeploymentInformationConfiguration(OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

    SystemConfigurationAuditType getAuditConfiguration(OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

    List<MergeConfigurationType> getMergeConfiguration(OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

    AccessCertificationConfigurationType getCertificationConfiguration(OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException;

    /**
     * Checks if the supplied password matches with current user password. This method is NOT subject to any
     * password expiration policies, it does not update failed login counters, it does not change any data or meta-data.
     * This method is NOT SUPPOSED to be used to validate password on login. This method is supposed to check
     * old password when the password is changed by the user. We assume that the user already passed normal
     * system authentication.
     *
     * Note: no authorizations are checked in the implementation. It is assumed that authorizations will be
     * enforced at the page level.
     *
     * @return true if the password matches, false otherwise
     */
    boolean checkPassword(String userOid, ProtectedStringType password, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

    // TEMPORARY
    List<? extends Scene> visualizeDeltas(List<ObjectDelta<? extends ObjectType>> deltas, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException;

    @NotNull
    Scene visualizeDelta(ObjectDelta<? extends ObjectType> delta, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException;

    @NotNull
    Scene visualizeDelta(ObjectDelta<? extends ObjectType> delta, ObjectReferenceType objectRef, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException;

    List<ConnectorOperationalStatus> getConnectorOperationalStatus(String resourceOid, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException;

    <O extends ObjectType> MergeDeltas<O> mergeObjectsPreviewDeltas(Class<O> type,
            String leftOid, String rightOid, String mergeConfigurationName, Task task, OperationResult result)
                    throws ObjectNotFoundException, SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException, SecurityViolationException ;

    <O extends ObjectType> PrismObject<O> mergeObjectsPreviewObject(Class<O> type,
            String leftOid, String rightOid, String mergeConfigurationName, Task task, OperationResult result)
                    throws ObjectNotFoundException, SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException, SecurityViolationException ;

    /**
     * TEMPORARY. Need to find out better way how to deal with generated values
     *
     * @param policy
     * @param defaultLength
     * @param generateMinimalSize
     * @param object object for which we generate the value (e.g. user or shadow)
     * @param inputResult
     * @return
     * @throws ExpressionEvaluationException
     */
    <O extends ObjectType> String generateValue(ValuePolicyType policy, int defaultLength, boolean generateMinimalSize,
            PrismObject<O> object, String shortDesc, Task task, OperationResult inputResult) throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException;

    <O extends ObjectType> void generateValue(
            PrismObject<O> object, PolicyItemsDefinitionType policyItemsDefinition, Task task, OperationResult parentResult) throws ObjectNotFoundException,
            SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException;

    <O extends ObjectType> void validateValue(PrismObject<O> object, PolicyItemsDefinitionType policyItemsDefinition, Task task,
            OperationResult parentResult) throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException, PolicyViolationException;

    /**
     * Gets "deputy assignees" i.e. users that are deputies of assignees. Takes limitations into account.
     *
     * MAY NOT CHECK AUTHORIZATIONS (uses repository directly, at least at some places) - TODO
     * TODO parameterize on limitation kind
     */
    @NotNull
    List<ObjectReferenceType> getDeputyAssignees(AbstractWorkItemType workItem, Task task, OperationResult parentResult)
            throws SchemaException;

    @NotNull
    List<ObjectReferenceType> getDeputyAssignees(ObjectReferenceType assignee, QName limitationItemName, Task task, OperationResult parentResult)
            throws SchemaException;

    /**
     * Computes effective status for the current ActivationType in for an assignment
     */
    ActivationStatusType getAssignmentEffectiveStatus(String lifecycleStatus, ActivationType activationType);

    MidPointPrincipal assumePowerOfAttorney(PrismObject<UserType> donor, Task task, OperationResult result)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

    MidPointPrincipal dropPowerOfAttorney(Task task, OperationResult result)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

    // Maybe a bit of hack: used to deduplicate processing of localizable message templates
    @NotNull
    LocalizableMessageType createLocalizableMessageType(LocalizableMessageTemplateType template,
            VariablesMap variables, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException;

    ExecuteCredentialResetResponseType executeCredentialsReset(PrismObject<UserType> user,
            ExecuteCredentialResetRequestType executeCredentialResetRequest, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException;

    void refreshPrincipal(String oid) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

    List<RelationDefinitionType> getRelationDefinitions();

    @NotNull
    TaskType submitTaskFromTemplate(String templateTaskOid, List<Item<?, ?>> extensionItems, Task opTask, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException;

    @NotNull
    TaskType submitTaskFromTemplate(String templateTaskOid, Map<QName, Object> extensionValues, Task opTask, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException;

    /**
     * Efficiently determines information about archetype policy applicable for a particular object.
     * Returns null if no archetype policy is applicable.
     * This is a "one stop" method for archetype policy in the GUI. The method returns archetype policy even
     * for "legacy" situations, e.g. if the policy needs to be determined from system configuration using legacy subtype.
     * GUI should not need to to any other processing to determine archetype information.
     *
     * This method is invoked very often, usually when any object is displayed (including display of object lists
     * and search results). Therefore this method is supposed to be very efficient.
     * It should be using caching as much as possible.
     */
    <O extends AssignmentHolderType> ArchetypePolicyType determineArchetypePolicy(PrismObject<O> assignmentHolder, OperationResult result) throws SchemaException, ConfigurationException;

    /**
     * Returns data structure that contains information about possible assignment targets for a particular holder object.
     *
     * This method should be used when editing assignment holder (e.g. user) and looking for available assignment target.
     * The determineAssignmentHolderSpecification is a "reverse" version of this method.
     *
     * This method is not used that often. It is used when an object is edited. But it should be quite efficient anyway.
     * It should use cached archetype information.
     */
    <O extends AssignmentHolderType> AssignmentCandidatesSpecification determineAssignmentTargetSpecification(PrismObject<O> assignmentHolder, OperationResult result) throws SchemaException, ConfigurationException;

    /**
     * Returns data structure that contains information about possible assignment holders for a particular target object.
     *
     * This method should be used when editing assignment target (role, org, service) and looking for object that
     * can be potential members. The determineAssignmentTargetSpecification is a "reverse" version of this method.
     *
     * This method is not used that often. It is used when an object is edited. But it should be quite efficient anyway.
     * It should use cached archetype information.
     */
    <O extends AbstractRoleType> AssignmentCandidatesSpecification determineAssignmentHolderSpecification(PrismObject<O> assignmentTarget, OperationResult result) throws SchemaException, ConfigurationException;

    /**
     * Returns all policy rules that apply to the collection.
     * Later, the policy rules are compiled from all the applicable sources (target, meta-roles, etc.).
     * But for now we support only policy rules that are directly placed in collection assignments.
     * EXPERIMENTAL. Quite likely to change later.
     */
    @Experimental
    @NotNull
    Collection<EvaluatedPolicyRule> evaluateCollectionPolicyRules(@NotNull PrismObject<ObjectCollectionType> collection, @Nullable CompiledObjectCollectionView collectionView, @Nullable Class<? extends ObjectType> targetTypeClass, @NotNull Task task, @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException;

    @Experimental
    @NotNull
    CompiledObjectCollectionView compileObjectCollectionView(@NotNull PrismObject<ObjectCollectionType> collection, @Nullable Class<? extends ObjectType> targetTypeClass, @NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, ObjectNotFoundException;

    @Experimental
    @NotNull
    <O extends ObjectType> CollectionStats determineCollectionStats(@NotNull CompiledObjectCollectionView collectionView, @NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, ConfigurationException, CommunicationException, ExpressionEvaluationException;

}
