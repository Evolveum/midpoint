lexer grammar AxiomStatementLexer;

tokens {
    SEMICOLON,
    LEFT_BRACE,
    RIGHT_BRACE,
    SEP,
    IDENTIFIER,
    COLON,
    PLUS
}

SEMICOLON : ';' -> type(SEMICOLON);
LEFT_BRACE : '{' -> type(LEFT_BRACE);
RIGHT_BRACE : '}' -> type(RIGHT_BRACE);
COLON : ':' -> type(COLON);
PLUS : '+' -> type(PLUS);

LINE_COMMENT :  [ \n\r\t]* ('//' (~[\r\n]*)) [ \n\r\t]* -> skip;

START_BLOCK_COMMENT : '/*' ->pushMode(BLOCK_COMMENT_MODE), skip;

SEP: [ \n\r\t]+ -> type(SEP);
IDENTIFIER : [a-zA-Z_/][a-zA-Z0-9_\-.:/]* -> type(IDENTIFIER);

fragment SUB_STRING : ('"' (ESC | ~["])*? '"') | ('\'' (ESC | ~['])* '\'');
fragment ESC : '\\' (["\\/bfnrt] | UNICODE);
fragment UNICODE : 'u' HEX HEX HEX HEX;
fragment HEX : [0-9a-fA-F] ;
STRING: ((~( '\r' | '\n' | '\t' | ' ' | ';' | '{' | '"' | '\'' | '}' | '/' | '+')~( '\r' | '\n' | '\t' | ' ' | ';' | '{' | '}' )* ) | SUB_STRING );

mode BLOCK_COMMENT_MODE;
END_BLOCK_COMMENT : '*/' -> popMode, skip;
BLOCK_COMMENT :  . -> skip;
