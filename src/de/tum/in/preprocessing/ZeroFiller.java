package de.tum.in.preprocessing;

import java.util.ArrayList;
import java.util.List;

import mulan.data.MultiLabelInstances;
import weka.core.Instance;

/**
 * @author LehnereS
 */
public class ZeroFiller implements Filler {
	
	private static List<Integer> getMissing(final MultiLabelInstances copy, final int l) {
		final List<Integer> result = new ArrayList<>();
		for (int i = 0; i < copy.getDataSet().numInstances(); i++) {
			final Instance inst = copy.getDataSet().get(i);
			if (inst.isMissing(l)) result.add(i);
		}
		return result;
	}
	
	@Override
	public MultiLabelInstances fillMissing(final MultiLabelInstances mli) {
		final MultiLabelInstances copy = mli.clone();
		// For each label
		for (final int l : copy.getLabelIndices()) {
			final List<Integer> missing = getMissing(copy, l);
			for (int i = 0; i < missing.size(); i++) {
				final Instance inst = copy.getDataSet().get(missing.get(i));
				inst.setValue(l, 0.0);
			}
		}
		return copy;
	}
	
	@Override
	public String getName() {
		return "ZeroFiller";
	}
	
}
