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

import math
import networkx

from Planner.dvnet import DVNet
from Planner.parser import parser
from Planner._base import Port
from automata.regex.parser import get_regex_lexer, StringToken
import automata.regex.parser
import re

_NAME = "DPVNet"


def _parser_init():
    def _get_regex_lexer(input_symbols):
        lexer = get_regex_lexer(input_symbols)
        lexer.register_token(StringToken, r'[\u4e00-\u9fa5]')
        return lexer

    automata.regex.parser.get_regex_lexer = _get_regex_lexer


_parser_init()


class Planner:
    allow_char_regex = re.compile("^[a-zA-Z0-9]$")
    INF = math.inf

    def __init__(self):
        self.ports = {}  # type:dict[str, dict[str, set[Port]]]
        self.devices = set()
        self.node_num = {}
        self.rename_index = ord("\u4e00")
        self.rename_dict = {"[*]": "[*]"}
        self.rename_dict_reverse = {"[*]": "[*]"}
        self._edge_map = {}  # type:dict[str,int]
        self.parser = parser
        self.topologies = []
        self.graph = networkx.Graph()
        self.min_table = None
        self.ingresses = []
        self.node_to_id = {}
        self.destination = None

    def add_topologies(self, edges):
        for edge in edges:
            self._add_topology(edge)

    def _add_topology(self, edge):
        d1 = edge[0]  # type:str
        d2 = edge[1]
        self._add_port_link(d1, d2, d2, d1)

    def read_topology_from_file(self, filename):
        with open(filename, mode="r") as f:
            line = f.readline()
            while line:
                token = line.strip().split(" ")

                if len(token) == 4:
                    self._add_port_link(token[0], token[1], token[2], token[3])
                elif len(token) == 2:
                    self._add_port_link(token[0], token[0] + '->' + token[1], token[1], token[1] + '->' + token[0])
                else:
                    print(str(token) + " can not parsed")
                line = f.readline()

    def rename(self, old_name):
        if old_name in self.rename_dict:
            return self.rename_dict[old_name]
        if re.match(Planner.allow_char_regex, old_name) is None:
            name = chr(self.rename_index)
            self.rename_index += 1
        else:
            name = old_name
        self.rename_dict[old_name] = name
        self.rename_dict_reverse[name] = old_name
        return name

    def _add_port_link(self, device1, port1, device2, port2):
        self.add_device(device1)
        self.add_device(device2)
        p1 = Port(device1, port1)
        p2 = Port(device2, port2)
        p1.link(p2)
        self.ports[device1].setdefault(device2, set()).add(p2)
        self.ports[device2].setdefault(device1, set()).add(p1)
        self._add_edge(device1, device2)
        d1 = self.rename(device1)
        d2 = self.rename(device2)
        self.topologies.append((d1, d2))

    def add_device(self, device):
        if device in self.devices:
            return
        self.graph.add_node(device)
        self.devices.add(device)
        self.ports.setdefault(device, {})

    def _add_edge(self, device1, device2):
        if device1 > device2:
            device1, device2 = device2, device1
        edge_name = device1+"-"+device2
        if edge_name not in self._edge_map:
            self._edge_map[edge_name] = len(self._edge_map)+1
            self.graph.add_edge(device1, device2)

    def mark_state_index(self, states):
        for state in states:
            if state.device == "[*]":
                continue
            state.device = self.rename_dict_reverse[state.device]
        for device in self.devices:
            self.node_num.setdefault(device, 0)

        for state in states:
            if state.device == "[*]":
                continue
            state.index = self.node_num[state.device]
            self.node_num[state.device] += 1

    def output_puml(self, states_pair: list, output, hide_label=False):
        with open(output, mode="w", encoding="utf-8") as f:
            f.write("@startuml\n")
            count = 0
            for packet_space, ingress, match, path_exp, states in states_pair:
                self.mark_state_index(states)
                f.write("state %s%d {\n" % (_NAME, count))
                f.write("'packet_space:%s\n" % packet_space)
                f.write("'ingress:%s\n" % ingress)
                f.write("'match: %s\n" % match)
                f.write("'path: %s\n" % path_exp)
                for state in states:
                    for e in state.edge_out:
                        f.write("%s-->%s:%s\n" % (state.get_name(), e.dst.get_name(), e.get_label()))
                    if state.is_accept:
                        f.write("%s-->[*]:[[]]\n" % (state.get_name()))
                f.write("}\n")
                count += 1

            f.write("@enduml\n")

    def get_min_table(self):
        if self.min_table is None:
            self.min_table = networkx.floyd_warshall_numpy(self.graph)
            self.node_to_id = {k: i for i, k in enumerate(self.graph.nodes)}
        return self.min_table

    def parse_filter(self, filter: str):
        shortest = 9999
        func = lambda a: True

        if "shortest" in filter:
            if len(self.ingresses) != 1:
                print("the number of ingress must be 1.")
            shortest = self.get_min_table()[self.node_to_id[self.ingresses[0]]][self.node_to_id[self.destination]]
            if shortest == self.INF:
                return self.INF, func
            filter = filter.replace("shortest", str(int(shortest)), 1)
            number = eval(filter.lstrip("<>="))
            if filter.startswith(">="):
                func = lambda a: a >= number
            elif filter.startswith("=="):
                func = lambda a: a <= number
            elif filter.startswith("<="):
                func = lambda a: a <= number
            elif filter.startswith("<"):
                func = lambda a: a < number
            elif filter.startswith(">"):
                func = lambda a: a > number
            else:
                print("can't parse filter:" + filter)
        return shortest, func

    def gen(self, output, packet_space, ingresses, behavior_raw, fault_scenes=None):

        behavior = parser.parse(behavior_raw)
        if behavior is None:
            print("error in parse behavior!")
            return None

        self.ingresses = ingresses
        self.destination = behavior["path"]["path_exp"][-1]

        shortest, length_filter = 9999, lambda l: True

        if "length_filter" in behavior["path"]:
            shortest, length_filter = self.parse_filter(behavior["path"]["length_filter"])
            if shortest == self.INF:
                return []

        path_exp = "".join([self.rename_dict[i] if i in self.rename_dict else i for i in behavior["path"]["path_exp"]])
        new_ingress = [self.rename_dict[ingress] for ingress in ingresses]
        dvnet = DVNet()
        dvnet.add_topologies(self.topologies)
        k_dict = {
            "any_one": 1,
            "any_two": 2,
            "any_three": 3,
            None: 0,
        }
        k = 0 if fault_scenes not in k_dict else k_dict[fault_scenes]
        states = dvnet.gen_dvnet(path_exp, new_ingress, k, length_filter, False, shortest_length=shortest)
        if output:
            self.output_puml([(packet_space, ingresses, behavior["match"], "".join(behavior["path"]["path_exp"]), states)], output, True)
        else:
            return states

    def gen_all_pairs_reachability(self, output):
        total_states = []
        for device1 in self.devices:
            for device2 in self.devices:
                if device1 == device2:
                    continue
                states = self.gen(
                    None, device2, [device1],  r"(exist >= 1, (`%s`.*`%s` , (<= shortest+2)))" % (device1, device2)
                )
                if states:
                    total_states.append((
                        device2, [device1], "exists >= 1", "%s.*%s" % (device1, device2), states
                    ))
        self.output_puml(total_states, output, True)


if __name__ == "__main__":
    packet_space = "D"
    ingress = ["--S"]
    planner = Planner()
    planner.add_topologies((("--S", "A"), ("A", "B"), ("A", "W"), ("B", "W"), ("B", "C"), ("C", "W"), ("C", "D"), ("D", "W")))
    planner.gen(None, packet_space, ingress, "(equal, (`--S`.*D , (<= shortest+2)))", fault_scenes="any_two")
    # planner.gen_all_pairs_reachability("1.puml")
