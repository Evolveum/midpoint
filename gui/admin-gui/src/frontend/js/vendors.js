/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

import '@popperjs/core'
import 'bootstrap'
import 'admin-lte'

import 'bootstrap-multiselect';

import 'moment/dist/moment'

import 'daterangepicker/daterangepicker';

import 'select2/dist/js/select2';

import 'sparklines';

import 'bootstrap5-toggle';

import 'ace-builds/src-noconflict/ace';

import '@eonasdan/tempus-dominus';

ace.config.setModuleUrl('ace/theme/eclipse',
    require('../../../node_modules/ace-builds/src-noconflict/theme-eclipse.js'));
ace.config.setModuleUrl('ace/theme/idle_fingers',
    require('../../../node_modules/ace-builds/src-noconflict/theme-idle_fingers.js'));
ace.config.setModuleUrl('ace/mode/xml',
    require('../../../node_modules/ace-builds/src-noconflict/mode-xml.js'));
ace.config.setModuleUrl('ace/mode/yaml',
    require('../../../node_modules/ace-builds/src-noconflict/mode-yaml.js'));
ace.config.setModuleUrl('ace/mode/json',
    require('../../../node_modules/ace-builds/src-noconflict/mode-json.js'));
ace.config.setModuleUrl('ace/mode/velocity',
    require('../../../node_modules/ace-builds/src-noconflict/mode-velocity.js'));
ace.config.setModuleUrl('ace/mode/javascript',
    require('../../../node_modules/ace-builds/src-noconflict/mode-javascript.js'));
ace.config.setModuleUrl('ace/mode/python',
    require('../../../node_modules/ace-builds/src-noconflict/mode-python.js'));
ace.config.setModuleUrl('ace/mode/velocity',
    require('../../../node_modules/ace-builds/src-noconflict/mode-velocity.js'));
ace.config.setModuleUrl('ace/mode/groovy',
    require('../../../node_modules/ace-builds/src-noconflict/mode-groovy.js'));
ace.config.setModuleUrl('ace/ext/language_tools',
    require('../../../node_modules/ace-builds/src-noconflict/ext-language_tools.js'));
ace.config.setModuleUrl('ace/ext/searchbox',
    require('../../../node_modules/ace-builds/src-noconflict/ext-searchbox.js'));

// xml worker module is always being loaded using some url, it's different that theme and ext modules, therefore we'll compute
// <midpoint_context>/static/worker-xml.js from current script URL and use it to load correctly worker-xml.js script
var url = document.currentScript.src;
url = url.replace(/vendors.js/, "worker-xml.js");

ace.config.setModuleUrl('ace/mode/xml_worker', url);
