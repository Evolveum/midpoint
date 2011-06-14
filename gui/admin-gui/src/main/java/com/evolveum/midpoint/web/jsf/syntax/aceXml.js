/*
 * author: lazyman
 * 
 * This file is only for information purposes only - for better reading of what is written in 
 * AceXmlInput.java in encodeEnd() method. This script is builded with StringBuilder.
 */

/*
 * this method can be used for testing if client browser supports html5 canvas, not integrated yet
 */
function supports_canvas() {
	return !!document.createElement('canvas').getContext;
}

// TODO: refactor to use this - no window.onload but ice.onLoad
// var onLoadCallback = function() {
// loadEditor();
// var postUpdateHandler = function(updates) {
// loadEditor();
// };
// ice.onAfterUpdate(postUpdateHandler);
// };
// ice.onLoad(onLoadCallback);

ice.onLoad(function() {
	alert('vilkooo');
});

ice.onLoad(function() {
	loadEditor("editorId", "editoIdReal", true);
	ice.onAfterUpdate(function(updates) {
		loadEditor("editorId", "editoIdReal", true);
	});
});

function loadEditor() {
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