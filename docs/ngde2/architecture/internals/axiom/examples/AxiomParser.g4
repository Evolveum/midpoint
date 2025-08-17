parser grammar AxiomStatementParser;

options {
    tokenVocab = AxiomStatementLexer;
}

statement : SEP* keyword SEP* (argument)? SEP* (SEMICOLON | LEFT_BRACE SEP* (statement)* SEP* RIGHT_BRACE SEP*) SEP*;
keyword : (IDENTIFIER COLON)? IDENTIFIER;

argument : STRING (SEP* PLUS SEP* STRING)* | IDENTIFIER;
