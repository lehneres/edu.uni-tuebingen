package de.tum.in.mulan.experiments;

import java.io.File;
import java.io.FileReader;
import java.util.Map;

import mulan.data.LabelNodeImpl;
import mulan.data.LabelsMetaDataImpl;
import mulan.data.MultiLabelInstances;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.ConverterUtils.DataSource;
import de.tum.in.mulan.classifier.meta.AbstractNoble;
import de.tum.in.preprocessing.EnsembleOfClassifierChainsFiller;
import de.tum.in.preprocessing.ValueCoordinate;

@SuppressWarnings ("all")
public class TestFiller {
	
	public static void main(final String args[]) throws Exception {
		final String leftFile = weka.core.Utils.getOption("left", args);
		final String topFile = weka.core.Utils.getOption("top", args);
		final String centerFile = weka.core.Utils.getOption("center", args);
		final String outputFile = weka.core.Utils.getOption("output", args);
		
		final Instances left = new Instances(new FileReader(new File(leftFile)));
		final Instances top = new Instances(new FileReader(new File(topFile)));
		final Instances center = new Instances(new FileReader(new File(centerFile)));
		
		AbstractNoble.debug("Filling up missing values...");
		
		// filling missing values in center matrix
		final int numChains = 5;
		
		final EnsembleOfClassifierChainsFiller filler1 =
				new EnsembleOfClassifierChainsFiller(new RandomForest(), numChains);
		
		final EnsembleOfClassifierChainsFiller filler2 =
				new EnsembleOfClassifierChainsFiller(new RandomForest(), numChains);
		
		final LabelsMetaDataImpl leftMeta = new LabelsMetaDataImpl();
		for (int i = 0; i < center.numAttributes(); i++)
			leftMeta.addRootNode(new LabelNodeImpl(center.attribute(i).name()));
		
		// FIXME hack as there seems to be a bug in WEKA. Cannot merge
		// instances which
		// are generated by deleting from same dataset
		
		final ArffSaver saver = new ArffSaver();
		saver.setInstances(left);
		final String fileleft = "/tmp/left" + System.currentTimeMillis() + ".arff";
		final String fileright = "/tmp/right" + System.currentTimeMillis() + ".arff";
		saver.setFile(new File(fileleft));
		saver.writeBatch();
		saver.setInstances(center);
		saver.setFile(new File(fileright));
		saver.writeBatch();
		
		DataSource source = new DataSource(fileleft);
		final Instances leftcopy = source.getDataSet();
		
		source = new DataSource(fileright);
		final Instances centercopy = source.getDataSet();
		
		// FIXME hack end
		
		final Instances allLeft = Instances.mergeInstances(leftcopy, centercopy);
		
		final FastVector<Attribute> atts = new FastVector<Attribute>();
		final LabelsMetaDataImpl rightMeta = new LabelsMetaDataImpl();
		for (int i = 0; i < center.numInstances(); i++) {
			final FastVector<String> attVals = new FastVector<String>();
			
			attVals.addElement("0");
			attVals.addElement("1");
			
			atts.addElement(new Attribute("att" + i, attVals));
			rightMeta.addRootNode(new LabelNodeImpl("att" + i));
		}
		
		final Instances centertrans = new Instances("transposed", atts, 0);
		
		for (int i = 0; i < center.numAttributes(); i++) {
			final double[] vals = center.attributeToDoubleArray(i);
			centertrans.add(new DenseInstance(1.0, vals));
		}
		
		final Instances allRight = Instances.mergeInstances(top, centertrans);
		
		final MultiLabelInstances fromLeft = new MultiLabelInstances(allLeft, leftMeta);
		final MultiLabelInstances fromRight = new MultiLabelInstances(allRight, rightMeta);
		
		// Noble.debug("filling from left...");
		// filler1.fillMissing(fromLeft);
		// Noble.debug("filling from left...done");
		// Noble.debug("filling from right...");
		// filler2.fillMissing(fromRight);
		// Noble.debug("filling from right...done");
		
		AbstractNoble.debug("filling from left...");
		final Map<ValueCoordinate, Double> fillmapleft = filler1.getVotes(fromLeft);
		AbstractNoble.debug("filling from left...done");
		AbstractNoble.debug("filling from right...");
		final Map<ValueCoordinate, Double> fillmaptop = filler2.getVotes(fromRight);
		AbstractNoble.debug("filling from right...done");
		
		for (int i = 0; i < center.numInstances(); i++)
			for (int j = 0; j < center.numAttributes(); j++)
				if (center.instance(i).isMissing(j)) {
					// double val = (summatrix.get(i,j)/20<0.5)?0.0:1.0;
					final double leftdoub = fillmapleft.get(new ValueCoordinate(i, j + left.numAttributes()));
					final double topdoub = fillmaptop.get(new ValueCoordinate(j, i + top.numAttributes()));
					
					final double val = (topdoub + leftdoub) / (numChains * 2) <= 0.5 ? 0.0 : 1.0;
					center.instance(i).setValue(j, val);
				}
		
		saver.setFile(new File(outputFile));
		saver.setInstances(center);
		saver.writeBatch();
	}
	
}
