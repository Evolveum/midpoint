/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.duplication;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperFactory;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Containerable;

/**
 * Execute changes for duplicated containerable object.
 * For example name 'Superuser' of original object will be changed to 'Copy of Superuser'.
 */
public interface ContainerableDuplicateResolver<C extends Containerable> extends WrapperFactory {

    C duplicateObject(C originalObject, PageBase pageBase);
}
