package com.evolveum.midpoint.ninja.action.stats;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemFactory;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.exception.SchemaException;

public interface PrismTransformableRecord {

    PrismContainerValue<Containerable> asPrismContainerValue(ItemFactory itemFactory) throws SchemaException;

}
