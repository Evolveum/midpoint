/**
 * Represents a keyboard for checking adjacency on.
 *
 * @param {object} object
 * @param {number} average
 */
function Keyboard(object, average) {
    this.keyboard = object;
    this.averageNeighbours = average;

    this.areAdjacent = function(a, b) {
        if (a in this.keyboard) {
            for (var i = 0; i < this.keyboard[a].length; i++) {
                if (this.keyboard[a][i] !== null && this.keyboard[a][i].indexOf(b) >= 0) {
                    return true;
                }
            }
        }

        return false;
    };
};

/**
 * Constructor.
 *
 * @param {string} password
 */
function Score(password) {
    this.password = password;
}

/**
 * Used to estimate the score of the given password.
 *
 * @class
 * @author David Stutz
 */
Score.prototype = {

    constructor: Score,

    LOWER: 26,
    UPPER: 26,
    NUMBER: 10,
    PUNCTUATION: 34,
    OTHER: 34, // this includes special characters, umlauts (i.e. diacritics) etc.
               // it's not easy to get a reasonable count of these (which are commonly used around the world)
               // see e.g. https://en.wikipedia.org/wiki/Diaeresis_(diacritic)
               // therefore, these are treated like punctuation

    DAYS: 31,
    MONTHS: 31,
    YEARS: 2000,

    sequences: {
        lower: 'abcdefghijklmnopqrstuvwxyz',
        numbers: '01234567890'
    },

    leet: {
        '1': ['i', 'l'],
        '2': ['n', 'r', 'z'],
        '3': ['e'],
        '4': ['a'],
        '5': ['s'],
        '6': ['g'],
        '7': ['t'],
        '8': ['b'],
        '9': ['g', 'o'],
        '0': ['o'],
        '@': ['a'],
        '(': ['c'],
        '[': ['c'],
        '<': ['c'],
        '&': ['g'],
        '!': ['i'],
        '|': ['i', 'l'],
        '?': ['n', 'r'],
        '$': ['s'],
        '+': ['t'],
        '%': ['x']
    },

    regex: {
        repetition: {
            single: /(.)\1+/g,
            group: /(..+)\1+/g
        },
        date: {
            DMY: /(0?[1-9]|[12][0-9]|3[01])([\- \/.])?(0?[1-9]|1[012])([\- \/.])?([0-9]{2})/g,
            DMYY: /(0?[1-9]|[12][0-9]|3[01])([\- \/.])?(0?[1-9]|1[012])([\- \/.])?([0-9]{4})/g,
            MDY: /(0?[1-9]|1[012])([\- \/.])?(0?[1-9]|[12][0-9]|3[01])([\- \/.])?([0-9]{2})/g,
            MDYY: /(0?[1-9]|1[012])([\- \/.])?(0?[1-9]|[12][0-9]|3[01])([\- \/.])?([0-9]{4})/g,
            YDM: /([0-9]{2})([\- \/.])?(0?[1-9]|[12][0-9]|3[01])([\- \/.])?(0?[1-9]|1[012])/g,
            YYDM: /([0-9]{4})([\- \/.])?(0?[1-9]|[12][0-9]|3[01])([\- \/.])?(0?[1-9]|1[012])/g,
            YMD: /([0-9]{2})([\- \/.])?(0?[1-9]|1[012])([\- \/.])?(([0-9]{2})?[0-9]{2})(0?[1-9]|[12][0-9]|3[01])/g,
            YYMD: /([0-9]{4})([\- \/.])?(0?[1-9]|1[012])([\- \/.])?(([0-9]{2})?[0-9]{2})(0?[1-9]|[12][0-9]|3[01])/g,
            DM: /(0?[1-9]|[12][0-9]|3[01])([\- \/.])?(0?[1-9]|1[012])/g,
            MY: /(0?[1-9]|1[012])([\- \/.])?([0-9]{2})/g,
            MYY: /(0?[1-9]|1[012])([\- \/.])?([0-9]{4})/g
        },
        number: /[0-9]+/,
        numberOnly: /^[0-9]+$/,
        punctuation: /[\^°!"§\$%&\/\(\)=\?\\\.:,;\-_#'\+~\*<>\|\[\]\{\}`´]+/, // 34
        punctuationOnly: /^[\^°!"§\$%&\/\(\)=\?\\\.:,;\-_#'\+~\*<>\|\[\]\{\}`´]+$/,
        lower: /[a-z]+/,
        lowerOnly: /^[a-z]+$/,
        upper: /[A-Z]+/,
        upperOnly: /^[A-Z]+$/,
        upperFirst: /^[A-Z]+[A-Za_z]*$/,
        upperFirstOnly: /^[A-Z]{1}[a-z]+$/,
        other: /[^\^°!"§\$%&\/\(\)=\?\\\.:,;\-_#'\+~\*<>\|\[\]\{\}`´0-9a-zA-Z]+/,
        otherOnly: /^[^\^°!"§\$%&\/\(\)=\?\\\.:,;\-_#'\+~\*<>\|\[\]\{\}`´0-9a-zA-Z]+$/,
    },

    keyboards: {
        QWERTZ: new Keyboard({ // 480/105 = 4.571428571
            '^': [null, null, null, '^°', '1!', null, null], // 1
            '°': [null, null, null, '^°', '1!', null, null], // 1
            '1': [null, null, '^°', '1!', '2"', null, 'qQ@'], // 3
            '!': [null, null, '^°', '1!', '2"', null, 'qQ@'], // 3
            '2': [null, null, '1!', '2"', '3§', 'qQ@', 'wW'], // 4
            '"': [null, null, '1!', '2"', '3§', 'qQ@', 'wW'], //4
            '3': [null, null, '2"', '3§', '4$', 'wW', 'eE€'], //4
            '§': [null, null, '2"', '3§', '4$', 'wW', 'eE€'], //4
            '4': [null, null, '3§', '4$', '5%', 'eE€', 'rR'], //4
            '$': [null, null, '3§', '4$', '5%', 'eE€', 'rR'], //4
            '5': [null, null, '4$', '5%', '6&', 'rR', 'tT'], //4
            '%': [null, null, '4$', '5%', '6&', 'rR', 'tT'], //4
            '6': [null, null, '5%', '6&', '7/{', 'tT', 'yY'], //4
            '&': [null, null, '5%', '6&', '7/{', 'tT', 'yY'], //4
            '7': [null, null, '6&', '7/{', '8([', 'yY', 'uU'], //4
            '/': [null, null, '6&', '7/{', '8([', 'yY', 'uU'], //4
            '{': [null, null, '6&', '7/{', '8([', 'yY', 'uU'], //4
            '8': [null, null, '7/{', '8([', '9)]', 'uU', 'iI'], // 4
            '(': [null, null, '7/{', '8([', '9)]', 'uU', 'iI'], // 4
            '[': [null, null, '7/{', '8([', '9)]', 'uU', 'iI'], // 4
            '9': [null, null, '8([', '9)]', '0=}', 'iI', 'oO'], // 4
            ')': [null, null, '8([', '9)]', '0=}', 'iI', 'oO'], // 4
            ']': [null, null, '8([', '9)]', '0=}', 'iI', 'oO'], // 4
            '0': [null, null, '9)]', '0=}', 'ß?\\', 'oO', 'pP'], // 4
            '=': [null, null, '9)]', '0=}', 'ß?\\', 'oO', 'pP'], // 4
            '}': [null, null, '9)]', '0=}', 'ß?\\', 'oO', 'pP'], // 4
            'ß': [null, null, '0=}', 'ß?\\', '´`', 'pP', 'üÜ'], // 4
            '?': [null, null, '0=}', 'ß?\\', '´`', 'pP', 'üÜ'], // 4
            '\\': [null, null, '0=}', 'ß?\\', '´`', 'pP', 'üÜ'], // 4
            '`': [null, null, 'ß?\\', '´`', null, 'üÜ', '+*~'], // 3
            '´': [null, null, 'ß?\\', '´`', null, 'üÜ', '+*~'], // 3
            'q': ['1!', '2"', null, 'qQ@', 'wW', null, 'aA'], // 4
            'Q': ['1!', '2"', null, 'qQ@', 'wW', null, 'aA'], // 4
            '@': ['1!', '2"', null, 'qQ@', 'wW', null, 'aA'], // 4
            'w': ['2"', '3§', 'qQ@', 'wW', 'eE€', 'aA', 'sS'], // 6
            'W': ['2"', '3§', 'qQ@', 'wW', 'eE€', 'aA', 'sS'], // 6
            'e': ['3§', '4$', 'wW', 'eE€', 'rR', 'sS', 'dD'], // 6
            'E': ['3§', '4$', 'wW', 'eE€', 'rR', 'sS', 'dD'], // 6
            '€': ['3§', '4$', 'wW', 'eE€', 'rR', 'sS', 'dD'], // 6
            'r': ['4$', '5%', 'eE€', 'rR', 'tT', 'dD', 'fF'], // 6
            'R': ['4$', '5%', 'eE€', 'rR', 'tT', 'dD', 'fF'], // 6
            't': ['5%', '6&', 'rR', 'tT', 'zZ', 'fF', 'gG'], // 6
            'T': ['5%', '6&', 'rR', 'tT', 'zZ', 'fF', 'gG'], // 6
            'z': ['6&', '7/{', 'tT', 'zZ', 'uU', 'gG', 'hH'], // 6
            'Z': ['6&', '7/{', 'tT', 'zZ', 'uU', 'gG', 'hH'], // 6
            'u': ['7/{', '8([', 'zZ', 'uU', 'iI', 'hH', 'jJ'], // 6
            'U': ['7/{', '8([', 'zZ', 'uU', 'iI', 'hH', 'jJ'], // 6
            'i': ['8([', '9)]', 'uU', 'iI', 'oO', 'jJ', 'kK'], // 6
            'I': ['8([', '9)]', 'uU', 'iI', 'oO', 'jJ', 'kK'], // 6
            'o': ['9)]', '0=}', 'iI', 'oO', 'pP', 'kK', 'lL'], // 6
            'O': ['9)]', '0=}', 'iI', 'oO', 'pP', 'kK', 'lL'], // 6
            'p': ['0=}', 'ß?\\', 'oO', 'pP', 'üÜ', 'lL', 'öÖ'], // 6
            'P': ['0=}', 'ß?\\', 'oO', 'pP', 'üÜ', 'lL', 'öÖ'], // 6
            'ü': ['ß?\\', '´`', 'pP', 'üÜ', '+*~', 'öÖ', 'äÄ'], // 6
            'Ü': ['ß?\\', '´`', 'pP', 'üÜ', '+*~', 'öÖ', 'äÄ'], // 6
            '+': ['´``', null, 'üÜ', '+*~', null, 'äÄ', '\'#'], // 4
            '*': ['´``', null, 'üÜ', '+*~', null, 'äÄ', '\'#'], // 4
            '~': ['´``', null, 'üÜ', '+*~', null, 'äÄ', '\'#'], // 4
            'a': ['qQ@', 'wW', null, 'aA', 'sS', '<>|', 'yY'], // 5
            'A': ['qQ@', 'wW', null, 'aA', 'sS', '<>|', 'yY'], // 5
            's': ['wW', 'eE€', 'aA', 'sS', 'dD', 'yY', 'xX'], // 6
            'S': ['wW', 'eE€', 'aA', 'sS', 'dD', 'yY', 'xX'], // 6
            'd': ['eE€', 'rR', 'sS', 'dD', 'fF', 'xX', 'cC'], // 6
            'D': ['eE€', 'rR', 'sS', 'dD', 'fF', 'xX', 'cC'], // 6
            'f': ['rR', 'tT', 'dD', 'fF', 'gG', 'cC', 'vV'], // 6
            'F': ['rR', 'tT', 'dD', 'fF', 'gG', 'cC', 'vV'], // 6
            'g': ['tT', 'zZ', 'fF', 'gG', 'hH', 'vV', 'bB'], // 6
            'G': ['tT', 'zZ', 'fF', 'gG', 'hH', 'vV', 'bB'], // 6
            'h': ['zZ', 'uU', 'gG', 'hH', 'jJ', 'bB', 'nN'], // 6
            'H': ['zZ', 'uU', 'gG', 'hH', 'jJ', 'bB', 'nN'], // 6
            'j': ['uU', 'iI', 'hH', 'jJ', 'kK', 'nN', 'mM'], // 6
            'J': ['uU', 'iI', 'hH', 'jJ', 'kK', 'nN', 'mM'], // 6
            'k': ['iI', 'oO', 'jJ', 'kK', 'lL', 'mM', ',;'], // 6
            'K': ['iI', 'oO', 'jJ', 'kK', 'lL', 'mM', ',;'], // 6
            'l': ['oO', 'pP', 'kK', 'lL', 'öÖ', ',;', '.:'], // 6
            'L': ['oO', 'pP', 'kK', 'lL', 'öÖ', ',;', '.:'], // 6
            'ö': ['pP', 'üÜ', 'lL', 'öÖ', 'äÄ', '.:', '-_'], // 6
            'Ö': ['pP', 'üÜ', 'lL', 'öÖ', 'äÄ', '.:', '-_'], // 6
            'ä': ['üÜ', '+*~', 'öÖ', 'äÄ', '#\'', '-_', null], // 5
            'Ä': ['üÜ', '+*~', 'öÖ', 'äÄ', '#\'', '-_', null], // 5
            '#': ['+*~', null, 'äÄ', '#\'', null, null, null], // 2
            '\'': ['+*~', null, 'äÄ', '#\'', null, null, null], // 2
            '<': [null, 'aA', null, '<>|', 'yY', null, null], // 2
            '>': [null, 'aA', null, '<>|', 'yY', null, null], // 2
            '|': [null, 'aA', null, '<>|', 'yY', null, null], // 2
            'y': ['aA', 'sS', '<>|', 'yY', 'xX', null, null], // 4
            'Y': ['aA', 'sS', '<>|', 'yY', 'xX', null, null], // 4
            'x': ['sS', 'dD', 'yY', 'xX', 'cC', null, null], // 4
            'X': ['sS', 'dD', 'yY', 'xX', 'cC', null, null], // 4
            'c': ['dD', 'fF', 'xX', 'cC', 'vV', null, null], // 4
            'C': ['dD', 'fF', 'xX', 'cC', 'vV', null, null], // 4
            'v': ['fF', 'gG', 'cC', 'vV', 'bB', null, null], // 4
            'V': ['fF', 'gG', 'cC', 'vV', 'bB', null, null], // 4
            'b': ['gG', 'hH', 'vV', 'bB', 'nN', null, null], // 4
            'B': ['gG', 'hH', 'vV', 'bB', 'nN', null, null], // 4
            'n': ['hH', 'jJ', 'bB', 'nN', 'mM', null, null], // 4
            'N': ['hH', 'jJ', 'bB', 'nN', 'mM', null, null], // 4
            'm': ['jJ', 'kK', 'nN', 'mM', ',;', null, null], // 4
            'M': ['jJ', 'kK', 'nN', 'mM', ',;', null, null], // 4
            ',': ['kK', 'lL', 'mM', ',;', '.:', null, null], // 4
            ';': ['kK', 'lL', 'mM', ',;', '.:', null, null], // 4
            '.': ['lL', 'öÖ', ',;', '.:', '-_', null, null], // 4
            ':': ['lL', 'öÖ', ',;', '.:', '-_', null, null], // 4
            '-': ['öÖ', 'ä', '.:', '-_', null, null, null], // 3
            '_': ['öÖ', 'ä', '.:', '-_', null, null, null] // 3
        }, 4.571428571),
        QWERTY: new Keyboard({
            '~': [null, null, null, '~`', '1!', null, null], // 1
            '`': [null, null, null, '~`', '1!', null, null], // 1
            '1': [null, null, '`~', '1!', '2@', null, 'qQ'], // 3
            '!': [null, null, '`~', '1!', '2@', null, 'qQ'], // 3
            '2': [null, null, '1!', '2@', '3#', 'qQ', 'wW'], // 4
            '@': [null, null, '1!', '2@', '3#', 'qQ', 'wW'], //4
            '3': [null, null, '2@', '3#', '4$', 'wW', 'eE'], //4
            '#': [null, null, '2@', '3#', '4$', 'wW', 'eE'], //4
            '4': [null, null, '3#', '4$', '5%', 'eE', 'rR'], //4
            '$': [null, null, '3#', '4$', '5%', 'eE', 'rR'], //4
            '5': [null, null, '4$', '5%', '6^', 'rR', 'tT'], //4
            '%': [null, null, '4$', '5%', '6^', 'rR', 'tT'], //4
            '6': [null, null, '5%', '6^', '7&', 'tT', 'yY'], //4
            '^': [null, null, '5%', '6^', '7&', 'tT', 'yY'], //4
            '7': [null, null, '6^', '7&', '8*', 'yY', 'uU'], //4
            '&': [null, null, '6^', '7&', '8*', 'yY', 'uU'], //4
            '8': [null, null, '7&', '8*', '9(', 'uU', 'iI'], // 4
            '*': [null, null, '7&', '8*', '9(', 'uU', 'iI'], // 4
            '9': [null, null, '8*', '9(', '0)', 'iI', 'oO'], // 4
            '(': [null, null, '8*', '9(', '0)', 'iI', 'oO'], // 4
            '0': [null, null, '9(', '0)', '-_', 'oO', 'pP'], // 4
            ')': [null, null, '9(', '0)', '-_', 'oO', 'pP'], // 4
            '-': [null, null, '0)', '-_', '=+', 'pP', '[{'], // 4
            '_': [null, null, '0)', '-_', '=+', 'pP', '[{'], // 4
            '=': [null, null, '-_', '=+', '\\|', '{[', '}]'], // 4
            '+': [null, null, '-_', '=+', '\\|', '{[', '}]'], // 4
            '\\': [null, null, '=+', '\\|', null, '}]', null], // 2
            '|': [null, null, '=+', '\\|', null, '}]', null], // 2
            'q': ['1!', '2@', null, 'qQ', 'wW', null, 'aA'], // 4
            'Q': ['1!', '2@', null, 'qQ', 'wW', null, 'aA'], // 4
            'w': ['2@', '3#', 'qQ', 'wW','eE', 'aA', 'sS'], // 6
            'W': ['2@', '3#', 'qQ', 'wW','eE', 'aA', 'sS'], // 6
            'e': ['3#', '4$', 'wW', 'eE', 'rR', 'sS', 'dD'], // 6
            'E': ['3#', '4$', 'wW', 'eE', 'rR', 'sS', 'dD'], // 6
            'r': ['4$', '5%', 'eE', 'rR', 'tT', 'dD', 'fF'], // 6
            'R': ['4$', '5%', 'eE', 'rR', 'tT', 'dD', 'fF'], // 6
            't': ['5%', '6^', 'rR', 'tT', 'yY', 'fF', 'gG'], // 6
            'T': ['5%', '6^', 'rR', 'tT', 'yY', 'fF', 'gG'], // 6
            'y': ['6^', '7&', 'tT', 'yY', 'uU', 'gG', 'hH'], // 6
            'Y': ['6^', '7&', 'tT', 'yY', 'uU', 'gG', 'hH'], // 6
            'u': ['7&', '8*', 'yY', 'uU', 'iI', 'hH', 'jJ'], // 6
            'U': ['7&', '8*', 'yY', 'uU', 'iI', 'hH', 'jJ'], // 6
            'i': ['8*', '9(', 'uU', 'iI', 'oO', 'jJ', 'kK'], // 6
            'I': ['8*', '9(', 'uU', 'iI', 'oO', 'jJ', 'kK'], // 6
            'o': ['9(', '0)', 'iI', 'oO', 'pP', 'kK', 'lL'], // 6
            'O': ['9(', '0)', 'iI', 'oO', 'pP', 'kK', 'lL'], // 6
            'p': ['0)', '-_', 'oO', 'pP', '[{', 'lL', ':;'], // 6
            'P': ['0)', '-_', 'oO', 'pP', '[{', 'lL', ':;'], // 6
            '[': ['-_', '=+', 'pP', '[{', ']}', ':;', '\'"'], // 6
            '{': ['-_', '=+', 'pP', '[{', ']}', ':;', '\'"'], // 6
            ']': ['=+', '\\|', '[{', ']}', null, '\'"', null], // 4
            '}': ['=+', '\\|', '[{', ']}', null, '\'"', null], // 4
            'a': ['qQ', 'wW', null, 'aA', 'sS', null, 'zZ'], // 4
            'A': ['qQ', 'wW', null, 'aA', 'sS', null, 'zZ'], // 4
            's': ['wW', 'eE', 'aA', 'sS', 'dD', 'zZ', 'xX'], // 6
            'S': ['wW', 'eE', 'aA', 'sS', 'dD', 'zZ', 'xX'], // 6
            'd': ['eE', 'rR', 'sS', 'dD', 'fF', 'xX', 'cC'], // 6
            'D': ['eE', 'rR', 'sS', 'dD', 'fF', 'xX', 'cC'], // 6
            'f': ['rR', 'tT', 'dD', 'fF', 'gG', 'cC', 'vV'], // 6
            'F': ['rR', 'tT', 'dD', 'fF', 'gG', 'cC', 'vV'], // 6
            'g': ['tT', 'yY', 'fF', 'gG', 'hH', 'vV', 'bB'], // 6
            'G': ['tT', 'yY', 'fF', 'gG', 'hH', 'vV', 'bB'], // 6
            'h': ['yY', 'uU', 'gG', 'hH', 'jJ', 'bB', 'nN'], // 6
            'H': ['yY', 'uU', 'gG', 'hH', 'jJ', 'bB', 'nN'], // 6
            'j': ['uU', 'iI', 'hH', 'jJ', 'kK', 'nN', 'mM'], // 6
            'J': ['uU', 'iI', 'hH', 'jJ', 'kK', 'nN', 'mM'], // 6
            'k': ['iI', 'oO', 'jJ', 'kK', 'lL', 'mM', ',;'], // 6
            'K': ['iI', 'oO', 'jJ', 'kK', 'lL', 'mM', ',;'], // 6
            'l': ['oO', 'pP', 'kK', 'lL', ':;', ',<', '.>'], // 6
            'L': ['oO', 'pP', 'kK', 'lL', ':;', ',<', '.>'], // 6
            ':': ['pP', '[{', 'lL', ':;', '\'"', '.>', '?/'], // 6
            ';': ['pP', '[{', 'lL', ':;', '\'"', '.>', '?/'], // 6
            '\'': ['[{', ']}', ':;', '\'"', null, '?/', null], // 5
            '"': ['[{', ']}', ':;', '\'"', null, '?/', null], // 5
            'z': ['aA', 'sS', null, 'zZ', 'xX', null, null], // 4
            'Z': ['aA', 'sS', null, 'zZ', 'xX', null, null], // 4
            'x': ['sS', 'dD', 'zZ', 'xX', 'cC', null, null], // 4
            'X': ['sS', 'dD', 'zZ', 'xX', 'cC', null, null], // 4
            'c': ['dD', 'fF', 'xX', 'cC', 'vV', null, null], // 4
            'C': ['dD', 'fF', 'xX', 'cC', 'vV', null, null], // 4
            'v': ['fF', 'gG', 'cC', 'vV', 'bB', null, null], // 4
            'V': ['fF', 'gG', 'cC', 'vV', 'bB', null, null], // 4
            'b': ['gG', 'hH', 'vV', 'bB', 'nN', null, null], // 4
            'B': ['gG', 'hH', 'vV', 'bB', 'nN', null, null], // 4
            'n': ['hH', 'jJ', 'bB', 'nN', 'mM', null, null], // 4
            'N': ['hH', 'jJ', 'bB', 'nN', 'mM', null, null], // 4
            'm': ['jJ', 'kK', 'nN', 'mM', ',<', null, null], // 4
            'M': ['jJ', 'kK', 'nN', 'mM', ',<', null, null], // 4
            ',': ['kK', 'lL', 'mM', ',<', '.>', null, null], // 4
            '<': ['kK', 'lL', 'mM', ',<', '.>', null, null], // 4
            '.': ['lL', ':;', ',<', '.>', '/?', null, null], // 4
            '>': ['lL', ':;', ',<', '.>', '/?', null, null], // 4
            '/': [':;', '\'"', '.>', '/?', null, null, null], // 3
            '?': [':;', '\'"', '.>', '/?', null, null, null] // 3
        }, 4.571428571),
        QWERTZNumpad: new Keyboard({
            '0': ['1', '2', ',', null], // 2
            ',': ['3', '0', null, null], // 2
            '1': ['4', null, '2', '0'], // 3
            '2': ['5', '1', '3', '0'], // 4
            '3': ['3', '2', null, ','], // 3
            '4': ['7', null, '5', '1'], // 3
            '5': ['8', '4', '6', '2'], // 4
            '6': ['9', '5', '6', '2'], // 4
            '7': [null, null, '8', '4'], // 2
            '8': ['/', '7', '9', '5'], // 4
            '9': ['*', '8', '+', '6'], // 4
            '+': ['-', '9', '6', null], // 3
            '/': [null, null, '*', '8'], // 2
            '*': [null, '/', '-', '9'], // 3
            '-': [null, '*', null, '+'] // 2
        }, 3),
        QWERTYNumpad: new Keyboard({
            '0': ['1', '2', ',', null], // 2
            ',': ['3', '0', null, null], // 2
            '1': ['4', null, '2', '0'], // 3
            '2': ['5', '1', '3', '0'], // 4
            '3': ['6', '2', null, ','], // 3
            '4': ['7', null, '5', '1'], // 3
            '5': ['8', '4', '6', '2'], // 4
            '6': ['9', '5', '+', '2'], // 4
            '7': [null, null, '8', '4'], // 2
            '8': ['/', '7', '9', '5'], // 4
            '9': ['*', '8', '+', '6'], // 4
            '+': ['-', '9', '6', null], // 3
            '/': [null, null, '*', '8'], // 2
            '*': [null, '/', '-', '9'], // 3
            '-': [null, '*', null, '+'] // 2
        }, 3)
    },

    /**
     * Cache will hold all collected matches of the last score estimation.
     *
     * @property cache
     * @type {object}
     */
    cache: {

        /**
         * Clear the cache.
         */
        clear: function() {
            for (var key in this) {
                if (key !== 'set' && key !== 'clear') {
                    this[key] = undefined;
                }
            }
        },

        /**
         * Set cache data.
         *
         * @param {string} key
         * @param {mixed} value
         */
        set: function(key, value) {
            this[key] = value;
        }
    },

    /**
     * Represent log of abse 2.
     *
     * @param {number} x
     * @return {number}
     */
    lg: function(x) {
        return Math.log(x)/Math.log(2);
    },

    /**
     * Get the time to crack.
     *
     * @param {number} entropy
     * @param {number} cores
     * @return {number}
     */
    calculateAverageTimeToCrack: function(entropy, cores) {
        return 0.5*Math.pow(2, entropy)*0.005/cores;
    },

    /**
     * Calculates a naive score ased on the brute force entropy.
     *
     * @param {string} password
     * @return {number}
     */
    calculateBruteForceEntropy: function() {
        var base = 0;
        var any = false;

        if (this.regex['lower'].test(this.password)) {
            base += this.LOWER;
            any = true;
        }

        if (this.regex['upper'].test(this.password)) {
            base += this.UPPER;
            any = true;
        }

        if (this.regex['number'].test(this.password)) {
            base += this.NUMBER;
            any = true;
        }

        if (this.regex['punctuation'].test(this.password)) {
            base += this.PUNCTUATION;
            any = true;
        }

        if (this.regex['other'].test(this.password)) {
            base += this.OTHER;
        }

        var naiveEntropy = this.lg(base)*this.password.length;
        this.cache.set('naiveEntropy', naiveEntropy);

        return naiveEntropy;
    },

    /**
     * Gather matches using the given sources.
     *
     * @param {array} options
     * @param {boolean} append
     * @returns {number}
     */
    collectMatches: function(options, append) {

        // Default parameters: use default options or append to default options.
        options = (options === undefined) ? [] : options;
        append = (append === undefined) ? true : append;

        if (append === true && this.options !== undefined) {
            options = options.concat(this.options);
        }

        var matches = [];

        // First collect all possible matches.
        for (var i = 0; i < options.length; i++) {
            var optionMatches = [];

            if (!'type' in options[i]) {
                continue;
            }

            switch (options[i]['type']) {
                // Dictionary used for word lists, passwords, names, cities etc.
                case 'dictionary':
                    if ('dictionary' in options[i]) {
                        optionMatches = this.collectDictionaryMatches(options[i]['dictionary']);

                        if ('leet' in options[i] && options[i]['leet'] === true) {
                            var leetMatches = this.collectLeetSpeakMatches(options[i]['dictionary']);
                            optionMatches = optionMatches.concat(leetMatches);
                        }
                    }
                    break;
                case 'keyboard':
                    if ('keyboard' in options[i]) {
                        optionMatches = this.collectKeyboardMatches(options[i]['keyboard']);
                    }
                    break;
                case 'repetition':
                    optionMatches = this.collectRepetitionMatches();
                    break;
                case 'sequences':
                    optionMatches = this.collectSequenceMatches();
                    break;
                case 'dates':
                    optionMatches = this.collectDateMatches();
                    break;
            }

            if ('key' in options[i]) {
                this.cache.set(options[i]['key'], optionMatches);
            }

            matches = matches.concat(optionMatches);
        }

        return matches;
    },

    /**
     * Calculate entropy score.
     *
     * @param {array} options
     * @param {boolean} append
     * @return {number}
     */
    calculateEntropyScore: function(options, append) {
        var matches = this.collectMatches(options, append);

        var entropies = [];
        var entropyMatches = [];
        var currentEntropy = this.calculateBruteForceEntropy(this.password);

        // Minimize entropy as far as possible. This approach assumes the attacker
        // to know as much as possible about the form of the password.
        for (var i = 0; i < this.password.length; i++) {

            // Add current character as match.
            // If the character is not found within a pattern this will be taken.
            matches[matches.length] = {
                pattern: this.password[i],
                entropy: this.calculateBruteForceEntropy(this.password[i]),
                start: i,
                end: i,
                type: 'letter'
            };

            // Set to infinity - we want to minimize the entropy.
            entropies[i] = Number.POSITIVE_INFINITY;

            for (var j = 0; j < matches.length; j++) {
                var start = matches[j]['start'];
                var end = matches[j]['end'];

                if (end !== i) {
                    continue;
                }

                var currentEntropy = matches[j]['entropy'];
                if (start > 0) {
                    currentEntropy += entropies[start - 1];
                }

                if (currentEntropy < entropies[i]) {
                    entropies[i] = currentEntropy;
                    entropyMatches[i] = matches[j];
                }
            }
        }

        // Gather the used matches.
        var minimumMatches = [];
        var i = this.password.length - 1;
        while (i >= 0) {
            if (entropyMatches[i]) {
                minimumMatches[minimumMatches.length] = entropyMatches[i];
                i = entropyMatches[i]['start'] - 1;
            }
            else {
                i--;
            }
        }

        this.cache.set('minimumMatches', minimumMatches);
        this.cache.set('entropy', entropies[this.password.length - 1]);

        return entropies[this.password.length - 1];
    },

    /**
     * Check whether string ocurres in the dictionary.
     *
     * @param {array} dictionary
     * @return {array}
     */
    collectDictionaryMatches: function(dictionary) {
        var matches = [];
        for (var i = 0; i < this.password.length; i++) {
            for (var j = i; j < this.password.length; j++) {
                var original = this.password.substring(i, j + 1);
                var string = original.toLowerCase();
                var reversed = this.getReversedString(string);

                // Simple match.
                if (string in dictionary) {
                    if (dictionary[string]) {
                        matches[matches.length] = {
                            pattern: original,
                            entropy: this.calculateDictionaryEntropy(original, string, dictionary[string]),
                            start: i,
                            end: j,
                            type: 'dictionary'
                        };
                    }
                }

                // Reversed match.
                if (reversed in dictionary) {
                    if (dictionary[reversed]) {
                        matches[matches.length] = {
                            pattern: original,
                            entropy: this.calculateReversedDictionaryEntropy(original, string, dictionary[reversed]),
                            start: i,
                            end: j,
                            type: 'dictionary'
                        };
                    }
                }
            }
        }

        return matches;
    },

    /**
     * Calculate entropy for dictionary.
     *
     * @param {string} original
     * @param {string} word
     * @param {number} rank
     * @return {number}
     */
    calculateDictionaryEntropy: function(original, word, rank) {
        if (this.regex['lower'].test(original) && this.regex['upper'].test(original)) {
            // First upper only is simple capitalization.
            if (this.regex['upperFirstOnly'].test(original)) {
                return this.lg(rank) + 1;
            }
            else {
                // Base entropy plus entropy of possiblities to choose between upper and lower per letter.
                return this.lg(rank) + original.length; // = lg(rank) + lg(2^original.length) = lg(rank) + lg(2)*original.length
            }
        }

        return this.lg(rank);
    },

    /**
     * Calculate dictionary entropy for reversed.
     *
     * @param {string} original
     * @param {string} word
     * @param {number} rank
     * @return {number}
     */
    calculateReversedDictionaryEntropy: function(original, word, rank) {
        // Two possibilities: reversed or not => 1 bit of extra entropy.
        return this.calculateDictionaryEntropy(original, word, rank) + 1;
    },

    /**
     * Search all leet speak matches.
     *
     * @param {array} dictionary
     * @return {array}
     */
    collectLeetSpeakMatches: function(dictionary) {
        var matches = [];

        var subs = this.collectLeetSpeakSubstitutions(this.password);

        for (var k = 0; k < subs.length; k++) {
            for (var i = 0; i < subs[k].length; i++) {
                for (var j = i; j < subs[k].length; j++) {
                    var original = subs[k].substring(i, j + 1);
                    var string = original.toLowerCase();
                    var reversed = this.getReversedString(string);

                    var originalPattern = this.password.substring(i, j + 1);

                    if (string in dictionary) {
                        if (dictionary[string]) {
                            matches[matches.length] = {
                                pattern: originalPattern,
                                entropy: this.calculateLeetSpeakEntropy(this.password.substring(i, j + 1), string, dictionary[string]),
                                start: i,
                                end: j,
                                type: 'leet'
                            };
                        }
                    }

                    if (reversed in dictionary) {
                        if (dictionary[string]) {
                            matches[matches.length] = {
                                pattern: originalPattern,
                                entropy: this.calculateReversedLeetSpeakEntropy(this.password.substring(i, j + 1), string, dictionary[string]),
                                start: i,
                                end: j,
                                type: 'leet'
                            };
                        }
                    }
                }
            }
        }

        return matches;
    },

    /**
     * Calculate dictionary entropy for leet speak.
     *
     * @param {string} original
     * @param {string} word
     * @param {number} rank
     * @return {number}
     */
    calculateLeetSpeakEntropy: function(original, word, rank) {
        // Simple apporach: calculate possiblities of leet speak substitutions.
        var possibilities = 1;
        for (var key in this.leet) {
            if (original.indexOf(key) >= 0) {
                // Add the possiblity to not substitute.
                possibilities *= (this.leet[key].length + 1);
            }
        }

        return this.calculateDictionaryEntropy(original, word, rank) + this.lg(possibilities);
    },

    /**
     * Calculate leet speak entropy for reversed.
     *
     * @param {string} original
     * @param {string} word
     * @param {number} rank
     * @return {number}
     */
    calculateReversedLeetSpeakEntropy: function(original, word, rank) {
        // Two possibilities: reversed or not => 1 bit of extra entropy.
        return this.calculateLeetSpeakEntropy(original, word, rank) + 1;
    },

    /**
     * Get all leet speak substitutions.
     *
     * Considering a leet speak substitution matrix with a row for each letter to be translated
     * we iterate over all columns giving one
     *
     * @return {array}
     */
    collectLeetSpeakSubstitutions: function() {

        var leet = {};
        for (var char in this.leet) {
            if (this.password.indexOf(char) >= 0) {
                leet[char] = this.leet[char];
            }
        }

        var recursiveSubstitutions = function(string) {
            if (string[0] in leet) {
                if (string.length === 1) {
                    return leet[string[0]];
                }
                else {
                    var substrings = recursiveSubstitutions(string.substring(1, string.length));

                    var subs = [];
                    for (var i = 0; i < substrings.length; i++) {
                        for (var j = 0; j < leet[string[0]].length; j++) {
                            subs[subs.length] = leet[string[0]][j] + substrings[i];
                        }
                    }

                    return subs;
                }
            }
            else {
                if (string.length === 1) {
                    return [string[0]];
                }
                else {
                    var substrings = recursiveSubstitutions(string.substring(1, string.length));

                    var subs = [];
                    for (var i = 0; i < substrings.length; i++) {
                        subs[subs.length] = string[0] + substrings[i];
                    }

                    return subs;
                }
            }
        };

        return recursiveSubstitutions(this.password);
    },

    /**
     * Get all matched paths on the given keyboard.
     *
     * @param {object} keyboard
     * @return {array}
     */
    collectKeyboardMatches: function(keyboard) {
        var matches = [];
        var currentPath = this.password[0];
        var currentTurns = 0;
        var currentStart = 0;

        // Keyboard automatically takes care of lower and upper case and special characters.
        for (var i = 0; i < this.password.length - 1; i++) {
            if (keyboard.areAdjacent(this.password[i], this.password[i + 1])) {
                currentPath += this.password[i + 1];
                if (this.password[i + 1] !== this.password[i]) {
                    currentTurns++;
                }
            }
            else if (currentPath.length > 1) { // only consider sequences longer than one
                matches[matches.length] = {
                    pattern: currentPath,
                    entropy: this.calculateKeyboardEntropy(currentPath, currentTurns, keyboard),
                    start: currentStart,
                    end: i,
                    type: 'keyboard'
                };

                currentPath = this.password[i + 1];
                currentTurns = 0;
                currentStart = i + 1;
            }
            else {
                currentPath = this.password[i + 1];
                currentTurns = 0;
                currentStart = i + 1;
            }
        }

        // Remember to add the last path.
        if (currentPath.length > 1) {
            matches[matches.length] = {
                pattern: currentPath,
                entropy: this.calculateKeyboardEntropy(currentPath, currentTurns, keyboard),
                start: currentStart,
                end: this.password.length - 1,
                type: 'keyboard'
            };
        }

        return matches;
    },

    /**
     * Calculate entropy for keyboard patterns.
     *
     * @param {string} original
     * @param {number} turns
     * @param {object} keyboard
     * @return {number}
     */
    calculateKeyboardEntropy: function(original, turns, keyboard) {
        // Initialization with 0 will not work because lg(0) is undefined.
        var possiblities = 1;

        if (this.regex['lower'].test(original[0])) {
            possiblities = this.LOWER;
        }
        else if (this.regex['upper'].test(original[0])) {
            possiblities = this.UPPER;
        }
        else if (this.regex['number'].test(original[0])) {
            possiblities = this.NUMBER;
        }
        else if (this.regex['punctuation'].test(original[0])) {
            possiblities = this.PUNCTUATION;
        }

        return this.lg(possiblities) + turns*this.lg(keyboard.averageNeighbours);
    },

    /**
     * Check for all repetitions.
     *
     * @return {array}
     */
    collectRepetitionMatches: function() {
        var matches = [];

        var singleMatches = this.password.match(this.regex['repetition']['single']) || [];
        for (var i = 0; i < singleMatches.length; i++) {
            matches[matches.length] = {
                pattern: singleMatches[i],
                entropy: this.calculateSingleRepetitionEntropy(singleMatches[i]),
                start: this.password.indexOf(singleMatches[i]),
                end: this.password.indexOf(singleMatches[i]) + singleMatches[i].length - 1,
                type: 'repetition'
            };
        }

        var groupMatches = this.password.match(this.regex['repetition']['group']) || [];
        for (var i = 0; i < groupMatches.length; i++) {
            matches[matches.length] = {
                pattern: groupMatches[i],
                entropy: this.calculateGroupRepetitionEntropy(groupMatches[i]),
                start: this.password.indexOf(groupMatches[i]),
                end: this.password.indexOf(groupMatches[i]) + groupMatches[i].length - 1,
                type: 'repetition'
            };
        }

        return matches;
    },

    /**
     * Calculate repetition entropy.
     *
     * @param {string} original substring
     * @return {number}
     */
    calculateSingleRepetitionEntropy: function(original) {
        if (this.regex['number'].test(original)) {
            return this.lg(this.NUMBER*original.length);
        }
        if (this.regex['lower'].test(original) || this.regex['upper'].test(original)) {
            return this.lg(this.LOWER*original.length);
        }
        if (this.regex['punctuation'].test(original)) {
            return this.lg(this.PUNCTUATION*original.length);
        }

        return this.calculateBruteForceEntropy(original);
    },

    /**
     * Calculate repetition entropy for groups.
     *
     * @param {string} original substring
     * @return {number}
     */
    calculateGroupRepetitionEntropy: function(original) {

        // First determine the length of the repeated string.
        var result = this.regex['repetition']['group'].exec(original);
        var length = original.length;

        while (result !== null) {
            length = result[1].length;
            result = this.regex['repetition']['group'].exec(result[1]);
        }

        var possibilities = 0;
        if (this.regex['number'].test(original)) {
            possibilities += this.NUMBER;
        }
        if (this.regex['lower'].test(original) || this.regex['upper'].test(original)) {
            possibilities += this.LOWER;
        }
        if (this.regex['punctuation'].test(original)) {
            possibilities += this.PUNCTUATION;
        }

        return this.lg(possibilities*length);
    },

    /**
     * Check for sequences.
     *
     * @return {array}
     */
    collectSequenceMatches: function() {

        var lowerSeq = '';
        var lowerRevSeq = '';
        var numberSeq = '';
        var numberRevSeq = '';

        for (var i = 0; i < this.password.length; i++) {
            // At least two characters needed for a sequence.
            for (var j = i + 2; j <= this.password.length; j++) {
                var original = this.password.substring(i, j);
                var string = original.toLowerCase();
                var reversed = this.getReversedString(string);

                if (string.length === 0) {
                    continue;
                }

                // Check alphabetical sequence.
                if (this.sequences['lower'].indexOf(string) >= 0 && string.length > lowerSeq.length) {
                    lowerSeq = original;
                }
                if (this.sequences['lower'].indexOf(reversed) >= 0 && string.length > lowerRevSeq.length) {
                    lowerRevSeq = original;
                }

                // Check number sequence.
                if (this.sequences['numbers'].indexOf(string) >= 0 && string.length > numberSeq.length) {
                    numberSeq = original;
                }
                if (this.sequences['numbers'].indexOf(reversed) >= 0 && string.length > numberSeq.length) {
                    numberRevSeq = original;
                }
            }
        }

        var matches = [];
        if (lowerSeq.length > 0) {
            matches[matches.length] = {
                pattern: lowerSeq,
                entropy: this.calculateSequenceEntropy(lowerSeq),
                start: this.password.indexOf(lowerSeq),
                end: this.password.indexOf(lowerSeq) + lowerSeq.length - 1,
                type: 'sequence'
            };
        }
        if (lowerRevSeq.length > 0) {
            matches[matches.length] = {
                pattern: lowerRevSeq,
                entropy: this.calculateSequenceEntropy(lowerRevSeq),
                start: this.password.indexOf(lowerRevSeq),
                end: this.password.indexOf(lowerRevSeq) + lowerRevSeq.length - 1,
                type: 'sequence'
            };
        }
        if (numberSeq.length > 0) {
            matches[matches.length] = {
                pattern: numberSeq,
                entropy: this.calculateSequenceEntropy(numberSeq),
                start: this.password.indexOf(numberSeq),
                end: this.password.indexOf(numberSeq) + numberSeq.length - 1,
                type: 'sequence'
            };
        }
        if (numberRevSeq.length > 0) {
            matches[matches.length] = {
                pattern: numberRevSeq,
                entropy: this.calculateSequenceEntropy(numberRevSeq),
                start: this.password.indexOf(numberRevSeq),
                end: this.password.indexOf(numberRevSeq) + numberRevSeq.length - 1,
                type: 'sequence'
            };
        }

        return matches;
    },

    /**
     * Calculate entropy for sequence.
     *
     * @param {string} original substring
     * @return {number}
     */
    calculateSequenceEntropy: function(original) {
        if (this.regex['number'].test(original)) {
            return this.lg(original.length*this.NUMBER);
        }
        else if (this.regex['lowerOnly'].test(original)) {
            return this.lg(original.length*this.LOWER);
        }
        else if (this.regex['upperOnly'].test(original)) {
            return this.lg(original.length*this.LOWER);
        }
        else if (this.regex['upper'].test(original) && this.regex['upper'].test(original)) {
            return this.lg(original.length*(this.LOWER + this.UPPER));
        }

        return this.calculateBruteForceEntropy(original);
    },

    /**
     * Calculate entropy for reversed sequence.
     *
     * @param {string} original substring
     * @return {number}
     */
    calculateReversedSequenceEntropy: function(original) {
        return this.calculateSequenceEntropy(original) + 1;
    },

    /**
     * Get all matched dates.
     *
     * Single numbers will be identified as years or day-month combinations.
     * Full (and "half") dates will be recognized when using -./ as separator.
     *
     * @param {array}formats
     * @returns {array}
     */
    collectDateMatches: function() {
        var matches = [];
        for (var type in this.regex.date) {
            var regexMatches = this.password.match(this.regex.date[type]) || [];
            for (var i = 0; i < regexMatches.length; i++) {
                if (regexMatches[i].length > 0) {
                    var start = this.password.indexOf(regexMatches[i]);
                    matches[matches.length] = {
                        pattern: regexMatches[i],
                        entropy: this.calculateDateEntropy(regexMatches[i], type),
                        start: start,
                        end: start + regexMatches[i].length - 1,
                        type: 'date'
                    };
                }
            }
        }

        return matches;
    },

    /**
     * Calculate date entropy for all types.
     *
     * @param {string} original
     * @param {string} type
     * @return {number}
     */
    calculateDateEntropy: function(original, type) {
        switch (type) {
            case 'DMY':
            case 'MDY':
            case 'YDM':
            case 'YMD':
                return this.lg(this.DAYS*this.MONTHS*10*10);
                break;
            case 'DMYY':
            case 'MDYY':
            case 'YYDM':
            case 'YYMD':
                return this.lg(this.DAYS*this.MONTHS*this.YEARS);
                break;
            case 'DM':
                return this.lg(this.DAYS*this.MONTHS);
                break;
            case 'MY':
                return this.lg(this.MONTHS*10*10);
                break;
            case 'MYY':
                return this.lg(this.MONTHS*this.YEARS);
                break;
        }

        return this.calculateBruteForceEntropy(original);
    },

    /**
     * Reverse the given string.
     *
     * @param {string} string
     * @return {string} reversed
     */
    getReversedString: function(string) {
        return string.split('').reverse().join('');
    }
};

window.Score = Score;
