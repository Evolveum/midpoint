/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

import MidPointTheme from './midpoint-theme';
import MidPointAceEditor from "./ace-editor";
import MidPointHoneypot from "./honeypot";
import Popper from '@popperjs/core';

window.MidPointTheme = new MidPointTheme();
window.MidPointAceEditor = new MidPointAceEditor();
window.MidPointHoneypot = new MidPointHoneypot();
window.Popper = Popper;
