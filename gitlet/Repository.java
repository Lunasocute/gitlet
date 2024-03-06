package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import static gitlet.Main.exitWithError;
import static gitlet.Utils.*;


/** Represents a gitlet repository.
 *  This is where the main logic of our program will live. (contains add, remove etc methods)
 *  does at a high level.
 *
 *  @author Luna, Tian
 */
public class Repository {

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));

    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    /** The commit directory. */
    public static final File COMMIT = join(GITLET_DIR, "commit");

    /** The blob_files directory. including all the files' data */
    public static final File BLOB_FILES = join(GITLET_DIR, "blob_files");

    /** The stage_add directory. */
    public static final File STAGE_ADD = join(GITLET_DIR, "stage_add");

    /** The stage_remove directory. */
    public static final File STAGE_REMOVE = join(GITLET_DIR, "stage_remove");

    /** The branches directory. */
    public static final File BRANCHES = join(GITLET_DIR, "branches");

    /** The config directory. (for remote) */
    public static final File CONFIG = join(GITLET_DIR, "config");


    /**
     * set up initialized system
     */
    public static class Branch implements Serializable {
        TreeMap<String, String> branchMap;
        String currbc;

        Branch() {
            branchMap = new TreeMap<>();
            currbc = "master";
        }

        public static void addBranch(String name) {
            Branch thisB = readObject(BRANCHES, Branch.class);
            TreeMap<String, String> currMap = thisB.branchMap;
            if (exist(name)) {
                exitWithError("A branch with that name already exists.");
            }
            currMap.put(name, currMap.get(thisB.currbc));
            Utils.writeObject(BRANCHES, thisB);
        }

        public static void rmBranch(String bname) {
            Branch thisB = readObject(BRANCHES, Branch.class);
            TreeMap<String, String> currMap = thisB.branchMap;
            if (!exist(bname)) {
                exitWithError("A branch with that name does not exist.");
            } else if (self(bname)) {
                exitWithError("Cannot remove the current branch.");
            }
            currMap.remove(bname);
            Utils.writeObject(BRANCHES, thisB);
        }

        public static void updateBranch(Commit curr) {
            Branch thisB = readObject(BRANCHES, Branch.class);
            thisB.branchMap.put(thisB.currbc, Utils.sha1(serialize(curr)));
            Utils.writeObject(BRANCHES, thisB);
        }

        private static boolean exist(String name) {
            Branch thisB = readObject(BRANCHES, Branch.class);
            TreeMap<String, String> currMap = thisB.branchMap;
            if (currMap.containsKey(name)) {
                return true;
            }
            return false;
        }

        private static boolean self(String name) {
            Branch thisB = readObject(BRANCHES, Branch.class);
            if (thisB.currbc.equals(name)) {
                return true;
            }
            return false;
        }
    }


    public static void setupInit() {
        if (GITLET_DIR.exists()) {
            exitWithError("A Gitlet version-control system already "
                    + "exists in the current directory.");
        }
        GITLET_DIR.mkdir();
        COMMIT.mkdir();
        BLOB_FILES.mkdir();
        STAGE_ADD.mkdir();
        STAGE_REMOVE.mkdir();
        CONFIG.mkdir();

        //BlobHashMap blobH = new BlobHashMap();
        //File blobHashMap = Utils.join(BLOB_FILES, "blob_map");
        //writeObject(blobHashMap, blobH);

        Commit curr = new Commit(null, "initial commit");
        curr.safeCommit();     //save ComNode in COMMIT

        Branch newB = new Branch();              //create new branch and save
        newB.branchMap.put("master", Utils.sha1(serialize(curr)));
        writeObject(Utils.join(BRANCHES), newB);
    }


    /**
     * overwrite if edit the files
     * if identical to current commit file , do not stage
     * the file will not for removal, if it has been flagged for remove
     */
    public static void addFile(String fileN) {
        File forAdd = Utils.join(CWD, fileN);
        if (!forAdd.exists()) {
            exitWithError("File does not exist.");
        }
        File rmFile = Utils.join(STAGE_REMOVE, fileN);
        if (rmFile.exists()) {
            rmFile.delete();
        }
        String shaName = Utils.sha1(readContents(forAdd));
        Commit curr = currCom();
        HashMap<String, String> currMap = curr.fileMap;
        if (currMap.containsKey(fileN) && currMap.get(fileN).equals(shaName)) {
            if (Utils.join(STAGE_ADD, fileN).exists()) {
                Utils.join(STAGE_ADD, fileN).delete();
            }
            return;
        }
        File copyFile = Utils.join(STAGE_ADD, fileN);
        writeContents(copyFile, readContents(forAdd));
    }


    public static void remove(String filename) {
        File stageadd = Utils.join(STAGE_ADD, filename);
        boolean check = false;
        if (stageadd.exists()) {
            stageadd.delete();
            check = true;
        }
        if (currCom().fileMap.containsKey(filename)) {
            String fileSha = currCom().fileMap.get(filename);
            File remove = Utils.join(BLOB_FILES, fileSha);
            File inRemove = Utils.join(STAGE_REMOVE, filename);   //stage for removal
            writeContents(inRemove, readContents(remove));
            if (join(CWD, filename).exists()) {
                restrictedDelete(join(CWD, filename));            //delete CWD file
            }
            check = true;
        }
        if (!check) {
            System.out.println("No reason to remove the file.");
        }
    }


    public static void commit(String message) {
        if (message.equals("")) {
            exitWithError("Please enter a commit message.");
        }
        List<String> stageFile = plainFilenamesIn(STAGE_ADD);
        List<String> rmFile = plainFilenamesIn(STAGE_REMOVE);
        if (stageFile.size() == 0 && rmFile.size() == 0) {
            exitWithError("No changes added to the commit.");
        }
        Commit curr = new Commit(currCom(), message);
        curr = curr.setCommit();
        curr.safeCommit();    //save curr Node
        Branch.updateBranch(curr);
    }


    public static void printLog(Commit node) {
        if (node == null) {
            return;
        }
        String sha1 = node.getSha1();        //information of curr Node
        String message = node.message;
        Calendar cal = Calendar.getInstance();
        cal.setTime(node.timestamp);

        String dateform = String.format("%1$ta %1$tb %1$td %1$tT %1$tY %1$tz", cal);
        System.out.println("===" + "\n" + "commit " + sha1 + "\n" + "Date: "
                + dateform + "\n" + message);
        System.out.println("");
        if (node.parent == null) {
            return;
        }
        printLog(readObject(join(COMMIT, node.parent.get(0)), Commit.class));
    }


    public static void printAllLog() {
        List<String> commits = plainFilenamesIn(join(COMMIT));
        for (String c: commits) {
            Commit thisC = Utils.readObject(join(COMMIT, c), Commit.class);
            String message = thisC.message;
            Calendar cal = Calendar.getInstance();
            cal.setTime(thisC.timestamp);

            String dateform = String.format("%1$ta %1$tb %1$td %1$tT %1$tY %1$tz", cal);
            System.out.println("===" + "\n" + "commit " + c + "\n" + "Date: "
                    + dateform + "\n" + message);
            System.out.println("");
        }
    }


    public static void find(String m) {
        List<String> commits = plainFilenamesIn(COMMIT);
        boolean pt = false;
        for (String c: commits) {
            Commit thisC = Utils.readObject(join(COMMIT, c), Commit.class);
            if (thisC.message.equals(m)) {
                System.out.println(c);
                pt = true;
            }
        }
        if (!pt) {
            System.out.println("Found no commit with that message.");
        }
    }


    public static void status() {
        System.out.println("=== Branches ===");              //print branch
        Branch br = readObject(Utils.join(BRANCHES), Branch.class);
        for (String k : br.branchMap.keySet()) {
            if (k.equals(br.currbc)) {
                System.out.println("*" + br.currbc);
            } else {
                System.out.println(k);
            }
        }
        System.out.println("");

        System.out.println("=== Staged Files ===");          //print staged
        List<String> stagedL = plainFilenamesIn(join(STAGE_ADD));
        TreeSet<String> stagedS = new TreeSet<>();
        for (int i = 0; i < stagedL.size(); i++) {
            stagedS.add(stagedL.get(i));
        }
        Iterator itr1 = stagedS.iterator();
        while (itr1.hasNext()) {
            System.out.println(itr1.next());
        }
        System.out.println("");

        System.out.println("=== Removed Files ===");          //print removed
        List<String> removedL = plainFilenamesIn(join(STAGE_REMOVE));
        TreeSet<String> removedS = new TreeSet<>();
        for (int i = 0; i < removedL.size(); i++) {
            removedS.add(removedL.get(i));
        }
        Iterator itr2 = removedS.iterator();
        while (itr2.hasNext()) {
            System.out.println(itr2.next());
        }
        System.out.println("");

        System.out.println("=== Modifications Not Staged For Commit ===");
        TreeSet<String> mod = new TreeSet<>();
        HashMap<String, String> comFileMap = currCom().fileMap;
        for (String k : comFileMap.keySet()) {
            String fiSha1 = fileSha1(Utils.join(CWD, k));
            String staSha1 = fileSha1(Utils.join(STAGE_ADD, k));
            String rmSha1 = fileSha1(Utils.join(STAGE_REMOVE, k));
            String cfileSha1 = comFileMap.get(k);
            if (fiSha1 == null && rmSha1 == null) {
                mod.add(k + " (deleted)");
            } else if ((fiSha1 != null)
                    && !(fiSha1.equals(cfileSha1)) && (staSha1 == null)) {
                mod.add(k + " (modified)");
            }
        }
        for (String l : stagedS) {                //Staged for addition
            String fiSha2 = fileSha1(Utils.join(CWD, l));
            String staSha2 = fileSha1(Utils.join(STAGE_ADD, l));
            if (fiSha2 == null || !fiSha2.equals(staSha2)) {
                mod.add(l);
            }
        }
        Iterator itr3 = mod.iterator();
        while (itr3.hasNext()) {
            System.out.println(itr3.next());
        }
        System.out.println("");

        System.out.println("=== Untracked Files ===");
        TreeSet<String> untracked = new TreeSet<>();
        List<String> fileinCWD = plainFilenamesIn(Utils.join(CWD));
        for (String m : fileinCWD) {
            if ((!comFileMap.containsKey(m) && !join(STAGE_ADD, m).exists())
                    || join(STAGE_REMOVE, m).exists()) {
                untracked.add(m);
            }
        }
        Iterator itr4 = untracked.iterator();
        while (itr4.hasNext()) {
            System.out.println(itr4.next());
        }
        System.out.println("");
    }


    /**
     * checkout 1
     */
    public static void checkout(String dash, String fname) {
        if (!dash.equals("--")) {
            exitWithError("Incorrect operands.");
        }
        Commit prevHead = currCom();
        String fileSha1 = prevHead.fileMap.get(fname);
        if (fileSha1 == null) {
            exitWithError("File does not exist in that commit.");
        }
        File headfile = Utils.join(BLOB_FILES, fileSha1);
        File overWrite = Utils.join(CWD, fname);
        writeContents(overWrite, readContents(headfile));
    }

    /**
     * checkout 2
     */
    public static void checkout(String cid, String dash, String fname) {
        if (!dash.equals("--")) {
            exitWithError("Incorrect operands.");
        }
        File comFile = Utils.join(COMMIT, cid);

        if (!comFile.exists()) {
            String completeID = checkAbb(cid);
            if (completeID == null) {
                exitWithError("No commit with that id exists.");
            } else {
                comFile = Utils.join(COMMIT, completeID);
            }
        }
        Commit getCom = readObject(comFile, Commit.class);   //get commit by its id
        String fileSha1 = getCom.fileMap.get(fname);
        if (fileSha1 == null) {
            exitWithError("File does not exist in that commit.");
        }
        File getF = Utils.join(BLOB_FILES, fileSha1);
        File overWrite = Utils.join(CWD, fname);
        writeContents(overWrite, readContents(getF));
    }

    /**
     * checkout 3
     */
    public static void checkout(String bname) {
        Branch thisB = readObject(BRANCHES, Branch.class);
        TreeMap<String, String> currMap = thisB.branchMap;      //get the branch map
        if (!currMap.containsKey(bname)) {
            exitWithError("No such branch exists.");
        }
        if (thisB.currbc.equals(bname)) {
            exitWithError("No need to checkout the current branch.");
        }
        HashMap<String, String> currBFiles = currCom().fileMap;
        File branHead = join(COMMIT, currMap.get(bname));
        HashMap<String, String> checkFiles = readObject(branHead, Commit.class).fileMap;
        List<String> trackcurrB = trackin(currBFiles);
        List<String> trackcheckB = trackin(checkFiles);
        for (String f : trackcheckB) {
            if (!trackcurrB.contains(f)) {
                exitWithError("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
            }
        }
        for (String m : trackcurrB) {
            if (!trackcheckB.contains(m)) {
                restrictedDelete(join(CWD, m));
            }
        }
        putfiles(checkFiles);
        thisB.currbc = bname;
        Utils.writeObject(BRANCHES, thisB);
        clearStage();
    }


    public static void reset(String cid) {
        File comF = Utils.join(COMMIT, cid);
        if (!comF.exists()) {
            cid = checkAbb(cid);
            if (cid == null) {
                exitWithError("No commit with that id exists.");
            } else {
                comF = Utils.join(COMMIT, cid);
            }
        }
        HashMap<String, String> currBFiles = currCom().fileMap;
        HashMap<String, String> resetFiles = readObject(comF, Commit.class).fileMap;
        List<String> trackcurrB = trackin(currBFiles);
        List<String> trackreset = trackin(resetFiles);
        for (String f: trackreset) {
            if (!trackcurrB.contains(f)) {
                exitWithError("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
            }
        }
        for (String l: trackcurrB) {
            if (!trackreset.contains(l)) {
                restrictedDelete(join(CWD, l));
            }
        }
        putfiles(resetFiles);
        Branch thisB = readObject(BRANCHES, Branch.class);
        thisB.branchMap.put(thisB.currbc, cid);
        writeObject(BRANCHES, thisB);
        clearStage();
    }


    public static void merge(String bname) {
        if (plainFilenamesIn(STAGE_ADD).size() != 0 || plainFilenamesIn(STAGE_REMOVE).size() != 0) {
            exitWithError("You have uncommitted changes.");       //check stage add and rm
        }
        if (!Branch.exist(bname)) {
            exitWithError("A branch with that name does not exist.");
        }
        if (Branch.self(bname)) {
            exitWithError("Cannot merge a branch with itself.");
        }
        Branch branch = readObject(BRANCHES, Branch.class);
        String mergeSha = readObject(BRANCHES, Branch.class).branchMap.get(bname);
        Commit mergeCom = readObject(join(COMMIT, mergeSha), Commit.class);
        Commit splitNode = findAncestor(currCom(), mergeCom);
        if (splitNode.getSha1().equals(mergeCom.getSha1())) {
            exitWithError("Given branch is an ancestor of the current branch.");
        }
        if (splitNode.getSha1().equals(currCom().getSha1())) {
            checkout(bname);
            exitWithError("Current branch fast-forwarded.");
        }
        List<String> trackcurrB = trackin(currCom().fileMap);
        List<String> trackmergeB = trackin(mergeCom.fileMap);
        for (String f: trackmergeB) {
            if (!trackcurrB.contains(f)) {
                exitWithError("There is an untracked file in the way; delete it, "
                        + "or add and commit it first.");
            }
        }
        HashMap<String, String> current = currCom().fileMap;
        HashMap<String, String> merge = mergeCom.fileMap;
        HashMap<String, String> split = splitNode.fileMap;
        boolean conflict = false;
        for (String f: current.keySet()) {
            if (split.containsKey(f) && merge.containsKey(f)) {
                if (split.get(f).equals(current.get(f))
                        && !merge.get(f).equals(current.get(f))) {   //case 1
                    writeContents(join(CWD, f), readContents(join(BLOB_FILES, merge.get(f))));
                    addFile(f);
                } else if (!split.get(f).equals(current.get(f))
                        && !merge.get(f).equals(current.get(f))
                        && !split.get(f).equals(merge.get(f))) {
                    conflict(f, current, merge);         //case 8.1
                    addFile(f);
                    conflict = true;
                }
            }
            if (split.containsKey(f) && !merge.containsKey(f)) {
                if (split.get(f).equals(current.get(f))) {
                    remove(f);    //case 6
                } else if (!split.get(f).equals(current.get(f))) {
                    conflict(f, current, null);    //case 8.2
                    addFile(f);
                    conflict = true;
                }
            }
        }
        for (String i: merge.keySet()) {
            if (!split.containsKey(i) && !current.containsKey(i)) {
                writeContents(join(CWD, i), readContents(join(BLOB_FILES, merge.get(i))));
                addFile(i);            //case 5
            } else if (split.containsKey(i) && !current.containsKey(i)
                    && !split.get(i).equals(merge.get(i))) {
                conflict(i, null, merge);    //case 8.3
                addFile(i);
                conflict = true;
            } else if (!split.containsKey(i) && current.containsKey(i)
                    && !current.get(i).equals(merge.get(i))) {
                conflict(i, current, merge);    //case 8.4
                addFile(i);
                conflict = true;
            }
        }
        String message = "Merged " + bname + " into " + branch.currbc + ".";
        merCommit(message, mergeCom);
        if (conflict) {
            exitWithError("Encountered a merge conflict.");
        }
    }


    private static void conflict(String file, HashMap<String, String> current,
                                 HashMap<String, String> merge) {
        String fir = "<<<<<<< HEAD\n";
        String currStr = "";
        if (current != null) {
            currStr = readContentsAsString(join(BLOB_FILES, current.get(file)));
        }
        String sep = "=======\n";
        String givenStr = "";
        if (merge != null) {
            givenStr = readContentsAsString(join(BLOB_FILES, merge.get(file)));
        }
        String las = ">>>>>>>\n";
        writeContents(join(CWD, file), fir + currStr + sep + givenStr  + las);
    }


    private static void merCommit(String message, Commit parent2) {
        List<String> stageFile = plainFilenamesIn(STAGE_ADD);
        List<String> rmFile = plainFilenamesIn(STAGE_REMOVE);
        if (stageFile.size() == 0 && rmFile.size() == 0) {
            exitWithError("No changes added to the commit.");
        }
        Commit curr = new Commit(currCom(), parent2, message);
        curr = curr.setCommit();
        curr.safeCommit();    //save curr Node
        Branch.updateBranch(curr);
    }


    private static Commit findAncestor(Commit a, Commit b) {
        LinkedList<String> parentA = new LinkedList<>();
        LinkedList<String> fringeA = new LinkedList<>();
        fringeA.add(a.getSha1());
        while (!fringeA.isEmpty()) {
            String currSha = fringeA.pop();
            Commit curr = readObject(join(COMMIT, currSha), Commit.class);
            if (curr.parent != null) {
                for (String c : curr.parent) {
                    if (!fringeA.contains(c) && !parentA.contains(c)) {
                        fringeA.add(c);
                    }
                }
            }
            parentA.add(currSha);
        }
        LinkedList<String> parentB = new LinkedList<>();
        LinkedList<String> fringeB = new LinkedList<>();
        fringeB.add(b.getSha1());
        while (!fringeB.isEmpty()) {
            String currSha = fringeB.pop();
            Commit curr = readObject(join(COMMIT, currSha), Commit.class);
            if (curr.parent != null) {
                for (String c : curr.parent) {
                    if (!fringeB.contains(c) && !parentB.contains(c)) {
                        fringeB.add(c);
                    }
                }
            }
            parentB.add(currSha);
        }
        while (parentA.size() > 0) {
            String rt = parentA.pop();
            if (parentB.contains(rt)) {
                return readObject(join(COMMIT, rt), Commit.class);
            }
        }
        return null;
    }


    public static void addremote(String rmname, String routine) {
        File remote = join(CONFIG, rmname);
        if (remote.exists()) {
            exitWithError("A remote with that name already exists.");
        }
        String content = "";
        while (routine.length() != 0) {
            if (routine.charAt(0) == '/') {
                content = content + java.io.File.separator;
            } else {
                content = content + routine.charAt(0);
            }

            routine = routine.substring(1);
        }
        writeContents(remote, content);
    }


    public static void rmremote(String rmname) {
        File remote = join(CONFIG, rmname);
        if (!remote.exists()) {
            exitWithError("A remote with that name does not exist.");
        }
        remote.delete();
    }


    public static void push(String rmname, String bname) {
        File remote = join(readContentsAsString(join(CONFIG, rmname)));     //remoteFile path
        if (!remote.exists()) {
            exitWithError("Remote directory not found.");
        }
        Branch currBranch = readObject(join(BRANCHES), Branch.class);
        Branch rmBranch = readObject(join(remote, "branches"), Branch.class);
        LinkedList<String> ancestors = travelAncestor(currCom());
        String rmhead = rmBranch.branchMap.get(bname);
        if (!ancestors.contains(rmhead)) {
            exitWithError("Please pull down remote changes before pushing.");
        } else if (rmBranch.branchMap.get(bname) == null) {
            rmBranch.branchMap.put(currBranch.currbc, currCom().getSha1());
            for (String s: ancestors) {
                File addCom = join(remote, "commit", s);
                writeContents(addCom, readContents(join(COMMIT, s)));
            }
        } else {
            LinkedList<String> addS = new LinkedList<>();
            String curr = ancestors.pop();
            while (!curr.equals(rmhead)) {
                addS.add(curr);
                curr = ancestors.pop();
            }
            for (String m: addS) {
                File addCom = join(remote, "commit", m);
                writeContents(addCom, readContents(join(COMMIT, m)));
            }
            rmBranch.branchMap.put(bname, currCom().getSha1());
        }
        writeObject(join(remote, "branches"), rmBranch);
    }


    private static LinkedList travelAncestor(Commit a) {
        LinkedList<String> parentA = new LinkedList<>();
        LinkedList<String> fringeA = new LinkedList<>();
        fringeA.add(a.getSha1());
        while (!fringeA.isEmpty()) {
            String currSha = fringeA.pop();
            Commit curr = readObject(join(COMMIT, currSha), Commit.class);
            if (curr.parent != null) {
                for (String c : curr.parent) {
                    if (!fringeA.contains(c) && !parentA.contains(c)) {
                        fringeA.add(c);
                    }
                }
            }
            parentA.add(currSha);
        }
        return parentA;
    }


    public static void fetch(String rmname, String bname) {
        File remote = join(readContentsAsString(join(CONFIG, rmname)));     //remoteFile path
        if (!remote.exists()) {
            exitWithError("Remote directory not found.");
        }
        Branch rmBranch = readObject(join(remote, "branches"), Branch.class);
        if (rmBranch.branchMap.get(bname) == null) {
            exitWithError("That remote does not have that branch.");
        }
        Branch currBranch = readObject(join(BRANCHES), Branch.class);
        File rmCom = join(remote, "commit", rmBranch.branchMap.get(bname));
        Commit rmHead = readObject(rmCom, Commit.class);
        LinkedList<String> copy = fetchAncestor(rmHead, currCom(), remote);
        for (String c: copy) {
            File blobFile = Utils.join(remote, "blob_files");
            Commit copyC = readObject(join(remote, "commit", c), Commit.class);
            writeObject(join(COMMIT, c), copyC);
            for (String f : copyC.fileMap.keySet()) {
                File addF = join(blobFile, copyC.fileMap.get(f));
                writeContents(join(BLOB_FILES, copyC.fileMap.get(f)), readContents(addF));
            }
        }
        currBranch.branchMap.put(rmname + "/" + bname, rmHead.getSha1());
        writeObject(BRANCHES, currBranch);
    }


    private static LinkedList<String> fetchAncestor(Commit a, Commit b, File remote) {
        LinkedList<String> parentA = new LinkedList<>();
        LinkedList<String> fringeA = new LinkedList<>();
        fringeA.add(a.getSha1());
        while (!fringeA.isEmpty()) {
            String currSha = fringeA.pop();
            Commit curr = readObject(join(remote, "commit", currSha), Commit.class);
            if (curr.parent != null) {
                for (String c : curr.parent) {
                    if (!fringeA.contains(c) && !parentA.contains(c)) {
                        fringeA.add(c);
                    }
                }
            }
            parentA.add(currSha);
        }
        LinkedList<String> parentB = new LinkedList<>();
        LinkedList<String> fringeB = new LinkedList<>();
        fringeB.add(b.getSha1());
        while (!fringeB.isEmpty()) {
            String currSha = fringeB.pop();
            Commit curr = readObject(join(COMMIT, currSha), Commit.class);
            if (curr.parent != null) {
                for (String c : curr.parent) {
                    if (!fringeB.contains(c) && !parentB.contains(c)) {
                        fringeB.add(c);
                    }
                }
            }
            parentB.add(currSha);
        }
        LinkedList<String> rtAncestors = new LinkedList<>();
        while (parentA.size() > 0) {
            String rt = parentA.pop();
            if (parentB.contains(rt)) {
                return rtAncestors;
            }
            rtAncestors.add(rt);
        }
        return null;
    }


    public static void pull(String rmname, String bname) {
        fetch(rmname, bname);
        merge(rmname + "/" + bname);
    }


    /** check if the abbreviation commit id exist, return null if not */
    private static String checkAbb(String cid) {
        List<String> commitF = plainFilenamesIn(COMMIT);
        for (int i = 0; i < commitF.size(); i++) {
            if (commitF.get(i).contains(cid)) {
                return commitF.get(i);
            }
        }
        return null;
    }


    /** clear Stage files */
    private static void clearStage() {
        List<String> addFile = plainFilenamesIn(STAGE_ADD);        //reset the current branch
        for (String a: addFile) {
            Utils.join(STAGE_ADD, a).delete();
        }                                                          //clear the staging area
        List<String> rmFile = plainFilenamesIn(STAGE_REMOVE);
        for (String b: rmFile) {
            Utils.join(STAGE_REMOVE, b).delete();
        }
    }


    /** helper function, put the hashset files in CWD */
    private static void putfiles(HashMap<String, String> map) {
        for (String f: map.keySet()) {
            File putin = join(CWD, f);
            File getF = Utils.join(BLOB_FILES, map.get(f));
            writeContents(putin, readContents(getF));
        }
    }

    /** return files name list if this file in CWD tracked in branch's commit filemap
     * tracked means filename exist, no need same content */
    public static List<String> trackin(HashMap<String, String> map) {
        List<String> localFiles = plainFilenamesIn(CWD);
        List<String> tracked = new ArrayList<>();
        for (String f: localFiles) {
            if (map.containsKey(f)) {
                tracked.add(f);
            }
        }
        return tracked;
    }


    /** get the current branch's head commit */
    public static Commit currCom() {
        Branch thisB = readObject(BRANCHES, Branch.class);
        String headSha1 = thisB.branchMap.get(thisB.currbc);
        Commit prevHead = readObject(Utils.join(COMMIT, headSha1), Commit.class);
        return prevHead;
    }


    /** return sha1 of file, if file doesn't exist, return null */
    public static String fileSha1(File file) {
        if (!file.exists()) {
            return null;
        }
        return Utils.sha1(readContents(file));
    }
}
