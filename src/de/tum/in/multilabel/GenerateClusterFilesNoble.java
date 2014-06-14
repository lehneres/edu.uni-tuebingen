package de.tum.in.multilabel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author LehnereS
 */
@SuppressWarnings ("all")
public class GenerateClusterFilesNoble {
	
	static String[]	additional	= { " -bipart", " " };
	
	static String[]	datasets	= { "NCI", "Kinase" };
	static String[]	methods		= { "IBk", "SMO", "RandomForest" };
	static String	tmp			=
										"#!/bin/bash\n#$-M lehnerer@in.tum.de\n#$-S /bin/bash\n#$-N Njob$id\n#$-o $HOME/noble/job$id.out -j y\n#$-l mf=2000M\n#$-l march=x86_64\n. /etc/profile\nmodule load java\ncd\n"
												+ "java -cp $workingbin/:$workinglib/weka.jar:$workinglib/mulan.jar de.tum.in.mulan.classifier.transformation.Noble "
												+ " -id $id"
												+ " -main $workingresources/$data/$data.arff"
												+ " -labels $workingresources/$data/$data_labels.arff"
												+ " -mainXML $workingresources/$data/$data.xml"
												+ " -S $method -k $k -t $t"
												+ " -dir $working$data -output $workingresults$data"
												+ " -folds 10"
												+ " -dbp $workingresources/DBP-progs_x64" + " -keep";
	static String	working		= "$HOME/lxkramer/de.tum.in.multilabel/";
	
	// static String working = "./";
	
	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(final String[] args) throws IOException {
		int i = 1;
		for (final String method : GenerateClusterFilesNoble.methods)
			for (final String data : GenerateClusterFilesNoble.datasets) {
				if (data.equals("NCI") && method.equals("IBk")) continue;
				for (double t = 0.1; t <= 1; t += .1)
					for (int k = 2; k <= 64; k++)
						for (final String add : GenerateClusterFilesNoble.additional) {
							final BufferedWriter writer =
									new BufferedWriter(new FileWriter(new File("./grid/Njob" + i)));
							writer.write(GenerateClusterFilesNoble.tmp.replace("$id", String.valueOf(i))
									.replace("$data", data).replace("$working", GenerateClusterFilesNoble.working)
									.replace("$method", method).replace("$t", String.valueOf(t))
									.replace("$k", String.valueOf(k))
									+ add);
							writer.flush();
							writer.close();
							i++;
						}
			}
	}
}
