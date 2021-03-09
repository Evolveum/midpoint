/*
 * Copyright (C) 2020-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.query;

import com.evolveum.midpoint.prism.PrismNamespaceContext;

public interface PrismQuerySerialization {

    PrismNamespaceContext namespaceContext();

    String filterText();

    class NotSupportedException extends Exception {

        private static final long serialVersionUID = -5393426442630191647L;

        public NotSupportedException(String message, Throwable cause) {
            super(message, cause);
        }

        public NotSupportedException(String message) {
            super(message);
        }
    }

}
