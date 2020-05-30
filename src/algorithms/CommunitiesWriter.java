package algorithms;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import utils.CustomLogger;
import utils.Graph;

/**
 * Takes input:
 * <ul>
 * <li>graphPath</li>
 * <li>resultPath</li>
 * <li>N, an integer</li>
 * <li>isDirected, "directed" or "undirected"</li>
 * </ul>
 * @version 1.14.30012019
 */
public abstract class CommunitiesWriter {

	public static final CustomLogger logger = new CustomLogger("CommunitiesWriter", Level.FINER);
	
	protected static int countGroups=0;
	protected Graph<String> graph;
	protected ArrayList<String> groupedNodes;
	
	public CommunitiesWriter(String graphFile, boolean directed) {
		graph = utils.Graph.loadFromFile(graphFile, directed);
	}
	
	public CommunitiesWriter(Graph<String> graph) {
		this.graph = graph;
	}
	
	/**
	 * calculates the sum of weights to edges going from "node" to nodes inside "group"
	 * @param graph
	 * @param group
	 * @param node
	 * @return
	 */
	public double sumWeightsInternal(Graph<String> graph, ArrayList<String> group, String node) {
		double sum=0;
		ArrayList<String> successors = graph.getSuccessors(node);
		successors.retainAll(group);
		for(String s:successors) {
			sum+=graph.getWeight(node, s);
		}
		return sum;
	}

	
	
	/**
	 * 
	 * @param groupsList
	 * @param groupsAttributes
	 * @param file
	 * @param attributeName
	 */
	public void writeResults(HashMap<Integer, ArrayList<String>> groupsList, String file,
			String attributeName) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			writer.write("Id\t"+attributeName+"\n");
			for(Integer groupId:groupsList.keySet()) {
				for(String node: groupsList.get(groupId)) {
					writer.write(node+"\t"+groupId+"\n");
				}
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * writes results to a file
	 * @param attributes contains the classification for each node
	 * @param attributeName
	 * @param file
	 * @param edgeAttrs if true, the string keys are considered to be tuples delimited with a ','
	 */
	public void writeResults(HashMap<String, Double> attributes, String attributeName, String file, boolean edgeAttrs) {
		try {
			//write results with attributes
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			if(!edgeAttrs)writer.write("Id\t"+attributeName+"\n");else writer.write("Source\tTarget\t"+attributeName+"\n");
			writeResults(attributes, writer, edgeAttrs);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeResults(HashMap<String, Double> attributes, BufferedWriter writer, boolean edgeAttrs) {
		try {
			//write results with attributes
			for(String node:attributes.keySet()) {
				String edge = node;
				if(edgeAttrs) {
					node = node.replace(",", "\t");
				}
				writer.write(node+"\t"+attributes.get(edge)+"\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
