
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

import argparse
import heapq
import time
import bisect
import networkx
import numpy
from collections import deque

from Planner._base import Port, Node
from automata.fa.dfa import DFA
from automata.fa.nfa import NFA

from bitarray import bitarray, frozenbitarray


class State:
    dvnet = None  # type:None|DVNet

    @classmethod
    def get_state(cls, device, path, dfa_state):
        return cls.dvnet.state_map.setdefault(device, {}).setdefault(frozenset(path), State(device, path, dfa_state))

    def __init__(self, device, path, dfa_state):
        self.device = device  # type:str
        self.path = path  # type:list[str]
        self.edge_in = []  # type:list[StateEdge]
        self.edge_out = []  # type:list[StateEdge]
        self.conditions = set()  # type:set[frozenbitarray]
        self.edges = bitarray(self.dvnet.edge_count + 1)  # type:bitarray
        self.edges.setall(0)
        self.is_explored = False
        self.is_accept = False
        self.index = 0
        self.length = len(self.path)
        self.flag = 0
        self.dfa_state = dfa_state

    def get_length(self):
        return self.length

    def __str__(self):
        return self.device + "(" + "".join(self.path) + ")"

    def __repr__(self):
        return self.__str__()

    def get_name(self):
        if self.device == "[*]":
            return "[*]"
        return self.device + "." + str(self.index)

    def __lt__(self, other):
        return self.length < other.length


class StateEdge:
    def __init__(self, src: State, dst: State, origin_edge: int):
        self.src = src  # type:State
        self.dst = dst  # type:State
        self.conditions = set()  # type:set[frozenbitarray]
        self.origin_edge = origin_edge  # type:int

    def is_virtual(self):
        return self.origin_edge == 0

    def get_label(self):
        r = [i.search(1) for i in self.conditions]
        return r

    def __repr__(self):
        return self.src.device+"-"+self.dst.device


class DVNet:
    MAX_HOPS = 9999

    def get_zero_condition(self):
        if self.zero_condition is None:
            self.zero_condition = bitarray(self.edge_count + 1)
            self.zero_condition.setall(0)
            self.zero_condition = frozenbitarray(self.zero_condition)
        return self.zero_condition

    def __init__(self):
        self.devices = set()  # type:set[str]
        self.ports = {}  # type:dict[str, dict[str, set[Port]]]
        self._edge_map = {}  # type:dict[str,int]
        self._device_name = {}
        self.loop_free_dfa = None
        self.dfa = None  # type:DFA
        self.dfa_transitions = None  # type:dict[str,dict[str, str]]
        self.init_state = None  # type:State
        self._unexplored_states = {}  # type:dict[int, list[State]]
        self._terminal_states = []  # type:list[State]
        self._unreachable_conditions = set()  # type:set[frozenbitarray]
        self._explored_level = 0
        self.used_edges = set()
        self.reachable_table = {}  # type:dict[str,dict[str,0]]
        self.device_to_id = {}  # type:dict[str,int]
        self.node_num = {}  # type:dict[str, int]
        self.state_map = {}
        State.dvnet = self
        self.edge_count = 0
        self.zero_condition = None

    def _init(self):
        self.dfa = None
        self.dfa_transitions = None
        # self.destination = None
        self.init_state = None
        self._unexplored_states.clear()
        self._terminal_states.clear()
        self._unreachable_conditions = set()
        self._explored_level = 0
        self.state_map.clear()
        self.used_edges.clear()

    def add_topologies(self, edges):
        for edge in edges:
            self.add_topology(edge)

    def add_topology(self, edge):
        d1 = edge[0]
        d2 = edge[1]
        self._add_port_link(d1, d2, d2, d1)

    def add_device(self, device):
        if device in self.devices:
            return
        self.devices.add(device)
        self.ports.setdefault(device, {})
        self.device_to_id[device] = len(self.device_to_id)

    def _add_port_link(self, device1, port1, device2, port2):
        # if len(device1) > 1:
        #     device1 = self._device_name.setdefault(device1, chr(65 + len(self._device_name)))
        # if len(device2) > 1:
        #     device2 = self._device_name.setdefault(device2, chr(65 + len(self._device_name)))
        self.add_device(device1)
        self.add_device(device2)
        p1 = Port(device1, port1)
        p2 = Port(device2, port2)
        p1.link(p2)
        self.ports[device1].setdefault(device2, set()).add(p2)
        self.ports[device2].setdefault(device1, set()).add(p1)
        self._add_edge(device1, device2)

    def _add_edge(self, device1, device2):
        if device1 > device2:
            device1, device2 = device2, device1
        edge_name = device1+"-"+device2
        if edge_name not in self._edge_map:
            self._edge_map[edge_name] = len(self._edge_map)+1
            self.edge_count += 1

    def _get_edge_id(self, device1: str, device2: str):
        if device1 == "[*]" or device2 == "[*]":
            return 0
        if device1 > device2:
            device1, device2 = device2, device1
        edge_name = device1+"-"+device2
        return self._edge_map[edge_name]

    def read_topology_file(self, filename):
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

    def add_requirement(self, requirement, ingresses: list[str]):
        """
        step 1: 生成自动机。
        :param requirement: 需求，使用正则表达式来描述。
        :param ingresses: 输入端口，填设备名。
        :return:
        """
        self._init()
        nfa = NFA.from_regex(requirement, input_symbols=self.devices)
        self.dfa = DFA.from_nfa(nfa, minify=True)
        self.dfa_transitions = self.dfa.transitions
        # self.destination = requirement
        self.init_state = State.get_state("[*]", [], self.dfa.initial_state)
        self.init_state.is_explored = True
        init_queue = []
        for ingress in ingresses:
            ingress_state = State.get_state(ingress, [ingress], self.dfa_transitions[self.dfa.initial_state][ingress])
            init_queue.append(ingress_state)
            self._link_state(self.init_state, ingress_state)
        return init_queue

    def search(self, condition: frozenbitarray, search_queue: list[State], length_filter, smallest_length=MAX_HOPS):
        """
        step 2: 判断树中是否存在满足不经过condition的终点。
        :param condition: 搜索条件
        :param search_queue: 搜索队列
        :param additional_hop: 最短路径外还运行的额外跳数，默认为0，即只找最短路。
        :param smallest_length: 最短路径长度,可以不设置，找到后就记录。
        :return:
        """
        terminal_meet_nodes = []
        # search_queue.sort(key=lambda a: a.length)
        while len(search_queue) > 0:
            queue = []
            for state in search_queue:
                self._explored_level = state.length
                # 判断路径长度是否符合需求
                if not length_filter(state.length-1):
                    self._unexplored_states.setdefault(state.length, []).append(state)
                    continue
                if state.is_explored is True:
                    continue
                state.is_explored = True
                # 判断是否是终点，若是则判断是否为满足需求的节点。
                if self._is_accept(state):
                    state.is_accept = True
                    bisect.insort(self._terminal_states, state)
                    if self._check_condition(state, condition):
                        terminal_meet_nodes.append(state)
                        state.conditions.add(condition)
                        if state.length < smallest_length:
                            smallest_length = state.length
                    continue
                # 若不是则寻找下一跳
                for next_hop in self.ports[state.device]:
                    #  loop检测
                    if next_hop in state.path:
                        continue
                    dfa_state = self.dfa_transitions[state.dfa_state][next_hop]
                    new_state = State.get_state(next_hop, state.path + [next_hop], dfa_state)
                    self._link_state(state, new_state)
                    if new_state.is_explored is False:
                        queue.append(new_state)
            search_queue = queue
        return smallest_length

    def find_search_condition(self, k) -> set[frozenbitarray]:
        """
        step 3: 查找k取值下的搜索条件。
        :param k:
        :return: k时需要更新路径的case。
        """
        new_conditions = set()
        for state in self._terminal_states:
            for condition in state.conditions:
                if condition.count(1) == k-1:
                    tmp_condition = bitarray(condition)
                    for index in state.edges.search(1):
                        tmp_condition[index] = True
                        if self._is_unreachable(tmp_condition):
                            tmp_condition[index] = False
                            continue
                        new_conditions.add(frozenbitarray(tmp_condition))
                        tmp_condition[index] = False
        return new_conditions

    def update_tree(self, conditions: set[frozenbitarray], length_filter):
        """
        step 4: 根据当前的搜索条件，更新树。
        :param conditions: 接下来要搜索的条件。
        :param additional_hop: 要求的最短路径+x跳，默认为0，即只允许最短路径。
        :return:
        """
        exist_path = False
        # print(len(conditions))

        for condition in conditions:
            smallest_hops = DVNet.MAX_HOPS
            # 先看目前的终点中是否由符合condition的
            for terminal_state in self._terminal_states:
                if not length_filter(terminal_state.length-1):
                    break
                if self._check_condition(terminal_state, condition):
                    terminal_state.conditions.add(condition)
                    if terminal_state.length < smallest_hops:
                        smallest_hops = terminal_state.length

            without_violation_states = []
            for k, v in self._unexplored_states.items():
                if length_filter(k-1):
                    without_violation_states.extend(v)
                    v.clear()
            new_smallest_hops = self.search(condition, without_violation_states, length_filter, smallest_hops)

            if new_smallest_hops >= DVNet.MAX_HOPS:
                self._unreachable_conditions.add(condition)
                continue
            else:
                exist_path = True
        return exist_path

    def label_edge(self, state: State):
        """
        step 6: 从终点向根节点遍历，为经过的所有边打上终点上的condition
        :param state:
        :return:
        """
        conditions = state.conditions
        q = deque([state])
        while len(q) > 0:
            state = q.popleft()
            if state == self.init_state:
                continue
            if state.flag == 1:
                continue
            state.flag = 1
            for e in state.edge_in:
                e.conditions = e.conditions.union(conditions)
                self.used_edges.add(e.origin_edge)
                q.append(e.src)

    def minimization(self):
        """
        step 7:
        :return:
        """
        states = []

        # 去除未标记的边和状态
        queue = deque([self.init_state])  # type:deque[State]
        while len(queue) > 0:
            state = queue.popleft()
            if state.is_explored is False:
                continue
            state.is_explored = False
            states.append(state)
            state.edge_out = [edge for edge in state.edge_out if len(edge.conditions) > 0]
            for edge in state.edge_out:
                queue.append(edge.dst)
        # self.print(states, "before_minimization.puml", False)
        sorted_states = {}  # type:dict[str,list[state]]
        for state in states:
            sorted_states.setdefault(state.device, []).append(state)
        # 合并不可区分状态
        merged = True
        while merged is True:
            merged = False
            for device, states in sorted_states.items():
                state_nums = len(states)
                for i in range(state_nums):
                    state = states[i]
                    merge_list = []
                    for j in range(i+1, state_nums):
                        if self._is_undistinguished(state, states[j]):
                            merge_list.append(states[j])
                    if len(merge_list) > 0:
                        merged = True
                        for s in merge_list:
                            self._merge_state(state, s)
                            states.remove(s)
                        self._merge_edge(state)
                        break
        states = []
        for i in sorted_states.values():
            states.extend(i)
        return states

    def all_pair_reachability(self, k, x, output):
        count = 0
        t1 = time.time_ns()
        d = networkx.floyd_warshall_numpy(self.graph)
        states = []
        # print(d)
        # print(self.device_to_id)
        for d1 in self.devices:
            for d2 in self.devices:
                if d1 == d2:
                    continue
                shortest_length = d[self.device_to_id[d1]][self.device_to_id[d2]]
                if shortest_length == numpy.inf:
                    continue
                states.append((d2, d1, self.gen_dvnet("%s.*%s"%(d2, d1), d2, k, x, output, shortest_length=shortest_length+1)))
                # print(len(self.used_edges)/ len(self._edge_map) )
                # print("%s -> %s" % (d1, d2))

        t2 = time.time_ns()
        self.clean_device_count()
        self.output_puml(states, output)
        return (t2-t1)/1000000.0

    def gen_dvnet(self, requirement, ingress, k_max, length_filter, output, shortest_length=MAX_HOPS):
        # step 1
        search_queue = self.add_requirement(requirement, ingress)

        # step 2
        r = self.search(self.get_zero_condition(), search_queue,
                        length_filter=length_filter, smallest_length=shortest_length)

        if r == 0:
            if output:
                print("no reachable")
            return
        k = 1
        dvnet_num = 1
        while k <= k_max:
            dvnet_num = 1

            # step 3
            conditions = self.find_search_condition(k)

            # print(len(conditions))
            dvnet_num += len(conditions)
            # if k == k_max:
            #     break
            # step 4
            r = self.update_tree(conditions, length_filter)
            if r is False:
                if output:
                    print("k=%s no reachable" % k)
                break
            # step 5
            k += 1

        # step 6
        # if k_max > 0:
        for state in self._terminal_states:
            if state.conditions:
                self.label_edge(state)

        # step 7
        states = self.minimization()

        if output is not None:
            edge_size = 0
            label_size = 0
            for state in states:
                for edge in state.edge_out:
                    edge_size += 1
                    label_size += len(edge.conditions)
            # print(
            #     "%s->%s, k:%s, additional_hop: %s, nodes: %s, edges: %s, avg_label: %.2f, unreachable:%s" % (
            #         ingress, requirement, k_max, additional_hop, len(states),
            #         edge_size, label_size/edge_size,
            #         len(self._unreachable_conditions)))
            # self.output_puml(states, output, False, dst=requirement)
        return states

    def clean_device_count(self):
        for device in self.devices:
            self.node_num[device] = 0

    def print_topology(self, output):
        with open(output, mode="w", encoding="utf-8") as f:
            f.write("@startuml\n")
            for k, v in self._edge_map.items():
                d1, d2 = k.split("-", 2)
                f.write("(%s) -- (%s): %s\n" % (d1, d2, v))
            f.write("@enduml\n")

    @staticmethod
    def _is_undistinguished(state1: State, state2: State):
        if state1.device != state2.device or len(state1.edge_out) != len(state2.edge_out):
            return False
        nxt_state1 = set([edge.dst for edge in state1.edge_out])
        nxt_state2 = set([edge.dst for edge in state2.edge_out])
        return nxt_state1 == nxt_state2

    @staticmethod
    def _merge_edge(state: State):
        edges = state.edge_in + state.edge_out
        merged = True
        while merged is True:
            merged = False
            edges_len = len(edges)
            for i in range(edges_len):
                edge = edges[i]
                merge_list = []
                for j in range(i+1, edges_len):
                    if edge.src == edges[j].src and edge.dst == edges[j].dst:
                        merge_list.append(edges[j])
                if len(merge_list) > 0:
                    merged = True
                    for e in merge_list:
                        edge.conditions = edge.conditions.union(e.conditions)
                        try:
                            e.src.edge_out.remove(e)
                        except ValueError:
                            pass
                        try:
                            e.dst.edge_in.remove(e)
                        except ValueError:
                            pass
                        edges.remove(e)
                    break

    @staticmethod
    def _merge_state(state1: State, state2: State):
        for edge in state2.edge_out:
            edge.src = state1
            state1.edge_out.append(edge)
        for edge in state2.edge_in:
            edge.dst = state1
            state1.edge_in.append(edge)

    def _link_state(self, src: State, dst: State):
        edge_id = self._get_edge_id(src.device, dst.device)
        edge = StateEdge(src, dst, edge_id)
        src.edge_out.append(edge)
        dst.edge_in.append(edge)
        dst.edges = src.edges.copy()
        dst.edges[edge_id] = True

    def _gen_loop_free_dfa(self):
        dfa = None
        for device in self.devices:
            dfa2 = DFA.of_length(self.devices,min_length=0, max_length=1, symbols_to_count={device}).complement()
            dfa = dfa2 if dfa is None else dfa.union(dfa2)
        self.loop_free_dfa = dfa

    def _is_unreachable(self, condition: bitarray):
        """
        根据已经有的不可达的条件，判断这个条件是否一定不可达。
        :param condition:
        :return:
        """
        for unreachable_condition in self._unreachable_conditions:
            if (unreachable_condition & condition) == unreachable_condition:
                return True
        return False

    def _is_loop(self, path):
        return self.loop_free_dfa.accepts_input(path)

    def _is_accept(self, node: State):
        # return self.requirement_dfa.accepts_input(node.path)
        return node.dfa_state in self.dfa.final_states

    @staticmethod
    def _check_condition(state: State, condition: bitarray):
        """
        判断该终点是否满足该条件
        :param state: 终点
        :param condition: 搜索条件
        :return: 判断结果
        """
        return not (state.edges & condition).any()

    # def _get_path_edges(self, state: State):
    #     """
    #     将该点至根节点的所有边，添加至该节点的edges属性中
    #     :param node: 节点，可以为非终点，但只能向上搜索。
    #     :return: None
    #     """
    #     if len(state.edges)>0:
    #         return
    #     # state.edges.clear()
    #     e = state.edge_in[0]
    #     s = state
    #     while e.is_virtual() is False:
    #         state.edges.append(e.origin_edge)
    #         s = e.src
    #         e = s.edge_in[0]
