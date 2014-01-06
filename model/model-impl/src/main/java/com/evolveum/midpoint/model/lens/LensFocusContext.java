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
package com.evolveum.midpoint.model.lens;

import java.util.Collection;

import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.model.model_context_2.LensFocusContextType;
import org.apache.commons.lang.StringUtils;

/**
 * @author semancik
 *
 */
public class LensFocusContext<O extends ObjectType> extends LensElementContext<O> {

    private static final Trace LOGGER = TraceManager.getTrace(LensFocusContext.class);

	private ObjectDeltaWaves<O> secondaryDeltas = new ObjectDeltaWaves<O>();
	
	private int getProjectionWave() {
		return getLensContext().getProjectionWave();
	}

	public LensFocusContext(Class<O> objectTypeClass, LensContext<O> lensContext) {
		super(objectTypeClass, lensContext);
	}

	@Override
	public void setOid(String oid) {
		super.setOid(oid);
		secondaryDeltas.setOid(oid);
	}

	public ObjectDelta<O> getProjectionWavePrimaryDelta() throws SchemaException {
    	if (getProjectionWave() == 0) {
    		return getPrimaryDelta();
    	} else {
    		return secondaryDeltas.getMergedDeltas(getPrimaryDelta(), getProjectionWave());
    	}
    }
	
	public boolean isDelete() {
		if (getPrimaryDelta() != null && getPrimaryDelta().isDelete()) {
			return true;
		}
		return false;
	}
	
	public boolean isAdd() {
		if (getPrimaryDelta() != null && getPrimaryDelta().isAdd()) {
			return true;
		}
		return false;
	}

	
	@Override
	public ObjectDelta<O> getSecondaryDelta() {
        try {
			return secondaryDeltas.getMergedDeltas();
		} catch (SchemaException e) {
			// This should not happen
			throw new SystemException("Unexpected delta merging problem: "+e.getMessage(), e);
		}
    }
    
    public ObjectDelta<O> getSecondaryDelta(int wave) {
    	return secondaryDeltas.get(wave);
    }

    public ObjectDeltaWaves<O> getSecondaryDeltas() {
        return secondaryDeltas;
    }
    
    public ObjectDelta<O> getProjectionWaveSecondaryDelta() throws SchemaException {
        return getWaveSecondaryDelta(getProjectionWave());
    }
    
    public ObjectDelta<O> getWaveSecondaryDelta(int wave) throws SchemaException {
        return secondaryDeltas.get(wave);
    }

    @Override
	public void setSecondaryDelta(ObjectDelta<O> secondaryDelta) {
		throw new UnsupportedOperationException("Cannot set secondary delta to focus without a wave number");
	}

	public void setSecondaryDelta(ObjectDelta<O> secondaryDelta, int wave) {
        this.secondaryDeltas.set(wave, secondaryDelta);
    }
    
    public void setProjectionWaveSecondaryDelta(ObjectDelta<O> secondaryDelta) {
        this.secondaryDeltas.set(getProjectionWave(), secondaryDelta);
    }
    
    public void swallowToProjectionWaveSecondaryDelta(ItemDelta<?> propDelta) throws SchemaException {
    	if (alreadyHaveDelta(propDelta)) {
    		return;
    	}
		ObjectDelta<O> secondaryDelta = getProjectionWaveSecondaryDelta();
		if (secondaryDelta == null) {
            secondaryDelta = new ObjectDelta<O>(getObjectTypeClass(), ChangeType.MODIFY, getPrismContext());        
            secondaryDelta.setOid(getOid());
            setProjectionWaveSecondaryDelta(secondaryDelta);
        }
        secondaryDelta.swallow(propDelta);
	}
    
    public void swallowToSecondaryDelta(ItemDelta<?> propDelta) throws SchemaException {
    	if (alreadyHaveDelta(propDelta)) {
    		return;
    	}
    	ObjectDelta<O> secondaryDelta = getSecondaryDelta(0);
		if (secondaryDelta == null) {
            secondaryDelta = new ObjectDelta<O>(getObjectTypeClass(), ChangeType.MODIFY, getPrismContext());        
            secondaryDelta.setOid(getOid());
            setSecondaryDelta(secondaryDelta, 0);
        }
        secondaryDelta.swallow(propDelta);
	}
        
	private boolean alreadyHaveDelta(ItemDelta<?> itemDelta) {
		ObjectDelta<O> primaryDelta = getPrimaryDelta();
		if (primaryDelta != null && primaryDelta.containsModification(itemDelta)) {
			return true;
		}
		if (secondaryDeltas != null) {
			for (ObjectDelta<O> waveSecondaryDelta: secondaryDeltas) {
				if (waveSecondaryDelta != null && waveSecondaryDelta.containsModification(itemDelta)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
     * Returns user delta, both primary and secondary (merged together) for a current wave.
     * The returned object is (kind of) immutable. Changing it may do strange things (but most likely the changes will be lost).
     */
    public ObjectDelta<O> getProjectionWaveDelta() throws SchemaException {
    	return getWaveDelta(getProjectionWave());
    }
    
    public ObjectDelta<O> getWaveDelta(int wave) throws SchemaException {
    	if (wave == 0) {
    		// Primary delta is executed only in the first wave (wave 0)
    		return ObjectDelta.union(getPrimaryDelta(), getWaveSecondaryDelta(wave));
    	} else {
    		return getWaveSecondaryDelta(wave);
    	}
    }
    
    /**
     * Returns delta of user assignments, both primary and secondary (merged together).
     * The returned object is (kind of) immutable. Changing it may do strange things (but most likely the changes will be lost).
     * Only works for UserType now.
     * 
     * This is relative to execution wave to avoid re-processing of already executed assignments.
     */
    public ContainerDelta<AssignmentType> getExecutionWaveAssignmentDelta() throws SchemaException {
    	if (!FocusType.class.isAssignableFrom(getObjectTypeClass())) {
    		throw new UnsupportedOperationException("Attempt to get assignment deltas from "+getObjectTypeClass());
    	}
        ObjectDelta<? extends FocusType> userDelta = (ObjectDelta<? extends FocusType>) getWaveDelta(getLensContext().getExecutionWave());
        if (userDelta == null) {
            return createEmptyAssignmentDelta();
        }
        ContainerDelta<AssignmentType> assignmentDelta = userDelta.findContainerDelta(new ItemPath(FocusType.F_ASSIGNMENT));
        if (assignmentDelta == null) { 
            return createEmptyAssignmentDelta();
        }
        return assignmentDelta;
    }
    
	public Collection<? extends ItemDelta<?>> getExecutionWaveAssignmentItemDeltas(Long id) throws SchemaException {
		if (!FocusType.class.isAssignableFrom(getObjectTypeClass())) {
    		throw new UnsupportedOperationException("Attempt to get assignment deltas from "+getObjectTypeClass());
    	}
        ObjectDelta<? extends FocusType> userDelta = (ObjectDelta<? extends FocusType>) getWaveDelta(getLensContext().getExecutionWave());
        if (userDelta == null) {
            return null;
        }
        return userDelta.findItemDeltasSubPath(new ItemPath(new NameItemPathSegment(FocusType.F_ASSIGNMENT),
        									  new IdItemPathSegment(id)));
	}

    private ContainerDelta<AssignmentType> createEmptyAssignmentDelta() {
        return new ContainerDelta<AssignmentType>(getAssignmentContainerDefinition());
    }
    
    private PrismContainerDefinition<AssignmentType> getAssignmentContainerDefinition() {
		return getObjectDefinition().findContainerDefinition(FocusType.F_ASSIGNMENT);
	}
    
    @Override
	public void cleanup() {
		super.cleanup();
		// Clean up only delta in current wave. The deltas in previous waves are already done.
		// FIXME: this somehow breaks things. don't know why. but don't really care. the waves will be gone soon anyway
//		if (secondaryDeltas.get(getWave()) != null) {
//			secondaryDeltas.remove(getWave());
//		}
	}

	@Override
	public void normalize() {
		super.normalize();
		if (secondaryDeltas != null) {
			secondaryDeltas.normalize();
		}
	}
	
	@Override
	public void reset() {
		super.reset();
		secondaryDeltas = new ObjectDeltaWaves<O>();
	}

	@Override
	public void adopt(PrismContext prismContext) throws SchemaException {
		super.adopt(prismContext);
		if (secondaryDeltas != null) {
			secondaryDeltas.adopt(prismContext);
		}
	}
    
    @Override
	public LensFocusContext<O> clone(LensContext lensContext) {
    	LensFocusContext<O> clone = new LensFocusContext<O>(getObjectTypeClass(), lensContext);
    	copyValues(clone, lensContext);
    	return clone;
	}

	protected void copyValues(LensFocusContext<O> clone, LensContext lensContext) {
		super.copyValues(clone, lensContext);
		if (this.secondaryDeltas != null) {
			clone.secondaryDeltas = this.secondaryDeltas.clone();
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
        sb.append(getDebugDumpTitle());
        if (!isFresh()) {
        	sb.append(", NOT FRESH");
        }
        sb.append(", oid=");
        sb.append(getOid());
        
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("old"), getObjectOld(), indent+1);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("current"), getObjectCurrent(), indent+1);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("new"), getObjectNew(), indent+1);
        
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("primary delta"), getPrimaryDelta(), indent+1);

        sb.append("\n");
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append(getDebugDumpTitle("secondary delta")).append(":");
        if (secondaryDeltas.isEmpty()) {
            sb.append(" empty");
        } else {
            sb.append("\n");
            sb.append(secondaryDeltas.debugDump(indent + 2));
        }
        
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("executed deltas"), getExecutedDeltas(), indent+1);

        return sb.toString();
    }

	@Override
	protected String getElementDefaultDesc() {
		return "focus";
	}

	@Override
	public String toString() {
		return "LensFocusContext(" + getObjectTypeClass().getSimpleName() + ":" + getOid() + ")";
	}
	
	public String getHumanReadableName() {
		StringBuilder sb = new StringBuilder();
		sb.append("focus(");
		PrismObject<O> object = getObjectNew();
		if (object == null) {
			object = getObjectOld();
		}
		if (object == null) {
			sb.append(getOid());
		} else {
			sb.append(object.toString());
		}
		sb.append(")");
		return sb.toString();
	}

    public void addToPrismContainer(PrismContainer<LensFocusContextType> lensFocusContextTypeContainer) throws SchemaException {

        LensFocusContextType lensFocusContextType = lensFocusContextTypeContainer.createNewValue().asContainerable();

        super.storeIntoLensElementContextType(lensFocusContextType);
        lensFocusContextType.setSecondaryDeltas(secondaryDeltas.toObjectDeltaWavesType());
    }

    public static LensFocusContext fromLensFocusContextType(LensFocusContextType focusContextType, LensContext lensContext, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {

        String objectTypeClassString = focusContextType.getObjectTypeClass();
        if (StringUtils.isEmpty(objectTypeClassString)) {
            throw new SystemException("Object type class is undefined in LensFocusContextType");
        }
        LensFocusContext lensFocusContext;
        try {
            lensFocusContext = new LensFocusContext(Class.forName(objectTypeClassString), lensContext);
        } catch (ClassNotFoundException e) {
            throw new SystemException("Couldn't instantiate LensFocusContext because object type class couldn't be found", e);
        }

        lensFocusContext.retrieveFromLensElementContextType(focusContextType, result);
        lensFocusContext.secondaryDeltas = ObjectDeltaWaves.fromObjectDeltaWavesType(focusContextType.getSecondaryDeltas(), lensContext.getPrismContext());

        // fixing provisioning type in delta (however, this is not usually needed, unless primary object is shadow or resource
        Objectable object;
        if (lensFocusContext.getObjectNew() != null) {
            object = lensFocusContext.getObjectNew().asObjectable();
        } else if (lensFocusContext.getObjectOld() != null) {
            object = lensFocusContext.getObjectOld().asObjectable();
        } else {
            object = null;
        }
        for (Object o : lensFocusContext.secondaryDeltas) {
            ObjectDelta<? extends ObjectType> delta = (ObjectDelta<? extends ObjectType>) o;
            if (delta != null) {
                lensFocusContext.fixProvisioningTypeInDelta(delta, object, result);
            }
        }

        return lensFocusContext;
    }

    @Override
	public void checkEncrypted() {
		super.checkEncrypted();
		secondaryDeltas.checkEncrypted("secondary delta");
	}

    @Override
    public void checkConsistence(String desc) {
        super.checkConsistence(desc);

        // all executed deltas should have the same oid (if any)
        String oid = null;
        for (LensObjectDeltaOperation operation : getExecutedDeltas()) {
            String oid1 = operation.getObjectDelta().getOid();
            if (oid == null) {
                if (oid1 != null) {
                    oid = oid1;
                }
            } else {
                if (oid1 != null && !oid.equals(oid1)) {
                    String m = "Different OIDs in focus executed deltas: " + oid + ", " + oid1;
                    LOGGER.error("{}: context = \n{}", m, this.debugDump());
                    throw new IllegalStateException(m);
                }
            }
        }
    }
    
}
