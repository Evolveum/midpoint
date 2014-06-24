/**
 * InlineMenu initialization function
 */
function initInlineMenu(menuId, hideByDefault) {
    var menu = $('#' + menuId).find('ul.cog');

    var parent = menu.parent().parent();     //this is inline menu div
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
    menu.hide();

    parent.hover(function () {
        //over
        menu.show();
    }, function () {
        //out
        menu.hide();
    })
}

function isCogInTable(inlineMenuDiv) {
    return inlineMenuDiv.hasClass('cog') && inlineMenuDiv[0].tagName.toLowerCase() == 'td';
}

/**
 * jquery function which provides fix "table" striping by adding css class
 * to proper "rows". Used in PrismObjectPanel.
 */
function fixStripingOnPrismForm(formId, stripClass) {
    var objects = $('#' + formId).find('div.attributeComponent > div.visible');
    for (var i = 0; i < objects.length; i++) {
        if (i % 2 == 0) {
            objects[i].className += " " + stripClass;
        }
    }
}

/**
 * Used in SimplePieChart class.
 *
 * @param chartId
 */
function initPieChart(chartId) {
    $('#' + chartId).easyPieChart({
        barColor: function (percent) {
            percent /= 100;
            return "rgb(" + Math.round(255 * percent) + ", " + Math.round(255 * (1 - percent)) + ", 0)";
        },
        trackColor: '#ccc',
        scaleColor: false,
        lineCap: 'butt',
        lineWidth: 15,
        animate: 1000,
        size: 90
    });
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
    var bodyTopPadding = $(".navbar-fixed-top").outerHeight(false);
    var mainContainerHeight = $('div.mainContainer').outerHeight(true);
    var elementHeight =  $('#' + elementId).outerHeight(true);

    console.log("Document height: " + documentHeight + ", mainContainer: "
        + mainContainerHeight + ", body top-padding: " + bodyTopPadding);

    var height = documentHeight - mainContainerHeight - bodyTopPadding - elementHeight;
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
 * Used in PageBase class.
 *
 * This function updates body element top padding based on fixed top menu. Top menu height (outerHeight(false) is
 * by default 41px), but it can change when browser width is small for all menu items, but not small enough to show
 * menu in "mobile" (minimized) style.
 */
function updateBodyTopPadding() {
    updateBodyTopPaddingReal();

    $(window).resize(function() {
        updateBodyTopPaddingReal();
    });
}

function updateBodyTopPaddingReal() {
    var menuHeight = $(".navbar-fixed-top").outerHeight(false);
    $("body").css("padding-top", menuHeight + "px");
}

/**
 * Used in PageSizePopover class, in table panel.
 *
 * @param buttonId
 * @param popoverId
 */
function initPageSizePopover(buttonId, popoverId) {
    var button = $('#' + buttonId);
    button.click(function () {
        var popover = $('#' + popoverId);

        var position = button.position();

        var left = position.left - popover.outerWidth();
        var top = position.top + button.outerHeight() / 2 - popover.outerHeight() / 2;

        popover.css("top", top);
        popover.css("left", left);

        popover.toggle();
    });
}