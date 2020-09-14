/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;

/**
 * Secondary deltas from already completed waves.
 *
 * The meaning of this structure changed in midPoint 4.2. Originally, we stored here "live" secondary deltas for each
 * wave. Since 4.2, the current secondary deltas are stored directly in LensFocusContext; and they are moved here
 * after change execution.
 */
public class ObjectDeltaWaves<O extends ObjectType> implements Iterable<ObjectDelta<O>>, DebugDumpable, Serializable {

    private final List<WaveDelta<O>> waveDeltas = new ArrayList<>();

    public void add(int wave, ObjectDelta<O> delta) {
        waveDeltas.add(new WaveDelta<>(wave, delta));
    }

    public boolean isEmpty() {
        return waveDeltas.isEmpty();
    }

    public void clear() {
        waveDeltas.clear();
    }

    public int size() {
        return waveDeltas.size();
    }

    public WaveDelta<O> get(int index) {
        return waveDeltas.get(index);
    }

    @Override
    public @NotNull Iterator<ObjectDelta<O>> iterator() {
        return new Iterator<ObjectDelta<O>>() {

            private final Iterator<WaveDelta<O>> waveDeltaIterator = waveDeltas.iterator();

            @Override
            public boolean hasNext() {
                return waveDeltaIterator.hasNext();
            }

            @Override
            public ObjectDelta<O> next() {
                return waveDeltaIterator.next().delta;
            }
        };
    }

    public void setOid(String oid) {
        for (WaveDelta<O> waveDelta: waveDeltas) {
            waveDelta.setOid(oid);
        }
    }

    public void checkConsistence(boolean requireOid, String shortDesc) {
        for (WaveDelta<O> waveDelta : waveDeltas) {
            waveDelta.checkConsistence(requireOid, shortDesc);
        }
    }

    public void checkEncrypted(String shortDesc) {
        for (WaveDelta<O> waveDelta : waveDeltas) {
            waveDelta.checkEncrypted(shortDesc);
        }
    }

    public void normalize() {
        for (WaveDelta<O> waveDelta: waveDeltas) {
            waveDelta.normalize();
        }
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public ObjectDeltaWaves<O> clone() {
        ObjectDeltaWaves<O> clone = new ObjectDeltaWaves<>();
        copyValues(clone);
        return clone;
    }

    private void copyValues(ObjectDeltaWaves<O> clone) {
        for (WaveDelta<O> thisWaveDelta: this.waveDeltas) {
            clone.waveDeltas.add(thisWaveDelta.clone());
        }
    }

    public void adopt(PrismContext prismContext) throws SchemaException {
        for (WaveDelta<O> waveDelta: this.waveDeltas) {
            waveDelta.adopt(prismContext);
        }
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabel(sb, "ObjectDeltaWaves", indent);

        if (waveDeltas.isEmpty()) {
            sb.append(" empty");
        } else {
            for (WaveDelta<O> waveDelta : waveDeltas) {
                sb.append("\n");
                sb.append(waveDelta.debugDump(indent + 1));
            }
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return "ObjectDeltaWaves(" + waveDeltas + ")";
    }

    public ObjectDeltaWavesType toObjectDeltaWavesType() throws SchemaException {
        ObjectDeltaWavesType objectDeltaWavesBean = new ObjectDeltaWavesType();
        for (int i = 0; i < waveDeltas.size(); i++) {
            WaveDelta<O> waveDelta = waveDeltas.get(i);
            ObjectDeltaWaveType objectDeltaWaveBean = new ObjectDeltaWaveType();
            objectDeltaWaveBean.setNumber(i);
            if (waveDelta.delta != null) {
                objectDeltaWaveBean.setDelta(DeltaConvertor.toObjectDeltaType(waveDelta.delta));
            }
            objectDeltaWavesBean.getWave().add(objectDeltaWaveBean);
        }
        return objectDeltaWavesBean;
    }

    // don't forget to apply provisioning definitions to resulting deltas (it's the client responsibility)
    public static <O extends ObjectType> ObjectDeltaWaves<O> fromObjectDeltaWavesType(ObjectDeltaWavesType objectDeltaWavesBean,
            PrismContext prismContext) throws SchemaException {
        if (objectDeltaWavesBean == null) {
            return null;
        }

        ObjectDeltaWaves<O> objectDeltaWaves = new ObjectDeltaWaves<>();
        for (ObjectDeltaWaveType objectDeltaWaveBean : objectDeltaWavesBean.getWave()) {
            if (objectDeltaWaveBean.getNumber() == null) {
                throw new SchemaException("Missing wave number in " + objectDeltaWaveBean);
            }
            if (objectDeltaWaveBean.getDelta() != null) {
                objectDeltaWaves.add(objectDeltaWaveBean.getNumber(),
                        DeltaConvertor.createObjectDelta(objectDeltaWaveBean.getDelta(), prismContext));
            } else {
                objectDeltaWaves.add(objectDeltaWaveBean.getNumber(), null);
            }
        }
        return objectDeltaWaves;
    }

    /**
     * Delta for a specific execution wave.
     */
    public static class WaveDelta<O extends ObjectType> implements DebugDumpable, Serializable {

        private final int wave;
        private final ObjectDelta<O> delta;

        private WaveDelta(int wave, ObjectDelta<O> delta) {
            this.wave = wave;
            this.delta = delta;
        }

        @SuppressWarnings("MethodDoesntCallSuperMethod")
        public WaveDelta<O> clone() {
            return new WaveDelta<>(wave, delta != null ? delta.clone() : null);
        }

        public void adopt(PrismContext prismContext) throws SchemaException {
            if (delta != null) {
                prismContext.adopt(delta);
            }
        }

        public void setOid(String oid) {
            if (delta != null) {
                delta.setOid(oid);
            }
        }

        public void checkConsistence(boolean requireOid, String shortDesc) {
            if (delta != null) {
                try {
                    delta.checkConsistence(requireOid, true, true, ConsistencyCheckScope.THOROUGH);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(e.getMessage() + "; in " + shortDesc + ", wave " + wave, e);
                } catch (IllegalStateException e) {
                    throw new IllegalStateException(e.getMessage() + "; in " + shortDesc + ", wave " + wave, e);
                }
            }
        }

        public void checkEncrypted(String shortDesc) {
            if (delta != null) {
                try {
                    CryptoUtil.checkEncrypted(delta);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(e.getMessage() + "; in " + shortDesc + ", wave " + wave, e);
                } catch (IllegalStateException e) {
                    throw new IllegalStateException(e.getMessage() + "; in " + shortDesc + ", wave " + wave, e);
                }
            }
        }

        public void normalize() {
            if (delta != null) {
                delta.normalize();
            }
        }

        @Override
        public String debugDump(int indent) {
            StringBuilder sb = new StringBuilder();
            DebugUtil.debugDumpWithLabelLn(sb, "wave", wave, indent);
            DebugUtil.debugDumpWithLabel(sb, "delta", delta, indent);
            return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof WaveDelta)) {
                return false;
            }
            WaveDelta<?> waveDelta = (WaveDelta<?>) o;
            return wave == waveDelta.wave &&
                    Objects.equals(delta, waveDelta.delta);
        }

        @Override
        public int hashCode() {
            return Objects.hash(wave, delta);
        }
    }
}
