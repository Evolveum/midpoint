// import MidPointTheme from './midpoint-theme';

import '../../../../../target/node_modules/admin-lte/plugins/bootstrap/js/bootstrap';
import '../../../../../target/node_modules/admin-lte/dist/js/adminlte';

import MidPointTheme from './midpoint/midpoint-theme';
import MidPointAceEditor from "./midpoint/ace-editor";

// Test.abc();

window.MidPointTheme = new MidPointTheme();
window.MidPointAceEditor = new MidPointAceEditor();