/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer.output;

import com.evolveum.midpoint.model.api.visualizer.VisualizationItemValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;

public class VisualizationItemValueImpl implements VisualizationItemValue {

    private final LocalizableMessage text;
    private final LocalizableMessage additionalText;
    private PrismValue sourceValue;

    public VisualizationItemValueImpl(String text) {
        this(text, null);
    }

    public VisualizationItemValueImpl(String text, String additionalText) {
        this.text = text != null ? new SingleLocalizableMessage(text, new Object[0], text) : null;
        this.additionalText = additionalText != null ? new SingleLocalizableMessage(additionalText, new Object[0], additionalText) : null;
    }

    public VisualizationItemValueImpl(LocalizableMessage text) {
        this(text, null);
    }

    public VisualizationItemValueImpl(LocalizableMessage text, LocalizableMessage additionalText) {
        this.text = text;
        this.additionalText = additionalText;
    }

    @Override
    public LocalizableMessage getText() {
        return text;
    }

    @Override
    public LocalizableMessage getAdditionalText() {
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

        VisualizationItemValueImpl that = (VisualizationItemValueImpl) o;

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
