/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.model.impl.lens;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.prism.ConsistencyCheckScope;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.model_context_3.LensElementContextType;
import com.evolveum.midpoint.xml.ns._public.model.model_context_3.LensObjectDeltaOperationType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public abstract class LensElementContext<O extends ObjectType> implements ModelElementContext<O> {

    private static final long serialVersionUID = 1649567559396392861L;

    private PrismObject<O> objectOld;
    private transient PrismObject<O> objectCurrent;
	private PrismObject<O> objectNew;
	private ObjectDelta<O> primaryDelta;
	private ObjectDelta<O> secondaryDelta;
	private List<LensObjectDeltaOperation<O>> executedDeltas = new ArrayList<LensObjectDeltaOperation<O>>();
	private Class<O> objectTypeClass;
	private String oid = null;
	private int iteration;
    private String iterationToken;
    
    /**
     * Initial intent regarding the account. It indicated what the initiator of the operation WANTS TO DO with the
     * context. 
     * If set to null then the decision is left to "the engine". Null is also a typical value
     * when the context is created. It may be pre-set under some circumstances, e.g. if an account is being unlinked.
     */
    private SynchronizationIntent synchronizationIntent;
    
	private transient boolean isFresh = false;
	
	private LensContext<? extends ObjectType> lensContext;
	
	private transient PrismObjectDefinition<O> objectDefinition = null;
	
	public LensElementContext(Class<O> objectTypeClass, LensContext<? extends ObjectType> lensContext) {
		super();
		Validate.notNull(objectTypeClass, "Object class is null");
		Validate.notNull(lensContext, "Lens context is null");
		this.lensContext = lensContext;
		this.objectTypeClass = objectTypeClass;
	}
	
	public int getIteration() {
		return iteration;
	}

	public void setIteration(int iteration) {
		this.iteration = iteration;
	}

	public String getIterationToken() {
		return iterationToken;
	}

	public void setIterationToken(String iterationToken) {
		this.iterationToken = iterationToken;
	}
	
	public SynchronizationIntent getSynchronizationIntent() {
		return synchronizationIntent;
	}

	public void setSynchronizationIntent(SynchronizationIntent synchronizationIntent) {
		this.synchronizationIntent = synchronizationIntent;
	}

	public LensContext<? extends ObjectType> getLensContext() {
		return lensContext;
	}
	
	protected PrismContext getNotNullPrismContext() {
		return getLensContext().getNotNullPrismContext();
	}

	@Override
    public Class<O> getObjectTypeClass() {
		return objectTypeClass;
	}
	
	public PrismContext getPrismContext() {
		return lensContext.getPrismContext();
	}
	
	@Override
	public PrismObject<O> getObjectOld() {
		return objectOld;
	}
	
	public void setObjectOld(PrismObject<O> objectOld) {
		this.objectOld = objectOld;
	}
	
	public PrismObject<O> getObjectCurrent() {
		return objectCurrent;
	}

	public void setObjectCurrent(PrismObject<O> objectCurrent) {
		this.objectCurrent = objectCurrent;
	}
	
	public PrismObject<O> getObjectAny() {
		if (objectNew != null) {
			return objectNew;
		}
		if (objectCurrent != null) {
			return objectCurrent;
		}
		return objectOld;
	}
	
	/**
	 * Sets current and possibly also old object. This method is used with
	 * freshly loaded object. The object is set as current object.
	 * If the old object was not initialized yet (and if it should be initialized)
	 * then the object is also set as old object. 
	 */
	public void setLoadedObject(PrismObject<O> object) {
		setObjectCurrent(object);
		if (objectOld == null && !isAdd()) {
			setObjectOld(object.clone());
		}
	}

	@Override
	public PrismObject<O> getObjectNew() {
		return objectNew;
	}
	
	public void setObjectNew(PrismObject<O> objectNew) {
		this.objectNew = objectNew;
	}
	
	@Override
	public ObjectDelta<O> getPrimaryDelta() {
		return primaryDelta;
	}
	
	public void setPrimaryDelta(ObjectDelta<O> primaryDelta) {
		this.primaryDelta = primaryDelta;
	}
	
	public void addPrimaryDelta(ObjectDelta<O> delta) throws SchemaException {
        if (primaryDelta == null) {
        	primaryDelta = delta;
        } else {
        	primaryDelta.merge(delta);
        }
    }
	
	@Override
	public ObjectDelta<O> getSecondaryDelta() {
		return secondaryDelta;
	}

	@Override
	public void setSecondaryDelta(ObjectDelta<O> secondaryDelta) {
		this.secondaryDelta = secondaryDelta;
	}
	
	public void addSecondaryDelta(ObjectDelta<O> delta) throws SchemaException {
        if (secondaryDelta == null) {
        	secondaryDelta = delta;
        } else {
        	secondaryDelta.merge(delta);
        }
    }
	
	public void swallowToPrimaryDelta(ItemDelta<?> itemDelta) throws SchemaException {
        if (primaryDelta == null) {
        	primaryDelta = new ObjectDelta<O>(getObjectTypeClass(), ChangeType.MODIFY, getPrismContext());
        	primaryDelta.setOid(oid);
        }
        primaryDelta.swallow(itemDelta);
    }
	
	public void swallowToSecondaryDelta(ItemDelta<?> itemDelta) throws SchemaException {
        if (secondaryDelta == null) {
            secondaryDelta = new ObjectDelta<O>(getObjectTypeClass(), ChangeType.MODIFY, getPrismContext());
            secondaryDelta.setOid(oid);
        }
        secondaryDelta.swallow(itemDelta);
    }
	
	public boolean isAdd() {
		if (ObjectDelta.isAdd(getPrimaryDelta())) {
			return true;
		}
		if (ObjectDelta.isAdd(getSecondaryDelta())) {
			return true;
		}
		return false;
	}
    
    public boolean isModify() {
		if (ObjectDelta.isModify(getPrimaryDelta())) {
			return true;
		}
		if (ObjectDelta.isModify(getSecondaryDelta())) {
			return true;
		}
		return false;
	}

	public boolean isDelete() {
		if (ObjectDelta.isDelete(getPrimaryDelta())) {
			return true;
		}
		if (ObjectDelta.isDelete(getSecondaryDelta())) {
			return true;
		}
		return false;
	}

    @Override
	public List<LensObjectDeltaOperation<O>> getExecutedDeltas() {
		return executedDeltas;
	}
	
	List<LensObjectDeltaOperation<O>> getExecutedDeltas(Boolean audited) {
		if (audited == null) {
			return executedDeltas;
		}
		List<LensObjectDeltaOperation<O>> deltas = new ArrayList<LensObjectDeltaOperation<O>>();
		for (LensObjectDeltaOperation<O> delta: executedDeltas) {
			if (delta.isAudited() == audited) {
				deltas.add(delta);
			}
		}
		return deltas;
	}
	
	public void markExecutedDeltasAudited() {
		for(LensObjectDeltaOperation<O> executedDelta: executedDeltas) {
			executedDelta.setAudited(true);
		}
	}
	
	public void addToExecutedDeltas(LensObjectDeltaOperation<O> executedDelta) {
		executedDeltas.add(executedDelta);
	}

	/**
     * Returns user delta, both primary and secondary (merged together).
     * The returned object is (kind of) immutable. Changing it may do strange things (but most likely the changes will be lost).
     */
    public ObjectDelta<O> getDelta() throws SchemaException {
        return ObjectDelta.union(primaryDelta, getSecondaryDelta());
    }
    
    public ObjectDeltaObject<O> getObjectDeltaObject() throws SchemaException {
		return new ObjectDeltaObject<O>(objectOld, getDelta(), objectNew);
	}

    @Override
    public String getOid() {
    	if (oid == null) {
    		oid = determineOid();
    	}
    	return oid;
    }
    
    public String determineOid() {
    	if (getObjectOld() != null && getObjectOld().getOid() != null) {
    		return getObjectOld().getOid();
    	}
    	if (getObjectCurrent() != null && getObjectCurrent().getOid() != null) {
    		return getObjectCurrent().getOid();
    	}
    	if (getObjectNew() != null && getObjectNew().getOid() != null) {
    		return getObjectNew().getOid();
    	}
    	if (getPrimaryDelta() != null && getPrimaryDelta().getOid() != null) {
    		return getPrimaryDelta().getOid();
    	}
    	if (getSecondaryDelta() != null && getSecondaryDelta().getOid() != null) {
    		return getSecondaryDelta().getOid();
    	}
    	return null;
    }
    
    /**
     * Sets oid to the field but also to the deltas (if applicable).
     */
    public void setOid(String oid) {
        this.oid = oid;
        if (primaryDelta != null) {
            primaryDelta.setOid(oid);
        }
        if (secondaryDelta != null) {
            secondaryDelta.setOid(oid);
        }
        if (objectNew != null) {
        	objectNew.setOid(oid);
        }
    }
    
    public PrismObjectDefinition<O> getObjectDefinition() {    	
		if (objectDefinition == null) {
			if (objectOld != null) {
				objectDefinition = objectOld.getDefinition();
			} else if (objectCurrent != null) {
	    		objectDefinition = objectCurrent.getDefinition();
	    	} else if (objectNew != null) {
	    		objectDefinition = objectNew.getDefinition();
	    	} else {
	    		objectDefinition = getNotNullPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(getObjectTypeClass());
	    	}
		}
		return objectDefinition;
	}
    
    public boolean isFresh() {
		return isFresh;
	}

	public void setFresh(boolean isFresh) {
		this.isFresh = isFresh;
	}

	public void recompute() throws SchemaException {
		PrismObject<O> base = objectCurrent;
		if (base == null) {
			base = objectOld;
		}
    	ObjectDelta<O> delta = getDelta();
        if (delta == null) {
            // No change
            objectNew = base;
            return;
        }
        objectNew = delta.computeChangedObject(base);
    }
	
	/**
	 * Make the context as clean as new. Except for the executed deltas and other "traces" of
	 * what was already done and cannot be undone. Also the configuration items that were loaded may remain.
	 * This is used to restart the context computation but keep the trace of what was already done.
	 */
	public void reset() {
		secondaryDelta = null;
		isFresh = false;
	}

    public void checkConsistence() {
    	checkConsistence(null);
    }
    
	public void checkConsistence(String contextDesc) {
    	if (getObjectOld() != null) {
    		checkConsistence(getObjectOld(), "old "+getElementDesc() , contextDesc);
    	}
    	if (getObjectCurrent() != null) {
    		checkConsistence(getObjectCurrent(), "current "+getElementDesc() , contextDesc);
    	}
    	if (primaryDelta != null) {
    		checkConsistence(primaryDelta, false, getElementDesc()+" primary delta in "+this + (contextDesc == null ? "" : " in " +contextDesc));
    	}
    	if (secondaryDelta != null) {
    		boolean requireOid = isRequireSecondardyDeltaOid();
    		// Secondary delta may not have OID yet (as it may relate to ADD primary delta that doesn't have OID yet)
    		checkConsistence(secondaryDelta, requireOid, getElementDesc()+" secondary delta in "+this + (contextDesc == null ? "" : " in " +contextDesc));
    	}
    	if (getObjectNew() != null) {
    		checkConsistence(getObjectNew(), "new "+getElementDesc(), contextDesc);
    	}
	}
	
	private void checkConsistence(ObjectDelta<O> delta, boolean requireOid, String contextDesc) {
		try {
			delta.checkConsistence(requireOid, true, true, ConsistencyCheckScope.THOROUGH);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e.getMessage()+"; in "+contextDesc, e);
		} catch (IllegalStateException e) {
			throw new IllegalStateException(e.getMessage()+"; in "+contextDesc, e);
		}
		if (delta.isAdd()) {
			checkConsistence(delta.getObjectToAdd(), "add object", contextDesc);
		}
	}

	protected boolean isRequireSecondardyDeltaOid() {
		return primaryDelta == null;
	}
	
	protected void checkConsistence(PrismObject<O> object, String elementDesc, String contextDesc) {
		String desc = elementDesc+" in "+this + (contextDesc == null ? "" : " in " +contextDesc);
    	try {
    		object.checkConsistence(true, ConsistencyCheckScope.THOROUGH);
    	} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e.getMessage()+"; in "+desc, e);
		} catch (IllegalStateException e) {
			throw new IllegalStateException(e.getMessage()+"; in "+desc, e);
		}
		if (object.getDefinition() == null) {
			throw new IllegalStateException("No "+getElementDesc()+" definition "+desc);
		}
    	O objectType = object.asObjectable();
    	if (objectType instanceof ShadowType) {
    		ShadowUtil.checkConsistence((PrismObject<? extends ShadowType>) object, desc);
    	}
    }
	
	/**
	 * Cleans up the contexts by removing secondary deltas and other working state. The context after cleanup
	 * should be the same as originally requested.
	 */
	public void cleanup() {
		secondaryDelta = null;
	}
	
	public void normalize() {
		if (objectNew != null) {
			objectNew.normalize();
		}
		if (objectOld != null) {
			objectOld.normalize();
		}
		if (objectCurrent != null) {
			objectCurrent.normalize();
		}
		if (primaryDelta != null) {
			primaryDelta.normalize();
		}
		if (secondaryDelta != null) {
			secondaryDelta.normalize();
		}
	}
	
	public void adopt(PrismContext prismContext) throws SchemaException {
		if (objectNew != null) {
			prismContext.adopt(objectNew);
		}
		if (objectOld != null) {
			prismContext.adopt(objectOld);
		}
		if (objectCurrent != null) {
			prismContext.adopt(objectCurrent);
		}
		if (primaryDelta != null) {
			prismContext.adopt(primaryDelta);
		}
		if (secondaryDelta != null) {
			prismContext.adopt(secondaryDelta);
		}
		// TODO: object definition?
	}
	
	public abstract LensElementContext<O> clone(LensContext<? extends ObjectType> lensContext);
	
	protected void copyValues(LensElementContext<O> clone, LensContext lensContext) {
		clone.lensContext = lensContext;
		// This is de-facto immutable
		clone.objectDefinition = this.objectDefinition;
		clone.objectNew = cloneObject(this.objectNew);
		clone.objectOld = cloneObject(this.objectOld);
		clone.objectCurrent = cloneObject(this.objectCurrent);
		clone.objectTypeClass = this.objectTypeClass;
		clone.oid = this.oid;
		clone.primaryDelta = cloneDelta(this.primaryDelta);
		clone.secondaryDelta = cloneDelta(this.secondaryDelta);
		clone.isFresh = this.isFresh;
		clone.iteration = this.iteration;
		clone.iterationToken = this.iterationToken;
	}
	
	private ObjectDelta<O> cloneDelta(ObjectDelta<O> thisDelta) {
		if (thisDelta == null) {
			return null;
		}
		return thisDelta.clone();
	}

	private PrismObject<O> cloneObject(PrismObject<O> thisObject) {
		if (thisObject == null) {
			return null;
		}
		return thisObject.clone();
	}

    public void storeIntoLensElementContextType(LensElementContextType lensElementContextType) throws SchemaException {
        lensElementContextType.setObjectOld(objectOld != null ? objectOld.asObjectable() : null);
        lensElementContextType.setObjectNew(objectNew != null ? objectNew.asObjectable() : null);
        lensElementContextType.setPrimaryDelta(primaryDelta != null ? DeltaConvertor.toObjectDeltaType(primaryDelta) : null);
        lensElementContextType.setSecondaryDelta(secondaryDelta != null ? DeltaConvertor.toObjectDeltaType(secondaryDelta) : null);
        for (LensObjectDeltaOperation executedDelta : executedDeltas) {
            lensElementContextType.getExecutedDeltas().add(executedDelta.toLensObjectDeltaOperationType());
        }
        lensElementContextType.setObjectTypeClass(objectTypeClass != null ? objectTypeClass.getName() : null);
        lensElementContextType.setOid(oid);
        lensElementContextType.setIteration(iteration);
        lensElementContextType.setIterationToken(iterationToken);
        lensElementContextType.setSynchronizationIntent(synchronizationIntent != null ? synchronizationIntent.toSynchronizationIntentType() : null);
    }

    public void retrieveFromLensElementContextType(LensElementContextType lensElementContextType, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {

        ObjectType objectTypeOld = lensElementContextType.getObjectOld();
        this.objectOld = objectTypeOld != null ? objectTypeOld.asPrismObject() : null;
        fixProvisioningTypeInObject(this.objectOld, result);

        ObjectType objectTypeNew = lensElementContextType.getObjectNew();
        this.objectNew = objectTypeNew != null ? objectTypeNew.asPrismObject() : null;
        fixProvisioningTypeInObject(this.objectNew, result);

        ObjectType object = objectTypeNew != null ? objectTypeNew : objectTypeOld;

        ObjectDeltaType primaryDeltaType = lensElementContextType.getPrimaryDelta();
        this.primaryDelta = primaryDeltaType != null ? (ObjectDelta) DeltaConvertor.createObjectDelta(primaryDeltaType, lensContext.getPrismContext()) : null;
        fixProvisioningTypeInDelta(this.primaryDelta, object, result);

        ObjectDeltaType secondaryDeltaType = lensElementContextType.getSecondaryDelta();
        this.secondaryDelta = secondaryDeltaType != null ? (ObjectDelta) DeltaConvertor.createObjectDelta(secondaryDeltaType, lensContext.getPrismContext()) : null;
        fixProvisioningTypeInDelta(this.secondaryDelta, object, result);

        for (LensObjectDeltaOperationType eDeltaOperationType : lensElementContextType.getExecutedDeltas()) {
            LensObjectDeltaOperation objectDeltaOperation = LensObjectDeltaOperation.fromLensObjectDeltaOperationType(eDeltaOperationType, lensContext.getPrismContext());
            if (objectDeltaOperation.getObjectDelta() != null) {
                fixProvisioningTypeInDelta(objectDeltaOperation.getObjectDelta(), object, result);
            }
            this.executedDeltas.add(objectDeltaOperation);
        }

        this.oid = lensElementContextType.getOid();
        
        this.iteration = lensElementContextType.getIteration() != null ? lensElementContextType.getIteration() : 0;
        this.iterationToken = lensElementContextType.getIterationToken();
        this.synchronizationIntent = SynchronizationIntent.fromSynchronizationIntentType(lensElementContextType.getSynchronizationIntent());

        // note: objectTypeClass is already converted (used in the constructor)
    }

    protected void fixProvisioningTypeInDelta(ObjectDelta<O> delta, Objectable object, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
        if (delta != null && delta.getObjectTypeClass() != null && (ShadowType.class.isAssignableFrom(delta.getObjectTypeClass()) || ResourceType.class.isAssignableFrom(delta.getObjectTypeClass()))) {
            lensContext.getProvisioningService().applyDefinition(delta, object, result);
        }
    }

    private void fixProvisioningTypeInObject(PrismObject<O> object, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
        if (object != null && object.getCompileTimeClass() != null && (ShadowType.class.isAssignableFrom(object.getCompileTimeClass()) || ResourceType.class.isAssignableFrom(object.getCompileTimeClass()))) {
            lensContext.getProvisioningService().applyDefinition(object, result);
        }
    }

    public void checkEncrypted() {
		if (objectNew != null) {
			CryptoUtil.checkEncrypted(objectNew);
		}
		if (objectOld != null) {
			CryptoUtil.checkEncrypted(objectOld);
		}
		if (objectCurrent != null) {
			CryptoUtil.checkEncrypted(objectCurrent);
		}
		if (primaryDelta != null) {
			CryptoUtil.checkEncrypted(primaryDelta);
		}
		if (secondaryDelta != null) {
			CryptoUtil.checkEncrypted(secondaryDelta);
		}
	}
    
	protected abstract String getElementDefaultDesc();
	
	protected String getElementDesc() {
		PrismObject<O> object = getObjectNew();
		if (object == null) {
			object = getObjectOld();
		}
		if (object == null) {
			object = getObjectCurrent();
		}
		if (object == null) {
			return getElementDefaultDesc();
		}
		return object.toDebugType();
	}
	
	protected String getDebugDumpTitle() {
		return StringUtils.capitalize(getElementDesc());
	}
	
	protected String getDebugDumpTitle(String suffix) {
		return getDebugDumpTitle()+" "+suffix;
	}
	
	public abstract String getHumanReadableName();

}
