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
}