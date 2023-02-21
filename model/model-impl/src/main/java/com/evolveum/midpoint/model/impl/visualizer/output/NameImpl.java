/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer.output;

import java.util.Objects;

import com.evolveum.midpoint.model.api.visualizer.Name;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

public class NameImpl implements Name {

    private final String simpleName;
    private String displayName;
    private String id;
    private String description;
    private boolean namesAreResourceKeys;

    private String simpleIcon;
    private LocalizableMessage simpleDescription;

    public NameImpl(String simpleName) {
        this.simpleName = simpleName;
    }

    @Override
    public String getSimpleName() {
        return simpleName;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean namesAreResourceKeys() {
        return namesAreResourceKeys;
    }

    public void setNamesAreResourceKeys(boolean namesAreResourceKeys) {
        this.namesAreResourceKeys = namesAreResourceKeys;
    }

    @Override
    public String getSimpleIcon() {
        return simpleIcon;
    }

    public void setSimpleIcon(String simpleIcon) {
        this.simpleIcon = simpleIcon;
    }

    @Override
    public LocalizableMessage getSimpleDescription() {
        return simpleDescription;
    }

    public void setSimpleDescription(LocalizableMessage simpleDescription) {
        this.simpleDescription = simpleDescription;
    }

    @Override
    public String toString() {
        return toDebugDump();
    }

    public String toDebugDump() {
        return simpleName + " (" + displayName + "), id=" + id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}

        NameImpl name = (NameImpl) o;

        if (namesAreResourceKeys != name.namesAreResourceKeys) {return false;}
        if (!Objects.equals(simpleName, name.simpleName)) {return false;}
        if (!Objects.equals(displayName, name.displayName)) {return false;}
        if (!Objects.equals(id, name.id)) {return false;}
        if (!Objects.equals(description, name.description)) {return false;}
        if (!Objects.equals(simpleIcon, name.simpleIcon)) {return false;}
        return Objects.equals(simpleDescription, name.simpleDescription);
    }

    @Override
    public int hashCode() {
        int result = simpleName != null ? simpleName.hashCode() : 0;
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (namesAreResourceKeys ? 1 : 0);
        result = 31 * result + (simpleIcon != null ? simpleIcon.hashCode() : 0);
        result = 31 * result + (simpleDescription != null ? simpleDescription.hashCode() : 0);
        return result;
    }
}
