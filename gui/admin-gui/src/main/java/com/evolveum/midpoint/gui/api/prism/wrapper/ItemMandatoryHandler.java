/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.prism.wrapper;

import java.io.Serializable;

@FunctionalInterface
public interface ItemMandatoryHandler extends Serializable {

    boolean isMandatory(ItemWrapper<?, ?> itemWrapper);

    /**
     * In some cases while executing a validation check of mandatory option, the validation can be skipped
     * (e.g. when mandatory item is a part of an empty container, see ticket #10210).
     * But there are also cases when the item should be forced to be checked for mandatory option
     * (e.g. when comment is set to be mandatory while approving cert. item, see ticket #10974).
     * The default return value of the method is false which means that additional checks can be executed
     * to resolve the final value of mandatory option. If forceValidation is overridden to return true value,
     * mandatory option will be taken as it is, without possibility to re-analyze it.
     * @return
     */
    default boolean forceValidation() {
        return false;
    }
}
