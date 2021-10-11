/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer.output;

import com.evolveum.midpoint.model.api.visualizer.SceneItemValue;
import com.evolveum.midpoint.prism.PrismValue;

/**
 * @author mederly
 */
public class SceneItemValueImpl implements SceneItemValue {

    private final String text;
    private final String additionalText;
    private PrismValue sourceValue;

    public SceneItemValueImpl(String text) {
        this.text = text;
        this.additionalText = null;
    }

    public SceneItemValueImpl(String text, String additionalText) {
        this.text = text;
        this.additionalText = additionalText;
    }

    @Override
    public String getText() {
        return text;
    }

    public String getAdditionalText() {
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
