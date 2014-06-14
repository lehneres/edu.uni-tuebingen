package de.tum.in.preprocessing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mulan.data.MultiLabelInstances;
import weka.core.Instance;

/**
 * @author LehnereS
 */
public class MajorityFiller implements Filler {
	
	private static double getMajorityClass(final MultiLabelInstances copy, final int l) {
		int cntPos = 0;
		int cntNeg = 0;
		for (int i = 0; i < copy.getDataSet().numInstances(); i++) {
			final Instance inst = copy.getDataSet().get(i);
			if (!inst.isMissing(l)) if (inst.value(l) == 1.0) cntPos++;
			else cntNeg++;
		}
		if (cntPos >= cntNeg) return 1.0;
		return 0.0;
	}
	
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
			// calculate the majority class
			final double majorityClass = getMajorityClass(copy, l);
			final List<Integer> missing = getMissing(copy, l);
			// fill gaps according to majority
			Collections.shuffle(missing);
			for (int i = 0; i < missing.size(); i++) {
				final Instance inst = copy.getDataSet().get(missing.get(i));
				inst.setValue(l, majorityClass);
			}
		}
		return copy;
	}
	
	@Override
	public String getName() {
		return "Majority";
	}
	
}
