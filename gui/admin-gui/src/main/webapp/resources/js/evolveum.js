/**
 * Copyright (c) 2011 Evolveum
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or CDDLv1.0.txt file in the source
 * code distribution. See the License for the specific language governing
 * permission and limitations under the License.
 * 
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * 
 * Portions Copyrighted 2011 Viliam Repan
 */

jQuery.noConflict();
window.onresize = function() {
	setToCenter();
};

function setToCenter() {
	var centerOfWindow = jQuery("#top-nav").width() / 2;
	var navBarHalfWidth = jQuery("#navBar").width() / 2;
	jQuery("#navBar").css("margin-left", centerOfWindow - navBarHalfWidth);
}

function displayMessageDetails(id, showImage, hideImage) {
	var buttonElement = document.getElementById(id + "ImageButton");
	var messageElement = document.getElementById(id);
	var value = messageElement.style.display;
	if (value == 'none' || value == '') {
		messageElement.style.display = 'block';
		buttonElement.src = hideImage;
	} else {
		messageElement.style.display = 'none';
		buttonElement.src = showImage;
	}
}

function displayMessageErrorDetails(id) {
	var blockElement = document.getElementById(id + "_block");
	var value = blockElement.style.display;
	if (value == 'none' || value == '') {
		blockElement.style.display = 'block';
	} else {
		blockElement.style.display = 'none';
	}
}

function displayMessageCauseDetails(id) {
	var buttonElement = document.getElementById(id);
	var blockElement = document.getElementById(id + "_block");
	var value = blockElement.style.display;
	if (value == 'none' || value == '') {
		blockElement.style.display = 'block';
		buttonElement.innerHTML = " [Less]";
	} else {
		blockElement.style.display = 'none';
		buttonElement.innerHTML = " [More]";
	}
}

/* //////////////////// jQuery ////////////////////////////// */
jQuery(document).ready(function() {
	
	jQuery("#blackWindow").css("opacity", "0.7");
	jQuery("#blackWindow").hide();
	jQuery("#preloader").hide();

	var y = jQuery(window).height() / 2;
	var x = jQuery(window).width() / 2;
	jQuery("#preloader").css("top", y - 50);
	jQuery("#preloader").css("left", x - 50);
	
	jQuery(".iceDatTblRow").mouseover(function(){
		jQuery(this).css("background-color","#CFDEE1");
	});
	
	jQuery(".iceDatTblRow").mouseout(function(){
		jQuery(this).css("background-color", "#FFFFFF");
	});

	jQuery("#logoutUserLinkSpan").css("color", "white");
	jQuery("#logoutUserLinkSpan").css("opacity", "0.5");

	jQuery("#helpSpan").css("color", "white");
	jQuery("#helpSpan").css("opacity", "0.5");

	jQuery("#logoutUserLinkSpan").mouseover(function() {
		jQuery(this).stop();
		jQuery(this).animate({
			opacity : 1
		}, 300);
	});

	jQuery("#logoutUserLinkSpan").mouseout(function() {
		jQuery(this).stop();
		jQuery(this).animate({
			opacity : 0.5
		}, 300);
	});

	jQuery("#helpSpan").mouseover(function() {
		jQuery(this).stop();
		jQuery(this).animate({
			opacity : 1
		}, 300);
	});

	jQuery("#helpSpan").mouseout(function() {
		jQuery(this).stop();
		jQuery(this).animate({
			opacity : 0.5
		}, 300);
	});

});

function waitScreen() {
	jQuery("#blackWindow").hide();
	jQuery("#preloader").hide();
}
