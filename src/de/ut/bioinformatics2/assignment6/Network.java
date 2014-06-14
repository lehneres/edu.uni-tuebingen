package de.ut.bioinformatics2.assignment6;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * simple class for network building and analysis
 * 
 * @author LehnereS
 * 
 */
public class Network {
	
	/**
	 * @param <T>
	 * @param args
	 *            no arguments here
	 * @throws IOException
	 */
	public static <T> void main(String[] args) throws IOException {
		
		int id = 0;

		final HashSet<Edge> edges = new HashSet<>();
		final HashMap<String, Node> nodes = new HashMap<>();
		final HashMap<String, Node> nodesHuman = new HashMap<>();
		final HashMap<String, Node> nodesHCV = new HashMap<>();
		final HashMap<String, Node> nodesHCVHuman = new HashMap<>();
		
		// building the human interactome
		try (BufferedReader reader = new BufferedReader(new FileReader("data/assignment6/human_interactome.csv"))) {
			String line;
			while ((line = reader.readLine()) != null) {
				final String[] nodeNames = line.split(",");
				
				Node a, b;
				
				if (nodes.containsKey(nodeNames[0])) a = nodes.get(nodeNames[0]);
				else {
					a = new Node(id++);
					a.setName(nodeNames[0]);
					a.neigbours = new HashSet<>();
				}
				
				if (nodes.containsKey(nodeNames[1])) b = nodes.get(nodeNames[1]);
				else {
					b = new Node(id++);
					b.setName(nodeNames[1]);
					b.neigbours = new HashSet<>();
				}
				
				a.neigbours.add(b);
				b.neigbours.add(a);
				
				nodesHuman.put(nodeNames[0], a);
				nodesHuman.put(nodeNames[1], b);
				
				nodes.put(nodeNames[0], a);
				nodes.put(nodeNames[1], b);
				
				edges.add(new Edge(a, b));
			}
		}
		
		System.out.println("number of nodes in human interactome: " + nodes.size());
		System.out.println("number of edges in human interactome: " + edges.size());
		
		// building the HCV - Human interactome
		try (BufferedReader reader = new BufferedReader(new FileReader("data/assignment6/hcv_human_ppi.csv"))) {
			String line;
			while ((line = reader.readLine()) != null) {
				final String[] nodeNames = line.split(",");
				
				Node a, b;
				
				if (nodes.containsKey(nodeNames[0])) a = nodes.get(nodeNames[0]);
				else {
					a = new Node(id++);
					a.setName(nodeNames[0]);
					a.neigbours = new HashSet<>();
					a.setOrganism("hcv");
					nodesHCV.put(nodeNames[0], a);
				}
				
				if (nodes.containsKey(nodeNames[1])) b = nodes.get(nodeNames[1]);
				else {
					b = new Node(id++);
					b.setName(nodeNames[1]);
					b.neigbours = new HashSet<>();
				}
				
				nodesHCVHuman.put(b.getName(), b);

				a.neigbours.add(b);
				b.neigbours.add(a);
				
				nodes.put(nodeNames[0], a);
				nodes.put(nodeNames[1], b);
				
				edges.add(new Edge(a, b));
			}
		}
		
		// find those human nodes with more then 1 interaction with more then one viral protein
		List<Node> HCVHuman = new LinkedList<>();
		for (Node node : nodesHCVHuman.values()) {
			int count = 0;
			for (Node humanOrHCV : node.neigbours) {
				if (humanOrHCV.getOrganism().equals("hcv")) count++;
				if (count > 1) {
					HCVHuman.add(node);
					break;
				}
			}
			
		}

		// sort the human nodes along their interactions sizes
		Collections.sort(HCVHuman, new Comparator<Node>() {
			
			@Override
			public int compare(Node o1, Node o2) {
				return new Integer(o2.neigbours.size()).compareTo(o1.neigbours.size());
			}
			
		});
		
		List<Node> top5 = HCVHuman.subList(0, 5);
		System.out.println("\nTOP5 human hubs interacting with HCV (number of interacting proteins):\n"
				+ Arrays.toString(top5.toArray()));
		
		// use depth-first search to find all connected components
		Set<Node> globalSearchSpace = new HashSet<>(nodesHuman.values());

		List<HashSet<Node>> components = new LinkedList<>();

		while (!globalSearchSpace.isEmpty()) {
			
			Set<Node> localSearchSpace = new HashSet<>();
			localSearchSpace.add((Node) globalSearchSpace.toArray()[0]);
			
			HashSet<Node> component = new HashSet<>();
			
			components.add(component);
			
			while (!localSearchSpace.isEmpty()) {

				Node a = (Node) localSearchSpace.toArray()[0];
				
				Set<Node> neigbours = new HashSet<>(a.neigbours);
				neigbours.removeAll(component);
				neigbours.removeAll(nodesHCV.values());
				
				component.addAll(neigbours);
				localSearchSpace.addAll(neigbours);
				
				localSearchSpace.remove(a);
			}
			
			globalSearchSpace.removeAll(component);
		}
		
		// sort the components along their size
		Collections.sort(components, new Comparator<HashSet<Node>>() {
			
			@Override
			public int compare(HashSet<Node> o1, HashSet<Node> o2) {
				return new Integer(o2.size()).compareTo(o1.size());
			}

		});
		
		System.out.println("\nsize of largest component: " + components.get(0).size() + " nodes with a total of "
				+ components.size() + " components\n");
		
		List<Node> component = new LinkedList<>(components.get(0));
		
		int[] distancesDistribution = new int[component.size()];
		int[][] distances = new int[id - 1][id - 1];

		Random random = new Random(666);

		// using dijkstra for computing all shortest pathes starting from random nodes
		for (int i = 0; i < 10; i++) {
			Node start = component.get(random.nextInt(component.size() - 1));
			// Node target = component.get(random.nextInt(component.size() - 1));
			
			// for (Node start : component) {
			
			int index = start.getId();
			
			Arrays.fill(distances[index], Integer.MAX_VALUE);
			
			System.out.println("shortes pathes for node #" + index);

			distances[index][index] = 0;
			
			HashSet<Node> currentSet = new HashSet<>(nodesHuman.values());
			DistanceComp compDist = new DistanceComp(distances[index]);
			
			while (!currentSet.isEmpty()) {
				
				Node u = Collections.min(currentSet, compDist);
				int uIndex = u.getId();

				currentSet.remove(u);

				if (distances[index][uIndex] == Integer.MAX_VALUE) break;
				// HashMap<Node, Node> previous = new HashMap<>();
				
				// if (u == target) {
				// int length = 0;
				// while (previous.containsKey(u)) {
				// length++;
				// u = previous.get(u);
				// }
				// distancesDistribution[length]++;
				// break;
				// }

				for (Node v : u.neigbours) {
					int vIndex = v.getId();
						int dist = distances[index][uIndex] + 1;
						if (dist < distances[index][vIndex]) {
							distances[index][vIndex] = dist;
							compDist.distances = distances[index];
							// previous.put(v, u);
						}
				}
			}
		}
		
		for (int[] row : distances)
			for (int dist : row)
				if (dist != Integer.MAX_VALUE && dist != 0) distancesDistribution[dist]++;

		System.out.println("\nprinting the distance distribution (distance;count);");

		for (int i = 0; i < distancesDistribution.length; i++)
			if (distancesDistribution[i] != Integer.MAX_VALUE && distancesDistribution[i] != 0)
				System.out.println(i + ";" + distancesDistribution[i]);
	}
}
