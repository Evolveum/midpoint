package com.evolveum.midpoint.ninja.action.stats;

import java.util.Collection;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemFactory;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;

public record FocusStats(Collection<FocusTypeStats> focusTypesStats) implements PrismTransformableRecord {
    private static final ItemName C_FOCUS = new ItemName(SchemaConstants.NS_C, "focus");

    @Override
    public PrismContainerValue<Containerable> asPrismContainerValue(ItemFactory itemFactory) throws SchemaException {
        final PrismContainerValue<Containerable> focusStatsValue = itemFactory.createContainerValue();
        final PrismContainer<Containerable> focus = itemFactory.createContainer(C_FOCUS);
        for (final FocusTypeStats focusTypeStats : this.focusTypesStats) {
            focus.addIgnoringEquivalents(focusTypeStats.asPrismContainerValue(itemFactory));
        }
        focusStatsValue.add(focus);

        return focusStatsValue;
    }

}
