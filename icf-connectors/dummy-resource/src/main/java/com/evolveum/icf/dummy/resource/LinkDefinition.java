/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.icf.dummy.resource;

import com.evolveum.icf.dummy.resource.LinkClassDefinition.ParticipantIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

/**
 * Describes the participation of an object class in a link class: the class definition itself
 * and whether we are first or second participant.
 */
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

    public @NotNull LinkClassDefinition.Participant getParticipant() {
        return isFirst() ? linkClassDefinition.getFirstParticipant() : linkClassDefinition.getSecondParticipant();
    }

    public @NotNull LinkClassDefinition.Participant getOtherParticipant() {
        return isFirst() ? linkClassDefinition.getSecondParticipant() : linkClassDefinition.getFirstParticipant();
    }

    public @NotNull ParticipantIndex getParticipantIndex() {
        return participantIndex;
    }

    @NotNull String getLinkClassName() {
        return linkClassDefinition.getName();
    }

    @Override
    public String toString() {
        return "LinkDefinition{" + participantIndex + " in " + linkClassDefinition.getName() + "}";
    }

    public boolean isVisible() {
        return getParticipant().isVisible();
    }

    public @NotNull String getLinkNameRequired() {
        return getParticipant().getLinkNameRequired();
    }

    @NotNull Set<String> getObjectClassNames() {
        return getParticipant().getObjectClassNames();
    }
}
