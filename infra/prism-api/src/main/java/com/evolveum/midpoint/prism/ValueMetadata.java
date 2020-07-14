package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.annotation.Experimental;

@Experimental
public interface ValueMetadata extends PrismContainerValue<Containerable>, ShortDumpable {

    ValueMetadata clone();

}
