/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer.output;

import java.util.Objects;

import com.evolveum.midpoint.model.api.visualizer.LocalizationCustomizationContext;
import com.evolveum.midpoint.model.api.visualizer.Name;
import com.evolveum.midpoint.model.api.visualizer.localization.WrapableLocalization;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;

public class NameImpl implements Name {

    private LocalizableMessage overview;

    private WrapableLocalization<String, LocalizationCustomizationContext> customizableOverview;

    private LocalizableMessage displayName;

    private LocalizableMessage simpleName;

    private LocalizableMessage description;

    private String id;

    public NameImpl(String simpleName) {
        this(simpleName != null ? new SingleLocalizableMessage(simpleName, new Object[0], simpleName) : null);
    }

    public NameImpl(LocalizableMessage simpleName) {
        this.simpleName = simpleName;
    }

    @Override
    public LocalizableMessage getSimpleName() {
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
    public LocalizableMessage getDisplayName() {
        return displayName;
    }

    public void setDisplayName(LocalizableMessage displayName) {
        this.displayName = displayName;
    }

    public void setDisplayName(String displayName) {
        LocalizableMessage msg = displayName != null ? new SingleLocalizableMessage(displayName, new Object[0], displayName) : null;

        setDisplayName(msg);
    }

    @Override
    public LocalizableMessage getDescription() {
        return description;
    }

    public void setDescription(LocalizableMessage description) {
        this.description = description;
    }

    public void setDescription(String description) {
        LocalizableMessage msg = description != null ? new SingleLocalizableMessage(description, new Object[0], description) : null;

        setDescription(msg);
    }

    public void setCustomizableOverview(WrapableLocalization<String, LocalizationCustomizationContext> customizableOverview) {
        this.customizableOverview = customizableOverview;
    }

    @Override
    public LocalizableMessage getOverview() {
        return overview;
    }

    @Override
    public WrapableLocalization<String, LocalizationCustomizationContext> getCustomizableOverview() {
        return this.customizableOverview;
    }

    public void setOverview(LocalizableMessage overview) {
        this.overview = overview;
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

        if (!Objects.equals(simpleName, name.simpleName)) {return false;}
        if (!Objects.equals(displayName, name.displayName)) {return false;}
        if (!Objects.equals(id, name.id)) {return false;}
        if (!Objects.equals(description, name.description)) {return false;}
        return Objects.equals(overview, name.overview);
    }

    @Override
    public int hashCode() {
        int result = simpleName != null ? simpleName.hashCode() : 0;
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (overview != null ? overview.hashCode() : 0);
        return result;
    }
}
