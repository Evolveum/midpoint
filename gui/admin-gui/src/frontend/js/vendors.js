/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

import '../../../node_modules/popper.js/dist/umd/popper';
import '../../../node_modules/admin-lte/plugins/bootstrap/js/bootstrap';
import '../../../node_modules/admin-lte/dist/js/adminlte';

import '../../../node_modules/bootstrap-select';
import '../../../node_modules/bootstrap-multiselect';

import '../../../node_modules/moment/dist/moment'

import '../../../node_modules/daterangepicker/daterangepicker';

import './passwords';

import '../../../node_modules/ace-builds/src-noconflict/ace';

ace.config.setModuleUrl('ace/theme/eclipse',
    require('../../../node_modules/ace-builds/src-noconflict/theme-eclipse.js'));
ace.config.setModuleUrl('ace/theme/eclipse',
    require('../../../node_modules/ace-builds/src-noconflict/theme-idle_fingers.js'));
ace.config.setModuleUrl('ace/mode/xml',
    require('../../../node_modules/ace-builds/src-noconflict/mode-xml.js'));
ace.config.setModuleUrl('ace/ext/language_tools',
    require('../../../node_modules/ace-builds/src-noconflict/ext-language_tools.js'));
ace.config.setModuleUrl('ace/ext/searchbox',
    require('../../../node_modules/ace-builds/src-noconflict/ext-searchbox.js'));

// ace.config.setModuleUrl('ace/mode/xml_worker', require('../../../node_modules/ace-builds/src-noconflict/worker-xml.js'));
require('file-loader?publicPath=../../static/&name=[name].[ext]&esModule=false!../../../node_modules/ace-builds/src-noconflict/worker-xml.js')


// xml worker module is always being loaded using some url, it's different that theme and ext modules, therefore we'll compute
// <midpoint_context>/static/worker-xml.js from current script URL and use it to load correctly worker-xml.js script
var url = document.currentScript.src;
url = url.replace(/vendors.js/, "worker-xml.js");

ace.config.setModuleUrl('ace/mode/xml_worker', url);
