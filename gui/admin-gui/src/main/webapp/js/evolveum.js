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

var objectFormHelpContainer = null;
var interval = 0;
var ajaxError = 0;

window.onload = initAjaxStatusSigns;
$(document).ready(function(){
	init();

/*	if (Wicket.Ajax) {
		Wicket.Ajax.registerPostCallHandler(MyApp.Ajax.firePostHandlers);
	}*/
});

function isIE9OrNewer() {
    //todo not working with jquery > 1.9 [lazyman]
//    return $.browser.msie && $.browser.version >= 9.0;
    return true;
}

function isIE() {
    //todo not working with jquery > 1.9 [lazyman]
//    return $.browser.msie;
    return false;
}

function init() {
	//loadScript("js/less.js");
	setMenuPositionWhileScroll();
	$(document).unbind("mousedown");
	$("#blackWindow").css("opacity", .8);
	$("#blackWindow").hide();
	$("#xmlExport").hide();
	
	$(".left-menu ul li").css("opacity", .8);
	$(".left-menu ul li a").css("opacity", .5);
	$(".left-menu ul").css("margin-top", - $(".left-menu ul").height() / 2);
	setTimeout("showLeftMenu()",500);
	$(".left-menu .selected-left").css("opacity", 1);
	$(".left-menu .selected-left").append("<img class='leftNavArrow' src='img/leftNavArrow.png' alt='' />");
	$(".left-menu .selected-left").parent().css("opacity", 1);
	$(".left-menu .selected-left").parent().css("background", "#333333");
	
	if (!isIE9OrNewer()){
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

    /*
    // not very good solution, css in javascript.
    // also what if there are more checkboxes in row.
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
	*/
	
	var el = $('.searchPanel');
    el.focus(function(e) {
        if (e.target.value == e.target.defaultValue)
            e.target.value = '';
    });
    el.blur(function(e) {
        if (e.target.value == '')
            e.target.value = e.target.defaultValue;
    });
    
    $(".searchText").keypress(function(e) {
        if(e.which == 13) {
        	$(this).parent().find(".submitSearch").click();
        }
    });
    
    $(document).bind("mousedown", function(e) {
		if ($("#xmlExport").has(e.target).length === 0) {
			$("#blackWindow").hide();
			$("#xmlExport").hide();
		}
	});
    
    $(".operatingFormButtons .button, .top-menu a").click(function(){
    	showDisableOperationFormButtons();
    });
    
    $(".pager a").click(function() {
    	showDisablePaging();
	});
}


function clickFuncWicket6(eventData) {
    var clickedElement = (window.event) ? event.srcElement : eventData.target;
    if ((clickedElement.tagName.toUpperCase() == 'BUTTON' || clickedElement.tagName.toUpperCase() == 'A' || clickedElement.parentNode.tagName.toUpperCase() == 'A'
        || (clickedElement.tagName.toUpperCase() == 'INPUT' && (clickedElement.type.toUpperCase() == 'BUTTON' || clickedElement.type.toUpperCase() == 'SUBMIT')))
        && clickedElement.parentNode.id.toUpperCase() != 'NOBUSY' ) {
        showBusysign();
    }
}

function initAjaxStatusSigns() {
    document.getElementsByTagName('body')[0].onclick = clickFuncWicket6;
    hideAjaxStatusSign();
    Wicket.Event.subscribe('/ajax/call/beforeSend', function( attributes, jqXHR, settings ) {
        showAjaxStatusSign('busy');
    });
    Wicket.Event.subscribe('/ajax/call/complete', function( attributes, jqXHR, textStatus) {
        hideAjaxStatusSign();
    });
}

function showAjaxStatusSign(sign) {
    hideAjaxStatusSign();

    if (sign == 'error') {
        document.getElementById('error_indicator').style.display = 'inline';
    } else if (sign == 'busy') {
        document.getElementById('bysy_indicator').style.display = 'inline';
    }
}

function hideAjaxStatusSign() {
    document.getElementById('bysy_indicator').style.display = 'none';
    document.getElementById('error_indicator').style.display = 'none';
}

function showLeftMenu() {
	if($(".left-menu ul").find(".leftMenuRow").html() != null){
		$(".left-menu ul").animate({left: -252}, {duration: 500, easing: "easeOutQuart"});
	}
}

function showFormHelpContainer(){
    //show if help is not empty
    if ($(objectFormHelpContainer).find('.objectFormHelpContent').text() == '') {
        return;
    }
	objectFormHelpContainer.show();
	clearTimeout(interval);
}

function hideFormHelpContainer(){
    //hide if help is not empty
    if ($(objectFormHelpContainer).find('.objectFormHelpContent').text() == '') {
        return;
    }
	clearTimeout(interval);
	objectFormHelpContainer.hide();
}

function setMenuPositionWhileScroll() {
	if (isIE9OrNewer() || !isIE()) {
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
	
	if (!isIE9OrNewer()){
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

function initXml(xml) {
	var output = "<small>*to hide the window, please click on the black background</small><h1>XML report</h1>" + xml;
	$("#xmlExportContent").html(output);
	$("#xmlExport").show();
	$("#blackWindow").show();
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

function setupFunc() {
	document.getElementsByTagName('body')[0].onclick = clickFunc;
	hideBusysign();

    Wicket.Event.subscribe('/ajax/call/before', function(jqEvent, attributes, jqXHR, errorThrown, textStatus) {
        // This dropClickEvent(attributes) is here because of selectable data table checkbox.
        // That checkbox trigger showBusysign but then event dropped in precondition and busy sign will not be hidden.
        if (!dropClickEvent(attributes)) {
            showBusysign();
        }
    });
    Wicket.Event.subscribe('/ajax/call/after', function(jqEvent, attributes, jqXHR, errorThrown, textStatus) {
        hideBusysign();
    });
    Wicket.Event.subscribe('/ajax/call/failure', function(jqEvent, attributes, jqXHR, errorThrown, textStatus) {
        showError();
    });
}

function hideBusysign() {
//	document.getElementById('bysy_indicator').style.display = 'none';
//	document.getElementById('error_indicator').style.display = 'none';
//	hideDisableOperationFormButtons();
//	hideDisablePaging();
	ajaxError = 0;
}

function showError() {
//	document.getElementById('bysy_indicator').style.display = 'none';
//	document.getElementById('error_indicator').style.display = 'inline';
//	showDisableOperationFormButtons();
	ajaxError = 1;
}

function showBusysign() {
//	if(ajaxError != 1) {
//		document.getElementById('bysy_indicator').style.display = 'inline';
//		document.getElementById('error_indicator').style.display = 'none';
//	}
}

var clickedElement = null;

function clickFunc(eventData) {
	clickedElement = (window.event) ? event.srcElement : eventData.target;
	if (clickedElement.tagName.toUpperCase() == 'BUTTON'
			|| clickedElement.tagName.toUpperCase() == 'A'
			|| clickedElement.parentNode.tagName.toUpperCase() == 'A'
			|| (clickedElement.tagName.toUpperCase() == 'INPUT' && (clickedElement.type
					.toUpperCase() == 'BUTTON' || clickedElement.type
					.toUpperCase() == 'SUBMIT'))) {
		showBusysign();
	}
}

if(clickedElement != null) {
	if ((clickedElement.tagName.toUpperCase() == 'A'
		&& ((clickedElement.target == null) || (clickedElement.target.length <= 0))
		&& (clickedElement.href.lastIndexOf('#') != (clickedElement.href.length - 1))
		&& (!('nobusy' in clickedElement))
		&& (clickedElement.href.indexOf('skype') < 0)
		&& (clickedElement.href.indexOf('mailto') < 0)
		&& (clickedElement.href.indexOf('WicketAjaxDebug') < 0)
		&& (clickedElement.href.lastIndexOf('.doc') != (clickedElement.href.length - 4))
		&& (clickedElement.href.lastIndexOf('.csv') != (clickedElement.href.length - 4))
		&& (clickedElement.href.lastIndexOf('.xls') != (clickedElement.href.length - 4)) && ((clickedElement.onclick == null) || (clickedElement.onclick
		.toString().indexOf('window.open') <= 0)))
		|| (clickedElement.parentNode.tagName.toUpperCase() == 'A'
				&& ((clickedElement.parentNode.target == null) || (clickedElement.parentNode.target.length <= 0))
				&& (clickedElement.parentNode.href.indexOf('skype') < 0)
				&& (clickedElement.parentNode.href.indexOf('mailto') < 0)
				&& (clickedElement.parentNode.href.lastIndexOf('#') != (clickedElement.parentNode.href.length - 1))
				&& (clickedElement.parentNode.href.lastIndexOf('.doc') != (clickedElement.parentNode.href.length - 4))
				&& (clickedElement.parentNode.href.lastIndexOf('.csv') != (clickedElement.parentNode.href.length - 4))
				&& (clickedElement.parentNode.href.lastIndexOf('.xls') != (clickedElement.parentNode.href.length - 4)) && ((clickedElement.parentNode.onclick == null) || (clickedElement.parentNode.onclick
				.toString().indexOf('window.open') <= 0)))
		|| (((clickedElement.onclick == null) || ((clickedElement.onclick
				.toString().indexOf('confirm') <= 0)
				&& (clickedElement.onclick.toString().indexOf('alert') <= 0) && (clickedElement.onclick
				.toString().indexOf('Wicket.Palette') <= 0))) && (clickedElement.tagName
				.toUpperCase() == 'INPUT' && (clickedElement.type.toUpperCase() == 'BUTTON'
				|| clickedElement.type.toUpperCase() == 'SUBMIT' || clickedElement.type
				.toUpperCase() == 'IMAGE')))) {
	showBusysign();
	}
}


function showDisableOperationFormButtons() {
	var operationFormBlock = $(".operatingFormButtons");
	var disablePanel = '<div class="disableOperationBlock" style="height: 100%; width: 100%; position: absolute; z-index: 4;"></div>';
	operationFormBlock.append(disablePanel);
	if(operationFormBlock.find(".operatingFormBlock").size() == 0) {
		$(".disableOperationBlock").insertBefore($(".operatingFormButtons").find(".button:first"));
	}
	//$(".operatingFormButtons").find(".button").css("opacity", .5);
	$(".operatingFormButtons").css("opacity", .5);
}

function hideDisableOperationFormButtons() {
	var disableOperationBlock = $(".disableOperationBlock");
	$(".operatingFormButtons").css("opacity", 1);
	//$(".operatingFormButtons").find(".button").css("opacity", 1);
	disableOperationBlock.remove();
}

function showDisablePaging() {
	$(".disablePaging").show();
	$(".pager").css("opacity", .5);
}

function hideDisablePaging() {
	$(".disablePaging").hide();
	$(".pager").css("opacity", 1);
}

/**
 * Method provides checks for full row click support - it decides which events should be dropped - we
 * want to stop event bubbling when user clicks for example on input event (then row shouldn't change
 * it's row selected status)
 *
 * @param attrs
 * @return {boolean} true if event should be dropped
 */
function dropClickEvent(attrs) {
    var evt = attrs.event;

    //if clicked on <tr>
    if (evt.target == evt.currentTarget) {
        return false;
    }
    //if clicked on <td> which is a child of <tr>
    if (evt.target.parentNode == evt.currentTarget) {
        return false;
    }

    //we drop event if it input or link
    var targetElement = evt.target.nodeName.toLowerCase();
    var isInput = (targetElement == 'input' || targetElement == 'select' || targetElement == 'option');
    var isLink = (targetElement == 'a');
    if (isInput || isLink) {
        return true;
    }

    return false;
}