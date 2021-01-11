grammar AxiomQuery;

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

STRING_SINGLEQUOTE: SQOUTE ((ESC SQOUTE) | ~[\n'])* SQOUTE;
STRING_DOUBLEQUOTE: DQOUTE ((ESC DQOUTE) | ~[\n"])* DQOUTE;
STRING_MULTILINE_START: '"""' ('\r')? '\n';


//statement : SEP* identifier SEP* (argument)? SEP* (SEMICOLON | LEFT_BRACE SEP* (statement)* SEP* RIGHT_BRACE SEP*) SEP*;
itemName: prefixedName #dataName
    | '@' prefixedName #infraName;


prefixedName: (prefix=IDENTIFIER COLON)? localName=IDENTIFIER
    | (prefix=IDENTIFIER)? COLON localName=(AND_KEYWORD | NOT_KEYWORD | OR_KEYWORD);


argument : prefixedName | string;
string : STRING_SINGLEQUOTE #singleQuoteString 
    | STRING_DOUBLEQUOTE #doubleQuoteString
    | STRING_MULTILINE_START (~('"""'))*'"""' # multilineString;

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
valueSpecification: string | path;
negation: NOT_KEYWORD;
// Filter could be Value filter or Logic Filter

filter: left=filter (SEP+ AND_KEYWORD SEP+ right=filter) #andFilter
           | left=filter (SEP+ OR_KEYWORD SEP+ right=filter) #orFilter
           | itemFilter #genFilter
           | subfilter #subFilter;


subfilter: '(' SEP* filter SEP* ')';
itemFilter: path (SEP+ negation)? SEP+ filterName (matchingRule)? (SEP+ (subfilterOrValue))?;
subfilterOrValue : subfilter | valueSpecification;


