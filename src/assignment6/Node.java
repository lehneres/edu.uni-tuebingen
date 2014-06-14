package assignment6;

import java.util.List;

@SuppressWarnings ("all")
public class Node {
	
	public String		name;
	public List<Node>	interacting;
	
	@Override
	public String toString() {
		return name;
	}
}
