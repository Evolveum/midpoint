/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.smart;

import com.evolveum.midpoint.smart.api.info.ObjectClassInfo;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTypesSuggestionType;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.List;

public interface ResourceStatus extends Serializable {

    String F_OBJECT_CLASSES_TEXT = "objectClassesText";
    String F_OBJECT_TYPES_SUGGESTIONS_TEXT = "objectTypesSuggestionsText";

    String getObjectClassesText();

    default void setObjectClassesText(String text) {
        // no-op
    }

    String getObjectTypesSuggestionsText();

    default void setObjectTypesSuggestionsText(String text) {
        // no-op
    }

    ObjectClassInfo getSuggestedObjectClassInfo();

    List<ObjectClassInfo> getObjectClassInfos();

    void checkObjectClassName(QName objectClassName) throws SchemaException, ConfigurationException;

    StatusInfo<ObjectTypesSuggestionType> getObjectTypesSuggestionToExplore();

    List<StatusInfo<ObjectTypesSuggestionType>> getObjectTypesSuggestions();

    boolean isReal();

    record ErrorStatus(String text) implements ResourceStatus {

        @Override
        public String toString() {
            return text;
        }

        @Override
        public String getObjectClassesText() {
            return text;
        }

        @Override
        public String getObjectTypesSuggestionsText() {
            return "";
        }

        @Override
        public ObjectClassInfo getSuggestedObjectClassInfo() {
            return null;
        }

        @Override
        public List<ObjectClassInfo> getObjectClassInfos() {
            return List.of();
        }

        @Override
        public void checkObjectClassName(QName objectClassName) {
            throw new IllegalStateException("Resource is not ready");
        }

        @Override
        public StatusInfo<ObjectTypesSuggestionType> getObjectTypesSuggestionToExplore() {
            return null;
        }

        @Override
        public List<StatusInfo<ObjectTypesSuggestionType>> getObjectTypesSuggestions() {
            return List.of();
        }

        @Override
        public boolean isReal() {
            return false;
        }
    }
}
