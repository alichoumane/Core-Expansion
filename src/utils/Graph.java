package utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;

/**
 * Encapsulates a graph, constituted of nodes and edges. It can be loaded and accessed but cannot be manipulated.
 * @version 1.10.16052019
 */
public class Graph<T> implements Cloneable{

	protected HashMap<T, ArrayList<T>> graph = new HashMap<>();
	
	protected HashMap<String, Double> weights = new HashMap<>();//TODO:clean make weights HashMap generic
	protected HashMap<T, Double> outWeights = new HashMap<>();
	protected ArrayList<Double> sortedOutWeights = new ArrayList<>();//stores out-weights sorted from min to max
	protected ArrayList<Double> sortedWeights = new ArrayList<>();//stores sorted edge weights v1.9.11012019
	
	protected boolean directed;
	public String sourceFile = null;

	protected HashMap<String, Double> optionalWeights = new HashMap<>();//stores optional weights
	protected ArrayList<T> hiddenNodes = new ArrayList<>();
	
	protected boolean flag_sortWeights=false;//when true, the weights and outWeights are sorted
	/**
	 * when -1 then it is not calculated, otherwise this value is ready to be returned
	 */
	protected int numberOfEdges = -1;
	
	public Graph(){
		
	}
	
	public Graph(HashMap<T, ArrayList<T>> graph){
		this.graph=graph;
	}
	
	public Graph(Graph<T> graph){
		this(graph.graph);
	}
	
	//TODO:clean-code add HashMap<String, HashMap<Node,attribute>> to store attributes in graph such as weights of edges and nodes classification
	
	@SuppressWarnings("unchecked")
	@Override
	public Object clone() {
		Graph<T> newGraph = new Graph<>(new HashMap<>());
		for(T node:this.graph.keySet()) {
			newGraph.graph.put(node, (ArrayList<T>)this.graph.get(node).clone());
		}
		newGraph.hiddenNodes = (ArrayList<T>)hiddenNodes.clone();
		newGraph.sourceFile = sourceFile+"";
		newGraph.weights = weights;//updated on 28/11/2018
		newGraph.outWeights = (HashMap<T, Double>)outWeights.clone();//updated on 18/12/2018
		newGraph.sortedOutWeights = (ArrayList<Double>)sortedOutWeights.clone();
		newGraph.sortedWeights = (ArrayList<Double>)sortedWeights.clone();
		return newGraph;
	}
	
	/**
	 * returns all nodes that have successors, doesn't exclude hidden nodes (starting from version 1.5)
	 * @return
	 */
	public Set<T> getNodes(){
		return graph.keySet();
	}
	
	/**
	 * @return all nodes in the graph, excluding hidden nodes (starting from v1.5)
	 */
	public ArrayList<T> getAllNodes() {
		ArrayList<T> result = new ArrayList<>();
		for(T node:graph.keySet()) {
			if(!result.contains(node))result.add(node);
			ArrayList<T> successors = graph.get(node);
			for(T s:successors) {
				if(!result.contains(s))result.add(s);
			}
		}
		result.removeAll(hiddenNodes);
		return result;
	}
	
	/**
	 * returns the total number of edges in the graph
	 * @param directed
	 * @param forceRecalculate forces the function to recalculate the number instead of returning precalculated value
	 * @return
	 */
	public int getNumberEdges(boolean forceRecalculate) {
		if(numberOfEdges>=0 && !forceRecalculate)return numberOfEdges;
		int n=0;
		for(T node:graph.keySet()) {
			if(hiddenNodes.contains(node))continue;
			for(T s:graph.get(node)) {
				if(!hiddenNodes.contains(s)) {
					n++;
				}
			}
		}
		
		numberOfEdges = n/2;
		return numberOfEdges;
	}
	
	public ArrayList<T> getSuccessors(T id){
		return getSuccessors(id, true);
	}
	/**
	 * Returns all the nodes where there is an edge pointing to them originated from the node with id.
	 * @param id
	 * @param copy if true, returns a clone of successors list for safe use, in case of false, hidden nodes are not returned
	 * @return ArrayList<T> containing ids of successors
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<T> getSuccessors(T id, boolean copy){
		if(hiddenNodes.contains(id))return new ArrayList<>();
		
		ArrayList<T> result = graph.get(id);
		if(result==null)result=new ArrayList<>();
		if(!copy)return result;
		
		result = (ArrayList<T>) result.clone();
		result.removeAll(hiddenNodes);
		
		return result;
	}
	
	public ArrayList<T> getPredecessors(T id){
		//TODO:performance store predecessors in separate HashMap
		
		if(hiddenNodes.contains(id))return new ArrayList<>();
		ArrayList<T> result = new ArrayList<T>();
		for(T p:graph.keySet()) {
			//check if 'id' is one of the successors of 'p'
			for(T s:graph.get(p)) {
				if(s.equals(id) && !result.contains(p)) {
					result.add(p);
				}
			}
		}
		result.removeAll(hiddenNodes);
		return result;
	}

	public void hideNode(T id) {
		hiddenNodes.add(id);
	}
	
	public void unhideNode(T id) {
		hiddenNodes.remove(id);
	}
	
	public void unhideAll() {
		hiddenNodes.clear();
	}
	
	public void hideAll() {
		unhideAll();//to avoid removing currently hidden nodes from all nodes when calling getAllNodes
		hiddenNodes = getAllNodes();
	}
	
	public void unhide(ArrayList<T> nodes) {
		for(T node:nodes) {
			unhideNode(node);
		}
	}
	
	public void hide(ArrayList<T> nodes) {
		for(T node:nodes) {
			hideNode(node);
		}
	}
	
	public void removeAll(ArrayList<T> ids) {
		for(T id:ids) {
			removeNode(id);
		}
	}
	
	public void removeNode(T id) {
		graph.remove(id);
		numberOfEdges=-1;
		for(T key : graph.keySet()) {
			graph.get(key).remove(id);
		}
	}
	
	public void removeEdge(String src, String trg, boolean directed) {
		removeEdge(src, trg, directed, false);
	}
	
	/**
	 * 
	 * @param src
	 * @param trg
	 * @param directed
	 * @param removeNodes removes node if no edges remaining
	 */
	@SuppressWarnings("unchecked")
	//TODO:clean-code find a way to represent edges generically or make the Graph class not generic
	public void removeEdge(String src, String trg, boolean directed, boolean removeNodes) {
		if(directed) {
			graph.get(src).remove(trg);
		}else {
			graph.get(src).remove(trg);
			graph.get(trg).remove(src);
		}
		if(removeNodes) {
			if(graph.get(src).size()==0) {
				removeNode((T) src);
				System.out.println("node "+src+" removed");
			}
			if(graph.get(trg).size()==0) {
				removeNode((T) trg);
				System.out.println("node "+trg+" removed");
			}
		}
	}
	
	public void keepOnly(ArrayList<T> group) {
		ArrayList<T> others = getAllNodes();
		others.removeAll(group);
		removeAll(others);
	}
	
	public static <T0> Graph<T0> keepOnly(ArrayList<T0> group,Graph<T0> graph) {
		@SuppressWarnings("unchecked")
		Graph<T0> localGraph = (Graph<T0>)graph.clone();
		localGraph.keepOnly(group);
		return localGraph;
	}
	
	public void setOptionalWeights(HashMap<String,Double> optionalWeights) {
		this.optionalWeights = optionalWeights;
	}
	
	public double getOptionalWeight(String a, String b) {
		if(optionalWeights.containsKey(a+","+b)) {
			return optionalWeights.get(a+","+b);
		}
		return 1.0;
	}
	
	/**
	 * added in v1.8.31122018, returns all weights of edges in a hashmap where the key is the id of 1st node + , + id of 2nd node. 
	 * <b>don't modify the returned map because this function returns the same reference in the graph.</b>
	 * @return
	 */
	public HashMap<String,Double> getWeights(){
		return weights;
	}
	
	/**
	 * added in 1.7.28112018
	 * bug fixed 1.11.23052019
	 * @param a
	 * @param b
	 * @return
	 */
	public double getWeight(String a, String b) {
		if(!graph.containsKey(a) || !graph.containsKey(b))
			throw new IllegalStateException("graph doesn't contain one of the requested nodes "+a+" or "+b);
		if(weights.containsKey(a+","+b)) {
			return weights.get(a+","+b);
		}else if((graph.get(a).contains(b) || graph.get(b).contains(a)) &&
				(!hiddenNodes.contains(a) && !hiddenNodes.contains(b))) {
			return 1.0;
		}
		return 0.0;
	}
	
	public ArrayList<String> getEdgesOfWeight(double weight) {
		ArrayList<String> edges = new ArrayList<>();
		for(String edge:weights.keySet()) {
			if(weights.get(edge)==weight) {
				//check if this edge still exists
				String src = edge.split(",")[0];
				String trgt = edge.split(",")[1];
				if(graph.get(src)!=null && graph.get(trgt)!=null && graph.get(src).contains(trgt)) {
					edges.add(edge);
				}
			}
		}
		return edges;
	}
	
	public double getOutWeight(T node, boolean forceRecalculate) {
		if(!forceRecalculate)return outWeights.get(node);
		ArrayList<T> successors = getSuccessors(node);
		double sumWeights = 0;
		for(T s:successors) {
			sumWeights += getWeight(node.toString(), s.toString());
		}
		return sumWeights;
	}
	
	public double getOutWeight(T node) {
		return getOutWeight(node, false);
		//TODO:performance don't force recalculation, but this will prevent weights from being updated with hidden nodes, 
		//forcing calculations will make checkNode function calculates wrong out-weights for some nodes
	}
	
	//v1.9.11012019
	public HashMap<T,Double> getOutWeights(){
		return outWeights;
	}
	
	/**
	 * works only if flag_sortWeights is set to true
	 * @return
	 */
	public double getMinOutWeight() {
		return sortedOutWeights.get(0);
	}
	
	/**
	 * works only if flag_sortWeights is set to true
	 * @return
	 */
	public ArrayList<Double> getSortedOutWeights(){
		return sortedOutWeights;
	}
	
	/**
	 * works only if flag_sortWeights is set to true
	 * @return
	 */
	public ArrayList<Double> getSortedWeights(){
		return sortedWeights;
	}
	
	public void setWeights(HashMap<String, Double> weights){
		setWeights(weights,true);
	}
			
	public void setWeights(HashMap<String, Double> weights,boolean recalculateOutWeights) {
		this.weights = weights;
		
		if(!recalculateOutWeights){
			return;
		}
		
		//recalculate out weights
		outWeights.clear();
		sortedOutWeights.clear();
		
		//sort outWeights
		ArrayList<T> nodes = getAllNodes();
		for(T node:nodes) {
			double outWeight = getOutWeight(node, true);
			outWeights.put(node, outWeight);
			
			if(flag_sortWeights) {
				//add this new out weight to the set of sorted out weights
				if(sortedOutWeights.size()==0 || outWeight<sortedOutWeights.get(0)) {
					sortedOutWeights.add(0,outWeight);
				}else if(outWeight>sortedOutWeights.get(sortedOutWeights.size()-1)) {
					sortedOutWeights.add(sortedOutWeights.size(), outWeight);
				}else {
					for(int i=0;i<sortedOutWeights.size()-1;i++) {
						if(outWeight>sortedOutWeights.get(i) && outWeight<sortedOutWeights.get(i+1)) {
							sortedOutWeights.add(i+1,outWeight);
							break;
						}else if(outWeight==sortedOutWeights.get(i) || outWeight==sortedOutWeights.get(i+1)) {
							break;
						}
					}
				}
			}
		}
		if(flag_sortWeights) {
			//sort weights
			sortedWeights.clear();
			for(Double weight:weights.values()) {
				if(sortedWeights.size()==0 || weight<sortedWeights.get(0)) {
					sortedWeights.add(0,weight);
				}else if(weight>sortedWeights.get(sortedWeights.size()-1)) {
					sortedWeights.add(sortedWeights.size(), weight);
				}else {
					for(int i=0;i<sortedWeights.size()-1;i++) {
						if(weight>sortedWeights.get(i) && weight<sortedWeights.get(i+1)) {
							sortedWeights.add(i+1,weight);
							break;
						}else if(weight==sortedWeights.get(i) || weight==sortedWeights.get(i+1)) {
							break;
						}
					}
				}
			}
		}
	}
	
	/**
	 * remove all nodes in the given array list from the graph, except for those who have <code>externalDeg - internalDeg > 0</code>
	 * @param ids
	 */
	public void removeAllWithCondition(ArrayList<T> ids) {
		ArrayList<T> toBeRemoved = new ArrayList<>();
		for(T id:ids) {
			int internalEdges=0;
			ArrayList<T> successors = graph.get(id);
			int totalEdges = successors.size();
			for(T s:successors) {
				if(ids.contains(s))internalEdges++;
			}
			if((totalEdges-internalEdges)>internalEdges)continue;
			toBeRemoved.add(id);
		}
		for(T id:toBeRemoved) {
			removeNode(id);
		}
	}
	
	/**
	 * Remove all nodes but the nodes with external degree >= deg
	 * @param ids
	 * @param deg minimum external degree to keep the node from removing
	 */
	public void removeAllWithCondition(ArrayList<T> ids, int deg) {
		ArrayList<T> toBeRemoved = new ArrayList<>();
		for(T id:ids) {
			int internalEdges=0;
			ArrayList<T> successors = graph.get(id);
			if(successors==null)successors=new ArrayList<>();
			int totalEdges = successors.size();
			for(T s:successors) {
				if(ids.contains(s))internalEdges++;
			}
			if(totalEdges-internalEdges>=deg)continue;
			toBeRemoved.add(id);
		}
		for(T id:toBeRemoved) {
			removeNode(id);
		}
	}
	
	/**
	 * remove all nodes in the given array list from the graph if they have internal edges more than external edges, 
	 * and has no relation with the previously removed group.
	 * @param ids
	 * @param previousGroupIds
	 */
	public void removeAllWithCondition(ArrayList<T> ids, ArrayList<T> previousGroupIds) {
		for(T id:ids) {
			//count internal edges
			boolean hasRelationWithPrev=false;
			int internalEdges=0;
			ArrayList<T> successors = graph.get(id);
			int totalEdges = successors.size();
			for(T s:successors) {
				if(ids.contains(s))internalEdges++;
				if(previousGroupIds.contains(s))hasRelationWithPrev=true;
			}
			if(internalEdges>(totalEdges-internalEdges) && !hasRelationWithPrev)removeNode(id);
		}
	}
	
	public boolean isDirected() {
		return directed;
	}
	
	/**
	 * Loads the graph from a file, the file should follow the format: source\ttarget.
	 * @param fileName path to graph file
	 * @param invert if true, the edges of the graph will be inverted.
	 */
	public static Graph<String> loadFromFile(String fileName, boolean directed){
		return loadFromFile(fileName, directed, false);
	}
	
	/**
	 * 
	 * @param fileName
	 * @param directed
	 * @param loadWeights load the weights from the column with header named 'weight' or 
	 * from the third column by default (v1.10.16052019)
	 * @return
	 */
	public static Graph<String> loadFromFile(String fileName, boolean directed, boolean loadWeights){
		Graph<String> inst = new Graph<>();
		inst.sourceFile = fileName;
		inst.directed = directed;
		HashMap<String, Double> weights = new HashMap<>();
		int weightIndex=2;
		//System.out.println("loading graph from file "+fileName);
		try {
			Scanner scanner = new Scanner(new FileReader(fileName));
			while(scanner.hasNext()){
				String rawLine = scanner.nextLine();
				if(rawLine.startsWith("#") || !rawLine.contains("\t") || rawLine.startsWith("Id") || rawLine.startsWith("Source")) {
					rawLine=rawLine.toLowerCase();
					if(rawLine.contains("weight")){
						String[] line = rawLine.split("\t");
						ArrayList<String> headerList = new ArrayList<String>(Arrays.asList(line));
						weightIndex = headerList.indexOf("weight");
					}
					continue;
				}
				String[] line = rawLine.split("\t");
				try{
					String srcId = line[0];
					String trgtId = line[1];
					
					if(srcId.equals(trgtId))continue;
					
					/* Add edge here */
					if(inst.graph.containsKey(srcId)){
						if(!inst.graph.get(srcId).contains(trgtId)) {
							inst.graph.get(srcId).add(trgtId);
						}
					}else{
						ArrayList<String> targets = new ArrayList<>();
						targets.add(trgtId);
						inst.graph.put(srcId, targets);
					}
					if(loadWeights)weights.put(srcId+","+trgtId, Double.parseDouble(line[weightIndex]));
					if(!directed) {
						//add the same edge in the other direction i.e. from trgtId -> srcId
						if(inst.graph.containsKey(trgtId)){
							if(!inst.graph.get(trgtId).contains(srcId)) {
								inst.graph.get(trgtId).add(srcId);
							}
						}else{
							ArrayList<String> targets = new ArrayList<>();
							targets.add(srcId);
							inst.graph.put(trgtId, targets);
						}
						if(loadWeights)weights.put(trgtId+","+srcId, Double.parseDouble(line[weightIndex]));
					}
				}catch (NumberFormatException e){
					System.out.println("error parsing one of the elements of '"+rawLine+"'");
				}
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		inst.setWeights(weights);
		return inst;
	}
	
	/**
	 * Writes the graph using the given Writer object
	 * @param writer
	 * @param writeHidden if true hidden nodes will be shown in the written file
	 */
	public void write(Writer writer, boolean writeHidden){
		try {
			writer.write("Source\tTarget\tEdgeWeight\n");
			for(T s:graph.keySet()){
				if(!writeHidden && hiddenNodes.contains(s))continue;
				for(T t:graph.get(s)){
					if(!writeHidden && hiddenNodes.contains(t))continue;
					writer.write(s.toString()+"\t"+t.toString()+"\t"+getWeight(s.toString(),t.toString())+"\n");
				}
			}

			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void write(Writer writer) {
		write(writer,true);
	}
}

