d <-read.table("../data_out.csv", header=T, sep=",") #TODO: adapt file name
d_mean <- rowMeans(d[,8:16]) #TODO: adapt column indices to your table
d_maxfc <- apply(d[,8:16],1,function(x) max(max(x)-mean(x), abs(min(x)-mean(x)))) #TODO: adapt column indices to your table
d_filtered <- d[which(d_maxfc > 1.0),]
d_filtered_z <- apply(d_filtered[,8:16],1,scale) #TODO: adapt column indices to your table
jpeg('heatmap.jpg')
heatmap(d_filtered_z)
dev.off()
