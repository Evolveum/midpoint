/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

$(window).load(function() {
    //dom not only ready, but everything is loaded MID-3668
    $("body").removeClass("custom-hold-transition");

    initAjaxStatusSigns();
});

function clickFuncWicket6(eventData) {
    var clickedElement = (window.event) ? event.srcElement : eventData.target;
    if ((clickedElement.tagName.toUpperCase() == 'BUTTON' 
        || clickedElement.tagName.toUpperCase() == 'A' 
        || clickedElement.parentNode.tagName.toUpperCase() == 'A'
        || (clickedElement.tagName.toUpperCase() == 'INPUT' 
        && (clickedElement.type.toUpperCase() == 'BUTTON' 
        || clickedElement.type.toUpperCase() == 'SUBMIT')))
        && clickedElement.parentNode.id.toUpperCase() != 'NOBUSY' ) {
        showAjaxStatusSign();
    }
}

function initAjaxStatusSigns() {
    document.getElementsByTagName('body')[0].onclick = clickFuncWicket6;
    hideAjaxStatusSign();
    Wicket.Event.subscribe('/ajax/call/beforeSend', function( attributes, jqXHR, settings ) {
        showAjaxStatusSign();
    });
    Wicket.Event.subscribe('/ajax/call/complete', function( attributes, jqXHR, textStatus) {
        hideAjaxStatusSign();
    });
}

function showAjaxStatusSign() {
    document.getElementById('ajax_busy').style.display = 'inline';
}

function hideAjaxStatusSign() {
    document.getElementById('ajax_busy').style.display = 'none';
}

/**
 * InlineMenu initialization function
 */
function initInlineMenu(menuId, hideByDefault) {
    var cog = $('#' + menuId).find('ul.cog');
    var menu = cog.children().find('ul.dropdown-menu');

    var parent = cog.parent().parent();     //this is inline menu div
    if (!hideByDefault && !isCogInTable(parent)) {
        return;
    }

    if (isCogInTable(parent)) {
        //we're in table, we now look for <tr> element
        parent = parent.parent('tr');
    }

    // we only want to hide inline menus that are in table <td> element,
    // inline menu in header must be visible all the time, or every menu
    // that has hideByDefault flag turned on
    cog.hide();

    parent.hover(function () {
        //over
        cog.show();
    }, function () {
        //out
        if (!menu.is(':visible')) {
            cog.hide();
        }
    });
}

function isCogInTable(inlineMenuDiv) {
    return inlineMenuDiv.hasClass('cog') && inlineMenuDiv[0].tagName.toLowerCase() == 'td';
}

function updateHeight(elementId, add, substract) {
    updateHeightReal(elementId, add, substract);
    $(window).resize(function() {
        updateHeightReal(elementId, add, substract);
    });
}

function updateHeightReal(elementId, add, substract) {
    $('#' + elementId).css("height","0px");

    var documentHeight = $(document).innerHeight();
    var elementHeight = $('#' + elementId).outerHeight(true);
    var mainContainerHeight = $('section.content-header').outerHeight(true)
        + $('section.content').outerHeight(true) + $('footer.main-footer').outerHeight(true)
        + $('header.main-header').outerHeight(true);

    console.log("Document height: " + documentHeight + ", mainContainer: " + mainContainerHeight);

    var height = documentHeight - mainContainerHeight - elementHeight - 1;
    console.log("Height clean: " + height);

    if (substract instanceof Array) {
        for (var i = 0; i < substract.length; i++) {
            console.log("Substract height: " + $(substract[i]).outerHeight(true));
            height -= $(substract[i]).outerHeight(true);
        }
    }
    if (add instanceof Array) {
        for (var i = 0; i < add.length; i++) {
            console.log("Add height: " + $(add[i]).outerHeight(true));
            height += $(add[i]).outerHeight(true);
        }
    }
    console.log("New css height: " + height);
    $('#' + elementId).css("height", height + "px");
}

/**
 * Used in TableConfigurationPanel (table page size)
 *
 * @param buttonId
 * @param popoverId
 * @param positionId
 */
function initPageSizePopover(buttonId, popoverId, positionId) {
    console.log("initPageSizePopover('" + buttonId + "','" + popoverId + "','" + positionId +"')");

    var button = $('#' + buttonId);
    button.click(function () {
        var popover = $('#' + popoverId);

        var positionElement = $('#' + positionId);
        var position = positionElement.position();

        var top = position.top + parseInt(positionElement.css('marginTop'));
        var left = position.left + parseInt(positionElement.css('marginLeft'));
        var realPosition = {top: top, left: left};

        var left = realPosition.left - popover.outerWidth();
        var top = realPosition.top + button.outerHeight() / 2 - popover.outerHeight() / 2;

        popover.css("top", top);
        popover.css("left", left);

        popover.toggle();
    });
}

/**
 * Used in SearchPanel for advanced search, if we want to store resized textarea dimensions.
 * 
 * @param textAreaId
 */
function storeTextAreaSize(textAreaId) {
    console.log("storeTextAreaSize('" + textAreaId + "')");

    var area = $('#' + textAreaId);
    $.textAreaSize = [];
    $.textAreaSize[textAreaId] = {
        height: area.height(), 
        width: area.width(),
        position: area.prop('selectionStart')
    }
}

/**
 * Used in SearchPanel for advanced search, if we want to store resized textarea dimensions.
 * 
 * @param textAreaId
 */
function restoreTextAreaSize(textAreaId) {
    console.log("restoreTextAreaSize('" + textAreaId + "')");

    var area = $('#' + textAreaId);

    var value = $.textAreaSize[textAreaId];

    area.height(value.height);
    area.width(value.width);
    area.prop('selectionStart', value.position);

    // resize also error message span
    var areaPadding = 70;
    area.siblings('.help-block').width(value.width + areaPadding);
}

/**
 * Used in SearchPanel class
 *
 * @param buttonId
 * @param popoverId
 * @param paddingRight value which will shift popover to the left from center bottom position against button
 */
function toggleSearchPopover(buttonId, popoverId, paddingRight) {
    console.log("Called toggleSearchPopover with buttonId=" + buttonId + ",popoverId="
        + popoverId + ",paddingRight=" + paddingRight);

    var button = $('#' + buttonId);
    var popover = $('#' + popoverId);

    var popovers = button.parents('.search-form').find('.popover:visible').each(function () {
        var id = $(this).attr('id');
        console.log("Found popover with id=" + id);

        if (id != popoverId) {
            $(this).hide(200);
        }
    });

    var position = button.position();

    var left = position.left - (popover.outerWidth() - button.outerWidth()) / 2 - paddingRight;
    var top = position.top + button.outerHeight();

    popover.css('top', top);
    popover.css('left', left);

    popover.toggle(200);

    //this will set focus to first form field on search item popup
    popover.find('input[type=text],textarea,select').filter(':visible:first').focus();

    //this will catch ESC or ENTER and fake close or update button click
    popover.find('input[type=text],textarea,select').off('keyup.search').on('keyup.search', function(e) {
        if (e.keyCode == 27) {
            popover.find('[data-type="close"]').click();
        } else if (e.keyCode == 13) {
            popover.find('[data-type="update"]').click();
        }
    });
}

/**
 * used in DropDownMultiChoice.java
 * 
 * @param compId
 * @param options
 */
function initDropdown(compId, options) {
    $('#' + compId).multiselect(options);
}