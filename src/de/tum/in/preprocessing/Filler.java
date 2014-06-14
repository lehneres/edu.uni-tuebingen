package de.tum.in.preprocessing;

import mulan.data.MultiLabelInstances;

@SuppressWarnings ("javadoc")
public interface Filler {
	public MultiLabelInstances fillMissing(MultiLabelInstances mli);
	
	public String getName();
}
