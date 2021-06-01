/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

public interface PrismItemAccessDefinition {

    /**
     * Returns true if this item can be read (displayed).
     * In case of containers this flag is, strictly speaking, not applicable. Container is an
     * empty shell. What matters is access to individual sub-item. However, for containers this
     * value has a "hint" meaning.  It means that the container itself contains something that is
     * readable. Which can be used as a hint by the presentation to display container label or block.
     * This usually happens if the container contains at least one readable item.
     * This does NOT mean that also all the container items can be displayed. The sub-item permissions
     * are controlled by similar properties on the items. This property only applies to the container
     * itself: the "shell" of the container.
     * <p>
     * Note: It was considered to use a different meaning for this flag - a meaning that would allow
     * canRead()=false containers to have readable items. However, this was found not to be very useful.
     * Therefore the "something readable inside" meaning was confirmed instead.
     */
    boolean canRead();

    /**
     * Returns true if this item can be modified (updated).
     * In case of containers this means that the container itself should be displayed in modification forms
     * E.g. that the container label or block should be displayed. This usually happens if the container
     * contains at least one modifiable item.
     * This does NOT mean that also all the container items can be modified. The sub-item permissions
     * are controlled by similar properties on the items. This property only applies to the container
     * itself: the "shell" of the container.
     */
    boolean canModify();

    /**
     * Returns true if this item can be added: it can be part of an object that is created.
     * In case of containers this means that the container itself should be displayed in creation forms
     * E.g. that the container label or block should be displayed. This usually happens if the container
     * contains at least one createable item.
     * This does NOT mean that also all the container items can be created. The sub-item permissions
     * are controlled by similar properties on the items. This property only applies to the container
     * itself: the "shell" of the container.
     */
    boolean canAdd();

    interface Mutable extends PrismItemAccessDefinition {

        void setCanRead(boolean val);

        void setCanModify(boolean val);

        void setCanAdd(boolean val);

    }
}
