/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.visualization;

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.util.LocalizableMessage;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ContainerVisualization implements Visualization {

    private LocalizableMessage simpleMessage;

    private LocalizableMessage name;

    private LocalizableMessage description;

    private ChangeType changeType;

    public LocalizableMessage getSimpleMessage() {
        return simpleMessage;
    }

    public void setSimpleMessage(LocalizableMessage simpleMessage) {
        this.simpleMessage = simpleMessage;
    }

    public LocalizableMessage getName() {
        return name;
    }

    public void setName(LocalizableMessage name) {
        this.name = name;
    }

    public LocalizableMessage getDescription() {
        return description;
    }

    public void setDescription(LocalizableMessage description) {
        this.description = description;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }
}
