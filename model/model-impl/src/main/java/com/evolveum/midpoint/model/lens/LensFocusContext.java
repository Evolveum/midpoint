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

import java.util.Collection;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.model.model_context_2.LensFocusContextType;
import org.apache.commons.lang.StringUtils;

/**
 * @author semancik
 *
 */
public class LensFocusContext<O extends ObjectType> extends LensElementContext<O> {


	private ObjectDeltaWaves<O> secondaryDeltas = new ObjectDeltaWaves<O>();
	
	private int getProjectionWave() {
		return getLensContext().getProjectionWave();
	}

	public LensFocusContext(Class<O> objectTypeClass, LensContext<O, ? extends ObjectType> lensContext) {
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
    	if (getObjectTypeClass() != UserType.class) {
    		throw new UnsupportedOperationException("Attempt to get assignment deltas from "+getObjectTypeClass());
    	}
        ObjectDelta<UserType> userDelta = (ObjectDelta<UserType>) getWaveDelta(getLensContext().getExecutionWave());
        if (userDelta == null) {
            return createEmptyAssignmentDelta();
        }
        ContainerDelta<AssignmentType> assignmentDelta = userDelta.findContainerDelta(new ItemPath(UserType.F_ASSIGNMENT));
        if (assignmentDelta == null) { 
            return createEmptyAssignmentDelta();
        }
        return assignmentDelta;
    }
    
	public Collection<? extends ItemDelta<?>> getExecutionWaveAssignmentItemDeltas(Long id) throws SchemaException {
		if (getObjectTypeClass() != UserType.class) {
    		throw new UnsupportedOperationException("Attempt to get assignment deltas from "+getObjectTypeClass());
    	}
        ObjectDelta<UserType> userDelta = (ObjectDelta<UserType>) getWaveDelta(getLensContext().getExecutionWave());
        if (userDelta == null) {
            return null;
        }
        return userDelta.findItemDeltasSubPath(new ItemPath(new NameItemPathSegment(UserType.F_ASSIGNMENT),
        									  new IdItemPathSegment(id)));
	}

    private ContainerDelta<AssignmentType> createEmptyAssignmentDelta() {
        return new ContainerDelta<AssignmentType>(getAssignmentContainerDefinition());
    }
    
    private PrismContainerDefinition<AssignmentType> getAssignmentContainerDefinition() {
		return getObjectDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);
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
        
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("old"), getObjectOld(), indent+1);

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
    
}
