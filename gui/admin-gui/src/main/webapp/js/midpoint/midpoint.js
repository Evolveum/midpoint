/**
 * InlineMenu initialization function
 */
function initInlineMenu(menuId) {
    var menu = $('#' + menuId).find('ul.cog');

    var parent = menu.parent().parent();
    if (!parent.hasClass('cog') || parent[0].tagName.toLowerCase() != 'td') {
        return;
    }

    // we only want to hide inline menus that are in table <td> element,
    // inline menu in header must be visible all the time
    menu.hide();

    parent.hover(function () {
        //over
        menu.show();
    }, function () {
        //out
        menu.hide();
    })
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