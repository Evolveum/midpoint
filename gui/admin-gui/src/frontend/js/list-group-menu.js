/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
import $ from 'jquery';

const NAME = 'listGroupMenu'
const VERSION = '0.1'
const DATA_KEY = 'midpoint.listGroupMenu'
const EVENT_KEY = `.${DATA_KEY}`
const DATA_API_KEY = '.data-api'
const JQUERY_NO_CONFLICT = $.fn[NAME]

// todo implement properly
class ListGroupMenu {

    constructor(element, config) {
    }

    static get VERSION() {
        return VERSION
    }

    static get Default() {
        return Default
    }

    static _jQueryInterface(config) {

    }
}

$.fn[NAME] = ListGroupMenu._jQueryInterface
$.fn[NAME].Constructor = ListGroupMenu
$.fn[NAME].noConflict = () => {
    $.fn[NAME] = JQUERY_NO_CONFLICT
    return ListGroupMenu._jQueryInterface
}

export default ListGroupMenu;

