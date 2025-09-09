/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.smart.impl.conndev;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

public class ConnectorDevelopment {

    private ConnectorType connectorObject;
    private ConnectorDevelopmentType developmentObject;

    public boolean developmentConnectorAvailable() {
        return connectorObject != null;
    }

//    public ProcessedDocumentation processedDocumentation() {
//        return null;
//    }

    public PrismObject<ResourceType> createTestResource() {
        return null;
    }
}
