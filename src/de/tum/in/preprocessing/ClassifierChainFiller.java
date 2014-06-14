package de.tum.in.preprocessing;

import java.util.Arrays;

import mulan.data.MultiLabelInstances;
import weka.classifiers.Classifier;
import weka.core.Instances;

@SuppressWarnings ("javadoc")
public class ClassifierChainFiller extends AbstractClassifierChainFiller {
	private int[]	order;
	
	public ClassifierChainFiller() {
		super();
	}
	
	public ClassifierChainFiller(final Classifier baseClassifier) {
		super(baseClassifier);
	}
	
	public ClassifierChainFiller(final Classifier baseClassifier, final int[] order) {
		super(baseClassifier);
		this.order = order;
	}
	
	@Override
	public MultiLabelInstances fillMissing(final MultiLabelInstances mli) {
		final MultiLabelInstances copy = mli.clone();
		if (order == null) order = getRandomOrder(mli.getLabelIndices());
		
		for (int i = 0; i < order.length; i++) {
			int l = order[i];
			// generate tariningSet
			final MultiLabelInstances train = copy.clone();
			if (i < order.length - 1) {
				train.getDataSet().setClassIndex(-1);
				l = deleteAtts(l, Arrays.copyOfRange(order, i + 1, order.length), train.getDataSet());
			}
			train.getDataSet().setClassIndex(l);
			// write("train_"+i+"_"+train.getDataSet().attribute(l).name()+".html",
			// DatasetHtmlRenderer.renderHtml(train));
			final Instances trainingSet = filter(false, l, train.getDataSet());
			trainingSet.setClassIndex(l);
			final Classifier c = getCopyOfBaseClassifier();
			// train learn set (a1,...,an,yi+1,ym) where yi != null
			try {
				c.buildClassifier(trainingSet);
			} catch (final Exception e1) {
				throw new RuntimeException(e1);
			}
			// generate predictionSet
			final int[] missing = filterIndices(true, l, train.getDataSet());
			
			for (final int element : missing)
				try {
					double prediction;
					prediction = c.classifyInstance(train.getDataSet().get(element));
					copy.getDataSet().get(element).setValue(order[i], prediction);
				} catch (final Exception e) {
					throw new RuntimeException("Exception in fillMissing", e);
				}
		}
		return copy;
	}
	
	@Override
	protected String getTransformationName() {
		return "CC";
	}
}
