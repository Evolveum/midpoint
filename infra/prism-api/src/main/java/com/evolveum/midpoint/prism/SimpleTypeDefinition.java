/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import javax.xml.namespace.QName;

/**
 * Primarily for enums. (Experimental.)
 *
 * Currently there are no special methods.
 *
 * @author mederly
 */
public interface SimpleTypeDefinition extends TypeDefinition {

    enum DerivationMethod {
        EXTENSION, RESTRICTION, SUBSTITUTION
    }

    QName getBaseTypeName();

    DerivationMethod getDerivationMethod();
}
