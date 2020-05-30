package networkanalysis;

import java.util.ArrayList;

import utils.Graph;

/**
 * This class gathers the methods that are used for general network analysis
 * @version 1.4.29112018
 */
public abstract class NetworkAnalysisHelper {
	
	/**
	 * calculates total internal and external edges count of the given node in the given group within the given graph,
	 * an overload for: {@link NetworkAnalysisHelper#getNumberIntAndExtLinks(Graph, ArrayList, String, ArrayList, boolean)}
	 * @param graph
	 * @param group
	 * @param node
	 * @return two values, count internal & count external
	 */
	public static int[] getNumberIntAndExtLinks(Graph<String> graph, ArrayList<String> group, String node) {
		return getNumberIntAndExtLinks(graph,group,node,true);
	}
	
	/**
	 * calculates total internal and external edges count of the given node in the given group within the given graph
	 * @param graph
	 * @param group
	 * @param node
	 * @param nodesToIgnore these nodes are ignored while counting successors of predecessors
	 * @param useSuccessors if true, we count internal and external links from successors list, otherwise we use predecessors list
	 * @return
	 */
	public static int[] getNumberIntAndExtLinks(Graph<String> graph, ArrayList<String> group, String node, boolean useSuccessors) {
		ArrayList<String> successors = (useSuccessors)?graph.getSuccessors(node):graph.getPredecessors(node);
		int internalLinks=0;
		int externalLinks=0;
		for(String s:successors) {
			if(group.contains(s)) {
				internalLinks++;
			}else {
				externalLinks++;
			}
		}
		return new int[] {internalLinks, externalLinks};
	}
}
