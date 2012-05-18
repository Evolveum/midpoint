/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
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

