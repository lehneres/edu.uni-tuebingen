function [B,S,clusters] = cdbpcluster(D,c,k,t,b,verbosity)
% CDBPCLUSTER - Solves DBP in clusters
% [B, S, clusters] = CDBPCLUSTER(D, c, k, t[, b][, verbose])
% Input:
%   D        data matrix
%   c        # of clusters
%   k        # of basis vectors
%   t        threshold value
%   b        bonus for covering 1's (optional, default = 1)
% verbose    verbosity level (optional, default = 0)
%
% Output:
%   B        basis vector matrix
%   S        basis usage matrix
% clusters   cluster indices.
  error(nargchk(4, 6, nargin))
  if nargin < 6,
    verbosity = 0;
    if nargin < 5,
      b = 1;
    end;
  end;
  
  [rows,cols]=size(D);
  %% Cluster data using SOM k-means
  if verbosity > 0,
    fprintf(1, 'clustering');
  end;
  if c > 1,
    [codes,clusters] = som_kmeans('batch', D, c);
  else 
    clusters = repmat(1,1,rows);
  end;
  if verbosity > 2,
    disp(clusters);
  end;

  %% Compute a set of candidate basis vectors within each cluster
  Aa = {};
  if verbosity > 0,
    fprintf(1, '\r                          \rcomputing associations:   ');
  end;
  for i=1:c,
    T = D(clusters==i,:);
    if isempty(T),
      continue;
    end;
    T = T'*T;  %% association values
    di = diag(T);  %% indices of all-zero rows
    T(di>0,:) = T(di>0,:)./repmat(di(di>0),1,cols);  %% normalize
    T = T(di>0, :);
    Aa{i} = double(T>=t);
    if verbosity > 0,
      fprintf(1, '\b\b%2i', i);
    end;
  end;
  
  %% Start to select best basis vectors from A.
  B = zeros(k, cols);
  S = zeros(rows, k);
  
  if verbosity > 0,
    fprintf(1, ['\r                                             '...
                '\rcomputing basis vectors:  ']);
  end;
  for i=1:k,
    %% compute the number of noncovered 1's in each cluster
    if verbosity > 0,
      fprintf(1, ['\r                                                 '...
                  '\rcomputing basis vectors: %i  '], i);
    end;
    
    R = min(1, S*B);
    for j=1:c,
      if verbosity > 0,
        fprintf(1, '\b\b%2i', j);
      end;
      clustercounts(j) = sum(sum(double(D(clusters==i,:) == 1 ...
                                        & R(clusters==i,:) == 0)));
    end;
    clust = find(clustercounts == max(clustercounts));
    clust = clust(1);  %% in case that there are many clusters
    
    %% 'clust' is now the cluster we want to work with
    A = Aa{clust};
    [nr, nc] = size(A);
    bestCover = -1;  %% We need to initialize this.
    bestJ = -1;
%    ncr = length(find(clusters==clust));
%    Dc = D(clusters==clust,:);
%    Rc = R(clusters==clust,:);
    if verbosity > 0,
      fprintf(1, '   ');
    end;
    for j=1:nr,
     
      if verbosity > 0,
        fprintf(1, '\b\b%2i', j);
      end;
     
      Aj = repmat(A(j,:),rows,1);      
      rowcount = sum(b * double(D == 1 & R == 0 & Aj == 1) -...
                     double(D == 0 & R == 0 & Aj == 1), 2);
      cover = sum(rowcount(rowcount > 0));
      
      if verbosity > 3,
        Aj
        Dc
        Rc
        rowcount
        cover
      end;
      
      if cover > bestCover,
        bestCover = cover;
        bestRowcount = rowcount;
        bestJ = j;
      end;
    end;
    
    %% So now A(bestJ,:) is our new basis vector.
    B(i,:) = A(bestJ,:);
    
    %% We will use it based on bestRowcount.
    S(:,i) = double(bestRowcount > 0);
    
  end;
