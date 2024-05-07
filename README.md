Dido Attic
==========

Old archived Dido code. 

Note
----

`dido-old` was copied from the `dido/dido-attic`. We seem to have lost history
so this might not be the best way

In `dido` we created a branch with just the `dido-attic` 

```
git subtree split -P dido-attic -b dido-old
```

Then in `diod-attic` we created a subtree from this branch
```
git subtree add -P dido-old ../dido/.git dido-old
```

Other techniques are to use `git filter-branch`, which seems deprecated and
`git-filter-repo` which required downloading something so wasn't tried.

references for moving a directory into a different repo:
- The subtree approach: https://stackoverflow.com/questions/359424/detach-move-subdirectory-into-separate-git-repository
- The filter-repo approach: https://docs.github.com/en/get-started/using-git/splitting-a-subfolder-out-into-a-new-repository
- The filter-branch approach: https://gist.github.com/trongthanh/2779392

Moving the old repositories `dido-poi` and `dido-assembly` was done with

```
git subtree add -P dido-poi ../dido-poi master
```
and 
```
git subtree add -P dido-assembly ../dido-assembly master
```

This seems to have kept history.

references for moving repositories to subdirectories:
- https://stackoverflow.com/questions/47559855/git-move-repository-to-a-subfolder-of-another-repository

