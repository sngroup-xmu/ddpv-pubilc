from Planner import planner


if __name__ == "__main__":
    planner = planner.Planner()
    planner.read_topology_from_file("../config/demo/topology")
    planner.gen("../config/demo/reachability.puml", "D", ["S"], "(exist >= 1, S.*D)", fault_scenes=None)
    planner.gen("../config/demo/waypoint.puml", "D", ["S"], "(exist >= 1, S.*W.*D)", fault_scenes=None)
    planner.gen("../config/demo/limited_path.puml", "D", ["W"], "(exist >= 1, WD|W.D|W..D)", fault_scenes=None)
    planner.gen("../config/demo/different_ingress.puml", "D", ["A", "B"], "(exist >= 1, A.*D|B..D)", fault_scenes=None)
    planner.gen("../config/demo/all-shortest-path.puml", "D", ["S"], "(equal, (S.*D, (==shortest)))", fault_scenes=None)
    planner.gen("../config/demo/non-redundant.puml", "D", ["S"], "(exist == 1, S.*D)", fault_scenes=None)
