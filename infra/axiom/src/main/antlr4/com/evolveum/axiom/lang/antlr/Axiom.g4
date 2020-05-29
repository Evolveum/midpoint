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


//fragment SUB_STRING : ('"' (ESC | ~["])*? '"') | ('\'' (ESC | ~['])* '\'');
//fragment ESC : '\\' (["\\/bfnrt] | UNICODE);
//fragment UNICODE : 'u' HEX HEX HEX HEX;
//fragment HEX : [0-9a-fA-F] ;
//STRING: ((~( '\r' | '\n' | '\t' | ' ' | ';' | '{' | '"' | '\'' | '}' | '/' | '+')~( '\r' | '\n' | '\t' | ' ' | ';' | '{' | '}' )* ) | SUB_STRING );

fragment ESC : '\\';

STRING_SINGLEQUOTE: SQOUTE ((ESC SQOUTE) | ~[\n'])* SQOUTE;
STRING_DOUBLEQUOTE: DQOUTE ((ESC DQOUTE) | ~[\n"])* DQOUTE;
//STRING_MULTILINE: '"""' (ESC | ~('"""'))* '"""';

statement : SEP* identifier SEP* (argument)? SEP* (SEMICOLON | LEFT_BRACE SEP* (statement)* SEP* RIGHT_BRACE SEP*) SEP*;

identifier : (prefix COLON)? localIdentifier;
prefix : IDENTIFIER;
localIdentifier : IDENTIFIER;


// argument : STRING (SEP* PLUS SEP* STRING)* | IDENTIFIER;
argument : identifier | string;
string : singleQuoteString | doubleQuoteString | multilineString;


singleQuoteString : STRING_SINGLEQUOTE;
doubleQuoteString : STRING_DOUBLEQUOTE;
multilineString: '"""\n' (~('"""'))*'"""';



