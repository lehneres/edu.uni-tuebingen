use Switch;
$directory = "../results/field/31052011";
$result    = "";
@dataSetsLRZ =
  ( "medical", "llog", "enron", "emotions", "scene", "slashdot", "yeast" );
@multiLabelLearner = (
					   "BinaryRelevance",
					   "ClassifierChain",
					   "EnsembleOfClassifierChains",
					   "LabelPowerset",
					   "MultiLabelStacking",
					   "mulan.classifier.lazy.MLkNN",
					   "SortedCC",
);
@singleLabelLearner = ( "IBk", "SMO",          "RandomForest" );
@clusterAlgorithmen = ( "EM",  "SimpleKMeans", "HierarchicalClusterer" );
@distanceFunctions = (
					   "ChebyshevDistance", "EuclideanDistance",
					   "ManhattanDistance", "MinkowskiDistance"
);
@clusterMethods = (
					"SINGLE",      "COMPLETE",
					"AVERAGE",     "MEAN",
					"CENTROID",    "WARD",
					"ADJCOMLPETE", "NEIGHBOR_JOINING"
);
@ASSearch = ( "BestFirst", "GreedyStepwise", "Ranker" );
@ASEvaluation = (
				  "CfsSubsetEval",          "ReliefFAttributeEval",
				  "GainRatioAttributeEval", "InfoGainAttributeEval",
				  "OneRAttributeEval",      "PrincipalComponents"
);
opendir DIR, $directory or die "ne";

while ( $entry = readdir DIR )
{
	if ( $entry =~ m/MLA/ )
	{
		open( in, "<$directory/$entry" );
		$curRes = "";
		while (<in>)
		{
			if ( $_ =~ m/^this is.*\n/ )
			{
				@arr = split( /,/, $_ );
				for ( $k = 0 ; $k < @arr ; $k++ )
				{
					switch ( @arr[$k] )
					{
						case "-data"
						{
							@arr[ $k + 1 ] = @dataSetsLRZ[ @arr[ $k + 1 ] ]
						}
						case "-ml"
						{
							@arr[ $k + 1 ] = @multiLabelLearner[ @arr[ $k + 1 ] ]
						}
						case "-sl"
                        {
                            @arr[ $k + 1 ] = @singleLabelLearner[ @arr[ $k + 1 ] ]
                        }
                        case "-cl"
                        {
                        	@arr[ $k + 1 ] = @clusterAlgorithmen[ @arr[ $k + 1 ] ]
                        }
                        case "-dm"
                        {
                        	@arr[ $k + 1 ] = @distanceFunctions[ @arr[ $k + 1 ] ]
                        }
                        case "-cm"
                        {
                            @arr[ $k + 1 ] = @clusterMethods[ @arr[ $k + 1 ] ]
                        }
                        case "-as"
                        {
                            @arr[ $k + 1 ] = @ASSearch[ @arr[ $k + 1 ] ]
                        }
                        case "-ae"
                        {
                            @arr[ $k + 1 ] = @ASEvaluation[ @arr[ $k + 1 ] ]
                        }
					}
				}
				$str = join( ",", @arr );
				chomp($str);
				if ( @arr < 16 )
				{
					for ( $i = 0 ; $i < ( 16 - @arr ) ; $i++ )
					{
						$str = $str . ",";
					}
				}
				$curRes = $curRes . $str . ",";

				#"data" . $1 . "\t" . $2 . "\t" . $3 . "\t";
			} elsif ( $_ =~ m/^Example-Based Accuracy: (0\.\d{4})/ )
			{
				$curRes = $curRes . "ex!" . $1 . ",";
			} elsif ( $_ =~ m/^Average Precision: (0\.\d{4})/ )
			{
				$curRes = $curRes . "prec! " . $1 . ",";
			}
		}
		if ( length($curRes) > 1 )
		{
			$result = $result . $curRes . "\n";
		}
		close in;
	}
}
closedir DIR;
print $result;
