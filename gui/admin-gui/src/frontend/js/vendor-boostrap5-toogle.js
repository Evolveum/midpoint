/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

import { BootstrapToggle } from 'bootstrap5-toggle';

function initBootstrapToggle() {
    if (HTMLElement.prototype.bootstrapToggle) {
        return;
    }

    HTMLElement.prototype.bootstrapToggle = function (options) {
        if (!this._bootstrapToggle) {
            this._bootstrapToggle = new BootstrapToggle(this, options);
        }
        return this._bootstrapToggle;
    };
}

export function initBootstrapPlugins() {
    initBootstrapToggle();
}
