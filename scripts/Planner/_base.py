class Node:
    def __init__(self, name):
        token = name.rsplit("^", 1)
        self.name = token[0]
        self.index = token[1]
        self.prev = set()
        self.next = set()

    @staticmethod
    def add_edge(src_node, dst_node):
        src_node.next.add(dst_node)
        dst_node.prev.add(src_node)

    def __str__(self):
        return "%s-%s prev:%s, next:%s" % (self.name, self.index, [i.name+"-"+i.index for i in self.prev], [i.name+"-"+i.index for i in self.next])

    def get_name(self):
        return self.name + "-" + str(self.index)


class Port:
    def __init__(self, device, name):
        self.device = device
        self.name = name
        self.to = None

    def link(self, port):
        self.to = port
        port.to = self

    def __hash__(self):
        return hash(self.device+self.name)

    def __eq__(self, other):
        return str(self) == str(other)

    def __str__(self):
        return self.device + " " + self.name