import ply.lex as lex


tokens = (
    'NODE',
    'MNODE',
    'DOT',
    'STAR',
    'COMMA',
    'NUMBER',
    'LPAREN',
    'RPAREN',
    'EXIST',
    'EQUAL',
    'COMPARISON',
    'AND',
    'NOT',
    'OR',
    'COR',
    'ADD',
    'LENGTH',
)

# Regular expression rules for simple tokens
t_LPAREN = r'\('
t_RPAREN = r'\)'
t_NODE = r'\w'
t_MNODE = r'`[\w\-]+`'
t_EXIST = r'exist'
t_EQUAL = r'equal'
t_COMPARISON = r'==|>=|>|<=|<'
t_NUMBER = r'\d+'
t_AND = r'and'
t_NOT = r'not'
t_OR = r'or'
t_DOT = r'\.'
t_STAR = r'\*'
t_COMMA = r','
t_COR = r'\|'
t_ADD = r'\+'
t_LENGTH = r'shortest'
t_ignore = ' \t'


def t_newline(t):
    r'\n+'
    t.lexer.lineno += len(t.value)


# Error handling rule
def t_error(t):
    print("error word:" + t.value)


# Build the lexer
lexer = lex.lex()
