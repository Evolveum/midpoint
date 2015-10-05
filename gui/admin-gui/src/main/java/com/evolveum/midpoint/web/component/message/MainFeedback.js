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

var offside;
window.onresize = setWidthForTempMessage;
function initMessages() {
	offside = $(".feedbackContainer").height() + 135;
	if($(window).scrollTop() < offside){
		$(".tempMessage").css("opacity", 0);
		$(".tempMessage").hide();
	}

	$("a").click(function(){
		$(".messagesTop").remove();
		$(".tempMessagesTop").remove();
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
		}  else if(className.indexOf("messages-topExp") >= 0){
			arrow = "messages-topExp";
		} else {
			arrow = "messages-topInfo";
		}
		
		var blockContent = $("#" + $(this).attr('id') + "_content");
		if(blockContent.length > 0){
			if(isFilled(blockContent)){
				$(this).append("<span class='"+ arrow +"-arrow arrow-down'></span>");
				var exceptionHeight = $(this).find(".messages-topException").height();
				var arrowPosition = $(this).height() / 2 - exceptionHeight + 3;
				//alert($(this).height());
				//var arrowPosition = $(this).height() / 2 + 4;
				$("." + arrow +"-arrow").css("marginTop", - arrowPosition);
			}
		}
	});
	
	setTempMessagePositionWhileScroll();
	
    addPointerOnMouseEnter(".messagePanel .messages-topError");
    addPointerOnMouseEnter(".messagePanel .messages-topSucc");
    addPointerOnMouseEnter(".messagePanel .messages-topExp");
    addPointerOnMouseEnter(".messagePanel .messages-topWarn");
    addPointerOnMouseEnter(".messagePanel .messages-topInfo");

    fixArrowsOnClick(".messagePanel .messages-topError", ".messages-topError-arrow");
    fixArrowsOnClick(".messagePanel .messages-topSucc", ".messages-topSucc-arrow");
    fixArrowsOnClick(".messagePanel .messages-topExp", ".messages-topExp-arrow");
    fixArrowsOnClick(".messagePanel .messages-topWarn", ".messages-topWarn-arrow");
    fixArrowsOnClick(".messagePanel .messages-topInfo", ".messages-topInfo-arrow");

    $(".messagePanel .messages-details-bold").click(function () {
        var idBlock = $(this).attr("id");
        if ($(this).parent().find(".messages-details-content").css("display") === "none") {
            $(this).parent().parent().addClass("selected-section");
            $("#" + idBlock + "_content").show();
            $("#" + idBlock + "_arrow").addClass("arrow-down");
        } else {
            $(this).parent().parent().removeClass("selected-section");
            $("#" + idBlock + "_content").hide();
            $("#" + idBlock + "_arrow").removeClass("arrow-down");
        }
    });

    $(".messagePanel .errorStack").click(function () {
        var idBlock = $(this).attr("id");
        if ($("#" + idBlock + "_content").css("display") === "none") {
            $(this).find(".showStackText").hide();
            $(this).find(".hideStackText").show();
            $("#" + idBlock + "_content").show();
        } else {
            $(this).find(".showStackText").show();
            $(this).find(".hideStackText").hide();
            $("#" + idBlock + "_content").hide();
        }
    });
	
	$(".tempMessage .messages-succ").css("opacity", .95);
	$(".tempMessage .messages-error").css("opacity", .95);
	$(".tempMessage .messages-warn").css("opacity", .95);
	$(".tempMessage .messages-info").css("opacity", .95);
	$(".tempMessage .messages-exp").css("opacity", .95);
	
	setWidthForTempMessage();
	
	$("ul.messages-topException").find("li:first").css("marginTop", "5px");
	
	$(".collapseAll").click(function(){
		collapseAll();
	});
	
	$(".expandAll").click(function(){
		expandAll();
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

function setTempMessagePositionWhileScroll() {
	var scroll;
	var isSelected;
	$(window).scroll(function() {
		isSelected = false;
		$(".messagePanel").find(".messages-topPanel").each(function(index){
			if($(this).hasClass("selected")){
				isSelected = true;
			}
		});
		offside = $(".feedbackContainer").height() + 135;
		scroll = $(window).scrollTop();
		if (scroll >= offside) {
			if(!isSelected) {
				if($(".tempMessage").css("display") == "none") {
					$(".tempMessage").stop();
					$(".tempMessage").css("opacity", 0);
					$(".tempMessage").show();
					$(".tempMessage").animate({opacity : 1}, 400);
				}
			} else {
				$(".tempMessage").hide();
			}
		} else {
			$(".tempMessage").stop();
			$(".tempMessage").animate({opacity : 0}, 400, function(){
				$(".tempMessage").hide();
			});
		}
	});
}

function scrollToReadMessage() {
	$('body,html').animate({
		scrollTop: 60
	}, 600);
	$(".tempMessage").animate({opacity : 0}, 400);
	return false;
}

function setWidthForTempMessage() {
	$(".tempMessage").width($(".messagePanel").width());
}

function expandAll(){
	$("ul.messages-details div").each(function(index){
	    if($(this).attr('id') != undefined){
	        var divId = $(this).attr('id');
	    	var selectedComponent;
	    	if(divId.indexOf("_content") >= 0){
	    		selectedComponent = $("#" + divId.replace("_content",""));
	    		if($("#" + divId).css("display","none") && !$("#" + divId).hasClass("errorStack-content")){
	    			selectedComponent.click();
	    		}
	    	}
	    } 
	});
}

function collapseAll(){
	$("ul.messages-details div").each(function(index){
	    if($(this).attr('id') != undefined){
	        var divId = $(this).attr('id');
	    	var selectedComponent;
	    	if(divId.indexOf("_content") >= 0){
	    		selectedComponent = $("#" + divId.replace("_content",""));
	    		if($("#" + divId).css("display","block") && !$("#" + divId).hasClass("errorStack-content")){
	    			selectedComponent.click();
	    		}
	    	}
	    } 
	});
}

function isFilled(blockContent){
	var topBlock = blockContent.find("ul").first().find("li").children().size() > 1;
	var content = blockContent.find(".messages-details").children().size() > 0;
	return topBlock || content;
}


function addPointerOnMouseEnter(selector) {
    $(selector).mouseenter(function () {
        var idBlock = $(this).attr("id");
        var blockContent = $("#" + idBlock + "_content");
        if (blockContent.length > 0) {
            if (isFilled(blockContent)) {
                $(this).css("cursor", "pointer");
            }
        }
    });
}

function fixArrowsOnClick(selector, arrowSelector) {
    $(selector).click(function () {
        var idBlock = $(this).attr("id");
        var blockContent = $("#" + idBlock + "_content");
        if (isFilled(blockContent)) {
            if (blockContent.css("display") === "none") {
                blockContent.show();
                $(this).find(arrowSelector).addClass("arrow-up");
                $(this).addClass("selected");
            } else {
                blockContent.hide();
                $(this).find(arrowSelector).removeClass("arrow-up");
                $(this).removeClass("selected");
            }
        }
    });
}