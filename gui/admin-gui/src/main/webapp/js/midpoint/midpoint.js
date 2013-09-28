/**
 * Top menu functions
 */
$(document).ready(function () {
    init();
});

function init() {
    $('td.cog').find('ul.cog').hide();

    $('td.cog').hover(function() {
        //over
        $(this).find('ul.cog').show();
    }, function() {
        //out
        $(this).find('ul.cog').hide();
    })
}