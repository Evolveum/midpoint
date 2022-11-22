/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType.*;

import java.io.Serializable;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.AbstractOptions;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

/**
 * Options for execution of Model operations. These options influence the way how the operations are executed.
 * The options are not mandatory. All options have reasonable default values. They may not be specified at all.
 *
 * @author semancik
 */
@SuppressWarnings("UnusedReturnValue")
public class ModelExecuteOptions extends AbstractOptions implements Serializable, Cloneable {

    /**
     * Majority of the content is present also in ModelExecuteOptionsType.
     * So let's reuse the schema instead of duplicating it.
     */
    @NotNull private final ModelExecuteOptionsType content;

    /**
     * Is this operation already authorized, i.e. should it be executed without any further authorization checks?
     * EXPERIMENTAL. Currently supported only for raw executions.
     */
    private Boolean preAuthorized;

    /**
     * Processes all assignment relations on recompute. Used for computing all assignments.
     * TEMPORARY. EXPERIMENTAL. Should be replaced by something more generic (e.g. setting optimization level).
     * Therefore we do not currently put this to XML version of the options.
     */
    private Boolean evaluateAllAssignmentRelationsOnRecompute;

    /**
     * Traces the model operation execution.
     * EXPERIMENTAL. (So not put into XML version of the options yet.)
     */
    private TracingProfileType tracingProfile;

    @Deprecated
    public ModelExecuteOptions(PrismContext ignored) {
        content = new ModelExecuteOptionsType();
    }

    public ModelExecuteOptions() {
        content = new ModelExecuteOptionsType();
    }

    private ModelExecuteOptions(@NotNull ModelExecuteOptionsType content) {
        this.content = content;
    }

    /** Assumes that {@link PrismContext#get()} can be called. Which usually is, after midPoint is initialized. */
    public static ModelExecuteOptions create() {
        return new ModelExecuteOptions();
    }

    @Deprecated(forRemoval = true)
    public static ModelExecuteOptions create(PrismContext ignored) {
        return new ModelExecuteOptions();
    }

    public static ModelExecuteOptions create(ModelExecuteOptions original) {
        return original != null ? original.clone() : new ModelExecuteOptions();
    }

    public static boolean is(ModelExecuteOptions options, ItemName itemName) {
        return is(options, itemName, false);
    }

    public static boolean is(ModelExecuteOptions options, ItemName itemName, boolean defaultValue) {
        if (options == null) {
            return defaultValue;
        }
        //noinspection unchecked
        Boolean value = (Boolean) options.content.asPrismContainerValue().getPropertyRealValue(itemName, Boolean.class);
        return value != null ? value : defaultValue;
    }

    //region Extension processing
    @SuppressWarnings("WeakerAccess")
    public <T> T getExtensionItemRealValue(ItemName name, Class<T> clazz) {
        Item<?, ?> item = content.asPrismContainerValue().findItem(ItemPath.create(F_EXTENSION, name));
        return item != null ? item.getRealValue(clazz) : null;
    }

    public static <T> T getExtensionItemRealValue(ModelExecuteOptions options, ItemName name, Class<T> clazz) {
        return options != null ? options.getExtensionItemRealValue(name, clazz) : null;
    }

    public ModelExecuteOptions setExtensionPropertyRealValues(PrismContext prismContext, ItemName propertyName, Object... values)
            throws SchemaException {
        ObjectTypeUtil.setExtensionPropertyRealValues(prismContext, content.asPrismContainerValue(), propertyName, values);
        return this;
    }
    //endregion

    //region Specific methods

    public Boolean getForce() {
        return content.isForce();
    }

    public ModelExecuteOptions force(Boolean force) {
        content.setForce(force);
        return this;
    }

    public ModelExecuteOptions force() {
        return force(true);
    }

    public static boolean isForce(ModelExecuteOptions options) {
        return is(options, ModelExecuteOptionsType.F_FORCE);
    }

    public Boolean getPushChanges() {
        return content.isPushChanges();
    }

    public static boolean isPushChanges(ModelExecuteOptions options) {
        return is(options, F_PUSH_CHANGES);
    }

    public ModelExecuteOptions pushChanges(Boolean value) {
        content.setPushChanges(value);
        return this;
    }

    public ModelExecuteOptions pushChanges() {
        return pushChanges(true);
    }

    public Boolean getRaw() {
        return content.isRaw();
    }

    public ModelExecuteOptions raw(Boolean raw) {
        content.setRaw(raw);
        return this;
    }

    public ModelExecuteOptions raw() {
        return raw(true);
    }

    public static boolean isRaw(ModelExecuteOptions options) {
        return is(options, ModelExecuteOptionsType.F_RAW);
    }

    public static Boolean getRaw(ModelExecuteOptions options) {
        return options != null ? options.getRaw() : null;
    }

    @Deprecated // kept because of (expected) external uses; use create(prismContext).raw() instead
    public static ModelExecuteOptions createRaw() {
        ModelExecuteOptions opts = new ModelExecuteOptions();
        opts.raw(true);
        return opts;
    }

    public Boolean getNoCrypt() {
        return content.isNoCrypt();
    }

    public ModelExecuteOptions noCrypt(Boolean noCrypt) {
        content.setNoCrypt(noCrypt);
        return this;
    }

    public static boolean isNoCrypt(ModelExecuteOptions options) {
        return is(options, F_NO_CRYPT);
    }

    public Boolean getReconcile() {
        return content.isReconcile();
    }

    public ModelExecuteOptions reconcile(Boolean reconcile) {
        content.setReconcile(reconcile);
        return this;
    }

    public ModelExecuteOptions reconcile() {
        return reconcile(true);
    }

    public static boolean isReconcile(ModelExecuteOptions options) {
        return is(options, ModelExecuteOptionsType.F_RECONCILE);
    }

    @Deprecated // kept because of (expected) external uses; use create().reconcile() instead
    public static ModelExecuteOptions createReconcile() {
        return new ModelExecuteOptions()
                .reconcile();
    }

    public ModelExecuteOptions reconcileFocus(Boolean reconcileFocus) {
        content.setReconcileFocus(reconcileFocus);
        return this;
    }

    public ModelExecuteOptions reconcileFocus() {
        return reconcileFocus(true);
    }

    public static boolean isReconcileFocus(ModelExecuteOptions options) {
        return is(options, ModelExecuteOptionsType.F_RECONCILE_FOCUS);
    }

    public Boolean getOverwrite() {
        return content.isOverwrite();
    }

    public ModelExecuteOptions overwrite(Boolean overwrite) {
        content.setOverwrite(overwrite);
        return this;
    }

    public ModelExecuteOptions overwrite() {
        return overwrite(true);
    }

    public static boolean isOverwrite(ModelExecuteOptions options) {
        return is(options, F_OVERWRITE);
    }

    // Intentionally using "set" to avoid confusion with asking on "isImport"
    @SuppressWarnings("WeakerAccess")
    public ModelExecuteOptions setIsImport(Boolean isImport) {
        content.setIsImport(isImport);
        return this;
    }

    // Intentionally using "set" to avoid confusion with asking on "isImport"
    public ModelExecuteOptions setIsImport() {
        setIsImport(true);
        return this;
    }

    public static boolean isIsImport(ModelExecuteOptions options) {
        return is(options, F_IS_IMPORT);
    }

    public ModelExecuteOptions executeImmediatelyAfterApproval(Boolean executeImmediatelyAfterApproval) {
        content.setExecuteImmediatelyAfterApproval(executeImmediatelyAfterApproval);
        return this;
    }

    public ModelExecuteOptions executeImmediatelyAfterApproval() {
        return executeImmediatelyAfterApproval(true);
    }

    public static boolean isExecuteImmediatelyAfterApproval(ModelExecuteOptions options) {
        return is(options, F_EXECUTE_IMMEDIATELY_AFTER_APPROVAL);
    }

    public Boolean getLimitPropagation() {
        return content.isLimitPropagation();
    }

    public ModelExecuteOptions limitPropagation(Boolean limitPropagation) {
        content.setLimitPropagation(limitPropagation);
        return this;
    }

    public static boolean isLimitPropagation(ModelExecuteOptions options) {
        return is(options, F_LIMIT_PROPAGATION);
    }

    public ModelExecuteOptions reevaluateSearchFilters(Boolean reevaluateSearchFilters) {
        content.setReevaluateSearchFilters(reevaluateSearchFilters);
        return this;
    }

    public static boolean isReevaluateSearchFilters(ModelExecuteOptions options) {
        return is(options, F_REEVALUATE_SEARCH_FILTERS);
    }

    @SuppressWarnings("WeakerAccess")
    public void preAuthorized(Boolean value) {
        this.preAuthorized = value;
    }

    public ModelExecuteOptions preAuthorized() {
        preAuthorized(true);
        return this;
    }

    public static boolean isPreAuthorized(ModelExecuteOptions options) {
        return options != null && options.preAuthorized != null && options.preAuthorized;
    }

    @SuppressWarnings("WeakerAccess")
    public OperationBusinessContextType getRequestBusinessContext() {
        return content.getRequestBusinessContext();
    }

    public ModelExecuteOptions requestBusinessContext(OperationBusinessContextType requestBusinessContext) {
        content.setRequestBusinessContext(requestBusinessContext);
        return this;
    }

    public static OperationBusinessContextType getRequestBusinessContext(ModelExecuteOptions options) {
        if (options == null) {
            return null;
        }
        return options.getRequestBusinessContext();
    }

    public PartialProcessingOptionsType getPartialProcessing() {
        return content.getPartialProcessing();
    }

    public ModelExecuteOptions partialProcessing(PartialProcessingOptionsType partialProcessing) {
        content.setPartialProcessing(partialProcessing);
        return this;
    }

    public static PartialProcessingOptionsType getPartialProcessing(ModelExecuteOptions options) {
        if (options == null) {
            return null;
        }
        return options.getPartialProcessing();
    }

    public PartialProcessingOptionsType getInitialPartialProcessing() {
        return content.getInitialPartialProcessing();
    }

    public ModelExecuteOptions initialPartialProcessing(PartialProcessingOptionsType initialPartialProcessing) {
        content.setInitialPartialProcessing(initialPartialProcessing);
        return this;
    }

    public static PartialProcessingOptionsType getInitialPartialProcessing(ModelExecuteOptions options) {
        if (options == null) {
            return null;
        }
        return options.getInitialPartialProcessing();
    }

    @SuppressWarnings("WeakerAccess")
    public ConflictResolutionType getFocusConflictResolution() {
        return content.getFocusConflictResolution();
    }

    public ModelExecuteOptions focusConflictResolution(ConflictResolutionType focusConflictResolution) {
        content.setFocusConflictResolution(focusConflictResolution);
        return this;
    }

    public static ConflictResolutionType getFocusConflictResolution(ModelExecuteOptions options) {
        if (options == null) {
            return null;
        }
        return options.getFocusConflictResolution();
    }

    @SuppressWarnings("WeakerAccess")
    public ModelExecuteOptions evaluateAllAssignmentRelationsOnRecompute(Boolean evaluateAllAssignmentRelationsOnRecompute) {
        this.evaluateAllAssignmentRelationsOnRecompute = evaluateAllAssignmentRelationsOnRecompute;
        return this;
    }

    public ModelExecuteOptions evaluateAllAssignmentRelationsOnRecompute() {
        return evaluateAllAssignmentRelationsOnRecompute(true);
    }

    public static boolean isEvaluateAllAssignmentRelationsOnRecompute(ModelExecuteOptions options) {
        return options != null && isTrue(options.evaluateAllAssignmentRelationsOnRecompute);
    }

    public TracingProfileType getTracingProfile() {
        return tracingProfile;
    }

    public ModelExecuteOptions tracingProfile(TracingProfileType tracingProfile) {
        this.tracingProfile = tracingProfile;
        return this;
    }

    public static TracingProfileType getTracingProfile(ModelExecuteOptions options) {
        return options != null ? options.tracingProfile : null;
    }

    // TEMPORARY
    public ModelExecuteOptions reconcileAffected(Boolean value) {
        content.setReconcileAffected(value);
        return this;
    }

    public static boolean isReconcileAffected(ModelExecuteOptions options) {
        return is(options, F_RECONCILE_AFFECTED);
    }

    public SimulationOptionsType getSimulationOptions() {
        return content.getSimulation();
    }

    public static boolean isAdvanceSequenceSafe(@Nullable ModelExecuteOptions options) {
        if (options == null) {
            return false;
        }
        SimulationOptionsType simulationOptions = options.getSimulationOptions();
        return simulationOptions != null
                && simulationOptions.getSequence() == SimulationOptionType.SAFE;
    }

    public ModelExecuteOptions simulationOptions(SimulationOptionsType options) {
        content.setSimulation(options);
        return this;
    }
    //endregion

    public static ModelExecuteOptionsType toModelExecutionOptionsBean(ModelExecuteOptions options) {
        return options != null ? options.toModelExecutionOptionsType() : null;
    }

    public ModelExecuteOptionsType toModelExecutionOptionsType() {
        return clone().content; // cloning for safety reasons
    }

    public static ModelExecuteOptions fromModelExecutionOptionsType(ModelExecuteOptionsType bean) {
        return bean != null ? new ModelExecuteOptions(bean.clone()) : null;
    }

    public static ModelExecuteOptions fromRestOptions(List<String> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }

        ModelExecuteOptions retVal = ModelExecuteOptions.create();
        for (String option : options) {
            if (ModelExecuteOptionsType.F_RAW.getLocalPart().equals(option)) {
                retVal.raw(true);
            }
            if (ModelExecuteOptionsType.F_EXECUTE_IMMEDIATELY_AFTER_APPROVAL.getLocalPart().equals(option)) {
                retVal.executeImmediatelyAfterApproval(true);
            }
            if (ModelExecuteOptionsType.F_FORCE.getLocalPart().equals(option)) {
                retVal.force(true);
            }
            if (F_PUSH_CHANGES.getLocalPart().equals(option)) {
                retVal.pushChanges(true);
            }
            if (F_NO_CRYPT.getLocalPart().equals(option)) {
                retVal.noCrypt(true);
            }
            if (F_OVERWRITE.getLocalPart().equals(option)) {
                retVal.overwrite(true);
            }
            if (ModelExecuteOptionsType.F_RECONCILE.getLocalPart().equals(option)) {
                retVal.reconcile(true);
            }
            if (ModelExecuteOptionsType.F_IS_IMPORT.getLocalPart().equals(option)) {
                retVal.setIsImport(true);
            }
            if (ModelExecuteOptionsType.F_LIMIT_PROPAGATION.getLocalPart().equals(option)) {
                retVal.limitPropagation(true);
            }
            if (ModelExecuteOptionsType.F_REEVALUATE_SEARCH_FILTERS.getLocalPart().equals(option)) {
                retVal.reevaluateSearchFilters(true);
            }
            // preAuthorized is purposefully omitted (security reasons)
        }

        return retVal;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ModelExecuteOptions(");
        appendFlag(sb, "executeImmediatelyAfterApproval", content.isExecuteImmediatelyAfterApproval());
        appendFlag(sb, "force", content.isForce());
        appendFlag(sb, "isImport", content.isIsImport());
        appendFlag(sb, "limitPropagation", content.isLimitPropagation());
        appendFlag(sb, "noCrypt", content.isNoCrypt());
        appendFlag(sb, "overwrite", content.isOverwrite());
        appendFlag(sb, "preAuthorized", preAuthorized);
        appendFlag(sb, "raw", content.isRaw());
        appendFlag(sb, "reconcile", content.isReconcile());
        appendFlag(sb, "reconcileFocus", content.isReconcileFocus());
        appendFlag(sb, "reevaluateSearchFilters", content.isReevaluateSearchFilters());
        appendFlag(sb, "requestBusinessContext", content.getRequestBusinessContext() == null ? null : true);
        appendVal(sb, "partialProcessing", format(content.getPartialProcessing()));
        appendVal(sb, "initialPartialProcessing", format(content.getInitialPartialProcessing()));
        appendVal(sb, "focusConflictResolution", content.getFocusConflictResolution());
        appendVal(sb, "tracingProfile", tracingProfile);
        removeLastComma(sb);
        sb.append(")");
        return sb.toString();
    }

    private Object format(PartialProcessingOptionsType pp) {
        if (pp == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        appendPpFlag(sb, pp.getLoad(), "L");
        appendPpFlag(sb, pp.getFocus(), "F");
        appendPpFlag(sb, pp.getInbound(), "I");
        appendPpFlag(sb, pp.getFocusActivation(), "FA");
        appendPpFlag(sb, pp.getObjectTemplateBeforeAssignments(), "OTBA");
        appendPpFlag(sb, pp.getAssignments(), "A");
        appendPpFlag(sb, pp.getAssignmentsOrg(), "AORG");
        appendPpFlag(sb, pp.getAssignmentsMembershipAndDelegate(), "AM&D");
        appendPpFlag(sb, pp.getAssignmentsConflicts(), "AC");
        appendPpFlag(sb, pp.getObjectTemplateAfterAssignments(), "OTAA");
        appendPpFlag(sb, pp.getFocusCredentials(), "FC");
        appendPpFlag(sb, pp.getFocusPolicyRules(), "FPR");
        appendPpFlag(sb, pp.getProjection(), "P");
        appendPpFlag(sb, pp.getOutbound(), "O");
        appendPpFlag(sb, pp.getProjectionValues(), "PV");
        appendPpFlag(sb, pp.getProjectionCredentials(), "PC");
        appendPpFlag(sb, pp.getProjectionReconciliation(), "PR");
        appendPpFlag(sb, pp.getProjectionLifecycle(), "PL");
        appendPpFlag(sb, pp.getApprovals(), "APP");
        appendPpFlag(sb, pp.getExecution(), "E");
        appendPpFlag(sb, pp.getNotification(), "N");
        removeLastComma(sb);
        sb.append(")");
        return sb.toString();
    }

    private void appendPpFlag(StringBuilder sb, PartialProcessingTypeType option, String label) {
        if (option == null) {
            return;
        }
        String value;
        switch (option) {
            case AUTOMATIC:
                return;
            case PROCESS:
                value = "+";
                break;
            case SKIP:
                value = "-";
                break;
            default:
                throw new AssertionError();
        }
        sb.append(label).append(value).append(",");
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public ModelExecuteOptions clone() {
        ModelExecuteOptions clone = new ModelExecuteOptions(content.clone());
        clone.preAuthorized = this.preAuthorized;
        clone.evaluateAllAssignmentRelationsOnRecompute = this.evaluateAllAssignmentRelationsOnRecompute;
        clone.tracingProfile = this.tracingProfile;
        return clone;
    }

    public boolean notEmpty() {
        // hack but quite effective
        return !toString().equals(new ModelExecuteOptions().toString());
    }

    public PartialProcessingOptionsType getOrCreatePartialProcessing() {
        if (content.getPartialProcessing() == null) {
            content.setPartialProcessing(new PartialProcessingOptionsType());
        }
        return content.getPartialProcessing();
    }

    public static GetOperationOptions toGetOperationOptions(ModelExecuteOptions modelOptions) {
        if (modelOptions == null) {
            return null;
        }
        GetOperationOptions getOptions = new GetOperationOptions();
        getOptions.setRaw(modelOptions.getRaw());
        return getOptions;
    }
}
