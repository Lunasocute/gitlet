# Gitlet Design Document

**Name**: Luna Tian

## Classes and Data Structures
### Class 1
Main

contains all the command and call corresponding method in Repository

#### Fields

### Class 1
Repository 

This is where the main logic of our program will live. (contains add, remove etc methods)

#### Fields
File CWD = new File(System.getProperty("user.dir"));

File GITLET_DIR = join(CWD, ".gitlet");

File COMMIT = join(GITLET_DIR, "commit");

File BLOB_FILES = join(GITLET_DIR, "blob_files");

File STAGE_ADD = join(GITLET_DIR, "stage_add");

File STAGE_REMOVE = join(GITLET_DIR, "stage_remove");

File BRANCHES = join(GITLET_DIR, "branches");

### Class 2
Commit

represents the commit and the parents' reference and fileMap, also with message and timestamp.

#### Fields
ArrayList<String> parent = new ArrayList<>();

HashMap<String, String> fileMap;  

String message;

Date timestamp;
### Class 3
Branch

contains the current branch's name and a TreeMap which corresponding to head commit of different branches' name.

#### Fields
String currbc;   represent current branch's Head Commit Sha1

TreeMap<String, String> branchMap;   ex; <"Master", head commit's Sha1>
### Class 3
BlobHashMap;  store all the file's data by using hashcode

#### Fields
int init_size;   

Collection<Blob>[] allblobs;

double loadfactor;

int itemSize; 

Set<String> keySet = new HashSet<>();
## Algorithms

## Persistence
File CWD = new File(System.getProperty("user.dir"));

File GITLET_DIR = join(CWD, ".gitlet");

File COMMIT = join(GITLET_DIR, "commit");

File BLOB_FILES = join(GITLET_DIR, "blob_files");

File STAGE_ADD = join(GITLET_DIR, "stage_add");

File STAGE_REMOVE = join(GITLET_DIR, "stage_remove");

File BRANCHES = join(GITLET_DIR, "branches");
