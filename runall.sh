#!/bin/bash


mkdir results_biodeg_coding2_imp_ecc_rf_biodeg_coding2

time java -Xmx5G -cp mulan-tum.jar:lib/mulan.jar:lib/weka.jar:lib/gridSearch.jar:lib/partialLeastSquares.jar de.tum.in.mulan.experiments.TCast -ds data/all_ml_coding2 -results results_biodeg_coding2_imp_ecc_rf_biodeg_coding2/  -basecl 1 -mlcl 3 -numfolds -1 -biodeg -noimpute -models models_biodeg_coding2_imp_ecc_rf_biodeg_coding2_2 2> log_biodeg_coding2_imp_ecc_rf_biodeg_coding2

mkdir results_biodeg_coding2_imp_mlknn_biodeg_coding2

time java -Xmx5G -cp mulan-tum.jar:lib/mulan.jar:lib/weka.jar:lib/gridSearch.jar:lib/partialLeastSquares.jar de.tum.in.mulan.experiments.TCast -ds data/all_ml_coding2 -results results_biodeg_coding2_imp_mlknn_biodeg_coding2/  -basecl 1 -mlcl 6 -numfolds -1 -biodeg  -noimpute -models models_biodeg_coding2_imp_mlknn_rf_biodeg_coding2_2 2> log_biodeg_coding2_imp_mlknn_biodeg_coding2

mkdir results_biodeg_coding2_imp_br2_rf_biodeg_coding2

time java -Xmx5G -cp mulan-tum.jar:lib/mulan.jar:lib/weka.jar:lib/gridSearch.jar:lib/partialLeastSquares.jar de.tum.in.mulan.experiments.TCast -ds data/all_ml_coding2 -results results_biodeg_coding2_imp_br2_rf_biodeg_coding2/  -basecl 1 -mlcl 2 -numfolds -1 -biodeg -noimpute -models models_biodeg_coding2_imp_br2_rf_biodeg_coding2_2 2> log_biodeg_coding2_imp_br2_rf_biodeg_coding2

mkdir results_biodeg_coding2_imp_lp_rf_biodeg_coding2

time java -Xmx5G -cp mulan-tum.jar:lib/mulan.jar:lib/weka.jar:lib/gridSearch.jar:lib/partialLeastSquares.jar de.tum.in.mulan.experiments.TCast -ds data/all_ml_coding2 -results results_biodeg_coding2_imp_lp_rf_biodeg_coding2/  -basecl 1 -mlcl 5 -numfolds -1 -biodeg -noimpute -models models_biodeg_coding2_imp_lp_rf_biodeg_coding2_2 2> log_biodeg_coding2_imp_lp_rf_biodeg_coding2

mkdir results_biodeg_coding2_imp_clr_rf_biodeg_coding2

time java -Xmx5G -cp mulan-tum.jar:lib/mulan.jar:lib/weka.jar:lib/gridSearch.jar:lib/partialLeastSquares.jar de.tum.in.mulan.experiments.TCast -ds data/all_ml_coding2 -results results_biodeg_coding2_imp_clr_rf_biodeg_coding2/  -basecl 1 -mlcl 4 -numfolds -1 -biodeg -noimpute -models models_biodeg_coding2_imp_clr_rf_biodeg_coding2_2 2> log_biodeg_coding2_imp_cls_rf_biodeg_coding2

mkdir results_biodeg_coding2_imp_il_rf_biodeg_coding2

time java -Xmx5G -cp mulan-tum.jar:lib/mulan.jar:lib/weka.jar:lib/gridSearch.jar:lib/partialLeastSquares.jar de.tum.in.mulan.experiments.TCast -ds data/all_ml_coding2 -results results_biodeg_coding2_imp_il_rf_biodeg_coding2/  -basecl 1 -mlcl 7 -numfolds -1 -biodeg  -noimpute -models models_biodeg_coding2_imp_il_rf_biodeg_coding2_2 2> log_biodeg_coding2_imp_il_rf_biodeg_coding2
