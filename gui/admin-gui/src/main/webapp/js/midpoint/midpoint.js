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

