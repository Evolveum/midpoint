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