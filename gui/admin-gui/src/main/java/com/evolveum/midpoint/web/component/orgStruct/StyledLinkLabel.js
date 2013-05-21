/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var cssInPanel = {
	'background' : '#e3e3e3',
};

var cssOutPanel = {
	'background' : '#FFFFFF',
};

function initMenuButtons() {
	$(document).unbind("mousedown");
	$(".treeButtonMenu").unbind("mouseup");
	$(".menuPanel a").unbind("click");
	
	var panel = null;
	var button = null;
	$(".treeButtonMenu").bind("mouseup", function(e) {
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
	
	$(document).bind("mousedown", function(e) {
		if (panel.has(e.target).length === 0) {
			button.css("z-index", "0");
			panel.css("z-index", "0");
			panel.hide();
		}
	});
	
	$(".menuPanel a").bind("click", function() {
		button.css("z-index", "0");
		panel.css("z-index", "0");
		panel.hide();
	});

	$(".menuPanel table td").mouseenter(function() {
		$(this).css(cssInPanel);
	}).mouseleave(function() {
		$(this).css(cssOutPanel);
	});
};
