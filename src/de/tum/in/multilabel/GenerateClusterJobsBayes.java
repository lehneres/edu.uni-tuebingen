package de.tum.in.multilabel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@SuppressWarnings ("all")
public class GenerateClusterJobsBayes {
	
	// static String command =
	// "#$-l vf=2000m\njava -Xmx2G -cp /home/proj/BIMSC/home/lehnerer/lxkramer/de.tum.in.multilabel/bin/:/home/proj/BIMSC/home/lehnerer/lxkramer/de.tum.in.multilabel/lib/weka.jar:/home/proj/BIMSC/home/lehnerer/lxkramer/de.tum.in.multilabel/lib/mulan.jar"
	// + " $mainMethod"
	// + " -id $id"
	// + " -output $output"
	// + " -t $dataset.arff"
	// + " -xml $dataset.xml"
	// + " -folds .33 -D"
	// + " -C \"-W $baseMethod\"";
	private static String	output												= "bayesResults";
	static String[]			additional											= { "-sCV -once", "-sCV", "-once", "" };
	static String[]			basemethods											= { "weka.classifiers.lazy.IBk",
			"weka.classifiers.functions.SMO", "weka.classifiers.trees.RandomForest" };
	static String			command												=
																						"#!/bin/bash\n#$-M lehnerer@in.tum.de\n#$-S /bin/bash\n#$-N job$id\n#$-o $HOME/job$id.out -j y\n#$-l mf=2000M\n#$-l march=x86_64\n. /etc/profile\nmodule load java\ncd\n"
																								+ "java -Xmx2G -cp $HOME/lxkramer/de.tum.in.multilabel/bin/:$HOME/lxkramer/de.tum.in.multilabel/lib/weka.jar:$HOME/lxkramer/de.tum.in.multilabel/lib/mulan.jar"
																								+ " $mainMethod"
																								+ " -id $id"
																								+ " -output $output"
																								+ " -t $dataset.arff"
																								+ " -xml $dataset.xml"
																								+ " -folds .33 -D"
																								+ " -C \"-W $baseMethod\"";
	
	static String[]			datasets											= {
			"$HOME/lxkramer/de.tum.in.multilabel/resources/SortedCC/enron",
			"$HOME/lxkramer/de.tum.in.multilabel/resources/SortedCC/llog",
			"$HOME/lxkramer/de.tum.in.multilabel/resources/SortedCC/medical",
			"$HOME/lxkramer/de.tum.in.multilabel/resources/SortedCC/emotions",
			"$HOME/lxkramer/de.tum.in.multilabel/resources/SortedCC/scene",
			"$HOME/lxkramer/de.tum.in.multilabel/resources/SortedCC/slashdot",
			"$HOME/lxkramer/de.tum.in.multilabel/resources/SortedCC/yeast"		};
	
	static String[]			mainmethods											= {
			"de.tum.in.mulan.classifier.transformation.SortedCC",
			"de.tum.in.mulan.classifier.transformation.EnsembleOfSortedCC",
			"de.tum.in.mulan.classifier.transformation.BinaryRelevance",
			"de.tum.in.mulan.classifier.transformation.ClassifierChains",
			"de.tum.in.mulan.classifier.transformation.EnsembleOfClassifierChains" };
	
	static String[]			searchalgos											= {
			// "weka.classifiers.bayes.net.search.ci.CISearchAlgorithm",
			// "weka.classifiers.bayes.net.search.ci.ICSSearchAlgorithm",
			// "weka.classifiers.bayes.net.search.global.GlobalScoreSearchAlgorithm",
			"weka.classifiers.bayes.net.search.global.K2",
			"weka.classifiers.bayes.net.search.global.TabuSearch",
			// "weka.classifiers.bayes.net.search.global.SimulatedAnnealing",
			// "weka.classifiers.bayes.net.search.global.TAN",
			"weka.classifiers.bayes.net.search.global.HillClimber",
			"weka.classifiers.bayes.net.search.global.RepeatedHillClimber",
			"weka.classifiers.bayes.net.search.local.K2",
			// "weka.classifiers.bayes.net.search.local.LocalScoreSearchAlgorithm",
			"weka.classifiers.bayes.net.search.local.TabuSearch",
			"weka.classifiers.bayes.net.search.local.HillClimber",
			"weka.classifiers.bayes.net.search.local.RepeatedHillClimber",
																				// "weka.classifiers.bayes.net.search.local.SimulatedAnnealing",
																				// "weka.classifiers.bayes.net.search.local.TAN",
																				// "weka.classifiers.bayes.net.search.local.LAGDHillClimber"
																						
																						};
	static String[]			searchalgoslocalWorkingWithoutClasslabel			= {
			"weka.classifiers.bayes.net.search.local.HillClimber",
			// "weka.classifiers.bayes.net.search.local.LAGDHillClimber",
			"weka.classifiers.bayes.net.search.local.RepeatedHillClimber",
			"weka.classifiers.bayes.net.search.local.TabuSearch"				};
	
	static String[]			searchalgosWithRandomSeed							= {
			// "weka.classifiers.bayes.net.search.global.SimulatedAnnealing",
			// "weka.classifiers.bayes.net.search.local.SimulatedAnnealing",
			"weka.classifiers.bayes.net.search.local.RepeatedHillClimber",
			"weka.classifiers.bayes.net.search.global.RepeatedHillClimber"		};
	
	static String[]			searchalgosWorkingWithoutClasslabelAndRandomSeed	=
																						{ "weka.classifiers.bayes.net.search.local.RepeatedHillClimber" };
	
	static String[]			sortmethods											= { "childDown", "childUp",
			"parentDown", "parentUp", "childDownD", "childUpD", "parentDownD", "parentUpD" };
	
	public static void main(final String[] args) throws IOException {
		int i = 1;
		for (final String dataset : GenerateClusterJobsBayes.datasets)
			for (final String basemethod : GenerateClusterJobsBayes.basemethods)
				for (final String mainmethod : GenerateClusterJobsBayes.mainmethods)
					if (mainmethod.equals("de.tum.in.mulan.classifier.transformation.SortedCC")) {
						for (final String add : GenerateClusterJobsBayes.additional)
							if (add.contains("once")) for (final String searchalgo : GenerateClusterJobsBayes.searchalgoslocalWorkingWithoutClasslabel)
								for (final String sortmethod : GenerateClusterJobsBayes.sortmethods) {
									final BufferedWriter writer =
											new BufferedWriter(new FileWriter(new File("./grid/job" + i)));
									writer.write(GenerateClusterJobsBayes.command.replace("$id", String.valueOf(i))
											.replace("$output", GenerateClusterJobsBayes.output + "Once")
											.replace("$mainMethod", mainmethod).replace("$dataset", dataset)
											.replace("$baseMethod", basemethod)
											+ " -B \"-D -Q $searchalgo -- -P 1000 -N\"".replace("$searchalgo",
													searchalgo) + " " + add + " -" + sortmethod);
									writer.flush();
									writer.close();
									i++;
								}
							else for (final String searchalgo : GenerateClusterJobsBayes.searchalgos)
								for (final String sortmethod : GenerateClusterJobsBayes.sortmethods) {
									final BufferedWriter writer =
											new BufferedWriter(new FileWriter(new File("./grid/job" + i)));
									writer.write(GenerateClusterJobsBayes.command.replace("$id", String.valueOf(i))
											.replace("$output", GenerateClusterJobsBayes.output)
											.replace("$mainMethod", mainmethod).replace("$dataset", dataset)
											.replace("$baseMethod", basemethod)
											+ " -B \"-D -Q $searchalgo -- -P 1000\"".replace("$searchalgo", searchalgo)
											+ " " + add + " -" + sortmethod);
									writer.flush();
									writer.close();
									i++;
								}
					} else if (mainmethod.equals("de.tum.in.mulan.classifier.transformation.EnsembleOfSortedCC")) {
						for (final String add : GenerateClusterJobsBayes.additional)
							if (add.contains("once")) for (final String searchalgo : GenerateClusterJobsBayes.searchalgosWorkingWithoutClasslabelAndRandomSeed)
								for (final String sortmethod : GenerateClusterJobsBayes.sortmethods) {
									final BufferedWriter writer =
											new BufferedWriter(new FileWriter(new File("./grid/job" + i)));
									writer.write(GenerateClusterJobsBayes.command.replace("$id", String.valueOf(i))
											.replace("$output", GenerateClusterJobsBayes.output + "Ensemble")
											.replace("$mainMethod", mainmethod).replace("$dataset", dataset)
											.replace("$baseMethod", basemethod)
											+ " -B \"-D -Q $searchalgo -- -P 1000 -N\"".replace("$searchalgo",
													searchalgo) + " " + add + " -" + sortmethod);
									writer.flush();
									writer.close();
									i++;
								}
							else for (final String searchalgo : GenerateClusterJobsBayes.searchalgosWithRandomSeed)
								for (final String sortmethod : GenerateClusterJobsBayes.sortmethods) {
									final BufferedWriter writer =
											new BufferedWriter(new FileWriter(new File("./grid/job" + i)));
									writer.write(GenerateClusterJobsBayes.command.replace("$id", String.valueOf(i))
											.replace("$output", GenerateClusterJobsBayes.output + "Ensemble")
											.replace("$mainMethod", mainmethod).replace("$dataset", dataset)
											.replace("$baseMethod", basemethod)
											+ " -B \"-D -Q $searchalgo\"".replace("$searchalgo", searchalgo)
											+ " "
											+ add + " -" + sortmethod);
									writer.flush();
									writer.close();
									i++;
								}
					} else {
						final BufferedWriter writer = new BufferedWriter(new FileWriter(new File("./grid/job" + i++)));
						writer.write(GenerateClusterJobsBayes.command.replace("$id", String.valueOf(i))
								.replace("$output", GenerateClusterJobsBayes.output + "Reference")
								.replace("$mainMethod", mainmethod).replace("$dataset", dataset)
								.replace("$baseMethod", basemethod));
						writer.flush();
						writer.close();
					}
	}
}
