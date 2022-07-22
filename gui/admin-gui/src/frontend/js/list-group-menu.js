/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
import $ from 'jquery';

const NAME = 'listGroupMenu'
const DATA_KEY = 'midpoint.listGroupMenu'
const EVENT_KEY = `.${DATA_KEY}`
const JQUERY_NO_CONFLICT = $.fn[NAME]

const EVENT_EXPANDED = `expanded${EVENT_KEY}`
const EVENT_COLLAPSED = `collapsed${EVENT_KEY}`
const EVENT_LOAD_DATA_API = `load${EVENT_KEY}`

const SELECTOR_LINK = '.item-link'
const SELECTOR_DATA_WIDGET = '[data-widget="list-group-menu"]'

const Default = {
    trigger: `${SELECTOR_DATA_WIDGET} ${SELECTOR_LINK}`,
    animationSpeed: 300,
    accordion: true
}

class ListGroupMenu {

    constructor(element, config) {
        this._config = config
        this._element = element
    }

    init() {
        const elementId = this._element.attr('id') !== undefined ? `#${this._element.attr('id')}` : ''
        $(document).on('click', `${elementId}${this._config.trigger}`, event => {
            this.toggle(event)
        })
    }

    toggle(event) {
        console.log('toggle');

        const link = $(event.currentTarget);

        const chevron = link.find('i.chevron');
        if (chevron.length == 0) {
            return;
        }

        event.preventDefault();

        const item = link.parent();
        const submenu = item.find('.list-group-submenu');

        if (!submenu.is(':visible')) {
            chevron.css('transform', 'rotate(270deg)');
            $(submenu).slideDown();
        } else {
            chevron.css('transform', 'rotate(0deg)');
            $(submenu).slideUp();
        }
    }

    static _jQueryInterface(config) {
        return this.each(function () {
            let data = $(this).data(DATA_KEY)
            const _options = $.extend({}, Default, $(this).data())

            if (!data) {
                data = new ListGroupMenu($(this), _options)
                $(this).data(DATA_KEY, data)
            }

            if (config === 'init') {
                data[config]()
            }
        })
    }
}

/**
 * Data API
 */

$(window).on(EVENT_LOAD_DATA_API, () => {
    $(SELECTOR_DATA_WIDGET).each(function () {
        ListGroupMenu._jQueryInterface.call($(this), 'init')
    })
})


/**
 * jQuery API
 */

$.fn[NAME] = ListGroupMenu._jQueryInterface
$.fn[NAME].Constructor = ListGroupMenu
$.fn[NAME].noConflict = () => {
    $.fn[NAME] = JQUERY_NO_CONFLICT
    return ListGroupMenu._jQueryInterface
}

export default ListGroupMenu;

