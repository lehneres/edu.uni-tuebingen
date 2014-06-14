package assignment6;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings ("all")
public class Assignment6 {
	public static void main(String[] args) throws IOException {
		
		final HashMap<String, Node> humanNetwork = readHumanNetwork();
		
		final List<Node> keyPlayersVirus = readHCVNetwork();
		final Node[] bestFive = new Node[5];
		
		for (int i = 0; i < 5; i++) {
			int size = 0;
			Node insert = null;
			for (final Node node : keyPlayersVirus)
				if (humanNetwork.containsKey(node.name)) {
					final Node humanNode = humanNetwork.get(node.name);
					
					if (humanNode.interacting.size() > size) {
						insert = humanNode;
						size = humanNode.interacting.size();
					}
					bestFive[i] = insert;
				}
			
			humanNetwork.remove(bestFive[i].name);
		}
		System.out.println(Arrays.toString(bestFive));
		
	}
	
	public static List<Node> readHCVNetwork() throws IOException {
		
		final List<Node> returnNodes = new LinkedList<Node>();
		
		// read in file and build network
		final BufferedReader reader = new BufferedReader(new FileReader("data/assignment6/hcv_human_ppi.csv"));
		
		final HashMap<String, Node> network = new HashMap<String, Node>();
		String line = reader.readLine();
		while (line != null) {
			final String[] nodes = line.split(",");
			final String key1 = nodes[0];
			final String key2 = nodes[1];
			
			Node node1;
			Node node2;
			
			// if (network.containsKey(key1)) {
			
			// node1 = network.get(key1);
			
			// } else {
			node1 = new Node();
			node1.interacting = new LinkedList();
			node1.name = key1;
			
			// network.put(key1, node1);
			// }
			
			if (network.containsKey(key2)) node2 = network.get(key2);
			else {
				node2 = new Node();
				node2.interacting = new LinkedList();
				node2.name = key2;
				
				network.put(key2, node2);
			}
			// node1.interacting.add(node2);
			node2.interacting.add(node1);
			line = reader.readLine();
		}
		
		for (final String key : network.keySet()) {
			final Node node = network.get(key);
			final List<Node> interacts = node.interacting;
			if (interacts.size() > 1) returnNodes.add(node);
			
		}
		
		return returnNodes;
	}
	
	public static HashMap<String, Node> readHumanNetwork() throws IOException {
		// read in file and build network
		final BufferedReader reader = new BufferedReader(new FileReader("data/assignment6/human_interactome.csv"));
		
		final HashMap<String, Node> network = new HashMap<String, Node>();
		int counter = 0;
		String line = reader.readLine();
		while (line != null) {
			final String[] nodes = line.split(",");
			final String key1 = nodes[0];
			final String key2 = nodes[1];
			
			Node node1;
			Node node2;
			
			if (network.containsKey(key1)) node1 = network.get(key1);
			else {
				node1 = new Node();
				node1.interacting = new LinkedList();
				node1.name = key1;
				
				network.put(key1, node1);
			}
			
			if (network.containsKey(key2)) node2 = network.get(key2);
			else {
				node2 = new Node();
				node2.interacting = new LinkedList();
				node2.name = key2;
				
				network.put(key2, node2);
			}
			node1.interacting.add(node2);
			node2.interacting.add(node1);
			counter++;
			
			line = reader.readLine();
		}
		// get number of nodes and number of edges
		System.out.println("Number of Nodes:" + network.size());
		System.out.println("Number of Edges:" + counter);
		
		// get node degree distribution
		final int[] degrees = new int[network.size()];
		for (final String key : network.keySet()) {
			final Node node = network.get(key);
			degrees[node.interacting.size()]++;
		}
		
		System.out.println(Arrays.toString(degrees));
		
		return network;
	}
	
}
