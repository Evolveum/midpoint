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
	//$(".messages-details-bold").disableTextSelection();
	//$(".messages-topPanel").disableTextSelection();
	
	$("a").click(function(){
		$(".messagesTop").remove();
	});
	
	$(".messagesTop").find(".messages-topPanel").each(function(index){
		var arrow;
		var className = $(this).attr('class');
		if(className.indexOf("messages-topError") >= 0){
			arrow = "messages-topError";
		} else if(className.indexOf("messages-topSucc") >= 0){
			arrow = "messages-topSucc";
		} else if(className.indexOf("messages-topWarn") >= 0){
			arrow = "messages-topWarn";
		} else {
			arrow = "messages-topInfo";
		}
		
		var blockContent = $("#" + $(this).attr('id') + "_content");
		if(blockContent.length > 0){
			if(blockContent.find("ul:first").children().size() > 0){
				$(this).append("<span class='"+ arrow +"-arrow arrow-down'></span>");
				if($(this).height() <= 16){
					$("." + arrow +"-arrow").css("marginTop", 0);
				}
			}
		}
	});
	
	//$(document).find("messages-topError");
	$(".messages-topError").mouseenter(function(){
		var idBlock = $(this).attr("id");
		var blockContent = $("#" + idBlock + "_content");
		if(blockContent.length > 0){
			if(blockContent.find("ul:first").children().size() > 0){
				$(this).css("backgroundColor","#FFC2AE");
				$(this).css("cursor","pointer");
			}
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
			if(blockContent.find("ul:first").children().size() > 0){
				$(this).css("backgroundColor","#d1eba6");
				$(this).css("cursor","pointer");
			}
		}
	}).mouseleave(function(){
		if(!($(this).attr("class").indexOf("selected") >= 0)){
			$(this).css("backgroundColor","#dff2bf");
		}
	});
	
	$(".messages-topWarn").mouseenter(function(){
		var idBlock = $(this).attr("id");
		var blockContent = $("#" + idBlock + "_content");
		if(blockContent.length > 0){
			if(blockContent.find("ul:first").children().size() > 0){
				$(this).css("backgroundColor","#fce48d");
				$(this).css("cursor","pointer");
			}
		}
	}).mouseleave(function(){
		if(!($(this).attr("class").indexOf("selected") >= 0)){
			$(this).css("backgroundColor","#FEEFB3");
		}
	});
	
	$(".messages-topInfo").mouseenter(function(){
		var idBlock = $(this).attr("id");
		var blockContent = $("#" + idBlock + "_content");
		if(blockContent.length > 0){
			if(blockContent.find("ul:first").children().size() > 0){
				$(this).css("backgroundColor","#c3bfff");
				$(this).css("cursor","pointer");
			}
		}
	}).mouseleave(function(){
		if(!($(this).attr("class").indexOf("selected") >= 0)){
			$(this).css("backgroundColor","#d2d1ff");
		}
	});
	
	
	$(".messages-topError").click(function(){
		var idBlock = $(this).attr("id");
		var blockContent = $("#" + idBlock + "_content");
		if(blockContent.find("ul:first").children().size() > 0){
			if(blockContent.css("display") === "none"){
				blockContent.show();
				$(this).find(".messages-topError-arrow").addClass("arrow-up");
				$(this).addClass("selected");
			} else {
				blockContent.hide();
				$(this).find(".messages-topError-arrow").removeClass("arrow-up");
				$(this).removeClass("selected");
			}
		}
	});
	
	$(".messages-topSucc").click(function(){
		var idBlock = $(this).attr("id");
		var blockContent = $("#" + idBlock + "_content");
		if(blockContent.find("ul:first").children().size() > 0){
			if(blockContent.css("display") === "none"){
				blockContent.show();
				$(this).find(".messages-topSucc-arrow").addClass("arrow-up");
				$(this).addClass("selected");
			} else {
				blockContent.hide();
				$(this).find(".messages-topSucc-arrow").removeClass("arrow-up");
				$(this).removeClass("selected");
			}
		}
	});
	
	$(".messages-topWarn").click(function(){
		var idBlock = $(this).attr("id");
		var blockContent = $("#" + idBlock + "_content");
		if(blockContent.find("ul:first").children().size() > 0){
			if(blockContent.css("display") === "none"){
				blockContent.show();
				$(this).find(".messages-topWarn-arrow").addClass("arrow-up");
				$(this).addClass("selected");
			} else {
				blockContent.hide();
				$(this).find(".messages-topWarn-arrow").removeClass("arrow-up");
				$(this).removeClass("selected");
			}
		}
	});
	
	$(".messages-topInfo").click(function(){
		var idBlock = $(this).attr("id");
		var blockContent = $("#" + idBlock + "_content");
		if(blockContent.find("ul:first").children().size() > 0){
			if(blockContent.css("display") === "none"){
				blockContent.show();
				$(this).find(".messages-topInfo-arrow").addClass("arrow-up");
				$(this).addClass("selected");
			} else {
				blockContent.hide();
				$(this).find(".messages-topInfo-arrow").removeClass("arrow-up");
				$(this).removeClass("selected");
			}
		}
	});

	$(".messages-details-bold").click(function(){
		var idBlock = $(this).attr("id");
		if($(this).parent().find(".messages-details-content").css("display") === "none"){
			$(this).parent().parent().addClass("selected-section");
			$("#"+idBlock+"_content").show();
			$("#"+idBlock+"_arrow").addClass("arrow-down");
		} else {
			$(this).parent().parent().removeClass("selected-section");
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

jQuery.fn.disableTextSelection = function(){
    return this.each(function(){
        if (typeof this.onselectstart != "undefined") { // IE
            this.onselectstart = function() { return false; };
        }
        else if (typeof this.style.MozUserSelect != "undefined") { // Firefox
            this.style.MozUserSelect = "none";
        }
        else { // All others
            this.onmousedown = function() { return false; };
        }
    });
};

