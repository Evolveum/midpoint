/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import java.io.Serializable;

/**
 * @author semancik
 *
 */
public interface Containerable extends Serializable {

    PrismContainerValue asPrismContainerValue();

    /**
     * Setup value to the containerable representation. This is used to after (empty) containerable is created to
     * initialize it with a correct prism container value.
     * Note: This method DOES NOT change the container value parent.
     */
    void setupContainerValue(PrismContainerValue container);

}
