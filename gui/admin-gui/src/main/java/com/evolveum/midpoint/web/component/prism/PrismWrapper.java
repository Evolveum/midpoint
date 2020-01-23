/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.prism;

import java.io.Serializable;

public abstract class PrismWrapper implements Serializable{


         private boolean showEmpty;

         @Deprecated //used only for projections, after switching projection to table, remove it.
         private boolean minimalized=true;
        private boolean sorted;
        private boolean showMetadata;
        private boolean expanded = true;

        public boolean isMinimalized() {
            return minimalized;
        }

        public void setMinimalized(boolean minimalized) {
            this.minimalized = minimalized;
        }

        public boolean isSorted() {
            return sorted;
        }

        public void setSorted(boolean sorted) {
            this.sorted = sorted;
        }

        public boolean isShowMetadata() {
            return showMetadata;
        }

        public void setShowMetadata(boolean showMetadata) {
            this.showMetadata = showMetadata;
        }

        public boolean isShowEmpty() {
            return showEmpty;
        }

        public void setShowEmpty(boolean showEmpty, boolean recursive) {
            this.showEmpty = showEmpty;
            computeStripes();
        }

        public boolean isExpanded() {
            return expanded;
        }

        public void setExpanded(boolean expanded) {
            this.expanded = expanded;
        }

        public abstract void computeStripes();
}
