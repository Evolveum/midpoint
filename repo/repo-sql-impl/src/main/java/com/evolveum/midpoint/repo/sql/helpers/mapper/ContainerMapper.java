/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.sql.data.common.container.Container;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class ContainerMapper<I extends Containerable, O extends Container> implements Mapper<I, O> {

}
