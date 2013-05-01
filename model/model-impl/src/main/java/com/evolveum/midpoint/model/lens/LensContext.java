/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.lens;

import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import com.evolveum.midpoint.xml.ns._public.model.model_context_2.LensContextType;
import com.evolveum.midpoint.xml.ns._public.model.model_context_2.LensProjectionContextType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author semancik
 *
 */
public class LensContext<F extends ObjectType, P extends ObjectType> implements ModelContext<F, P> {

    private static final long serialVersionUID = -778283437426659540L;

    private ModelState state = ModelState.INITIAL;
	
	/**
     * Channel that is the source of primary change (GUI, live sync, import, ...)
     */
    private String channel;
    
	private LensFocusContext<F> focusContext;
	private Collection<LensProjectionContext<P>> projectionContexts = new ArrayList<LensProjectionContext<P>>();

	private Class<F> focusClass;
	private Class<P> projectionClass;

	transient private ObjectTemplateType userTemplate;
	transient private AccountSynchronizationSettingsType accountSynchronizationSettings;
	transient private ValuePolicyType globalPasswordPolicy;

	transient private DeltaSetTriple<Assignment> evaluatedAssignmentTriple;
	
	/**
     * True if we want to reconcile all accounts in this context.
     */
    private boolean doReconciliationForAllProjections = false;
    
    /**
	 * Current wave of computation and execution.
	 */
	int projectionWave = 0;

    /**
	 * Current wave of execution.
	 */
	int executionWave = 0;

	transient private boolean isFresh = false;
	
	/**
     * Cache of resource instances. It is used to reduce the number of read (getObject) calls for ResourceType objects.
     */
    transient private Map<String, ResourceType> resourceCache;
	
	transient private PrismContext prismContext;
	
	private ModelExecuteOptions options;
	
	public LensContext(Class<F> focusClass, Class<P> projectionClass, PrismContext prismContext) {
		Validate.notNull(prismContext, "No prismContext");
		if (focusClass == null && projectionClass == null) {
			throw new IllegalArgumentException("Neither focus class nor projection class was specified");
		}
		
        this.prismContext = prismContext;
        this.focusClass = focusClass;
        this.projectionClass = projectionClass;
    }
	
	public PrismContext getPrismContext() {
		return prismContext;
	}
	
	protected PrismContext getNotNullPrismContext() {
		if (prismContext == null) {
			throw new IllegalStateException("Null prism context in "+this+"; the context was not adopted (most likely)");
		}
		return prismContext;
	}
	
	@Override
	public ModelState getState() {
		return state;
	}

	public void setState(ModelState state) {
		this.state = state;
	}

	@Override
	public LensFocusContext<F> getFocusContext() {
		return focusContext;
	}
	
	public void setFocusContext(LensFocusContext<F> focusContext) {
		this.focusContext = focusContext;
	}
	
	public LensFocusContext<F> createFocusContext() {
		return createFocusContext(null);
	}
	
	public LensFocusContext<F> createFocusContext(Class<F> explicitFocusClass) {
		if (explicitFocusClass != null) {
			this.focusClass = explicitFocusClass;
		}
		focusContext = new LensFocusContext<F>(focusClass, this);
		return focusContext;
	}
	
	public LensFocusContext<F> getOrCreateFocusContext() {
		return getOrCreateFocusContext(null);
	}
	
	public LensFocusContext<F> getOrCreateFocusContext(Class<F> explicitFocusClass) {
		if (focusContext == null) {
			createFocusContext(explicitFocusClass);
		}
		return focusContext;
	}

	@Override
	public Collection<LensProjectionContext<P>> getProjectionContexts() {
		return projectionContexts;
	}
	
	public void addProjectionContext(LensProjectionContext<P> projectionContext) {
		projectionContexts.add(projectionContext);
	}
	
	public LensProjectionContext<P> findProjectionContextByOid(String oid) {
		for (LensProjectionContext<P> projCtx: getProjectionContexts()) {
			if (oid.equals(projCtx.getOid())) {
				return projCtx;
			}
		}
		return null;
	}
	
	public LensProjectionContext<P> findProjectionContext(ResourceShadowDiscriminator rat) {
		for (LensProjectionContext<P> projCtx: getProjectionContexts()) {
			if (compareResourceShadowDiscriminator(rat, projCtx)) {
				return projCtx;
			}
		}
		return null;
	}
	
	private boolean compareResourceShadowDiscriminator(ResourceShadowDiscriminator rsd,
			LensProjectionContext<P> projCtx) {
		ResourceShadowDiscriminator ctxRsd = projCtx.getResourceShadowDiscriminator();
		if (rsd.equals(ctxRsd)) {
			return true;
		}
		if (rsd.getIntent() == null && rsd.getResourceOid().equals(ctxRsd.getResourceOid())) {
			try {
				return projCtx.getRefinedAccountDefinition().isDefaultInAKind();
			} catch (SchemaException e) {
				throw new SystemException("Internal error: "+e.getMessage(), e);
			}
		}
		return false;
	}

	public LensProjectionContext<P> findOrCreateProjectionContext(ResourceShadowDiscriminator rat) {
		LensProjectionContext<P> projectionContext = findProjectionContext(rat);
		if (projectionContext == null) {
			projectionContext = createProjectionContext(rat);
		}
		return projectionContext;
	}
	
	public ObjectTemplateType getUserTemplate() {
		return userTemplate;
	}

	public void setUserTemplate(ObjectTemplateType userTemplate) {
		this.userTemplate = userTemplate;
	}

	public AccountSynchronizationSettingsType getAccountSynchronizationSettings() {
		return accountSynchronizationSettings;
	}

	public void setAccountSynchronizationSettings(
			AccountSynchronizationSettingsType accountSynchronizationSettings) {
		this.accountSynchronizationSettings = accountSynchronizationSettings;
	}
	
	public ValuePolicyType getGlobalPasswordPolicy() {
		return globalPasswordPolicy;
	}
	
	public void setGlobalPasswordPolicy(ValuePolicyType globalPasswordPolicy) {
		this.globalPasswordPolicy = globalPasswordPolicy;
	}
	
	public int getProjectionWave() {
		return projectionWave;
	}

	public void setProjectionWave(int wave) {
		this.projectionWave = wave;
	}
	
	public void incrementProjectionWave() {
		projectionWave++;
	}
	
	public void resetProjectionWave() {
		projectionWave = executionWave;
	}
	
	public int getExecutionWave() {
		return executionWave;
	}

	public void setExecutionWave(int executionWave) {
		this.executionWave = executionWave;
	}

	public void incrementExecutionWave() {
		executionWave++;
	}

	public int getMaxWave() {
		int maxWave = 0;
		for (LensProjectionContext<P> projContext: projectionContexts) {
			if (projContext.getWave() > maxWave) {
				maxWave = projContext.getWave();
			}
		}
		return maxWave;
	}
	
	public boolean isFresh() {
		return isFresh;
	}

	public void setFresh(boolean isFresh) {
		this.isFresh = isFresh;
	}
	
	/**
	 * Makes the context and all sub-context non-fresh.
	 */
	public void rot() {
		setFresh(false);
		if (focusContext != null) {
			focusContext.setFresh(false);
		}
		for (LensProjectionContext<P> projectionContext: projectionContexts) {
			projectionContext.setFresh(false);
		}
	}
	
	/**
	 * Make the context as clean as new. Except for the executed deltas and other "traces" of
	 * what was already done and cannot be undone. Also the configuration items that were loaded may remain.
	 * This is used to restart the context computation but keep the trace of what was already done.
	 */
	public void reset() {
		state = ModelState.INITIAL;
		evaluatedAssignmentTriple = null;
		projectionWave = 0;
		executionWave = 0;
		isFresh = false;
		if (focusContext != null) {
			focusContext.reset();
		}
		if (projectionContexts != null) {
			for (LensProjectionContext<P> projectionContext: projectionContexts) {
				projectionContext.reset();
			}
		}
	}
	
	/**
	 * Removes projection contexts that are not fresh.
	 * These are usually artifacts left after the context reload. E.g. an account that used to be linked to a user before
	 * but was removed in the meantime.
	 */
	public void removeRottenContexts() {
		Iterator<LensProjectionContext<P>> projectionIterator = projectionContexts.iterator();
		while (projectionIterator.hasNext()) {
			LensProjectionContext<P> projectionContext = projectionIterator.next();
			if (projectionContext.getPrimaryDelta() != null && !projectionContext.getPrimaryDelta().isEmpty()) {
				// We must never remove contexts with primary delta. Even though it fails later on.
				// What the user wishes should be done (or at least attempted) regardless of the consequences.
				// Vox populi vox dei
				continue;
			}
			if (!projectionContext.isFresh()) {
				projectionIterator.remove();
			}
		}
	}
	
	

	public String getChannel() {
        return channel;
    }

    public void setChannel(String channelUri) {
        this.channel = channelUri;
    }
    
    public void setChannel(QName channelQName) {
        this.channel = QNameUtil.qNameToUri(channelQName);
    }

	public boolean isDoReconciliationForAllProjections() {
		return doReconciliationForAllProjections;
	}

	public void setDoReconciliationForAllProjections(boolean doReconciliationForAllProjections) {
		this.doReconciliationForAllProjections = doReconciliationForAllProjections;
	}
	
	public DeltaSetTriple<Assignment> getEvaluatedAssignmentTriple() {
		return evaluatedAssignmentTriple;
	}

	public void setEvaluatedAssignmentTriple(DeltaSetTriple<Assignment> evaluatedAssignmentTriple) {
		this.evaluatedAssignmentTriple = evaluatedAssignmentTriple;
	}
	
	public ModelExecuteOptions getOptions() {
		return options;
	}
	
	public void setOptions(ModelExecuteOptions options) {
		this.options = options;
	}

	/**
     * Returns all changes, user and all accounts. Both primary and secondary changes are returned, but
     * these are not merged.
     * TODO: maybe it would be better to merge them.
     */
    public Collection<ObjectDelta<? extends ObjectType>> getAllChanges() throws SchemaException {
        Collection<ObjectDelta<? extends ObjectType>> allChanges = new ArrayList<ObjectDelta<? extends ObjectType>>();
        if (focusContext != null) {
	        addChangeIfNotNull(allChanges, focusContext.getPrimaryDelta());
	        addChangeIfNotNull(allChanges, focusContext.getSecondaryDelta());
        }
        for (LensProjectionContext<P> projCtx: getProjectionContexts()) {
            addChangeIfNotNull(allChanges, projCtx.getPrimaryDelta());
            addChangeIfNotNull(allChanges, projCtx.getSecondaryDelta());
        }
        return allChanges;
    }

	private <T extends ObjectType> void addChangeIfNotNull(Collection<ObjectDelta<? extends ObjectType>> changes,
            ObjectDelta<T> change) {
        if (change != null) {
            changes.add(change);
        }
    }

    public void replacePrimaryFocusDelta(ObjectDelta<F> newDelta) {
        focusContext.setPrimaryDelta(newDelta);
        // todo any other changes have to be done?
    }

    public void replacePrimaryFocusDeltas(List<ObjectDelta<F>> deltas) throws SchemaException {
        replacePrimaryFocusDelta(null);
        if (deltas != null) {
            for (ObjectDelta<F> delta : deltas) {
                focusContext.addPrimaryDelta(delta);
            }
        }
        // todo any other changes have to be done?
    }


    /**
     * Returns all executed deltas, user and all accounts.
     */
    public Collection<ObjectDeltaOperation<? extends ObjectType>> getExecutedDeltas() throws SchemaException {
    	return getExecutedDeltas(null);
    }

	/**
     * Returns all executed deltas, user and all accounts.
     */
    public Collection<ObjectDeltaOperation<? extends ObjectType>> getUnauditedExecutedDeltas() throws SchemaException {
    	return getExecutedDeltas(false);
    }

    /**
     * Returns all executed deltas, user and all accounts.
     */
    Collection<ObjectDeltaOperation<? extends ObjectType>> getExecutedDeltas(Boolean audited) throws SchemaException {
        Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas = new ArrayList<ObjectDeltaOperation<? extends ObjectType>>();
        if (focusContext != null) {
	        executedDeltas.addAll(focusContext.getExecutedDeltas(audited));
        }
        for (LensProjectionContext<P> projCtx: getProjectionContexts()) {
        	executedDeltas.addAll(projCtx.getExecutedDeltas(audited));
        }
        return executedDeltas;
    }
    
    public void markExecutedDeltasAudited()  {
        if (focusContext != null) {
        	focusContext.markExecutedDeltasAudited();
        }
        for (LensProjectionContext<P> projCtx: getProjectionContexts()) {
        	projCtx.markExecutedDeltasAudited();
        }
    }
    
	public void recompute() throws SchemaException {
		recomputeFocus();
		recomputeProjections();
	}

    // mainly computes new state based on old state and delta(s)
	public void recomputeFocus() throws SchemaException {
		if (focusContext != null) {
			focusContext.recompute();
		}
	}
	
	public void recomputeProjections() throws SchemaException {
		for (LensProjectionContext<P> projCtx: getProjectionContexts()) {
			projCtx.recompute();
		}
	}

	public void checkConsistence() {
		if (focusContext != null) {
			focusContext.checkConsistence();
		}
		for (LensProjectionContext<P> projectionContext: projectionContexts) {
			projectionContext.checkConsistence(this.toString(), isFresh, ModelExecuteOptions.isForce(options));
		}
	}
	
	public void checkEncrypted() {
		if (focusContext != null) {
			focusContext.checkEncrypted();
		}
		for (LensProjectionContext<P> projectionContext: projectionContexts) {
			projectionContext.checkEncrypted();
		}
	}
	
	public LensProjectionContext<P> createProjectionContext() {
		return createProjectionContext(null);
	}
	
	public LensProjectionContext<P> createProjectionContext(ResourceShadowDiscriminator rat) {
		LensProjectionContext<P> projCtx = new LensProjectionContext<P>(projectionClass, this, rat);
		addProjectionContext(projCtx);
		return projCtx;
	}
	
	private Map<String, ResourceType> getResourceCache() {
		if (resourceCache == null) {
			resourceCache = new HashMap<String, ResourceType>();
		}
		return resourceCache;
	}

	/**
     * Returns a resource for specified account type.
     * This is supposed to be efficient, taking the resource from the cache. It assumes the resource is in the cache.
     *
     * @see SyncContext#rememberResource(ResourceType)
     */
    public ResourceType getResource(ResourceShadowDiscriminator rat) {
        return getResource(rat.getResourceOid());
    }
    
    /**
     * Returns a resource for specified account type.
     * This is supposed to be efficient, taking the resource from the cache. It assumes the resource is in the cache.
     *
     * @see SyncContext#rememberResource(ResourceType)
     */
    public ResourceType getResource(String resourceOid) {
        return getResourceCache().get(resourceOid);
    }
	
	/**
     * Puts resources in the cache for later use. The resources should be fetched from provisioning
     * and have pre-parsed schemas. So the next time just reuse them without the other overhead.
     */
    public void rememberResources(Collection<ResourceType> resources) {
        for (ResourceType resourceType : resources) {
            rememberResource(resourceType);
        }
    }

    /**
     * Puts resource in the cache for later use. The resource should be fetched from provisioning
     * and have pre-parsed schemas. So the next time just reuse it without the other overhead.
     */
    public void rememberResource(ResourceType resourceType) {
    	getResourceCache().put(resourceType.getOid(), resourceType);
    }
    
	/**
	 * Cleans up the contexts by removing secondary deltas and other working state. The context after cleanup
	 * should be the same as originally requested.
	 * However, the current wave number is retained. Otherwise it ends up in endless loop. 
	 */
	public void cleanup() throws SchemaException {
		if (focusContext != null) {
			focusContext.cleanup();
		}
		for (LensProjectionContext<P> projectionContext: projectionContexts) {
			projectionContext.cleanup();
		}
		recompute();
	}
    
    public void adopt(PrismContext prismContext) throws SchemaException {
    	this.prismContext = prismContext;
    	
    	if (focusContext != null) {
    		focusContext.adopt(prismContext);
    	}
    	for (LensProjectionContext<P> projectionContext: projectionContexts) {
    		projectionContext.adopt(prismContext);
    	}
    }
    
    public void normalize() {
    	if (focusContext != null) {
    		focusContext.normalize();
    	}
    	if (projectionContexts != null) {
    		for (LensProjectionContext<P> projectionContext: projectionContexts) {
    			projectionContext.normalize();
    		}
    	}
    }
    
    public LensContext<F, P> clone() {
    	LensContext<F, P> clone = new LensContext<F, P>(focusClass, projectionClass, prismContext);
    	copyValues(clone);
    	return clone;
    }
    
    protected void copyValues(LensContext<F, P> clone) {
    	clone.state = this.state;
    	clone.channel = this.channel;
    	clone.doReconciliationForAllProjections = this.doReconciliationForAllProjections;
    	clone.focusClass = this.focusClass;
    	clone.isFresh = this.isFresh;
    	clone.prismContext = this.prismContext;
    	clone.projectionClass = this.projectionClass;
    	clone.resourceCache = cloneResourceCache();
    	// User template is de-facto immutable, OK to just pass reference here.
    	clone.userTemplate = this.userTemplate;
    	clone.projectionWave = this.projectionWave;
    	
    	if (this.focusContext != null) {
    		clone.focusContext = this.focusContext.clone(this);
    	}
    	
    	for (LensProjectionContext<P> thisProjectionContext: this.projectionContexts) {
    		clone.projectionContexts.add(thisProjectionContext.clone(this));
    	}
    }

	private Map<String, ResourceType> cloneResourceCache() {
		if (resourceCache == null) {
			return null;
		}
		Map<String, ResourceType> clonedMap = new HashMap<String, ResourceType>();
		for (Entry<String, ResourceType> entry: resourceCache.entrySet()) {
			clonedMap.put(entry.getKey(), entry.getValue());
		}
		return clonedMap;
	}
	
	public void distributeResource() {
		for (LensProjectionContext<P> projCtx: getProjectionContexts()) {
			projCtx.distributeResource();
		}
	}

	@Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String dump() {
        return debugDump(0);
    }
    
    public String dump(boolean showTriples) {
        return debugDump(0, showTriples);
    }

    @Override
    public String debugDump(int indent) {
    	return debugDump(indent, true);
    }
    
    public String debugDump(int indent, boolean showTriples) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("LensContext: state=").append(state);
        sb.append(", Wave(e=").append(executionWave);
        sb.append(",p=").append(projectionWave);
        sb.append(",max=").append(getMaxWave());
        sb.append("), fresh=").append(isFresh);
        sb.append("\n");

        DebugUtil.debugDumpLabel(sb, "Channel", indent + 1);
        sb.append(" ").append(channel).append("\n");
        DebugUtil.debugDumpLabel(sb, "Settings", indent + 1);
        sb.append(" ");
        if (accountSynchronizationSettings != null) {
            sb.append("assignments=");
            sb.append(accountSynchronizationSettings.getAssignmentPolicyEnforcement());
        } else {
            sb.append("null");
        }
        sb.append("\n");

        DebugUtil.debugDumpWithLabel(sb, "FOCUS", focusContext, indent + 1);

        sb.append("\n");
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("PROJECTIONS:");
        if (projectionContexts.isEmpty()) {
            sb.append(" none");
        } else {
        	sb.append(" (").append(projectionContexts.size()).append(")");
            for (LensProjectionContext<P> projCtx : projectionContexts) {
            	sb.append(":\n");
            	sb.append(projCtx.debugDump(indent + 2, showTriples));
            }
        }

        return sb.toString();
    }

    public LensContextType toJaxb() throws SchemaException {
        LensContextType lensContextType = new LensContextType();

        lensContextType.setState(state != null ? state.toModelStateType() : null);
        lensContextType.setChannel(channel);
        lensContextType.setFocusContext(focusContext != null ? focusContext.toJaxb() : null);
        for (LensProjectionContext lensProjectionContext : projectionContexts) {
            lensContextType.getProjectionContext().add(lensProjectionContext.toJaxb());
        }
        lensContextType.setFocusClass(focusClass != null ? focusClass.getName() : null);
        lensContextType.setProjectionClass(projectionClass != null ? projectionClass.getName() : null);
        lensContextType.setDoReconciliationForAllProjections(doReconciliationForAllProjections);
        lensContextType.setProjectionWave(projectionWave);
        lensContextType.setExecutionWave(executionWave);
        lensContextType.setOptions(options != null ? options.toModelExecutionOptionsType() : null);

        return lensContextType;
    }

    public static LensContext fromJaxb(LensContextType lensContextType, PrismContext prismContext) throws SchemaException {

        String focusClassString = lensContextType.getFocusClass();
        String projectionClassString = lensContextType.getProjectionClass();

        if (StringUtils.isEmpty(focusClassString)) {
            throw new SystemException("Focus class is undefined in LensContextType");
        }
        if (StringUtils.isEmpty(projectionClassString)) {
            throw new SystemException("Projection class is undefined in LensContextType");
        }

        LensContext lensContext;
        try {
            lensContext = new LensContext(Class.forName(focusClassString), Class.forName(projectionClassString), prismContext);
        } catch (ClassNotFoundException e) {
            throw new SystemException("Couldn't instantiate LensContext because focus or projection class couldn't be found", e);
        }

        lensContext.setState(ModelState.fromModelStateType(lensContextType.getState()));
        lensContext.setChannel(lensContextType.getChannel());
        lensContext.setFocusContext(LensFocusContext.fromJaxb(lensContextType.getFocusContext(), lensContext));
        for (LensProjectionContextType lensProjectionContextType : lensContextType.getProjectionContext()) {
            lensContext.addProjectionContext(LensProjectionContext.fromJaxb(lensProjectionContextType, lensContext));
        }
        lensContext.setDoReconciliationForAllProjections(lensContextType.isDoReconciliationForAllProjections() != null ?
            lensContextType.isDoReconciliationForAllProjections() : false);
        lensContext.setProjectionWave(lensContextType.getProjectionWave() != null ?
                lensContextType.getProjectionWave() : 0);
        lensContext.setExecutionWave(lensContextType.getExecutionWave() != null ?
            lensContextType.getExecutionWave() : 0);
        lensContext.setOptions(ModelExecuteOptions.fromModelExecutionOptionsType(lensContextType.getOptions()));

        lensContext.adopt(prismContext);
        return lensContext;
    }

	@Override
	public String toString() {
		return "LensContext(s=" + state + ", W(e=" + executionWave + ",p=" + projectionWave + "): "+focusContext+", "+projectionContexts+")";
	}

}
