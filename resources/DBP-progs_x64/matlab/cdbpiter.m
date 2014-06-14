function [B,S] = cdbpiter(D, k, t, bonus, program)
% CDBPITER - solves DBP using C program and iterative post-processing
% [B,S] = CDBPITER(D, k, t [, bonus] [, program]), where the inputs are:
% D        the dataset 
% k        size of the decomposition
% t        threshold
% bonus    bonus for covering 1s (optional, defaults to 1)
% program  full name of the program to be used (optional)
%
% and the outputs are:
% B        basis vector matrix
% S        usage matrix
  
  error(nargchk(3, 5, nargin))
  
  if nargin < 5,
    program = [fileparts(mfilename('fullpath')) '/../iter-solver/iter-solver'];
  end;
  if nargin < 4,
    bonus = 1;
  end;
  
  %% Write D to a file:
  [rows, cols]=size(D);
  prefix = tempname;
  tmpfid = fopen(prefix, 'w');  %% to reserve the file name
  datafile = [prefix '.data'];
  basisfile = [prefix '.basis'];
  decompfile = [prefix '.decomp'];
  fid = fopen(datafile, 'w');
  fprintf(fid, '%i\n%i\n', rows, cols);
  for i=1:rows,
    for j=1:cols,
      fprintf(fid, '%i ', D(i,j));
    end;
    fprintf(fid, '\n');
  end;
  fclose(fid);
  
  %% Solve DBP
  options = [' -s' datafile ' -b' basisfile ' -D' decompfile ' -k' ...
             int2str(k) ' -t' num2str(t) ' -p' int2str(bonus)];
  %disp([program options])
  [status,result] = unix([program options]);
  % disp([program options])
  if status ~= 0,
    error(result);
  end;
  
  %% Clean B and S files by removing first two lines
  temporary = tempname;
  [status,result] = unix(['tail -' int2str(k) ' ' basisfile ' > ' ...
                      temporary ' && mv ' temporary ' ' basisfile]);
  if status ~= 0,
    error(result);
  end;
  [status,result] = unix(['tail -' int2str(rows) ' ' decompfile ' > ' ...
                      temporary ' && mv ' temporary ' ' decompfile]);
  if status ~= 0,
    error(result);
  end;
  
  %% Read B and S
  B = load(basisfile);
  S = load(decompfile);
  
  %% Cleanup
  delete(datafile); delete(basisfile); delete(decompfile);
  fclose(tmpfid); delete(prefix);