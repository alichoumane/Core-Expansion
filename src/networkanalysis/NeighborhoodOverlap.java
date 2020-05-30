package networkanalysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import utils.CustomLogger;
import utils.Graph;

/**
 * computes neighbourhood overlap on the edges of the given graph, gives the output as a weighted graph or directly alter the graph
 * based on the results.
 * @version 3.1.13032019
 */
public class NeighborhoodOverlap {
	
	public static CustomLogger logger = new CustomLogger("NeighborhoodOverlap", Level.FINER);
	
	/**test program
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String graphPath = "Dataset/zachary/zachary.csv";
		String weightsPath = "Dataset/zachary/zachary_weights.csv";
		String outWeightsPath = "Dataset/zachary/zachary_outWeights.csv";
		Graph<String> graph = Graph.loadFromFile(graphPath, false);
		HashMap<String, Double> weights = calculate(graph);
		//write results
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(weightsPath));
			writer.write("Source\tTarget\tweight\n");
			for(String edge:weights.keySet()) {
				writer.write(edge.split(",")[0]+"\t"+edge.split(",")[1]+"\t"+weights.get(edge).floatValue()+"\n");
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(outWeightsPath));
			ArrayList<String> nodes = graph.getAllNodes();
			writer.write("Id\toutWeight\n");
			for(String node:nodes) {
				writer.write(node+"\t"+graph.getOutWeight(node)+"\n");
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 
	 * @param graph
	 * @param useWeights
	 * @return
	 */
	public static HashMap<String, Double> calculate(Graph<String> graph) {
		ArrayList<String> nodes = graph.getAllNodes();
		HashMap<String, Double> weights = new HashMap<>();
		
		for(String a:nodes) {
			for(String b:graph.getSuccessors(a)) {
				//calculate the overlap between a and b
				double overlap = 0;
				overlap = overlap(graph, a, b, "o");
				logger.log(Level.FINEST, "weight: "+a+","+b+" = "+overlap+"\n");//out
				weights.put(a+","+b, overlap);
			}
		}
		graph.setWeights(weights);
		graph.setOptionalWeights(weights);
		
		return weights;
	}
	
	/**
	 * the overlap is the number of common neighbours over the number of all neighbours - 2 (because a and b belongs to neighbours set
	 * of each other)
	 * @param graph
	 * @param a first node
	 * @param b second node
	 * @param mode o for neighbourhood overlap, i for intersection, u for union
	 * @return overlap value
	 */
	private static double overlap(Graph<String> graph, String a, String b, String mode) {
		double nbNUnion = 0;
		double nbNInter = 0;
		ArrayList<String> sa = graph.getSuccessors(a);
		sa.remove(b);
		ArrayList<String> sb = graph.getSuccessors(b);
		sb.remove(a);
		
		nbNUnion = sa.size()+sb.size();
		sa.retainAll(sb);
		nbNUnion = nbNUnion - sa.size();
		nbNInter = sa.size();
		
		double overlap = 0.0;
		if(!sa.contains(b) || !sb.contains(a))nbNUnion+=2;//TODO temporal change to allow calculation NO even when no relation
		overlap = (nbNInter)/(nbNUnion-2);
		if(nbNUnion<=2)overlap = 0;
		
		if(mode.equals("o")) {
			//round overlap value
			BigDecimal bd = new BigDecimal(overlap);
			bd = bd.setScale(4, java.math.RoundingMode.HALF_UP);
			overlap = bd.doubleValue();
			return overlap;
		}else if(mode.equals("i")) {
			return nbNInter;
		}else if(mode.equals("u")) {
			return nbNUnion-2;
		}else {
			return overlap;
		}
	}
	
}
