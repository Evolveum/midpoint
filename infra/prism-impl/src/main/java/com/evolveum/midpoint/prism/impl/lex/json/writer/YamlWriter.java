/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json.writer;

import com.evolveum.midpoint.prism.SerializationContext;

public class YamlWriter extends AbstractWriter {

    @Override
    YamlWritingContext createWritingContext(SerializationContext prismSerializationContext) {
        return new YamlWritingContext(prismSerializationContext);
    }
}
