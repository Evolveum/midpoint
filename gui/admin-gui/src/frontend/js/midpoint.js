/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

import MidPointTheme from './midpoint-theme';
import MidPointAceEditor from "./ace-editor";
import MidPointHoneypot from "./honeypot";
import Popper from '@popperjs/core';

window.MidPointTheme = new MidPointTheme();
window.MidPointAceEditor = new MidPointAceEditor();
window.MidPointHoneypot = new MidPointHoneypot();
window.Popper = Popper;
