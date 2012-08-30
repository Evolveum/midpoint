/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

var cssInPanel = {
	'background' : '#e3e3e3',
};

var cssOutPanel = {
	'background' : '#FFFFFF',
};

function initMenuButtons() {
	$("body").unbind("mousedown");
	
	var panel = null;
	var button = null;
	$(".treeButtonMenu").mouseup(function(e) {
		var id = $(this).attr("id");
		panel = $("#" + id + "_panel");
		button = $(this);
		if (panel.is(":hidden")) {
			$(".menuPanel").hide();
			button.css("z-index", "2");
			panel.css("z-index", "1");
			panel.css("display", "inline");
		}

	});
	
	$("body").bind("mousedown", function(e) {
		if (panel.has(e.target).length === 0) {
			button.css("z-index", "0");
			panel.css("z-index", "0");
			panel.hide();
		}
	});

	$(".menuPanel table td").mouseenter(function() {
		$(this).css(cssInPanel);
	}).mouseleave(function() {
		$(this).css(cssOutPanel);
	});
}
