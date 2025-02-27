/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

import Sparkline from "sparklines";
import { TempusDominus } from '@eonasdan/tempus-dominus';
import { DateTime } from '@eonasdan/tempus-dominus/dist/js/tempus-dominus.js';

export default class MidPointTheme {

    constructor() {
        const self = this;

        $(window).on('load', function () {
            //dom not only ready, but everything is loaded MID-3668
            $("body").removeClass("custom-hold-transition");

            self.initAjaxStatusSigns();

            Wicket.Event.subscribe('/ajax/call/failure', function (attrs, jqXHR, textStatus, jqEvent, errorThrown) {
                console.error("Ajax call failure:\n" + JSON.stringify(attrs.target.location)
                    + "\nStatus:\n" + JSON.stringify(textStatus));
            });

            self.fixContentHeight();
            $(window, ".wrapper").resize(function () {
                self.fixContentHeight();
            });
        });
        // expand/collapse for sidebarMenuPanel
        jQuery(function ($) {
            $('.nav-sidebar li.nav-header').on("click", function (e) {
                if ($(this).hasClass('closed')) {
                    // expand the panel
                    $(this).nextUntil('.nav-header').slideDown();
                    $(this).removeClass('closed');
                    $(this).attr("aria-expanded", "true")
                } else {
                    // collapse the panel
                    $(this).nextUntil('.nav-header').slideUp();
                    $(this).addClass('closed');
                    $(this).attr("aria-expanded", "false")
                }
            });
        });

        jQuery(function ($) {
                    $('.nav-sidebar li.nav-item[aria-haspopup="true"]').on("click", function (e) {
                        if ($(this).hasClass('menu-open')) {
                            $(this).attr("aria-expanded", "false");
                        } else {
                            $(this).attr("aria-expanded", "true");
                        }
                    });
                });

        !function ($) {
            $.fn.passwordFieldValidatorPopover = function (inputId, popover) {
                return this.each(function () {

                    var parent = $(this).parent();

                    var showPopover = function () {
                        parent.find(inputId).each(function () {
                            var itemH = $(this).innerHeight() + 27;
                            parent.find(popover).fadeIn(300).css({top: itemH, left: 0}).css("display", "block");
                        });
                    }

                    showPopover();
                    $(this).on("focus", function () {
                        showPopover();
                    });

                    var deletePopover = function () {
                        parent.find(popover).fadeIn(300).css("display", "none");
                    };

                    $(this).on("blur", function () {
                        deletePopover();
                    });
                });
            };

            $.fn.passwordValidatorPopover = function (inputId, popover) {
                return this.each(function () {

                    var parent = $(this).parent();

                    var showPopover = function () {
                        if (parent.find(inputId + ":hover").length != 0) {
                            parent.find(inputId).each(function () {
                                var itemH = $(this).innerHeight() + 9;
                                parent.find(popover).fadeIn(300).css({top: itemH, left: 0}).css("display", "block");
                            });
                        }
                    }

                    $(this).on("mouseenter", function () {
                        showPopover();
                    });

                    var deletePopover = function () {
                        parent.find(popover).fadeIn(300).css("display", "none");
                    };

                    $(this).on("mouseleave", function () {
                        if (parent.find(popover + ":hover").length == 0) {
                            deletePopover();
                        }
                    });
                    parent.find(popover).on("mouseleave", function () {
                        if (parent.find(inputId + ":hover").length == 0) {
                            deletePopover();
                        }
                    });
                });
            };
        }(window.jQuery);

        (function ($) {
            $.fn.updateParentClass = function (successClass, parentSuccessClass, parentId, failClass, parentFailClass) {
                var child = this;
                var parent = $("#" + parentId);

                if (child.hasClass(successClass)) {
                    if (parent.hasClass(parentFailClass)) {
                        parent.removeClass(parentFailClass);
                    }
                    if (!parent.hasClass(parentSuccessClass)) {
                        parent.addClass(parentSuccessClass);
                    }
                } else if (child.hasClass(failClass)) {
                    if (parent.hasClass(parentSuccessClass)) {
                        parent.removeClass(parentSuccessClass);
                    }
                    if (!parent.hasClass(parentFailClass)) {
                        parent.addClass(parentFailClass);
                    }
                }
            }
        })(jQuery);

        (function ($) {
            $.fn.showTooltip = function () {
                if (typeof $(this).tooltip === "function") {
                    var wl = $.fn.tooltip.Constructor.Default.whiteList;
                    wl['xsd:documentation'] = [];
                    var parent = $(this).closest('.modal-dialog-content');
                    var container = "body";
                    if (parent.length != 0) {
                        container = '#' + parent.attr('id');
                    }
                    $(this).tooltip({html: true, whiteList: wl, 'container': container});
                    $(this).tooltip("show");
                };
            }
        })(jQuery);

        jQuery(function ($) {
            $(document).on("mouseenter", "*[data-toggle='tooltip']", function (e) {
                $(this).showTooltip();
            });
            $(document).on("focus", "*[data-toggle='tooltip']", function (e) {
                $(this).showTooltip();
            });
        });

        jQuery(function ($) {
            $(document).on("click", ".compositedButton[data-toggle='tooltip']", function (e, t) {
                var parent = $(this).closest('.modal-dialog-content');
                if (parent.length != 0) {
                    $(this).tooltip("hide");
                }
            });
        });

        jQuery(function ($) {
            $(document).on("click", ".showPasswordButton", function (e, t) {
                $(this).showPassword();
            });
        });

        jQuery(function ($) {
            $(document).on("keydown", ".showPasswordButton", function (e, t) {
                if (e.key == " " || e.code == "Space" || e.keyCode == 32 ||
                        e.key == "Enter" || e.keyCode == 13) {
                    $(this).showPassword();
                  }
            });
        });

        (function ($) {
            $.fn.showPassword = function () {
                var parent = $(this).closest(".password-parent");
                var input = parent.find("input");

                if (input.attr('type') === "password") {
                    input.attr('type', 'text');
                    $(this).addClass("fa-eye-slash");
                    $(this).removeClass("fa-eye");
                } else {
                    input.attr('type', 'password');
                    $(this).removeClass("fa-eye-slash");
                    $(this).addClass("fa-eye");
                }
            }
        })(jQuery);

        jQuery(function ($) {
            $(document).on("keydown", ".clickable-by-enter", function (e, t) {
                if (e.key == " " || e.code == "Space" || e.keyCode == 32 || e.key == "Enter" || e.keyCode == 13) {
                    $(this).click();
                }
            });
        });

        jQuery(function ($) {
            var sideBar = $(".nav-sidebar");
            self.keydownForMenuItems(sideBar, self);

            var detailsMenu = $(".details-panel-navigation");
            self.keydownForMenuItems(detailsMenu, self);
        });
    }

    keydownForMenuItems(sideBar, self) {
        if (!sideBar.length) {
            return;
        }

        sideBar.on("keydown", "li[role='menuitem']", function (e, t) {
            if ($(this).get(0) !== document.activeElement) {
                return;
            }

            if (e.key == " " || e.code == "Space" || e.keyCode == 32 || e.key == "Enter" || e.keyCode == 13) {
                var link = $(this).find("a");
                if (link.length) {
                    self.clickOnMenuItem(link, $(this), false, e);
                } else {
                    $(this).click();
                }
                e.preventDefault();
                return;
            }

            if (e.key == "Arrow Right" || e.code == "ArrowRight" || e.keyCode == 39) {
                var link = $(this).find("a");
                if (link.length) {
                    self.clickOnMenuItem(link, $(this), true, e);
                }
                e.preventDefault()
                return;
            }

            if (e.key == "Arrow Left" || e.code == "ArrowLeft" || e.keyCode == 37 || e.key == "ESC" || e.keyCode == 27) {
                var parent = $(this).parent().closest("li[role='menuitem']");
                if (parent.length) {
                    var link = parent.find("a");
                    link.get(0).click();
                    parent.get(0).focus();
                    parent.get(0).scrollIntoView({ block: "center" });
                }
                e.preventDefault()
                return;
            }

            var parent = $(this).closest("ul[role='menu']");
            var list = parent.children("li[role='menuitem']");
            var focusIndex = null;
            if (e.key == "Arrow Up" || e.code == "ArrowUp" || e.keyCode == 38) {
                focusIndex = list.index($(this)) - 1;
            }

            if (e.key == "Arrow Down" || e.code == "ArrowDown" || e.keyCode == 40) {
                focusIndex = list.index($(this)) + 1;
            }

            if (focusIndex < 0 || e.key == "End" || e.keyCode == 35) {
                focusIndex = list.length - 1;
            }

            if (focusIndex >= list.length || e.key == "Home" || e.keyCode == 36) {
                focusIndex = 0;
            }

            if (focusIndex == null) {
                return;
            }

            var focusItem = list.get(focusIndex);
            focusItem.focus();
            focusItem.scrollIntoView({ block: "center" });
            e.preventDefault()
        });
    }

    clickOnMenuItem(link, menuItem, onlySubmenu, e) {
        if (!onlySubmenu) {
            link.get(0).click();
        }
        var hasPopup = menuItem.attr("aria-haspopup");
        if (hasPopup == "true") {
            if (onlySubmenu) {
                link.get(0).click();
            }
            var subitems = menuItem.find("li[role='menuitem']");
            if (subitems.length) {
                subitems.get(0).focus();
                subitems.get(0).scrollIntoView({ block: "center" });
                e.preventDefault()
            }
        }
    }

    initSelect2MultiChoice(containerHtmlElement) {
        var container = $("#" + containerHtmlElement.id)
        if (container.length) {
            var select = container.find("select");
            if (select.length){
                var attribute = select.attr("aria-label")

                var combobox = container.find("span[role='combobox']");
                var selectContainer = container.find(".select2-container");

                if (attribute != null) {
                    var input = container.find("input");

                    if (!input.length) {
                        if (selectContainer.length) {
                            selectContainer.on("click", function (e, t) {
                                var input = $("input.select2-search__field[aria-controls='select2-" + select.attr("id") + "-results']");
                                if (input.length) {
                                    input.attr("aria-label", attribute);
                                }
                            });
                        }
                    } else {
                        input.attr("aria-label", attribute);
                    }

                    if (combobox.length) {
                        combobox.attr("aria-label", attribute);
                    }

                    var textbox = container.find("span[role='textbox']");
                    if (textbox.length) {
                        textbox.attr("aria-label", attribute);
                    }
                }

                if (combobox.length) {
                    if (selectContainer.length) {
                        var selectContainerId = "select2-" + select.attr("id") +"-container-custom";
                        selectContainer.attr("id", selectContainerId)
                        combobox.attr("aria-controls", selectContainerId);
                    }

                    combobox.on("keydown", function (e, t) {
                        if (e.key == " " || e.code == "Space" || e.keyCode == 32 ||
                            e.key == "Enter" || e.keyCode == 13) {
                                var input = $("input.select2-search__field[aria-controls='select2-" + select.attr("id") + "-results']");
                                if (input.length){
                                    input.get(0).focus();
                                }
                        }
                    });

                }
            }
        }
    }

    initDateTimePicker(containerId, configuration) {
        new TempusDominus(containerId, configuration);
    }

    createCurrentDateForDatePicker(containerId, configuration) {
        const date = new Date();
        return new DateTime(date.getFullYear(), date.getMonth(), date.getDate(), 0, 0, 0, 0);
    }

    breakLongerTextInTableCell(cellId) {
        $("#" + cellId).css("word-break", function (index, origValue) {
            var textOfColumn = document.getElementById(cellId).innerText.trim();
            if (textOfColumn != '' && textOfColumn != ' ') {
                var numberOfChars = 15;
                var regex = new RegExp(`[^\\s]{${numberOfChars}}`);

                // Check if there are 15 consecutive non-whitespace characters anywhere in the string
                if (regex.test(textOfColumn)) {
                    return "break-all";
                }
            }
            return "inherit";
        });
    }

    // I'm not sure why sidebar has 15px padding -> and why I had to use 10px constant here [lazyman]
    fixContentHeight() {
        if ($(".main-footer").length > 0) {
            return;
        }

        var window_height = $(window).height();
        var sidebar_height = $(".sidebar").height() || 0;

        if (window_height < sidebar_height) {
            $(".content-wrapper, .right-side").css('min-height', sidebar_height + 10); // footer size
        }
    }

    clickFuncWicket6(eventData) {
        var clickedElement = (window.event) ? event.srcElement : eventData.target;
        if ((clickedElement.tagName.toUpperCase() == 'BUTTON'
                || clickedElement.tagName.toUpperCase() == 'A'
                || clickedElement.parentNode.tagName.toUpperCase() == 'A'
                || (clickedElement.tagName.toUpperCase() == 'INPUT'
                    && (clickedElement.type.toUpperCase() == 'BUTTON'
                        || clickedElement.type.toUpperCase() == 'SUBMIT')))
            && clickedElement.parentNode.id.toUpperCase() != 'NOBUSY'
            && clickedElement.disabled == 'false') {
            showAjaxStatusSign();
        }
    }

    initAjaxStatusSigns() {
        document.getElementsByTagName('body')[0].onclick = this.clickFuncWicket6;

        this.hideAjaxStatusSign();

        const self = this;

        Wicket.Event.subscribe('/ajax/call/beforeSend', function (attributes, jqXHR, settings) {
            self.showAjaxStatusSign();
        });

        Wicket.Event.subscribe('/ajax/call/complete', function (attributes, jqXHR, textStatus) {
            self.hideAjaxStatusSign();
        });
    }

    showAjaxStatusSign() {
        var element = document.getElementById('ajax_busy');
        if (element != null) {
            element.style.visibility = 'visible';
        }
    }

    hideAjaxStatusSign() {
        var element = document.getElementById('ajax_busy');
        if (element == null) {
            return;
        }
        element.style.visibility = 'hidden';
        if (document.querySelectorAll("[role='tooltip']") != null) {
            var tooltips = document.querySelectorAll("[role='tooltip']"), i;
            for (i = 0; i < tooltips.length; ++i) {
                tooltips[i].style.display = 'none';
            }
        }
        ;
    }

    /**
     * InlineMenu initialization function
     */
    initInlineMenu(menuId, hideByDefault) {
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

    isCogInTable(inlineMenuDiv) {
        return inlineMenuDiv.hasClass('cog') && inlineMenuDiv[0].tagName.toLowerCase() == 'td';
    }

    updateHeight(elementId, add, substract) {
        updateHeightReal(elementId, add, substract);
        $(window).resize(function () {
            updateHeightReal(elementId, add, substract);
        });
    }

    updateHeightReal(elementId, add, substract) {
        $('#' + elementId).css("height", "0px");

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
    initPageSizePopover(buttonId, popoverId, positionId) {
        console.log("initPageSizePopover('" + buttonId + "','" + popoverId + "','" + positionId + "')");

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
            // popover.addClass("show");
            popover.toggleClass("show");
        });
    }

    /**
     * Used in SearchPanel for advanced search, if we want to store resized textarea dimensions.
     *
     * @param textAreaId
     */
    storeTextAreaSize(textAreaId) {
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
    restoreTextAreaSize(textAreaId) {
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
    toggleSearchPopover(buttonId, popoverId, paddingRight) {
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

        var offsetLeft = button.offset().left - position.left;

        if ((left + popover.outerWidth() + offsetLeft) > window.innerWidth) {
            left = window.innerWidth - popover.outerWidth() - offsetLeft - 15;
        } else if (left < 0) {
            left = 0;
        }

        popover.css('top', top);
        popover.css('left', left);

        popover.toggle(200);

        //this will set focus to first form field on search item popup
        popover.find('input[type=text],textarea,select').filter(':visible:first').focus();

        //this will catch ESC or ENTER and fake close or update button click
        popover.find('input[type=text],textarea,select').off('keyup.search').on('keyup.search', function (e) {
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
    initDropdown(compId, options) {
        $('#' + compId).multiselect(options);
    }

    togglePopover(refId, popupId) {
        var popup = $(popupId);

        this.showPopover(refId, popupId, !popup.is(':visible'));
    }

    showPopover(refId, popupId, show) {
        var ref = $(refId);
        var popup = $(popupId);
        var arrow = popup.find('.arrow');

        if (!show) {
            if (popup.is(':visible')) {
                popup.fadeOut(200);
            }
        } else {
            if (!popup.is(':visible')) {
                var position = ref.position();

                var left = position.left + (ref.outerWidth() - popup.outerWidth() - 9) / 2;// - paddingRight;
                var top = position.top + ref.outerHeight();

                var offsetLeft = ref.offset().left - position.left;

                if ((left + popup.outerWidth() + offsetLeft) > window.innerWidth) {
                    left = window.innerWidth - popup.outerWidth() - offsetLeft - 15;
                } else if (left < 0) {
                    left = 0;
                }

                popup.css('top', top);
                popup.css('left', left);

                arrow.css('left', (popup.innerWidth() - arrow.width()) / 2);
                popup.fadeIn(200);
            }
        }
    }

    updatePageUrlParameter(paramName, paramValue) {
        var queryParams = new URLSearchParams(window.location.search);
        queryParams.set(paramName, paramValue);
        history.replaceState(null, null, "?" + queryParams.toString());
    }

    increment(inputId, incrementId, decrementId, increment) {
        var input = $('#' + inputId);
        var inc = $('#' + incrementId);
        var dec = $('#' + decrementId);

        var value = parseInt(input.val(), 10) || 0;

        value = value + increment;
        if (value <= 5) {
            value = 5;
            dec.addClass("disabled");
        } else {
            dec.removeClass("disabled")
        }

        if (value >= 100) {
            value = 100;
            inc.addClass("disabled");
        } else {
            inc.removeClass("disabled");
        }

        input.val(value);
    }

    initResponsiveTable() {
        $('.table-responsive').on('show.bs.dropdown', function (e) {
            const $dropdownMenu = $(e.target).find('.dropdown-menu'),
                menuHeight = $dropdownMenu.outerHeight(true);
            const extraHeight = 20;

            $('.table-responsive').css("min-height", menuHeight + extraHeight + "px");
        });

        $('.table-responsive').on('hide.bs.dropdown', function () {
            $('.table-responsive').css("min-height", "auto");
        })
    }

    initPushMenuButton() {
        $('a[data-widget="pushmenu"]').on("click", function (e) {
            setAriaExpandedForPushMenu($(this), false);
        });
        setAriaExpandedForPushMenu($('a[data-widget="pushmenu"]'), true);

        function setAriaExpandedForPushMenu(menuButton, processAfterClick) {
            var valueExpand = "true";
            var valueCollapse = "false";
            if (!processAfterClick) {
                valueExpand = "false";
                valueCollapse = "true";
            }

            if ($('body').hasClass('sidebar-collapse')) {
                menuButton.attr("aria-expanded", valueCollapse);
            } else {
                menuButton.attr("aria-expanded", valueExpand);
            }
        }
    }

    createSparkline(id, options, data) {
        $(function () {
            var chart = new Sparkline($(id)[0], options)
            chart.draw(data)
        });
    }

//    /**
//     * used in SimulationModePanel.java
//     *
//     * @param compId
//     */
//    initDropdownResize(panelId) {
//        var panel = $('#' + panelId);
//        console.log('initDropdownResize panel: ' + panel);
//        panel.find("option.width-tmp-option").html(panel.find("select.resizing-select option:selected").text());
//        panel.find("select.resizing-select").width(panel.find("select.width-tmp-select").width());
//    }

    /**
    * Used for scaling tables, images and charts (Role Mining)
    *
    * @param containerId
    */
    initScaleResize(containerId) {
        let div = document.querySelector(containerId);
        let scale = 0.5;
        let component = null;
        const minDragDistance = 10

        if (!div) {
            console.error('Container not found');
            return;
        }

        if (containerId === '#tableScaleContainer') {
            component = div.querySelector('table');
        } else if (containerId === '#chartScaleContainer') {
            component = div.querySelector('canvas');
        }

        if (!component) {
            console.error('Component not found');
            return
        }

        div.addEventListener('wheel', handleZoom);
        div.addEventListener('mousedown', startDrag);
        div.addEventListener('mouseup', stopDrag);
        div.addEventListener('mouseleave', stopDrag);

        let startX, startY, startScrollLeft, startScrollTop;
        let isMouseDown = false
        let isDragging = false

        function startDrag(e) {
                e.preventDefault();
                isMouseDown = true
                startX = e.clientX;
                startY = e.clientY;
                startScrollLeft = div.scrollLeft;
                startScrollTop = div.scrollTop;
                div.addEventListener('mousemove', drag);
            }

            function drag(e) {
                e.preventDefault();
                const dx = e.clientX - startX;
                const dy = e.clientY - startY;
                const delta = Math.sqrt(dx * dx + dy * dy)
                if (!isDragging && isMouseDown && delta > minDragDistance) {
                  // mouse-down-move at least `minDragDistance` pixels from the origin to assume dragging
                  isDragging = true
                  // prevents other gesture handlers to interact
                  component.style['pointer-events'] = 'none'
                }
                if (!isDragging) {
                  return
                }
                div.scrollLeft = startScrollLeft - dx;
                div.scrollTop = startScrollTop - dy;
            }

            function stopDrag(e) {
                isMouseDown = false
                isDragging = false
                e.preventDefault()
                component.style['pointer-events'] = 'inherit'
                div.removeEventListener('mousemove', drag);
            }

        function handleZoom(e) {
            e.preventDefault();
            let rectBefore = component.getBoundingClientRect();

            if (e.deltaY < 0) {
                zoomIn(rectBefore, containerId === '#chartScaleContainer' ? true : false);
            } else if (e.deltaY > 0) {
                zoomOut(rectBefore, containerId === '#chartScaleContainer' ? 1.0 : 0.1);
            }
        }

        function zoomIn(rectBefore, isChart) {
            console.log('Zooming in');

            if(isChart && scale < 1.0){
                 scale = 1.0;
            }
            scale += 0.05;

            let prevScale = scale - 0.05;
            let scaleFactor = scale / prevScale;
            //TODO target to cursor (temporarily disabled because of display rendering issues)
            setTransform(0, 0, scale, rectBefore, scaleFactor);
        }

        function zoomOut(rectBefore, maxScale) {
            console.log('Zooming out');
            scale -= 0.05;
            scale = Math.max(maxScale, scale);

            setTransform(0, 0, scale, rectBefore, 1);
        }

        function setTransform(x, y, scale, rectBefore, scaleFactor) {
            component.style.transformOrigin = '0% 0%';
            component.style.transition = 'transform 0.3s';
            component.style.transform = `scale(${scale})`;
            div.scrollLeft = 0;
            div.scrollTop = 0;
        }
    }

    initAxiomSearchPanel(queryDslInputId) {
        const queryDslInput = $("#" + queryDslInputId);

        const autocomplete = $("<div/>")
            .hide()
            .attr('id', 'queryDslAutocomplete')
            .appendTo(queryDslInput.parent());

        $(document).on("click", function(event) {
            if (!$(event.target).closest("#" + queryDslInputId, "#queryDslAutocomplete").length) {
                autocomplete.hide()
                autocomplete.empty()
            }
        });
    }

    syncCodeCompletions(suggestions, queryDslInputId) {
        const queryDslInput = $("#" + queryDslInputId);
        const autocomplete = $("#queryDslAutocomplete");
        const inputOffset = queryDslInput.offset();
        const caretPosition = this.getCaretCoordinates(queryDslInput);

        // calculate position for autocomplete window
        autocomplete.css({
            top: inputOffset.top + (caretPosition.top === 0 ? 21 : caretPosition.top) + parseInt(queryDslInput.css('font-size')),
            left: inputOffset.left + caretPosition.left
        });

        autocomplete.empty()
        autocomplete.show()

        if (suggestions != undefined && suggestions.length != 0) {
            const query = queryDslInput.val();
            const cursorPosition = queryDslInput[0].selectionStart;
            const commands = query.slice(0, cursorPosition).split(" ");
            const positionCommand = commands[commands.length - 1];

            const regex = new RegExp(`(${positionCommand})`, "gi");
            let hideAutocomplete = true;

            let renderSuggestions = (name, alias) => autocomplete.append('<div class="line suggestion">' +
                    '<span class="name">' + name + '</span>' +
                    '<span class="alias">' + alias + '</span>' +
                '</div>'
            );

            suggestions.forEach(suggestion => {
                const highlightedName = suggestion.name.replace(regex, "<strong>$1</strong>");
                if (positionCommand === " " || suggestion.name.includes(positionCommand)) {
                    hideAutocomplete = false
                    renderSuggestions(highlightedName, suggestion.alias)
                }
            })

            if (hideAutocomplete) {
                autocomplete.hide()
            }

            autocomplete.on('click', '.suggestion', function() {
                commands[commands.length - 1] = $(this).find('.name').text()
                queryDslInput.val(commands.join(" ") + query.substring(cursorPosition))
                queryDslInput[0].focus()
                queryDslInput[0].setSelectionRange(commands.join(" ").length, commands.join(" ").length)
                autocomplete.hide()
                autocomplete.empty()
            });

            let currentIndex = -1; // No selection initially

            queryDslInput.on('input keydown click', function (e) {
                // navigation in suggestions list
                if (e.key === "ArrowDown" || e.key == "Arrow Down" || e.keyCode == 40)  {
                    e.preventDefault();
                    navigate(1)
                } else if (e.key === "ArrowUp" || e.key == "Arrow Up" || e.keyCode == 38) {
                    e.preventDefault();
                    navigate(-1)
                } else if (e.key === "Enter") {
                    // TODO action after touch Enter (send content or selected active suggestion)
//                    // select active suggestion by touch Enter key
//                    const suggestions = $("#queryDslAutocomplete").find(".suggestion");
//                    if (currentIndex >= 0 && currentIndex < suggestions.length) {
//                        commands[commands.length - 1] = $(suggestions[currentIndex]).find(".name").text()
//                        queryDslInput.val(commands.join(" ") + query.substring(cursorPosition))
//                        queryDslInput[0].focus()
//                        queryDslInput[0].setSelectionRange(commands.join(" ").length, commands.join(" ").length)
//                        autocomplete.hide();
//                        autocomplete.empty();
//                        currentIndex = -1;
//                    }
                } else {
                    window.MidPointTheme.cursorPosition = this.selectionStart;
                }
            })

            function navigate(direction) {
                const suggestions = $("#queryDslAutocomplete").find(".suggestion");
                if (suggestions.length === 0 || suggestions.length < currentIndex || currentIndex < -1) return;

                currentIndex = (currentIndex + direction + suggestions.length) % suggestions.length;
                suggestions.removeClass("active");
                const activeItem = $(suggestions[currentIndex]).addClass("active");
                activeItem[0].scrollIntoView({ block: "nearest", behavior: "smooth" });
            }
        }

//        $('.content-assist-error').remove()
//        contentAssist.validate.forEach(error => {
//            queryDslInput.after('<span class="text-wrap invalid-feedback content-assist-error" style="display: block">' +
//                    'Error at position: ' + error.charPositionInLineStart + ', ' + error.charPositionInLineStop + ' ' +
//                    error.message
//                + '</span>')
//        })
    }

    getCaretCoordinates(element) {
        const text = element[0].value.substring(0, element[0].selectionStart);
        // secret <span/> element for tracking position in body
        const span = $('<span/>').css({
            position: 'absolute',
            whiteSpace: 'pre-wrap',
            visibility: 'hidden',
            font: element.css('font'), // Match input font
            lineHeight: element.css('line-height'),
        }).text(text);

        $('body').append(span);

        const rect = span[0].getBoundingClientRect();
        const coordinates = { top: rect.height, left: rect.width };

        span.remove();
        return coordinates;
    }

    triggerAutocompleteShortcut(event, element) {
        if (event.ctrlKey && event.key === ' ') {
            event.preventDefault();
            element.dispatchEvent(new Event('keyup', { bubbles: true }));
        }
    }
}
