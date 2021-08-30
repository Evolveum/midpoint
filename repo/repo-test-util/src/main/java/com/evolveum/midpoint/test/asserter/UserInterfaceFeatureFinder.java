package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceFeatureType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainersSpecificationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.assertj.core.api.Assertions;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class UserInterfaceFeatureFinder<RA, F extends UserInterfaceFeatureType> {

    private String identifier;
    private String displayName;

    public UserInterfaceFeatureFinder() {
    }

    public UserInterfaceFeatureFinder<RA, F> identifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    public UserInterfaceFeatureFinder<RA, F> displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public RA find() {
        Predicate<F> filter = vc -> {
            if (identifier != null) {
                if (!identifier.equals(vc.getIdentifier())) {
                    return false;
                }
            }

            if (displayName != null) {
                if (!PrismTestUtil.createPolyString(displayName).equalsOriginalValue(getLabel(vc))) {
                    return false;
                }
            }
            return true;
        };
        return find(filter);
    }

    protected abstract RA find(Predicate<F> filter);

//    private RA find(Predicate<F> filter) {
//        List<VirtualContainersSpecificationType> foundVirtualContainers = virtualContainersAsserter.getVirtualContainers()
//                .stream()
//                .filter(filter)
//                .collect(Collectors.toList());
//        Assertions.assertThat(foundVirtualContainers).hasSize(1);
//        return new VirtualContainerSpecificationAsserter<>(foundVirtualContainers.iterator().next(), virtualContainersAsserter, "from list of virtual containers " + virtualContainersAsserter.getVirtualContainers());
//    }

    private PolyString getLabel(F container) {
        DisplayType display = container.getDisplay();
        if (display == null) {
            return null;
        }

        PolyStringType label = display.getLabel();
        if (label == null) {
            return null;
        }

        return label.toPolyString();
    }
}
