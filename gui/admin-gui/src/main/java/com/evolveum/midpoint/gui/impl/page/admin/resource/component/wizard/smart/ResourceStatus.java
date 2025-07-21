/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.smart;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

import javax.xml.namespace.QName;
import java.io.Serializable;

public interface ResourceStatus extends Serializable {

    String F_OBJECT_CLASSES_TEXT = "objectClassesText";
    String F_OBJECT_TYPES_SUGGESTIONS_TEXT = "objectTypesSuggestionsText";

    String getObjectClassesText();
    String getObjectTypesSuggestionsText();

    String getSuggestedObjectClassName();

    void checkObjectClassName(QName objectClassName) throws SchemaException, ConfigurationException;

    String getObjectTypesSuggestionToExplore();

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
        public String getSuggestedObjectClassName() {
            return "";
        }

        @Override
        public void checkObjectClassName(QName objectClassName) {
            throw new IllegalStateException("Resource is not ready");
        }

        @Override
        public String getObjectTypesSuggestionToExplore() {
            return "";
        }

        @Override
        public boolean isReal() {
            return false;
        }
    }
}
