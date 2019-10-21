/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.polystring;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringNormalizerConfigurationType;

/**
 * @author semancik
 *
 */
public interface ConfigurableNormalizer {

    void configure(PolyStringNormalizerConfigurationType configuration);

}
