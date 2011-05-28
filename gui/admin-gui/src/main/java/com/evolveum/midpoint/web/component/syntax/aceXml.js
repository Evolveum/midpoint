/*
 * author: lazyman
 * 
 * This file is only for information purposes only - for better reading of what is written in 
 * AceXmlInput.java in encodeEnd() method. This script is builded with StringBuilder.
 */

window.onload = function() {
	loadEditor();
	var postUpdateHandler = function(updates) {
		loadEditor();
	};
	ice.onAfterUpdate(postUpdateHandler);
};

function loadEditor() {
	var editorId = "";
	var editorHiddendId = "";

	var editor = ace.edit("j_idt53:editorReal");
	editor.setTheme("ace/theme/eclipse");

	var XmlMode = require("ace/mode/xml").Mode;
	editor.getSession().setMode(new XmlMode());
	document.getElementById('j_idt53:editorReal').style.fontSize = '13px';
	editor.setReadOnly(false);
	editor.getSession().on(
			'change',
			function() {
				document.getElementById('j_idt53:editor').value = editor
						.getSession().getValue();
			});
}