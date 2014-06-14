package de.tum.in.preprocessing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mulan.data.MultiLabelInstances;
import weka.core.Instance;

/**
 * @author LehnereS
 */
public class RandomFiller implements Filler {
	
	private static double getClassDistribution(final MultiLabelInstances copy, final int l) {
		int cnt = 0;
		for (int i = 0; i < copy.getDataSet().numInstances(); i++) {
			final Instance inst = copy.getDataSet().get(i);
			if (!inst.isMissing(l) && inst.value(l) == 1.0) cnt++;
		}
		return (double) cnt / copy.getDataSet().numInstances();
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
			// calculate the class distribution
			final double distribution = getClassDistribution(copy, l);
			final List<Integer> missing = getMissing(copy, l);
			// fill gaps according to distribution
			Collections.shuffle(missing);
			missing.size();
			final long numFillPositive = Math.round(distribution * missing.size());
			for (int i = 0; i < missing.size(); i++) {
				final Instance inst = copy.getDataSet().get(missing.get(i));
				if (i <= numFillPositive) inst.setValue(l, 1.0);
				else inst.setValue(l, 0.0);
			}
		}
		// System.out.println("RandomFiller: Missing total: "+sumMissing);
		return copy;
	}
	
	@Override
	public String getName() {
		return "Random";
	}
	
}
