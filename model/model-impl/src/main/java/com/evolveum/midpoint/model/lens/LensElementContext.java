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

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import com.evolveum.midpoint.xml.ns._public.model.model_context_2.LensElementContextType;
import com.evolveum.midpoint.xml.ns._public.model.model_context_2.LensObjectDeltaOperationType;
import com.evolveum.prism.xml.ns._public.types_2.ObjectDeltaType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ChangeType;
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
	private PrismObject<O> objectNew;
	private ObjectDelta<O> primaryDelta;
	private ObjectDelta<O> secondaryDelta;
	private List<LensObjectDeltaOperation<O>> executedDeltas = new ArrayList<LensObjectDeltaOperation<O>>();
	private Class<O> objectTypeClass;
	private String oid = null;
	private transient boolean isFresh = false;
	
	private LensContext<? extends ObjectType, ? extends ObjectType> lensContext;
	
	private transient PrismObjectDefinition<O> objectDefinition = null;
	
	public LensElementContext(Class<O> objectTypeClass, LensContext<? extends ObjectType, ? extends ObjectType> lensContext) {
		super();
		Validate.notNull(objectTypeClass, "Object class is null");
		Validate.notNull(lensContext, "Lens context is null");
		this.lensContext = lensContext;
		this.objectTypeClass = objectTypeClass;
	}

	public LensContext<? extends ObjectType, ? extends ObjectType> getLensContext() {
		return lensContext;
	}
	
	protected PrismContext getNotNullPrismContext() {
		return getLensContext().getNotNullPrismContext();
	}

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
	
	public void addToSecondaryDelta(PropertyDelta accountPasswordDelta) throws SchemaException {
        if (secondaryDelta == null) {
            secondaryDelta = new ObjectDelta<O>(getObjectTypeClass(), ChangeType.MODIFY, getPrismContext());
            secondaryDelta.setOid(oid);
        }
        secondaryDelta.swallow(accountPasswordDelta);
    }
	
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
    
    protected PrismObjectDefinition<O> getObjectDefinition() {
		if (objectDefinition == null) {
			objectDefinition = getNotNullPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(getObjectTypeClass());
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
    	ObjectDelta<O> delta = getDelta();
        if (delta == null) {
            // No change
            objectNew = objectOld;
            return;
        }
        objectNew = delta.computeChangedObject(objectOld);
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
			delta.checkConsistence(requireOid, true, true);
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
    		object.checkConsistence(true);
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
		if (primaryDelta != null) {
			prismContext.adopt(primaryDelta);
		}
		if (secondaryDelta != null) {
			prismContext.adopt(secondaryDelta);
		}
		// TODO: object definition?
	}
	
	public abstract LensElementContext<O> clone(LensContext lensContext);
	
	protected void copyValues(LensElementContext<O> clone, LensContext lensContext) {
		clone.lensContext = lensContext;
		// This is de-facto immutable
		clone.objectDefinition = this.objectDefinition;
		clone.objectNew = cloneObject(this.objectNew);
		clone.objectOld = cloneObject(this.objectOld);
		clone.objectTypeClass = this.objectTypeClass;
		clone.oid = this.oid;
		clone.primaryDelta = cloneDelta(this.primaryDelta);
		clone.secondaryDelta = cloneDelta(this.secondaryDelta);
		clone.isFresh = this.isFresh;
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
    }

    public void retrieveFromLensElementContextType(LensElementContextType lensElementContextType) throws SchemaException {

        ObjectType objectTypeOld = lensElementContextType.getObjectOld();
        this.objectOld = objectTypeOld != null ? objectTypeOld.asPrismObject() : null;

        ObjectType objectTypeNew = lensElementContextType.getObjectNew();
        this.objectNew = objectTypeOld != null ? objectTypeNew.asPrismObject() : null;

        ObjectDeltaType primaryDeltaType = lensElementContextType.getPrimaryDelta();
        this.primaryDelta = primaryDeltaType != null ? (ObjectDelta) DeltaConvertor.createObjectDelta(primaryDeltaType, lensContext.getPrismContext()) : null;

        ObjectDeltaType secondaryDeltaType = lensElementContextType.getSecondaryDelta();
        this.secondaryDelta = secondaryDeltaType != null ? (ObjectDelta) DeltaConvertor.createObjectDelta(secondaryDeltaType, lensContext.getPrismContext()) : null;

        for (LensObjectDeltaOperationType eDeltaOperationType : lensElementContextType.getExecutedDeltas()) {
            this.executedDeltas.add(LensObjectDeltaOperation.fromLensObjectDeltaOperationType(eDeltaOperationType, lensContext.getPrismContext()));
        }

        this.oid = lensElementContextType.getOid();

        // note: objectTypeClass is already converted (used in the constructor)
    }

	public void checkEncrypted() {
		if (objectNew != null) {
			CryptoUtil.checkEncrypted(objectNew);
		}
		if (objectOld != null) {
			CryptoUtil.checkEncrypted(objectOld);
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
		PrismObject<O> object = getObjectOld();
		if (object == null) {
			object = getObjectOld();
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

}
