/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.impl.schema.PrismSchemaImpl;

/**
 * @author semancik
 */
public class ConnectorSchemaImpl extends PrismSchemaImpl implements ConnectorSchema {

    /** Please use only from {@link ConnectorSchemaFactory}. */
    ConnectorSchemaImpl(String namespace) {
        super(namespace);
    }

    @Override
    public String getUsualNamespacePrefix() {
        // This functionality was either never present or was disabled at some point in time.
        // If needed, the original (unused) code can be recovered from the commit that introduced this comment.
        return null;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public ConnectorSchemaImpl clone() {
        ConnectorSchemaImpl clone = new ConnectorSchemaImpl(namespace);
        super.copyContent(clone);
        return clone;
    }
}
