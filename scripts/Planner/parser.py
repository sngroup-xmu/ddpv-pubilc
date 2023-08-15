

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

import ply.yacc as yacc
from Planner.lexer import tokens


def p_behavior(p):
    r"""behavior : match_op COMMA path_exp
                 | LPAREN behavior RPAREN"""
    if p[1] == "(":
        p[0] = p[2]
    else:
        p[0] = {
            "match": p[1],
            "path": p[3]
        }


def p_match_op_equal(p):
    r"""match_op : EQUAL
                 | EXIST COMPARISON NUMBER"""
    if len(p) == 2:
        p[0] = p[1]
    else:
        p[0] = "%s %s %s" % (p[1], p[2], p[3])


def p_path_exp(p):
    r"""path_exp : LPAREN _path_exp RPAREN
                 | _path_exp
                 | LPAREN _path_exp COMMA length_filter RPAREN
    """
    if len(p) == 2:
        p[0] = {
            "path_exp": p[1],
        }
    elif len(p) == 4:
        p[0] = {
            "path_exp": p[2],
        }
    else:
        p[0] = {
            "path_exp": p[2],
            "length_filter": p[4],
        }


def p_length_filter(p):
    r"""length_filter : LPAREN _length_filter RPAREN
                      | length_filter"""
    if len(p) == 2: p[0] = p[1]
    elif len(p) == 4: p[0] = p[2]


def p__length_filter(p):
    r"""_length_filter : COMPARISON NUMBER
                       | COMPARISON LENGTH
                       | COMPARISON LENGTH ADD NUMBER"""
    p[0] = " ".join(p[1:])


def p__path_exp(p):
    r"""_path_exp : _path_exp term
                 | term """
    if len(p) == 2:
        p[0] = [p[1]]
    else:
        p[0] = p[1] + [p[2]]


def p_term(p):
    r"""term : NODE
             | MNODE
             | STAR
             | DOT
             | COR
             | AND
             | NOT
             | OR"""
    p[0] = p[1].strip("`")


def p_error(p):
    print("Syntax error in input: '" + p.value + "'")


parser = yacc.yacc(start='behavior')

