package com.evolveum.midpoint.model.api.correlator;

import java.io.Serializable;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.util.ShortDumpable;

/**
 * Generalized confidence information.
 *
 * Its subtypes may contain additional information, like confidence for individual items.
 */
public class Confidence implements Serializable, ShortDumpable {

    private final double value;

    protected Confidence(double value) {
        this.value = value;
    }

    public static Confidence of(double value) {
        return new Confidence(value);
    }

    public static Confidence full() {
        return of(1.0);
    }

    public static Confidence zero() {
        return of(0.0);
    }

    public double getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append(value);
    }

    /** The {@link Confidence} extended by per-item confidence values. */
    public static class PerItemConfidence extends Confidence {

        @NotNull private final PathKeyedMap<Double> itemConfidences = new PathKeyedMap<>();

        private PerItemConfidence(double value) {
            super(value);
        }

        public static Confidence of(double value, @NotNull Map<ItemPath, Double> itemConfidences) {
            var confidence = new PerItemConfidence(value);
            confidence.itemConfidences.putAll(itemConfidences);
            return confidence;
        }

        public @Nullable Double getItemConfidence(@NotNull ItemPath itemPath) {
            return itemConfidences.get(itemPath);
        }
    }
}
