/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.visualizer;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.polystring.PolyString;

import java.io.Serializable;

public interface VisualizationItemValue extends Serializable {
    PolyString getText();
    PolyString getAdditionalText();            // this one should not be clickable (in case of references)
    PrismValue getSourceValue();
}
