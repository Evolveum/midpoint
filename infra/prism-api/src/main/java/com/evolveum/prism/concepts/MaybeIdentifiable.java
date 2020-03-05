package com.evolveum.prism.concepts;

import java.util.Optional;

public interface MaybeIdentifiable<T> {

    Optional<T> identifier();

}
