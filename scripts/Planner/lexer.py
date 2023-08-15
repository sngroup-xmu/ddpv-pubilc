
#  This program is free software: you can redistribute it and/or modify it under the terms of
#   the GNU General Public License as published by the Free Software Foundation, either
#    version 3 of the License, or (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful, but WITHOUT ANY
#   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
#    PARTICULAR PURPOSE. See the GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License along with this
#   program. If not, see <https://www.gnu.org/licenses/>.
#
#  Authors: Chenyang Huang (Xiamen University) <xmuhcy@stu.xmu.edu.cn>
#           Qiao Xiang     (Xiamen University) <xiangq27@gmail.com>
#           Ridi Wen       (Xiamen University) <23020211153973@stu.xmu.edu.cn>
#           Yuxin Wang     (Xiamen University) <yuxxinwang@gmail.com>

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
