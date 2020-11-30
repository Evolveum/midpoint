grammar AxiomQuery;

SEMICOLON : ';';
LEFT_BRACE : '{';
RIGHT_BRACE : '}';
COLON : ':';
PLUS : '+';
LINE_COMMENT :  [ \n\r\t]* ('//' (~[\r\n]*)) [ \n\r\t]* -> skip;
SEP: [ \n\r\t]+;
IDENTIFIER : [a-zA-Z_][a-zA-Z0-9_\-]*;

fragment SQOUTE : '\'';
fragment DQOUTE : '"';

fragment ESC : '\\';

STRING_SINGLEQUOTE: SQOUTE ((ESC SQOUTE) | ~[\n'])* SQOUTE;
STRING_DOUBLEQUOTE: DQOUTE ((ESC DQOUTE) | ~[\n"])* DQOUTE;
STRING_MULTILINE_START: '"""' ('\r')? '\n';



//statement : SEP* identifier SEP* (argument)? SEP* (SEMICOLON | LEFT_BRACE SEP* (statement)* SEP* RIGHT_BRACE SEP*) SEP*;
itemName: infraName | dataName;
dataName: prefixedName;
infraName: '@' prefixedName;

prefixedName : (prefix? COLON)? localName;

prefix : IDENTIFIER;
localName : IDENTIFIER;

argument : prefixedName | string;
string : singleQuoteString | doubleQuoteString | multilineString;

singleQuoteString : STRING_SINGLEQUOTE;
doubleQuoteString : STRING_DOUBLEQUOTE;
multilineString: STRING_MULTILINE_START (~('"""'))*'"""';

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


// TODO: Grammar seems pretty general, filter could be renamed to condition?

filterName: itemName | filterNameAlias;

// Currently value could be string or path
valueSpecification: string | path;
negation: 'not' | 'NOT';
// Filter could be Value filter or Logic Filter
filter: SEP* simpleFilter | logicFilter;
// AndFilter and OrFilter have special syntax since they allow chaining
logicFilter: andFilter | orFilter;
// Nested Filter could be value filter or subfilter
nestedFilter: simpleFilter | subfilter;
andFilter: nestedFilter (SEP+ 'and' SEP+ nestedFilter)+;
orFilter: nestedFilter (SEP+ 'or' SEP+ nestedFilter)+;

subfilter: '(' SEP* filter SEP* ')';
simpleFilter: path (SEP+ negation)? SEP+ filterName SEP+ (nestedFilter | valueSpecification);



