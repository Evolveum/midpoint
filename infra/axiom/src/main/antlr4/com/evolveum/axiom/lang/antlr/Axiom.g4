grammar Axiom;

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

file: SEP* item SEP* EOF;
item: itemName itemValue;
itemValue: (SEP+ argument)? SEP* (SEMICOLON | LEFT_BRACE SEP* (item SEP*)* RIGHT_BRACE);

prefixedName : (prefix COLON)? localName;

prefix : IDENTIFIER;
localName : IDENTIFIER;

argument : prefixedName | string;
string : singleQuoteString | doubleQuoteString | multilineString;

singleQuoteString : STRING_SINGLEQUOTE;
doubleQuoteString : STRING_DOUBLEQUOTE;
multilineString: STRING_MULTILINE_START (~('"""'))*'"""';

path: pathComponent ( '/' pathComponent)*;
pathComponent: itemName (pathValue)?;
pathDataItem: prefixedName;
pathInfraItem: '@' prefixedName;
pathValue: '[' argument ']';
