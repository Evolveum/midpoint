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

function initOptionPanel(isHideContent){
	if (!isHideContent) {
		$(".optionRightBar").show();
		$(".optionLeftBarArrow").css("backgroundPosition", "0px 0px");
		$(".optionResult").css("padding-left", "220px");
		unselectText();
	} else {
		$(".optionRightBar").hide();
		$(".optionResult").css("padding-left", "0");
		$(".optionLeftBarArrow").css("backgroundPosition", "0px 16px");
	}
	
	$(".optionLeftBar").mouseenter(function(){
		$(this).css("cursor", "pointer");
		$(this).css("backgroundColor", "#BBBBBB");
	}).click(function() {
		if ($(".optionRightBar").css("display") == "none") {
			$(".optionRightBar").show();
			$(".optionLeftBarArrow").css("backgroundPosition", "0px 0px");
			$(".optionResult").css("padding-left", "220px");
			unselectText();
		} else {
			$(".optionRightBar").hide();
			$(".optionResult").css("padding-left", "0");
			$(".optionLeftBarArrow").css("backgroundPosition", "0px 16px");
		}
	}).mouseleave(function() {
		$(this).css("backgroundColor", "#c9c9c9");
	});
}
