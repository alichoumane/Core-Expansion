package algorithms;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import networkanalysis.NetworkAnalysisHelper;
import networkanalysis.LocalMaximumFinder;
import networkanalysis.NeighborhoodOverlap;
import utils.CustomLogger;
import utils.Graph;
import utils.TimeTracker;

/**
 * Takes input:
 * <ul>
 * <li>graphPath</li>
 * <li>communities</li>
 * <li>logFolder</li>
 * <li>isDirected, "directed" or "undirected", however directed graphs are not supported until now</li>
 * </ul>
 * v6 of Core Expansion method
 * find cores as nodes corresponding to local maximal weights (calculated as neighborhood overlap), 
 * then add remaining nodes each to the nearest core according to its connections.
 * 
 *
 */
public class CoreExpansionAlgorithm extends CommunitiesWriter{

	public static CustomLogger logger = new CustomLogger("CoreExpansion_LocalMaximum", Level.FINER);
	
	public CoreExpansionAlgorithm(Graph<String> graph) {
		super(graph);
	}
	
        public static String outputDirectory = "D:\\Datasets\\Amazon_U";
	public static String graphPath = outputDirectory+"\\edges.csv";
	public static String communities = outputDirectory +"\\CoreExp_Communities.csv";
	public static String logFolder = outputDirectory+"\\logs";

	public static boolean useWeightsInAddition = true;
	
	public static void main(String[] args) {
		CommunitiesWriter.logger.setLevel(Level.FINEST);
		if(!loadArgs(args)) {
			return;
		}
		
		TimeTracker timeTracker = new TimeTracker();

		Graph<String> graph = Graph.loadFromFile(graphPath, false);
		CoreExpansionAlgorithm generator = new CoreExpansionAlgorithm(graph);
		HashMap<String,Double> weights = NeighborhoodOverlap.calculate(graph);
		if(logFolder!=null)generator.writeResults(weights, "weight-initial", logFolder+"/weights-initial.csv", true);
		if(logFolder!=null)generator.writeResults(graph.getOutWeights(), "outWeights", logFolder+"/outWeights_initial.csv", false);
		
		HashMap<Integer, ArrayList<String>> groups = new HashMap<>();

		groups = generator.generateClasses(graph);
		
		generator.writeResults(groups, communities, "class");
		
		logger.log(Level.FINER, groups.size()+" communities detected\n");
		int sum=0;
		for(Integer id:groups.keySet()) {
			int groupSize = groups.get(id).size();
			sum+=groupSize;
			logger.log(Level.FINER, "community "+id+": "+groupSize+" nodes\n");
		}
		timeTracker.stop();
		logger.log(Level.FINER, sum+" nodes classified out of "+graph.getAllNodes().size()+"\n");
		logger.log(Level.FINER, "time elapsed "+timeTracker.toString());
	}
	
	public HashMap<Integer, ArrayList<String>> generateClasses(Graph<String> graph){
		logger.log(Level.FINER, "finding local maximums, ");
		HashMap<String, Double> maximumNodes = LocalMaximumFinder.findLocalMaximumNodes(graph);
		writeResults(maximumNodes, "localOutWeightMax", logFolder+"/localOutWeightMax.csv", false);
		logger.log(Level.FINER, maximumNodes.size()+" maximums found\n");
		
		//find communities
		HashMap<Integer, ArrayList<String>> groupsList = new HashMap<>();
		int i=0;
		for(String node:maximumNodes.keySet()) {
			ArrayList<Integer> coresOfNode = new ArrayList<>();// stores the cores that 'node' could be added to
			for(String successor:graph.getSuccessors(node)) {
				Integer groupId = getGroupIdOf(successor, groupsList);
				if(groupId!=null) {
					groupsList.get(groupId).add(node);
					if(!coresOfNode.contains(groupId))coresOfNode.add(groupId);
				}
			}
			if(coresOfNode.size()==0){
				ArrayList<String> members = new ArrayList<String>();
				members.add(node);
				groupsList.put(i++, members);
			}else if(coresOfNode.size()>1) {
				//merge the two cores
				for (int j = 1; j < coresOfNode.size(); j++) {
					for (String member : groupsList.get(coresOfNode.get(j))) {
						if (!groupsList.get(coresOfNode.get(0)).contains(member))
							groupsList.get(coresOfNode.get(0)).add(member);
					}
					groupsList.remove(coresOfNode.get(j));
				}
			}
		}
		logger.log(Level.FINER, groupsList.size()+" cores constructed\n");
		
		//groupsList = addNodesUsingSortedWeights(groupsList);
		int j=1;
		logger.log(Level.FINER, "performing addition iteration #"+j+"...\n");
		HashMap<String, Integer> removed = addNodesToClosestGroup(groupsList, false, true, useWeightsInAddition);
		if(logFolder!=null)writeResults(groupsList, logFolder+"/coresAtIteration-"+j+".csv","classes-it-"+j);//write intermidiate results
		while(removed.isEmpty()==false) {
			j++;
			logger.log(Level.FINER, "performing addition iteration #"+j+"...\n");
			removed = addNodesToClosestGroup(groupsList, false, true, useWeightsInAddition);
			if(logFolder!=null)writeResults(groupsList, logFolder+"/coresAtIteration-"+j+".csv","classes-it-"+j);//write intermidiate results
		}
		//redo addition iteration without using weights to solve the problem of addition of nodes with out weight = 0
		if(useWeightsInAddition) {
			removed = addNodesToClosestGroup(groupsList, false, true, false);
			j=1;
			if(removed.isEmpty()==false) {
				logger.log(Level.FINER, "performed extra addition iteration #1...\n");
				if(logFolder!=null)writeResults(groupsList, logFolder+"/coresAtIteration-"+j+".csv","classes-it-"+j);
			}
			while(removed.isEmpty()==false) {
				removed = addNodesToClosestGroup(groupsList, false, true, false);
				j++;
				if(removed.isEmpty()==false) {
					logger.log(Level.FINER, "performed extra addition iteration #"+j+"...\n");
					if(logFolder!=null)writeResults(groupsList, logFolder+"/coresAtIteration-"+j+".csv","classes-it-"+j);
				}
			}
		}
		return groupsList;
	}
	
	private static Integer getGroupIdOf(String node, HashMap<Integer, ArrayList<String>> groups) {
		for(Integer key:groups.keySet()) {
			if(groups.get(key).contains(node))return key;
		}
		return null;
	}
	
	/**
	 * iterates over all unclassified nodes, for each node, we add it in case it maximises Lazar modularity
	 * @param groups
	 * @param usePredecessors
	 * @param useOnlyInt
	 * @param fixedClassification if false, all nodes are checked at each iteration to find out if it should be added to different group
	 * @return
	 */
	public HashMap<String, Integer> addNodesToClosestGroup(HashMap<Integer, ArrayList<String>> groups, boolean usePredecessors, 
			boolean useOnlyInt, boolean useWeightsInAddition) {
		ArrayList<String> nodes = graph.getAllNodes();
		HashMap<String, Integer> toBeAdded = new HashMap<>();
		
		//start adding
		for(String node:nodes) {
			if(groupsContains(groups, node)) {
				continue;//TODO:performance give unclassified nodes as parameter to avoid this check
			} else {
				//check if we can add to a group
				logger.log(Level.FINEST, "checking node: "+node+"\n");
				int possibleGroupId=-1;
				double maxDifference=0;
				boolean validMax = true;//max is not valid if it was repeated twice
				
				ArrayList<String> s = graph.getSuccessors(node);
				int[] nbLinks = null;
				for(Integer groupId:groups.keySet()) {
					ArrayList<String> group = groups.get(groupId);
					if(s.size()>0) {
						//depend on successors
						nbLinks = NetworkAnalysisHelper.getNumberIntAndExtLinks(graph, group, node);
						//starting from v1.8.28112018, add node to other group if it has edges to it more than to others,
						//previously, addition was based on adding node to group that maximise dif between int and ext
						double nb=(useOnlyInt)?nbLinks[0]:nbLinks[0]-nbLinks[1];
						//use weights to calculate nb
						if(useWeightsInAddition) {
							//double outWeight = graph.getOutWeight(node);
							double sumWeightsInternal = sumWeightsInternal(graph, group, node);
							nb = sumWeightsInternal;/*/outWeight;//v1.13.26012019*/
						}
						if(nb>=maxDifference) {
							if(nb==maxDifference)validMax=false;
							else if(nb>maxDifference) {
								validMax=true;
								maxDifference = nb;
								possibleGroupId = groupId;
							}
						}
					}else if(usePredecessors && s.size()==0) {
						//add if node have only predecessors from one group
						//current version, add node to group where in-edges coming are more
						nbLinks = NetworkAnalysisHelper.getNumberIntAndExtLinks(graph, group, node, false);
						double nbPre=(useOnlyInt)?nbLinks[0]:nbLinks[0]-nbLinks[1];
						if(nbPre>=maxDifference) {
							if(nbPre==maxDifference)validMax=false;
							else if(nbPre>maxDifference) {
								validMax=true;
								maxDifference = nbPre;
								possibleGroupId = groupId;
							}
						}
					}
					if((!useOnlyInt && nbLinks[0]-nbLinks[1]>maxDifference) || (useOnlyInt && nbLinks[0]>maxDifference)) {
						if(!useWeightsInAddition)throw new RuntimeException("int:"+nbLinks[0]+", ext:"+nbLinks[1]+", currentMaxDif:"+maxDifference);
					}
					logger.log(Level.FINEST, "wrt group:"+groupId+", int:"+nbLinks[0]+", ext:"+nbLinks[1]+", currentMaxDif:"+maxDifference+"\n");
				}
				
				if(possibleGroupId!=-1 && validMax) {
					//we can add to a group
					if(toBeAdded.get(node)!=null) {
						logger.log(Level.SEVERE, "node already marked for classification! "+node+", to Be added to "+possibleGroupId+", already added to "+toBeAdded.get(node)+"\n");
					}
					toBeAdded.put(node, possibleGroupId);
					logger.log(Level.FINEST, "maxDifference:"+maxDifference+" for group:"+possibleGroupId+", to be added\n");
				}else {
					logger.log(Level.FINEST, "no possible group to add to!\n");
				}
			}
		}
		//add the nodes we got
		for(String node: toBeAdded.keySet()) {
			logger.log(Level.FINEST, "add "+node+" to group "+toBeAdded.get(node)+"\n");
			groups.get(toBeAdded.get(node)).add(node);
		}
		
		return toBeAdded;
	}
	
	/**
	 * check if the given set of groups contains the given node
	 * @param groups
	 * @param node
	 * @return
	 */
	protected boolean groupsContains(HashMap<Integer, ArrayList<String>> groups, String node) {
		for(ArrayList<String> group:groups.values()) {
			if(group.contains(node)) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean loadArgs(String[] args) {
		ArrayList<String> argsList = new ArrayList<String>(Arrays.asList(args));
		if(args.length==0)return true;//run from IDE
		if(args.length==1 && args[0].equals("-h")) {
			//print help
			System.out.println("Please provide the following argument to run the program:");
			System.out.println("-f followed by the network file name (undirected, unweighted, one edge per line)");
			return false;
		}
		
		if(args.length == 2) {
			int index = -1;
			
			index = argsList.indexOf("-f");
			if(index==-1) return loadArgs(new String[]{"-h"});
			graphPath = argsList.get(index+1);
			
                        File graphFile = new File(graphPath);
			
			if(!graphFile.exists()) 
			{
				System.out.println(graphPath + " does not exist.");
				return false;
			}
                        
                    try {
                        outputDirectory=(new File(graphFile.getCanonicalPath())).getParentFile().getAbsolutePath();
                        communities = outputDirectory + "/communities_" + graphFile.getName();
			logFolder = outputDirectory + "/logs";
                        
                        File logFile = new File(logFolder);
                        if(!logFile.exists()) logFile.mkdir();
                        
                        
                    } catch (IOException ex) {
                        Logger.getLogger(CoreExpansionAlgorithm.class.getName()).log(Level.SEVERE, null, ex);
                    }
			
			return true;
		}
		
		return loadArgs(new String[]{"-h"});
	}

}
