/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.annotation.Experimental;

/**
 *  EXPERIMENTAL
 */
@Experimental
public interface MutableItemDefinition<I extends Item> extends ItemDefinition<I>, MutableDefinition {

    void setMinOccurs(int value);

    void setMaxOccurs(int value);

    void setCanRead(boolean val);

    void setCanModify(boolean val);

    void setCanAdd(boolean val);

    void setValueEnumerationRef(PrismReferenceValue valueEnumerationRef);

    void setOperational(boolean operational);

    void setDynamic(boolean value);

    // use with care
    void setItemName(QName name);

    void setReadOnly();

    void setDeprecatedSince(String value);

    void setPlannedRemoval(String value);

    void setElaborate(boolean value);

    void setHeterogeneousListItem(boolean value);

    void setSubstitutionHead(QName value);

    void setIndexOnly(boolean value);
}
