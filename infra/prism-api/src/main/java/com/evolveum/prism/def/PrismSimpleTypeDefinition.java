package com.evolveum.prism.def;

import java.util.Optional;

public interface PrismSimpleTypeDefinition extends PrismTypeDefinition {

    Optional<PrismComplexTypeDefinition> extendedFormDefinition();

}
