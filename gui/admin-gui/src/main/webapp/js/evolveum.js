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

var objectFormHelpContainer = null;
var interval = 0;

$(document).ready(function(){
	init();
	
/*	if (Wicket.Ajax) {
		Wicket.Ajax.registerPostCallHandler(MyApp.Ajax.firePostHandlers);
	}*/
});

function init() {
	//loadScript("js/less.js");
	setMenuPositionWhileScroll();
	
	$(".left-menu ul li").css("opacity", .8);
	$(".left-menu ul li a").css("opacity", .5);
	$(".left-menu ul").css("margin-top", - $(".left-menu ul").height() / 2);
	setTimeout("showLeftMenu()",500);
	$(".left-menu .selected-left").css("opacity", 1);
	$(".left-menu .selected-left").append("<img class='leftNavArrow' src='img/leftNavArrow.png' alt='' />");
	$(".left-menu .selected-left").parent().css("opacity", 1);
	$(".left-menu .selected-left").parent().css("background", "#333333");
	
	if ($.browser.msie && $.browser.version < 9.0){
		$(".acc .acc-section").css("height", "1px");
		$(".acc-content .sortedTable table").css("width", $(".acc-content").width());
	}
	
	$(".optionPanel").css("height",$(".optionRightBar").height());
	$(".optionLeftBar").css("height",$(".optionRightBar").height());
	
	//$(".sortedTable table thead").find(".sortable").find("a").find("div").append("<span class='sortableArrowIcon'></span>");
	
	$(".left-menu ul").mouseenter(function(){
		$(".left-menu ul").stop();
		$(".left-menu ul").animate({left: 0}, {duration: 500, easing: "easeOutQuart"});
	}).mouseleave(function(){
		$(".left-menu ul").stop();
		$(".left-menu ul").animate({left: -252}, {duration: 500, easing: "easeOutQuart"});
	});
	
	$(".left-menu ul li").mouseenter(function(){
		$(this).stop();
		$(this).find("a").stop();
		if($(this).attr("class") != "leftMenuTopClear" && $(this).attr("class") != "leftMenuBottomClear"){
			$(this).animate({opacity : 1}, 200);
			$(this).find("a").animate({opacity : 1}, 250);
		}
	}).mouseleave(function(){
		$(this).stop();
		$(this).find("a").stop();
		if($(this).find("a").attr("class") != "selected-left"){
			$(this).animate({opacity : .8}, 200);
			$(this).find("a").animate({opacity : .5}, 250);
		}
	});
	
	$(".objectFormAttribute").mouseenter(function(){
		objectFormHelpContainer = $(this).find(".objectFormHelpContainer");

		interval = setTimeout("showFormHelpContainer()",1000);
	}).mouseleave(function(){
		hideFormHelpContainer();
	});
	
	$(".submitTable tbody tr").mouseenter(function(){
		if($(this).find("input[type='checkbox']").is(":checked")){
			$(this).find("td").css("background", "#c6e9c6");
		} else {
			$(this).find("td").css("background", "#f2f2f2");
		}
	}).mouseleave(function(){
		if($(this).find("input[type='checkbox']").is(":checked")){
			$(this).find("td").css("background", "#d8f4d8");
			$(this).find("td").css("border-color","#FFFFFF");
		} else {
			$(this).find("td").css("background", "#FFFFFF");
			$(this).find("td").css("border-color","#F2F2F2");
		}
	});
	
	var el = $('.searchPanel');
    el.focus(function(e) {
        if (e.target.value == e.target.defaultValue)
            e.target.value = '';
    });
    el.blur(function(e) {
        if (e.target.value == '')
            e.target.value = e.target.defaultValue;
    });
    
}

function showLeftMenu() {
	if($(".left-menu ul").find(".leftMenuRow").html() != null){
		$(".left-menu ul").animate({left: -252}, {duration: 500, easing: "easeOutQuart"});
	}
}

function showFormHelpContainer(){
	objectFormHelpContainer.show();
	clearTimeout(interval);
}

function hideFormHelpContainer(){
	clearTimeout(interval);
	objectFormHelpContainer.hide();
}

function setMenuPositionWhileScroll() {
	if (($.browser.msie && $.browser.version >= 9.0) || (!$.browser.msie)) {
		$(window).scroll(function() {
			var scroll = $(window).scrollTop();
			if (scroll >= 60) {
				$(".top-menu").css("position", "fixed");
				$(".top-menu").css("top", "0px");
			} else {
				$(".top-menu").css("position", "absolute");
				$(".top-menu").css("top", "60px");
			}
		}); 
	}
	
	if ($.browser.msie && $.browser.version < 9.0){
		window.onresize = function() {
			$(".acc-content .sortedTable table").css("width", $(".acc-content").width());
		};
	}
}

function setFooterPos(){
	var winHeight = $(window).height();
	var contentHeight = $(".content").height() + 200;
	if(winHeight > contentHeight){
		$("#footer").css("position", "absolute");
		$("#footer").css("bottom", 0);
	} else {
		$("#footer").css("position", "relative");
		$("#footer").css("bottom", 0);
	}
}

function loadScript(url){
    var script = document.createElement("script");
    script.type = "text/javascript";

    if (script.readyState){  //IE
        script.onreadystatechange = function(){
            if (script.readyState == "loaded" ||
                    script.readyState == "complete"){
                script.onreadystatechange = null;
                //callback();
            }
        };
    } else {  //Others
        script.onload = function(){
            //callback();
        };
    }
    script.src = url;
    document.getElementsByTagName("head")[0].appendChild(script);
}

function unselectText(){
	var sel ;
	if(document.selection && document.selection.empty){
		document.selection.empty() ;
	} else if(window.getSelection) {
		sel=window.getSelection();
		if(sel && sel.removeAllRanges)
		sel.removeAllRanges() ;
	}
}

function scrollToTop() {
	$('body,html').animate({
		scrollTop: 0
	}, 800);
	return false;
}

function disablePaste(disable) {
	document.onkeydown = checkKeycode;
	function checkKeycode(e) {
		var keycode = null;
		if (window.event) keycode = window.event.keyCode;
		else if (e) keycode = e.which;
		if(keycode == 86){
			return !disable;
		}
	}
}
