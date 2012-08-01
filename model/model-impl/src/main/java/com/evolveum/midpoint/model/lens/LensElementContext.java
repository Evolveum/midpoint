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

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.common.valueconstruction.ObjectDeltaObject;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;

/**
 * @author semancik
 *
 */
public abstract class LensElementContext<O extends ObjectType> implements ModelElementContext<O> {

	private PrismObject<O> objectOld;
	private PrismObject<O> objectNew;
	private ObjectDelta<O> primaryDelta;
	private ObjectDelta<O> secondaryDelta;
	private Class<O> objectTypeClass;
	private String oid = null;
	
	private LensContext<? extends ObjectType, ? extends ObjectType> lensContext;
	
	private transient PrismObjectDefinition<O> objectDefinition = null;
	
	public LensElementContext(Class<O> objectTypeClass, LensContext<? extends ObjectType, ? extends ObjectType> lensContext) {
		super();
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
            secondaryDelta = new ObjectDelta<O>(getObjectTypeClass(), ChangeType.MODIFY);
            secondaryDelta.setOid(oid);
        }
        secondaryDelta.swallow(accountPasswordDelta);
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
    
    public void recompute() throws SchemaException {
    	ObjectDelta<O> delta = getDelta();
        if (delta == null) {
            // No change
            objectNew = objectOld;
            return;
        }
        objectNew = delta.computeChangedObject(objectOld);
    }

    public void checkConsistence() {
    	checkConsistence(null);
    }
    
	public void checkConsistence(String contextDesc) {
    	if (getObjectOld() != null) {
    		checkConsistence(getObjectOld(), "old "+getElementDesc() , contextDesc);
    	}
    	if (primaryDelta != null) {
    		try {
    			primaryDelta.checkConsistence();
    		} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(e.getMessage()+"; in "+getElementDesc()+" primary delta in "+this + (contextDesc == null ? "" : " in " +contextDesc), e);
			} catch (IllegalStateException e) {
				throw new IllegalStateException(e.getMessage()+"; in "+getElementDesc()+" primary delta in "+this + (contextDesc == null ? "" : " in " +contextDesc), e);
			}
    	}
    	if (secondaryDelta != null) {
    		try {
	    		// Secondary delta may not have OID yet (as it may relate to ADD primary delta that doesn't have OID yet)
	    		boolean requireOid = primaryDelta == null;
	    		secondaryDelta.checkConsistence(requireOid, true);
    		} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(e.getMessage()+"; in "+getElementDesc()+" secondary delta in "+this + (contextDesc == null ? "" : " in " +contextDesc), e);
			} catch (IllegalStateException e) {
				throw new IllegalStateException(e.getMessage()+"; in "+getElementDesc()+" secondary delta in "+this + (contextDesc == null ? "" : " in " +contextDesc), e);
			}

    	}
    	if (getObjectNew() != null) {
    		checkConsistence(getObjectNew(), "new "+getElementDesc(), contextDesc);
    	}
	}
	
	protected void checkConsistence(PrismObject<O> object, String elementDesc, String contextDesc) {
    	try {
    		object.checkConsistence();
    	} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e.getMessage()+"; in "+elementDesc+" in "+this + (contextDesc == null ? "" : " in " +contextDesc), e);
		} catch (IllegalStateException e) {
			throw new IllegalStateException(e.getMessage()+"; in "+elementDesc+" in "+this + (contextDesc == null ? "" : " in " +contextDesc), e);
		}
		if (object.getDefinition() == null) {
			throw new IllegalStateException("No new "+getElementDesc()+" definition "+elementDesc+" in "+this + (contextDesc == null ? "" : " in " +contextDesc));
		}
    	O objectType = object.asObjectable();
    	if (objectType instanceof ResourceObjectShadowType) {
    		PrismReference resourceRef = object.findReference(AccountShadowType.F_RESOURCE_REF);
        	if (resourceRef == null) {
        		throw new IllegalStateException("No resourceRef in "+elementDesc+" in "+this + (contextDesc == null ? "" : " in " +contextDesc));
        	}
        	if (StringUtils.isBlank(resourceRef.getOid())) {
        		throw new IllegalStateException("Null or empty OID in resourceRef in "+elementDesc+" in "+this + (contextDesc == null ? "" : " in " +contextDesc));
        	}
    		ResourceObjectShadowType shadowType = (ResourceObjectShadowType)objectType;
	    	if (shadowType.getObjectClass() == null) {
	    		throw new IllegalStateException("Null objectClass in "+elementDesc+" in "+this + (contextDesc == null ? "" : " in " +contextDesc));
	    	}
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
