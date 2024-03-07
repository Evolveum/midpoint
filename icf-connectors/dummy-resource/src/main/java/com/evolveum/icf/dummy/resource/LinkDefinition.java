/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.icf.dummy.resource;

import com.evolveum.icf.dummy.resource.LinkClassDefinition.ParticipantIndex;
import org.jetbrains.annotations.NotNull;

/** TODO */
public class LinkDefinition {

    @NotNull private final LinkClassDefinition linkClassDefinition;

    @NotNull private final ParticipantIndex participantIndex;

    LinkDefinition(@NotNull LinkClassDefinition linkClassDefinition, @NotNull ParticipantIndex participantIndex) {
        this.linkClassDefinition = linkClassDefinition;
        this.participantIndex = participantIndex;
    }

    public @NotNull LinkClassDefinition getLinkClassDefinition() {
        return linkClassDefinition;
    }

    public boolean isFirst() {
        return participantIndex == ParticipantIndex.FIRST;
    }

    @NotNull ParticipantIndex getParticipantIndex() {
        return participantIndex;
    }

    @NotNull String getLinkClassName() {
        return linkClassDefinition.getName();
    }

    @Override
    public String toString() {
        return "LinkDefinition{" + participantIndex + " in " + linkClassDefinition.getName() + "}";
    }
}
