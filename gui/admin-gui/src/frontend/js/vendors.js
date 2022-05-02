/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

import '../../../node_modules/admin-lte/plugins/bootstrap/js/bootstrap';
import '../../../node_modules/admin-lte/dist/js/adminlte';

import '../../../node_modules/bootstrap-select';
// import '../../../node_modules/bootstrap-multiselect';  // todo enable, used in DropDownMultiChoice (ListMultipleChoicePanel)

import '../../../node_modules/ace-builds/src-noconflict/ace';

ace.config.setModuleUrl('ace/theme/eclipse',
    require('../../../node_modules/ace-builds/src-noconflict/theme-eclipse.js'));
ace.config.setModuleUrl('ace/mode/xml',
    require('../../../node_modules/ace-builds/src-noconflict/mode-xml.js'));
ace.config.setModuleUrl('ace/ext/language_tools',
    require('../../../node_modules/ace-builds/src-noconflict/ext-language_tools.js'));

// ace.config.setModuleUrl('ace/mode/xml_worker', require('../../../node_modules/ace-builds/src-noconflict/worker-xml.js'));
ace.config.setModuleUrl('ace/mode/xml_worker',
    require('file-loader?name=[name].[ext]&esModule=false!../../../node_modules/ace-builds/src-noconflict/worker-xml.js'));
