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

function initMessages() {
	//$(document).find("messages-topError");
	$(".messages-topError").mouseenter(function(){
		var idBlock = $(this).attr("id");
		var blockContent = $("#" + idBlock + "_content");
		if(blockContent.length > 0){
			$(this).css("backgroundColor","#FFC2AE");
			$(this).css("cursor","pointer");
		}
	}).mouseleave(function(){
		if(!($(this).attr("class").indexOf("selected") >= 0)){
			$(this).css("backgroundColor","#FFD7CA");
		}
	});
	
	$(".messages-topSucc").mouseenter(function(){
		var idBlock = $(this).attr("id");
		var blockContent = $("#" + idBlock + "_content");
		if(blockContent.length > 0){
			$(this).css("backgroundColor","#d1eba6");
			$(this).css("cursor","pointer");
		}
	}).mouseleave(function(){
		if(!($(this).attr("class").indexOf("selected") >= 0)){
			$(this).css("backgroundColor","#dff2bf");
		}
	});
	
	
	$(".messages-topError").click(function(){
		var idBlock = $(this).attr("id");
		var blockContent = $("#" + idBlock + "_content");
		if(blockContent.css("display") === "none"){
			blockContent.show();
			$(this).find(".messages-topError-arrow").addClass("arrow-up");
			$(this).addClass("selected");
		} else {
			blockContent.hide();
			$(this).find(".messages-topError-arrow").removeClass("arrow-up");
			$(this).removeClass("selected");
		}
	});
	
	$(".messages-topSucc").click(function(){
		var idBlock = $(this).attr("id");
		var blockContent = $("#" + idBlock + "_content");
		if(blockContent.css("display") === "none"){
			blockContent.show();
			$(this).find(".messages-topSucc-arrow").addClass("arrow-up");
			$(this).addClass("selected");
		} else {
			blockContent.hide();
			$(this).find(".messages-topSucc-arrow").removeClass("arrow-up");
			$(this).removeClass("selected");
		}
	});

	$(".messages-details-bold").click(function(){
		var idBlock = $(this).attr("id");
		if($(this).parent().find(".messages-details-content").css("display") === "none"){
			$(this).parent().addClass("selected-section");
			$("#"+idBlock+"_content").show();
			$("#"+idBlock+"_arrow").addClass("arrow-down");
		} else {
			$(this).parent().removeClass("selected-section");
			$("#"+idBlock+"_content").hide();
			$("#"+idBlock+"_arrow").removeClass("arrow-down");
		}
	});

	$(".errorStack").click(function(){
		var idBlock = $(this).attr("id");
		var text = "";
		if($("#"+idBlock+"_content").css("display") === "none"){
			text = $(this).text().replace("SHOW","HIDE");
			$("#"+idBlock+"_content").show();
		} else {
			$("#"+idBlock+"_content").hide();
			text = $(this).text().replace("HIDE","SHOW");
		}
		$(this).text(text);
	});
}

