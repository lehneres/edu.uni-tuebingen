package de.ut.bioinfo1.assignment10;

import java.util.HashMap;

@SuppressWarnings ("javadoc")
public class Newick {
	
	public class Pair<k, v> {
		
		k	a;
		v	b;
		
		public Pair(k a, v b) {
			this.a = a;
			this.b = b;
		}
		
		@Override
		public String toString() {
			if (a instanceof String && b instanceof String) return a + "," + b;
			else if (a instanceof String) return a + "\n----" + b;
			else if (b instanceof String) return b + "\n----" + a;
			else return "--" + a + "\n--" + b;
		}
	}
	
	public static void main(String[] args) {
		
		final String input =
				"(pharus,((chusquea,(merxmuel-m,(((austrodant,karoochloa),((((zea,miscanthus),pennisetum),danthoniop),((pariana,eremitis),((melicaa,((triticum,lygeum),glycerias)),oryza)))),(centropodi,merxmuel-r)))),anomochloa))";
		
		final Newick n = new Newick(input);
		
		System.out.println(n);
	}
	
	Pair<?, ?>					root;
	
	HashMap<String, Pair<?, ?>>	map	= new HashMap<String, Pair<?, ?>>();
	
	public Newick(String input) {
		createTree(input);
	}
	
	private void createTree(String input) {
		
		do {
			final String[] split = input.split(",");
			
			for (int i = 0; i + 1 < split.length; i++)
				if (!split[i].endsWith(")") && !split[i + 1].startsWith("(")) {
					
					final String leftNode = split[i].replaceAll("\\(", "");
					final String rightNode = split[i + 1].replaceAll("\\)", "");
					
					if (map.containsKey(leftNode) && map.containsKey(rightNode)) {
						root = new Pair<Pair<?, ?>, Pair<?, ?>>(map.get(leftNode), map.get(rightNode));
						map.remove(leftNode);
						map.remove(rightNode);
					} else if (map.containsKey(leftNode)) {
						root = new Pair<Pair<?, ?>, String>(map.get(leftNode), rightNode);
						map.remove(leftNode);
					} else if (map.containsKey(rightNode)) {
						root = new Pair<String, Pair<?, ?>>(leftNode, map.get(rightNode));
						map.remove(rightNode);
					} else root = new Pair<String, String>(leftNode, rightNode);
					map.put(leftNode + rightNode, root);
					input = input.replace("(" + leftNode + "," + rightNode + ")", leftNode + rightNode);
				}
			
			// System.out.println(input);
		} while (input.contains(","));
		// System.out.println("ende");
	}
	
	@Override
	public String toString() {
		
		return root.toString();
		
	}
	
}
