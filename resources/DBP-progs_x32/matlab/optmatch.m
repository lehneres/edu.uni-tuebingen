function S = optmatch(D, B, penalty, verbose, program)
% OPTMATCH - finds optimal way to use B.
% S = OPTMATCH(D, B, [penalty], [verbose], [program]), where the inputs are
%       S      usage matrix
%       D      data matrix
%       B      basis vector matrix
% penalty      penalty for not covering 1s, optional
% verbose      verbosity level, optional
% program      program to use, optional
% 
% and the output is:
%       S      the matrix minimizing ||D - B o S||.
  
  if nargin < 5,
    program = [fileparts(mfilename('fullpath')) ...
               '/../create-decomp/create-decomp'];
  end;
  if nargin < 4,
    verbose = 0;
  end;
  if nargin < 3,
    penalty = 1;
  end;
  
  [n, d] = size(D);
  [k, dtmp] = size(B);
  if d ~= dtmp,
    error('Data has %i columns, but basis vectors have %i!\n', d, dtmp);
  end;
  
  %% Write D & B
  prefix = tempname;
  tmpfid = fopen(prefix, 'w');
  datafile = [prefix '.data'];
  basisfile = [prefix '.basis'];
  decompfile = [prefix '.decomp'];
  fid = fopen(datafile, 'w');
  fprintf(fid, '%i\n%i\n', n, d);
  for i=1:n,
    for j=1:d,
      fprintf(fid, '%i ', D(i,j));
    end;
    fprintf(fid, '\n');
  end;
  fclose(fid);
  fid = fopen(basisfile, 'w');
  fprintf(fid, '%i\n%i\n', k, d);
  for i=1:k,
    for j=1:d,
      fprintf(fid, '%i ', B(i,j));
    end;
    fprintf(fid, '\n');
  end;
  fclose(fid);

  %% Run program

  if verbose > 0,
    fprintf(2, 'start program');
  end;
  
  options = [' -s' datafile ' -b' basisfile ' -o' decompfile ...
             ' -p' int2str(penalty)];
  if verbose > 1,
    disp([program options]);
  end;
  [status, result] = unix([program options]);
  
  if status ~= 0,
    error(result);
  end;
  
  %% Read S
  temporary = tempname;
  [status,result] = unix(['tail -' int2str(n) ' ' decompfile ' > ' ...
                      temporary ' && mv ' temporary ' ' decompfile]);
  if status ~= 0,
    error(result);
  end;

  S = load(decompfile);
  
  %% Cleanup
  delete(datafile); delete(basisfile); delete(decompfile);
  fclose(tmpfid); delete(prefix);
