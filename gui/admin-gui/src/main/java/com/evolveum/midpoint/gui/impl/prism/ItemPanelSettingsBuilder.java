/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism;

public class ItemPanelSettingsBuilder {

        private ItemPanelSettings settings;

        public ItemPanelSettingsBuilder() {
            settings = new ItemPanelSettings();
        }

        public ItemPanelSettingsBuilder visibilityHandler(ItemVisibilityHandler handler) {
            settings.setVisibilityHandler(handler);
            return this;
        }

        public ItemPanelSettingsBuilder showOnTopLevel(boolean showOnTopLevel) {
            settings.setShowOnTopLevel(showOnTopLevel);
            return this;
        }

        public ItemPanelSettingsBuilder headerVisibility(boolean headerVisibility) {
            settings.setHeaderVisible(headerVisibility);
            return this;
        }

        public ItemPanelSettings build() {
            return settings;
        }
}
