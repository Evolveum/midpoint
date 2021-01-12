grammar AxiomQuery;


nullLiteral: 'null';
booleanLiteral: 'true' | 'false';

intLiteral: INT;
floatLiteral: FLOAT;
stringLiteral : STRING_SINGLEQUOTE #singleQuoteString 
    | STRING_DOUBLEQUOTE #doubleQuoteString
    | STRING_MULTILINE_START (~('"""'))*'"""' # multilineString;


literalValue: 
      value=('true' | 'false') #booleanValue
    | value=INT #intValue
    | value=FLOAT #floatValue
    | stringLiteral #stringValue
    | 'null' #nullValue;

// endgrammar axiom literals

//statement : SEP* identifier SEP* (argument)? SEP* (SEMICOLON | LEFT_BRACE SEP* (statement)* SEP* RIGHT_BRACE SEP*) SEP*;

itemName: prefixedName #dataName
    | '@' prefixedName #infraName;


prefixedName: (prefix=IDENTIFIER COLON)? localName=IDENTIFIER
    | (prefix=IDENTIFIER)? COLON localName=(AND_KEYWORD | NOT_KEYWORD | OR_KEYWORD);


argument : prefixedName | literalValue;

variable: '$' itemName;
parent: '..';
// Path could start with ../ or context variable ($var) or item name
firstComponent: (parent ( '/' parent )*) | variable | pathComponent;

path: firstComponent ( '/' pathComponent)*;
pathComponent: itemName (pathValue)?;
pathValue: '[' argument ']';


// Aliases for basic filters (equals, less, greater, lessOrEquals, greaterOrEquals
//
filterNameAlias: '=' | '<' | '>' | '<=' | '>=';


filterName: prefixedName | filterNameAlias;

matchingRule: '[' prefixedName ']';


// Currently value could be string or path
valueSpecification: literalValue | path;




negation: NOT_KEYWORD;
// Filter could be Value filter or Logic Filter

filter: left=filter (SEP+ AND_KEYWORD SEP+ right=filter) #andFilter
           | left=filter (SEP+ OR_KEYWORD SEP+ right=filter) #orFilter
           | itemFilter #genFilter
           | subfilterSpec #subFilter;


subfilterSpec: '(' SEP* filter SEP* ')';
itemFilter: path (SEP+ negation)? SEP+ filterName (matchingRule)? (SEP+ (subfilterOrValue))?;
subfilterOrValue : subfilterSpec | valueSpecification;






// grammar AxiomLiterals;

SEMICOLON : ';';
LEFT_BRACE : '{';
RIGHT_BRACE : '}';
COLON : ':';
PLUS : '+';
LINE_COMMENT :  [ \n\r\t]* ('//' (~[\r\n]*)) [ \n\r\t]* -> skip;
SEP: [ \n\r\t]+;

AND_KEYWORD: 'and';
OR_KEYWORD: 'or';
NOT_KEYWORD: 'not';
IDENTIFIER : [a-zA-Z_][a-zA-Z0-9_\-]*;

fragment SQOUTE : '\'';
fragment DQOUTE : '"';

fragment ESC : '\\';
//fragment ESC: '\\' ( ["\\/bfnrt] | UNICODE);



STRING_SINGLEQUOTE: SQOUTE ((ESC SQOUTE) | ~[\n'])* SQOUTE;
STRING_DOUBLEQUOTE: DQOUTE ((ESC DQOUTE) | ~[\n"])* DQOUTE;
STRING_MULTILINE_START: '"""' ('\r')? '\n';



fragment UNICODE: 'u' HEX HEX HEX HEX;

fragment HEX: [0-9a-fA-F];

fragment NONZERO_DIGIT: [1-9];
fragment DIGIT: [0-9];
fragment FRACTIONAL_PART: '.' DIGIT+;
fragment EXPONENTIAL_PART: EXPONENT_INDICATOR SIGN? DIGIT+;
fragment EXPONENT_INDICATOR: [eE];
fragment SIGN: [+-];
fragment NEGATIVE_SIGN: '-';

FLOAT: INT FRACTIONAL_PART
    | INT EXPONENTIAL_PART
    | INT FRACTIONAL_PART EXPONENTIAL_PART
    ;

INT: NEGATIVE_SIGN? '0'
    | NEGATIVE_SIGN? NONZERO_DIGIT DIGIT*
    ;

