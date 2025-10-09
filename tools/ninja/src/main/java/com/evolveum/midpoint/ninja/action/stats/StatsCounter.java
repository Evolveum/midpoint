package com.evolveum.midpoint.ninja.action.stats;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class StatsCounter {
    private final Map<Class<? extends ObjectType>, FocusTypeCounter> typesCounters;

    public StatsCounter() {
        typesCounters = new HashMap<>();
    }

    void count(ObjectType object) {
        final PrismObject<? extends ObjectType> prismObject = object.asPrismObject();
        final String objectName = prismObject.getElementName().getLocalPart();
        final FocusTypeCounter counter = this.typesCounters.computeIfAbsent(object.getClass(),
                key -> new FocusTypeCounter(objectName));

        counter.count(prismObject);
    }

    FocusStats calculate() {
        final List<FocusTypeStats> focusTypesStats = this.typesCounters.values().stream()
                .map(FocusTypeCounter::calculate)
                .toList();
        return new FocusStats(focusTypesStats);
    }

}
