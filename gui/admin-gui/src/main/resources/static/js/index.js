/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

// import MidPointTheme from './midpoint-theme';

import '../../../../../node_modules/admin-lte/plugins/bootstrap/js/bootstrap';
import '../../../../../node_modules/admin-lte/dist/js/adminlte';

import MidPointTheme from './midpoint/midpoint-theme';
import MidPointAceEditor from "./midpoint/ace-editor";

// Test.abc();

window.MidPointTheme = new MidPointTheme();
window.MidPointAceEditor = new MidPointAceEditor();
