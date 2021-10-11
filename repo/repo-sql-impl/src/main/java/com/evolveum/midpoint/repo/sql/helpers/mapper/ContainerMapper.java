/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.sql.data.common.container.Container;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class ContainerMapper<I extends Containerable, O extends Container> implements Mapper<I, O> {

}
