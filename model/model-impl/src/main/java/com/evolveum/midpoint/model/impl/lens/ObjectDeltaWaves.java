/*
 * Copyright (c) 2010-2014 Evolveum
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

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.prism.ConsistencyCheckScope;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaWaveType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaWavesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.io.Serializable;
import java.util.*;

/**
 * @author semancik
 *
 */
public class ObjectDeltaWaves<O extends ObjectType> implements List<ObjectDelta<O>>, DebugDumpable, Serializable {
	
	private List<ObjectDelta<O>> waves = new ArrayList<ObjectDelta<O>>();

	/**
	 * Get merged deltas from all the waves.
	 */
	public ObjectDelta<O> getMergedDeltas() throws SchemaException {
		return getMergedDeltas(null, -1);
	}

	/**
	 * Get merged deltas from the waves up to maxWave (including). Optional initial delta may be supplied.
	 * Negative maxWave means to merge all available waves.
	 */
	public ObjectDelta<O> getMergedDeltas(ObjectDelta<O> initialDelta, int maxWave) throws SchemaException {
		ObjectDelta<O> merged = null;
		if (initialDelta != null) {
			merged = initialDelta.clone();
		}
		int waveNum = 0;
		for (ObjectDelta<O> delta: waves) {
			if (delta == null) {
				continue;
			}
			if (merged == null) {
				merged = delta.clone();
			} else {
				merged.merge(delta);
			}
			if (maxWave >= 0 && waveNum >= maxWave) {
				break;
			}
		}
		return merged;
	}

	public void setOid(String oid) {
		for (ObjectDelta<O> delta: waves) {
			if (delta == null) {
				continue;
			}
			delta.setOid(oid);
		}
	}

	public void checkConsistence(boolean requireOid, String shortDesc) {
		for (int wave = 0; wave < waves.size(); wave++) {
			ObjectDelta<O> delta = waves.get(wave);
			if (delta == null) {
				continue;
			}
			try {
				delta.checkConsistence(requireOid, true, true, ConsistencyCheckScope.THOROUGH);
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(e.getMessage()+"; in "+shortDesc+", wave "+wave, e);
			} catch (IllegalStateException e) {
				throw new IllegalStateException(e.getMessage()+"; in "+shortDesc+", wave "+wave, e);
			}
		}		
	}
	
	public void normalize() {
		if (waves != null) {
			for (ObjectDelta<O> wave: waves) {
				if (wave != null) {
					wave.normalize();
				}
			}
		}
	}
	
	public ObjectDeltaWaves<O> clone() {
		ObjectDeltaWaves<O> clone = new ObjectDeltaWaves<O>();
		copyValues(clone);
		return clone;		
	}

	protected void copyValues(ObjectDeltaWaves<O> clone) {
		for (ObjectDelta<O> thisWave: this.waves) {
			if (thisWave != null) {
				clone.waves.add(thisWave.clone());
			} else {
				clone.waves.add(null);
			}
		}
	}
	
	public void adopt(PrismContext prismContext) throws SchemaException {
		for (ObjectDelta<O> thisWave: this.waves) {
			if (thisWave != null) {
				prismContext.adopt(thisWave);
			}
		}
	}


	// DELEGATED METHODS (with small tweaks)
	
	/* (non-Javadoc)
	 * @see java.util.List#size()
	 */
	@Override
	public int size() {
		return waves.size();
	}

	/* (non-Javadoc)
	 * @see java.util.List#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return waves.isEmpty();
	}

	/* (non-Javadoc)
	 * @see java.util.List#contains(java.lang.Object)
	 */
	@Override
	public boolean contains(Object o) {
		return waves.contains(o);
	}

	/* (non-Javadoc)
	 * @see java.util.List#iterator()
	 */
	@Override
	public Iterator<ObjectDelta<O>> iterator() {
		return waves.iterator();
	}

	/* (non-Javadoc)
	 * @see java.util.List#toArray()
	 */
	@Override
	public Object[] toArray() {
		return waves.toArray();
	}

	/* (non-Javadoc)
	 * @see java.util.List#toArray(T[])
	 */
	@Override
	public <T> T[] toArray(T[] a) {
		return waves.toArray(a);
	}

	/* (non-Javadoc)
	 * @see java.util.List#add(java.lang.Object)
	 */
	@Override
	public boolean add(ObjectDelta<O> e) {
		return waves.add(e);
	}

	/* (non-Javadoc)
	 * @see java.util.List#remove(java.lang.Object)
	 */
	@Override
	public boolean remove(Object o) {
		return waves.remove(o);
	}

	/* (non-Javadoc)
	 * @see java.util.List#containsAll(java.util.Collection)
	 */
	@Override
	public boolean containsAll(Collection<?> c) {
		return waves.containsAll(c);
	}

	/* (non-Javadoc)
	 * @see java.util.List#addAll(java.util.Collection)
	 */
	@Override
	public boolean addAll(Collection<? extends ObjectDelta<O>> c) {
		return waves.addAll(c);
	}

	/* (non-Javadoc)
	 * @see java.util.List#addAll(int, java.util.Collection)
	 */
	@Override
	public boolean addAll(int index, Collection<? extends ObjectDelta<O>> c) {
		return waves.addAll(index, c);
	}

	/* (non-Javadoc)
	 * @see java.util.List#removeAll(java.util.Collection)
	 */
	@Override
	public boolean removeAll(Collection<?> c) {
		return waves.removeAll(c);
	}

	/* (non-Javadoc)
	 * @see java.util.List#retainAll(java.util.Collection)
	 */
	@Override
	public boolean retainAll(Collection<?> c) {
		return waves.retainAll(c);
	}

	/* (non-Javadoc)
	 * @see java.util.List#clear()
	 */
	@Override
	public void clear() {
		waves.clear();
	}

	/* (non-Javadoc)
	 * @see java.util.List#get(int)
	 */
	@Override
	public ObjectDelta<O> get(int index) {
		if (index >= waves.size()) {
			return null;
		}
		return waves.get(index);
	}

	/* (non-Javadoc)
	 * @see java.util.List#set(int, java.lang.Object)
	 */
	@Override
	public ObjectDelta<O> set(int index, ObjectDelta<O> element) {
		if (index >= waves.size()) {
			for (int i = waves.size(); i < index; i++) {
				waves.add(null);
			}
			waves.add(element);
			return element;
		}
		return waves.set(index, element);
	}

	/* (non-Javadoc)
	 * @see java.util.List#add(int, java.lang.Object)
	 */
	@Override
	public void add(int index, ObjectDelta<O> element) {
		waves.add(index, element);
	}

	/* (non-Javadoc)
	 * @see java.util.List#remove(int)
	 */
	@Override
	public ObjectDelta<O> remove(int index) {
		return waves.remove(index);
	}

	/* (non-Javadoc)
	 * @see java.util.List#indexOf(java.lang.Object)
	 */
	@Override
	public int indexOf(Object o) {
		return waves.indexOf(o);
	}

	/* (non-Javadoc)
	 * @see java.util.List#lastIndexOf(java.lang.Object)
	 */
	@Override
	public int lastIndexOf(Object o) {
		return waves.lastIndexOf(o);
	}

	/* (non-Javadoc)
	 * @see java.util.List#listIterator()
	 */
	@Override
	public ListIterator<ObjectDelta<O>> listIterator() {
		return waves.listIterator();
	}

	/* (non-Javadoc)
	 * @see java.util.List#listIterator(int)
	 */
	@Override
	public ListIterator<ObjectDelta<O>> listIterator(int index) {
		return waves.listIterator(index);
	}

	/* (non-Javadoc)
	 * @see java.util.List#subList(int, int)
	 */
	@Override
	public List<ObjectDelta<O>> subList(int fromIndex, int toIndex) {
		return waves.subList(fromIndex, toIndex);
	}
	
	// DUMP
	
	@Override
    public String debugDump() {
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
        sb.append("ObjectDeltaWaves:");
        
        if (waves.isEmpty()) {
        	sb.append(" empty");
        	return sb.toString();
        }
        
        if (waves.size() == 1) {
        	sb.append(" single wave\n");
            if (waves.get(0) != null) {
        	    sb.append(waves.get(0).debugDump(indent + 1));
            } else {
                DebugUtil.indentDebugDump(sb, indent + 1);
                sb.append("null\n");
            }
        } else {
	        sb.append(waves.size()).append(" waves");
	        for (int wave = 0; wave < waves.size(); wave++) {
	        	sb.append("\n");
				ObjectDelta<O> delta = waves.get(wave);
				DebugUtil.indentDebugDump(sb, indent + 1);
				sb.append("wave ").append(wave).append(":");
				if (delta == null) {
					sb.append(" null");
				} else {
					sb.append("\n");
					sb.append(delta.debugDump(indent + 2));
				}
	        }
        }
        
        return sb.toString();
    }

	@Override
	public String toString() {
		return "ObjectDeltaWaves(" + waves + ")";
	}


    public ObjectDeltaWavesType toObjectDeltaWavesType() throws SchemaException {
        ObjectDeltaWavesType objectDeltaWavesType = new ObjectDeltaWavesType();
        for (int i = 0; i < waves.size(); i++) {
            ObjectDelta wave = waves.get(i);
            if (wave != null) {
                ObjectDeltaWaveType objectDeltaWaveType = new ObjectDeltaWaveType();
                objectDeltaWaveType.setNumber(i);
                objectDeltaWaveType.setDelta(DeltaConvertor.toObjectDeltaType(wave));
                objectDeltaWavesType.getWave().add(objectDeltaWaveType);
            }
        }
        return objectDeltaWavesType;
    }

    // don't forget to apply provisioning definitions to resulting deltas (it's the client responsibility)
    public static <O extends ObjectType> ObjectDeltaWaves<O> fromObjectDeltaWavesType(ObjectDeltaWavesType secondaryDeltas, PrismContext prismContext) throws SchemaException {
        if (secondaryDeltas == null) {
            return null;
        }

        ObjectDeltaWaves<O> retval = new ObjectDeltaWaves<>();

        int max = 0;
        for (ObjectDeltaWaveType odwt : secondaryDeltas.getWave()) {
            if (odwt.getNumber() > max) {
                max = odwt.getNumber();
            }
        }

        ObjectDelta<O>[] wavesAsArray = new ObjectDelta[max+1];
        for (ObjectDeltaWaveType odwt : secondaryDeltas.getWave()) {
            wavesAsArray[odwt.getNumber()] = DeltaConvertor.createObjectDelta(odwt.getDelta(), prismContext);
        }

        for (int i = 0; i <= max; i++) {
            retval.waves.add(wavesAsArray[i]);
        }
        return retval;
    }

	public void checkEncrypted(String shortDesc) {
		for (int wave = 0; wave < waves.size(); wave++) {
			ObjectDelta<O> delta = waves.get(wave);
			if (delta == null) {
				continue;
			}
			try {
				CryptoUtil.checkEncrypted(delta);
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(e.getMessage()+"; in "+shortDesc+", wave "+wave, e);
			} catch (IllegalStateException e) {
				throw new IllegalStateException(e.getMessage()+"; in "+shortDesc+", wave "+wave, e);
			}
		}		
	}

	public void deleteDeltas() {
		waves.clear();
	}
}
