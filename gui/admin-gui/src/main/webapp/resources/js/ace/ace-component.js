/* // generated for each jsf component
if (!isDefined('editors')) {
	window.editors = {};
}
window.editors['j_idt48:editor1'] = false;

ice.onLoad(function() {
	loadEditor("j_idt48:editor1");
	ice.onAfterUpdate(function(updates) {
		loadEditor("j_idt48:editor1");
	});
});
 */

function loadEditor(editorId) {
	var editor = ace.edit(editorId + "Real");
	editor.setTheme("ace/theme/eclipse");

	var XmlMode = require("ace/mode/xml").Mode;
	editor.getSession().setMode(new XmlMode());
	document.getElementById(editorId + "Real").style.fontSize = '13px';
	editor.setReadOnly(window.editors[editorId]);
	editor.getSession().on(
			'change',
			function() {
				document.getElementById(editorId).value = editor.getSession()
						.getValue();
			});
}

function isDefined(variable) {
	return (typeof (window[variable]) == "undefined") ? false : true;
}