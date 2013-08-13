function realResizeEditor(editorId) {

    var defaultAceSize = 0;
    var newHeight = $(document).height() - $('div#body').outerHeight(true) + defaultAceSize;
    //just validation checks
    if (newHeight < 300) {
        newHeight = 300;
    }
    if (newHeight > 1000) {
        newHeight = 1000;
    }

    var height = newHeight.toString() + 'px';
    $('#' + editorId).height(height);
    $('#' + editorId + '-section').height(height);

    var editor = ace.edit(editorId);
    editor.resize();
}

/**
 * This method should resize Ace editor based on page height. Now disabled. Not finished yet.
 *
 * @param editorId
 */
function resizeEditor(editorId) {
//    $(document).ready(function () {
//        realResizeEditor(editorId);
//    });
//    $(window).resize(function () {
//        realResizeEditor(editorId);
//    });
}

/**
 * This piece of JS is generated in AceEditor.java (omg)....
 */
//Wicket.Event.add(window, "domready", function (event) {
//    if (false == Wicket.Browser.isIELessThan9()) {
//        if ($('#aceEditor301_edit').length == 0) {
//            $("<div id='aceEditor301_edit'><\/div>").insertAfter($('#aceEditor301'));
//            $('#aceEditor301_edit').text($('#aceEditor301').val());
//            window.aceEditor301_edit = ace.edit("aceEditor301_edit");
//            aceEditor301_edit.setTheme("ace/theme/textmate");
//            aceEditor301_edit.getSession().setMode('ace/mode/xml');
//            $('#aceEditor301').hide();
//            aceEditor301_edit.setShowPrintMargin(false);
//            aceEditor301_edit.setFadeFoldWidgets(false);
//            aceEditor301_edit.setReadOnly(true);
//            aceEditor301_edit.on('blur', function () {
//                $('#aceEditor301').val(aceEditor301_edit.getSession().getValue());
//                $('#aceEditor301').trigger('onBlur');
//            });
//        }
//        if (true) {
//            $('.ace_scroller').css('background', '#F4F4F4');
//        } else {
//            $('.ace_scroller').css('background', '#FFFFFF');
//        }
//        $('#aceEditor301_edit textarea').attr('onkeydown', 'disablePaste(true);');
//        $('#aceEditor301_edit').append("<a class='helpButton' href='https://github.com/ajaxorg/ace/wiki/Default-Keyboard-Shortcuts' target='_blank' title='Show keyboard shortcuts'><\/a>");
//        if ($.browser.msie) {
//            $('#aceEditor301_edit').find('.ace_gutter').hide();
//        }
//    } else {
//        $('#aceEditor301').attr('readonly', 'readonly').css('background', '#F4F4F4');
//    }
//});