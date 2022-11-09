/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer.output;

import com.evolveum.midpoint.model.api.visualizer.SceneItemValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.polystring.PolyString;

public class SceneItemValueImpl implements SceneItemValue {

    private final PolyString text;
    private final PolyString additionalText;
    private PrismValue sourceValue;

    public SceneItemValueImpl(String text) {
        this(text, null);
    }

    public SceneItemValueImpl(String text, String additionalText) {
        this.text = text != null ? new PolyString(text) : null;
        this.additionalText = additionalText != null ? new PolyString(additionalText) : null;
    }

    public SceneItemValueImpl(PolyString text) {
        this(text, null);
    }

    public SceneItemValueImpl(PolyString text, PolyString additionalText) {
        this.text = text;
        this.additionalText = additionalText;
    }

    @Override
    public PolyString getText() {
        return text;
    }

    @Override
    public PolyString getAdditionalText() {
        return additionalText;
    }

    @Override
    public PrismValue getSourceValue() {
        return sourceValue;
    }

    public void setSourceValue(PrismValue sourceValue) {
        this.sourceValue = sourceValue;
    }

    @Override
    public String toString() {
        return "'" + text + "'" + (sourceValue != null ? "*" : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SceneItemValueImpl that = (SceneItemValueImpl) o;

        if (text != null ? !text.equals(that.text) : that.text != null) return false;
        return !(sourceValue != null ? !sourceValue.equals(that.sourceValue) : that.sourceValue != null);

    }

    @Override
    public int hashCode() {
        int result = text != null ? text.hashCode() : 0;
        result = 31 * result + (sourceValue != null ? sourceValue.hashCode() : 0);
        return result;
    }
}
