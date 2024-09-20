/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.validator;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Describes the result of a single limitation check. The {@link #minOccurs}, {@link #maxOccurs}, and {@link #mustBeFirst}
 * fields may or may not be applicable to a given limitation, and their meaning may vary (e.g. minOccurs for uniqueness check).
 *
 * @author skublik
 */
public class StringLimitationResult implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    /** Name of the limitation. May or may not be the same as the `name` in the limitation definition. */
    private PolyStringType name;

    /** More detailed description of the limitation. */
    private PolyStringType help;

    /** Was the check successful? */
    private boolean success = true;

    /** More detailed messages explaining the result of the check. */
    @NotNull private final List<LocalizableMessage> messages = new ArrayList<>();

    /** (Required) lower value related to the limitation - if applicable. */
    private Integer minOccurs;

    /** (Allowed) upper value related to the limitation - if applicable. */
    private Integer maxOccurs;

    /** "Must be first" flag in the limitation - if applicable. */
    private Boolean mustBeFirst;

    public static @NotNull List<LocalizableMessage> extractMessages(@NotNull List<StringLimitationResult> results) {
        return results.stream()
                .flatMap(r -> r.getMessages().stream())
                .toList();
    }

    public PolyStringType getName() {
        return name;
    }

    public void setName(PolyStringType name) {
        this.name = name;
    }

    public PolyStringType getHelp() {
        return help;
    }

    public void setHelp(PolyStringType help) {
        this.help = help;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public @NotNull List<LocalizableMessage> getMessages() {
        return messages;
    }

    public Integer getMinOccurs() {
        return minOccurs;
    }

    public void setMinOccurs(Integer minOccurs) {
        this.minOccurs = minOccurs;
    }

    public Integer getMaxOccurs() {
        return maxOccurs;
    }

    public void setMaxOccurs(Integer maxOccurs) {
        this.maxOccurs = maxOccurs;
    }

    public Boolean isMustBeFirst() {
        return mustBeFirst;
    }

    public void setMustBeFirst(Boolean mustBeFirst) {
        this.mustBeFirst = mustBeFirst;
    }

    public void recordFailure(@NotNull LocalizableMessage msg) {
        messages.add(msg);
        setSuccess(false);
    }
}
