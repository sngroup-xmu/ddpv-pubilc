from Planner import planner
import sys

config_dir = "../config/"

def gen_dpvnet(network):
    planner = planner2.Planner2()
    planner.read_topology_from_file(config_dir + network +"/topology")
    planner.gen_all_pairs_reachability(config_dir+network+"/DPVNet.puml")


if __name__ == "__main__":
    network = sys.argv[1]
    gen_dpvnet(network)