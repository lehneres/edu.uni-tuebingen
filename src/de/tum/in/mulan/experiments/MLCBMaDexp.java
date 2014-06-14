package de.tum.in.mulan.experiments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import mulan.data.MultiLabelInstances;
import mulan.evaluation.Evaluator;
import mulan.evaluation.MultipleEvaluation;
import weka.classifiers.trees.RandomForest;
import de.tum.in.mulan.classifier.transformation.MLCMAD;

@SuppressWarnings ("javadoc")
public class MLCBMaDexp {
	
	@SuppressWarnings ("unused")
	public static void main(String[] args) throws Exception {
		
		final String name = "yeast";
		
		final MultiLabelInstances mli = new MultiLabelInstances("data/" + name + ".arff", "data/" + name + ".xml");
		
		@SuppressWarnings ("resource")
		final BufferedWriter bw = new BufferedWriter(new FileWriter(new File("results_mlcmad_" + name)));
		
		for (int k = 7; k <= Math.min(64, mli.getNumLabels()); k++) {
			for (double t = 0.5; t <= 1.0; t += 0.1) {
				
				final RandomForest rf = new RandomForest();
				
				final MLCMAD classif = new MLCMAD(rf);
				classif.setThreading(false);
				classif.setK(k);
				classif.setT(t);
				classif.setExternalDecompCommand("resources/doDBP.sh $IN $OUT1 " + t + " " + k + " $OUT2");
				final Evaluator eval = new Evaluator();
				
				final int numFolds = 10;
				
				final MultipleEvaluation results = eval.crossValidate(classif, mli, numFolds);
				// System.out.println(results);
				
				bw.write(results.getMean("Hamming Loss") + " " + results.getMean("Subset Accuracy") + " "
						+ results.getMean("Example-Based Precision") + " " + results.getMean("Example-Based Recall")
						+ " " + results.getMean("Example-Based F Measure") + " "
						+ results.getMean("Example-Based Accuracy") + " " + results.getMean("Micro-averaged Precision")
						+ " " + results.getMean("Micro-averaged Recall") + " "
						+ results.getMean("Micro-averaged F-Measure") + " "
						+ results.getMean("Macro-averaged Precision") + " " + results.getMean("Macro-averaged Recall")
						+ " " + results.getMean("Macro-averaged F-Measure") + " "
				/*
				 * + results.getMean("Average Precision")+" "+ results.getMean("Coverage")+" "+
				 * results.getMean("OneError")+" "+ results.getMean("IsError")+" "+ results.getMean("ErrorSetSize")+" "+
				 * results.getMean("Ranking Loss")+" "+ results.getMean("Mean Average Precision")+" "+
				 * results.getMean("Micro-averaged AUC")+" "+ results.getMean("Macro-averaged AUC")+""
				 */);
				
				// results = eval.crossValidate(learner2, dataset, numFolds);
				break;
				// System.out.println(results);
			}
			break;
			
		}
		
		bw.close();
	}
	
}