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