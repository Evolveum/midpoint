/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.visualizer;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.LocalizableMessage;

import java.io.Serializable;

public interface VisualizationItemValue extends Serializable {
    LocalizableMessage getText();
    LocalizableMessage getAdditionalText();            // this one should not be clickable (in case of references)
    PrismValue getSourceValue();
}
