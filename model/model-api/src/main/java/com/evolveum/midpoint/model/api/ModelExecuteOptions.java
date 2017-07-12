/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.model.api;


import com.evolveum.midpoint.schema.AbstractOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationBusinessContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingTypeType;

import java.io.Serializable;
import java.util.List;

/**
 * Options for execution of Model operations. These options influence the way how the operations are executed.
 * The options are not mandatory. All options have reasonable default values. They may not be specified at all.
 * 
 * @author semancik
 */
public class ModelExecuteOptions extends AbstractOptions implements Serializable, Cloneable {
	
	/**
	 * Force the operation even if it would otherwise fail due to external failure. E.g. attempt to delete an account
	 * that no longer exists on resource may fail without a FORCE option. If FORCE option is used then the operation is
	 * finished even if the account does not exist (e.g. at least shadow is removed from midPoint repository).
	 */
	private Boolean force;
	
	/**
	 * Avoid any smart processing of the data except for schema application. Do not synchronize the data, do not apply
	 * any expressions, etc.
	 */
	private Boolean raw;
	
	/**
	 * Encrypt any cleartext data on write, decrypt any encrypted data on read. Applies only to the encrypted
	 * data formats (ProtectedString, ProtectedByteArray).
	 * It is not recommended to use in production environment. This option is provided only for diagnostic
	 * purposes to be used in development environments.
	 */
	private Boolean noCrypt;
	
	/**
	 * Option to reconcile focus and all projections while executing changes.
	 * (implies reconcileFocus)
	 */
	private Boolean reconcile;
	
	/**
	 * Option to reconcile focus while executing changes.
	 * If this option is set and the reconcile option is not set then the projections
	 * reconciliation will not be forced (but it may still happen if other configuration
	 * loads full projection).
	 */
	private Boolean reconcileFocus;

    /**
     * Option to reconcile affected objects after executing changes.
     * Typical use: after a role is changed, all users that have been assigned this role
     * would be reconciled.
     *
     * Because it is difficult to determine all affected objects (e.g. users that have
     * indirectly assigned a role), midPoint does a reasonable attempt to determine
     * and reconcile them. E.g. it may be limited to a direct assignees.
     *
     * Also, because of time complexity, the reconciliation may be executed in
     * a separate background task.
     */
	private Boolean reconcileAffected;

    /**
     * Option to execute changes as soon as they are approved. (For the primary stage approvals, the default behavior
     * is to wait until all changes are approved/rejected and then execute the operation as a whole.)
     */
	private Boolean executeImmediatelyAfterApproval;

    /**
     * Option to user overwrite flag. It can be used from web service, if we want to re-import some object
     */
	private Boolean overwrite;
    
	/**
	 * Option to simulate import operation. E.g. search filters will be resolved.
	 */
	private Boolean isImport;

	/**
	 * Causes reevaluation of search filters (producing partial errors on failure).
	 */
	private Boolean reevaluateSearchFilters;
    
    /**
     * Option to limit propagation only for the source resource
     */
	private Boolean limitPropagation;

	/**
	 * Is this operation already authorized, i.e. should it be executed without any further authorization checks?
	 * EXPERIMENTAL. Currently supported only for raw executions.
	 */
	private Boolean preAuthorized;
	
	/**
	 * Business context that describes this request.
	 */
	private OperationBusinessContextType requestBusinessContext;
	
	/**
	 * Options that control selective execution of model logic.
     * Use with extreme care. Some combinations may be dangerous.
	 */
	private PartialProcessingOptionsType partialProcessing;

	/**
	 * Partial processing for initial clockwork stage. Used primarily for eliminating overhead when starting
	 * operations that are expected to result in (background) approval processing.
	 *
	 * Note that if this option is used and if the clockwork proceeds to PRIMARY phase (e.g. because there are
	 * no approvals), the context will be rotten after INITIAL phase. This presents some overhead. So please use
	 * this option only if there is reasonable assumption that the request will stop after INITIAL phase.
	 */
	private PartialProcessingOptionsType initialPartialProcessing;

    public Boolean getForce() {
		return force;
	}

	public void setForce(Boolean force) {
		this.force = force;
	}
	
	public static boolean isForce(ModelExecuteOptions options) {
		if (options == null) {
			return false;
		}
		if (options.force == null) {
			return false;
		}
		return options.force;
	}
	
	public static ModelExecuteOptions createForce() {
		ModelExecuteOptions opts = new ModelExecuteOptions();
		opts.setForce(true);
		return opts;
	}

	public ModelExecuteOptions setForce() {
		setForce(true);
		return this;
	}

	public Boolean getRaw() {
		return raw;
	}

	public void setRaw(Boolean raw) {
		this.raw = raw;
	}
	
	public static boolean isRaw(ModelExecuteOptions options) {
		if (options == null) {
			return false;
		}
		if (options.raw == null) {
			return false;
		}
		return options.raw;
	}
	
	public static ModelExecuteOptions createRaw() {
		ModelExecuteOptions opts = new ModelExecuteOptions();
		opts.setRaw(true);
		return opts;
	}

	public ModelExecuteOptions setRaw() {
		setRaw(true);
		return this;
	}

	public Boolean getNoCrypt() {
		return noCrypt;
	}

	public void setNoCrypt(Boolean noCrypt) {
		this.noCrypt = noCrypt;
	}
	
	public static boolean isNoCrypt(ModelExecuteOptions options) {
		if (options == null) {
			return false;
		}
		if (options.noCrypt == null) {
			return false;
		}
		return options.noCrypt;
	}

	public static ModelExecuteOptions createNoCrypt() {
		ModelExecuteOptions opts = new ModelExecuteOptions();
		opts.setNoCrypt(true);
		return opts;
	}

	public ModelExecuteOptions setNoCrypt() {
		setNoCrypt(true);
		return this;
	}

	public Boolean getReconcile() {
		return reconcile;
	}
	
	public void setReconcile(Boolean reconcile) {
		this.reconcile = reconcile;
	}
	
	public static boolean isReconcile(ModelExecuteOptions options) {
		if (options == null){
			return false;
		}
		if (options.reconcile == null){
			return false;
		}
		return options.reconcile;
	}
	
	public static ModelExecuteOptions createReconcile() {
		ModelExecuteOptions opts = new ModelExecuteOptions();
		opts.setReconcile(true);
		return opts;
	}

	public static ModelExecuteOptions createReconcile(ModelExecuteOptions defaultOptions) {
		ModelExecuteOptions opts = defaultOptions != null ? defaultOptions.clone() : new ModelExecuteOptions();
		opts.setReconcile(true);
		return opts;
	}

	public ModelExecuteOptions setReconcile() {
		setReconcile(true);
		return this;
	}
	
	public Boolean getReconcileFocus() {
		return reconcileFocus;
	}

	public void setReconcileFocus(Boolean reconcileFocus) {
		this.reconcileFocus = reconcileFocus;
	}

	public static ModelExecuteOptions createReconcileFocus() {
		ModelExecuteOptions opts = new ModelExecuteOptions();
		opts.setReconcileFocus(true);
		return opts;
	}
	
	public static boolean isReconcileFocus(ModelExecuteOptions options) {
		if (options == null){
			return false;
		}
		if (options.reconcileFocus == null){
			return false;
		}
		return options.reconcileFocus;
	}

    public Boolean getReconcileAffected() {
        return reconcileAffected;
    }

    public void setReconcileAffected(Boolean reconcile) {
        this.reconcileAffected = reconcile;
    }

    public static boolean isReconcileAffected(ModelExecuteOptions options){
        if (options == null){
            return false;
        }
        if (options.reconcileAffected == null){
            return false;
        }
        return options.reconcileAffected;
    }

    public static ModelExecuteOptions createReconcileAffected(){
        ModelExecuteOptions opts = new ModelExecuteOptions();
        opts.setReconcileAffected(true);
        return opts;
    }

	public ModelExecuteOptions setReconcileAffected() {
		setReconcileAffected(true);
		return this;
	}

    public Boolean getOverwrite() {
		return overwrite;
	}
	
	public void setOverwrite(Boolean overwrite) {
		this.overwrite = overwrite;
	}
	
	public static boolean isOverwrite(ModelExecuteOptions options){
		if (options == null){
			return false;
		}
		if (options.overwrite == null){
			return false;
		}
		return options.overwrite;
	}
	
	public static ModelExecuteOptions createOverwrite(){
		ModelExecuteOptions opts = new ModelExecuteOptions();
		opts.setOverwrite(true);
		return opts;
	}

	public ModelExecuteOptions setOverwrite() {
		setOverwrite(true);
		return this;
	}

	public Boolean getIsImport() {
		return isImport;
	}
	
	public void setIsImport(Boolean isImport) {
		this.isImport = isImport;
	}
	
	public static boolean isIsImport(ModelExecuteOptions options){
		if (options == null){
			return false;
		}
		if (options.isImport == null){
			return false;
		}
		return options.isImport;
	}
	
	public static ModelExecuteOptions createIsImport(){
		ModelExecuteOptions opts = new ModelExecuteOptions();
		opts.setIsImport(true);
		return opts;
	}

	public ModelExecuteOptions setIsImport(){
		setIsImport(true);
		return this;
	}
	
    public void setExecuteImmediatelyAfterApproval(Boolean executeImmediatelyAfterApproval) {
        this.executeImmediatelyAfterApproval = executeImmediatelyAfterApproval;
    }

    public static boolean isExecuteImmediatelyAfterApproval(ModelExecuteOptions options){
        if (options == null){
            return false;
        }
        if (options.executeImmediatelyAfterApproval == null) {
            return false;
        }
        return options.executeImmediatelyAfterApproval;
    }

	public static ModelExecuteOptions createExecuteImmediatelyAfterApproval(){
		ModelExecuteOptions opts = new ModelExecuteOptions();
		opts.setExecuteImmediatelyAfterApproval(true);
		return opts;
	}

	public void setLimitPropagation(Boolean limitPropagation) {
		this.limitPropagation = limitPropagation;
	}
    
    public static boolean isLimitPropagation(ModelExecuteOptions options){
    	if (options == null){
    		return false;
    	}
    	if (options.limitPropagation == null){
    		return false;
    	}
    	return options.limitPropagation;
    }

	public static ModelExecuteOptions createLimitPropagation() {
		ModelExecuteOptions opts = new ModelExecuteOptions();
		opts.setLimitPropagation(true);
		return opts;
	}

	public ModelExecuteOptions setLimitPropagation() {
		setLimitPropagation(true);
		return this;
	}


	public Boolean getReevaluateSearchFilters() {
		return reevaluateSearchFilters;
	}

	public void setReevaluateSearchFilters(Boolean reevaluateSearchFilters) {
		this.reevaluateSearchFilters = reevaluateSearchFilters;
	}

	public static boolean isReevaluateSearchFilters(ModelExecuteOptions options){
		if (options == null){
			return false;
		}
		if (options.reevaluateSearchFilters == null){
			return false;
		}
		return options.reevaluateSearchFilters;
	}

	public static ModelExecuteOptions createReevaluateSearchFilters(){
		ModelExecuteOptions opts = new ModelExecuteOptions();
		opts.setReevaluateSearchFilters(true);
		return opts;
	}

	public ModelExecuteOptions setReevaluateSearchFilters(){
		setReevaluateSearchFilters(true);
		return this;
	}

	public Boolean getPreAuthorized() {
		return preAuthorized;
	}

	public void setPreAuthorized(Boolean value) {
		this.preAuthorized = value;
	}

	public static boolean isPreAuthorized(ModelExecuteOptions options) {
		if (options == null) {
			return false;
		}
		if (options.preAuthorized == null){
			return false;
		}
		return options.preAuthorized;
	}

	public static ModelExecuteOptions createPreAuthorized() {
		ModelExecuteOptions opts = new ModelExecuteOptions();
		opts.setPreAuthorized(true);
		return opts;
	}

	public ModelExecuteOptions setPreAuthorized() {
		setPreAuthorized(true);
		return this;
	}

	public OperationBusinessContextType getRequestBusinessContext() {
		return requestBusinessContext;
	}

	public void setRequestBusinessContext(OperationBusinessContextType requestBusinessContext) {
		this.requestBusinessContext = requestBusinessContext;
	}
	
	public static OperationBusinessContextType getRequestBusinessContext(ModelExecuteOptions options) {
		if (options == null) {
			return null;
		}
		return options.getRequestBusinessContext();
	}
	
	public static ModelExecuteOptions createRequestBusinessContext(OperationBusinessContextType requestBusinessContext) {
		ModelExecuteOptions opts = new ModelExecuteOptions();
		opts.setRequestBusinessContext(requestBusinessContext);
		return opts;
	}

	public PartialProcessingOptionsType getPartialProcessing() {
		return partialProcessing;
	}

	public void setPartialProcessing(PartialProcessingOptionsType partialProcessing) {
		this.partialProcessing = partialProcessing;
	}
	
	public static PartialProcessingOptionsType getPartialProcessing(ModelExecuteOptions options) {
		if (options == null) {
			return null;
		}
		return options.getPartialProcessing();
	}
	
	public static ModelExecuteOptions createPartialProcessing(PartialProcessingOptionsType partialProcessing) {
		ModelExecuteOptions opts = new ModelExecuteOptions();
		opts.setPartialProcessing(partialProcessing);
		return opts;
	}

	public PartialProcessingOptionsType getInitialPartialProcessing() {
		return initialPartialProcessing;
	}

	public void setInitialPartialProcessing(
			PartialProcessingOptionsType initialPartialProcessing) {
		this.initialPartialProcessing = initialPartialProcessing;
	}

	public static PartialProcessingOptionsType getInitialPartialProcessing(ModelExecuteOptions options) {
		if (options == null) {
			return null;
		}
		return options.getInitialPartialProcessing();
	}

	public static ModelExecuteOptions createInitialPartialProcessing(PartialProcessingOptionsType partialProcessing) {
		ModelExecuteOptions opts = new ModelExecuteOptions();
		opts.setInitialPartialProcessing(partialProcessing);
		return opts;
	}

	public ModelExecuteOptionsType toModelExecutionOptionsType() {
        ModelExecuteOptionsType retval = new ModelExecuteOptionsType();
        retval.setForce(force);
        retval.setRaw(raw);
        retval.setNoCrypt(noCrypt);
        retval.setReconcile(reconcile);
        retval.setReconcileFocus(reconcileFocus);
        retval.setExecuteImmediatelyAfterApproval(executeImmediatelyAfterApproval);
        retval.setOverwrite(overwrite);
        retval.setIsImport(isImport);
        retval.setLimitPropagation(limitPropagation);
		retval.setReevaluateSearchFilters(reevaluateSearchFilters);
		// preAuthorized is purposefully omitted (security reasons)
		retval.setRequestBusinessContext(requestBusinessContext);
		retval.setPartialProcessing(partialProcessing);
        return retval;
    }

    public static ModelExecuteOptions fromModelExecutionOptionsType(ModelExecuteOptionsType type) {
        if (type == null) {
            return null;
        }
        ModelExecuteOptions retval = new ModelExecuteOptions();
        retval.setForce(type.isForce());
        retval.setRaw(type.isRaw());
        retval.setNoCrypt(type.isNoCrypt());
        retval.setReconcile(type.isReconcile());
        retval.setReconcileFocus(type.isReconcileFocus());
        retval.setExecuteImmediatelyAfterApproval(type.isExecuteImmediatelyAfterApproval());
        retval.setOverwrite(type.isOverwrite());
        retval.setIsImport(type.isIsImport());
        retval.setLimitPropagation(type.isLimitPropagation());
		retval.setReevaluateSearchFilters(type.isReevaluateSearchFilters());
		// preAuthorized is purposefully omitted (security reasons)
		retval.setRequestBusinessContext(type.getRequestBusinessContext());
		retval.setPartialProcessing(type.getPartialProcessing());
        return retval;
    }
    
    public static ModelExecuteOptions fromRestOptions(List<String> options){
    	if (options == null || options.isEmpty()){
    		return null;
    	}
    	
    	ModelExecuteOptions retVal = new ModelExecuteOptions();
    	for (String option : options){
    		if (ModelExecuteOptionsType.F_RAW.getLocalPart().equals(option)){
    			retVal.setRaw(true);
    		}
    		if (ModelExecuteOptionsType.F_EXECUTE_IMMEDIATELY_AFTER_APPROVAL.getLocalPart().equals(option)){
    			retVal.setExecuteImmediatelyAfterApproval(true);
    		}
    		if (ModelExecuteOptionsType.F_FORCE.getLocalPart().equals(option)){
    			retVal.setForce(true);
    		}
    		if (ModelExecuteOptionsType.F_NO_CRYPT.getLocalPart().equals(option)){
    			retVal.setNoCrypt(true);
    		}
    		if (ModelExecuteOptionsType.F_OVERWRITE.getLocalPart().equals(option)){
    			retVal.setOverwrite(true);
    		}
    		if (ModelExecuteOptionsType.F_RECONCILE.getLocalPart().equals(option)){
    			retVal.setReconcile(true);
    		}
    		if (ModelExecuteOptionsType.F_IS_IMPORT.getLocalPart().equals(option)){
    			retVal.setIsImport(true);
    		}
    		if (ModelExecuteOptionsType.F_LIMIT_PROPAGATION.getLocalPart().equals(option)){
    			retVal.setIsImport(true);
    		}
			if (ModelExecuteOptionsType.F_REEVALUATE_SEARCH_FILTERS.getLocalPart().equals(option)){
				retVal.setReevaluateSearchFilters(true);
			}
			// preAuthorized is purposefully omitted (security reasons)
    	}
    	
    	return retVal;
    }
    
    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder("ModelExecuteOptions(");
    	appendFlag(sb, "executeImmediatelyAfterApproval", executeImmediatelyAfterApproval);
    	appendFlag(sb, "force", force);
    	appendFlag(sb, "isImport", isImport);
    	appendFlag(sb, "limitPropagation", limitPropagation);
    	appendFlag(sb, "noCrypt", noCrypt);
    	appendFlag(sb, "overwrite", overwrite);
    	appendFlag(sb, "preAuthorized", preAuthorized);
    	appendFlag(sb, "raw", raw);
    	appendFlag(sb, "reconcile", reconcile);
    	appendFlag(sb, "reconcileFocus", reconcileFocus);
    	appendFlag(sb, "reevaluateSearchFilters", reevaluateSearchFilters);
    	appendFlag(sb, "reconcileAffected", reconcileAffected);
    	appendFlag(sb, "requestBusinessContext", requestBusinessContext == null ? null : true);
    	appendVal(sb, "partialProcessing", format(partialProcessing));
    	appendVal(sb, "initialPartialProcessing", format(initialPartialProcessing));
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
			case AUTOMATIC: return;
			case PROCESS: value = "+"; break;
			case SKIP: value = "-"; break;
			default: throw new AssertionError();
		}
		sb.append(label).append(value).append(",");
	}

	public ModelExecuteOptions clone() {
        // not much efficient, but...
        ModelExecuteOptions clone = fromModelExecutionOptionsType(toModelExecutionOptionsType());
		clone.setPreAuthorized(this.preAuthorized);
		return clone;
    }

}
