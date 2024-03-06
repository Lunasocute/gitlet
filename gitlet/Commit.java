package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import static gitlet.Repository.*;
import static gitlet.Utils.*;
import static gitlet.Utils.readContents;


/** Represents a gitlet commit object.
 *  include the setCommit method and saveCommit method to save the commit.
 *  does at a high level.
 *
 *  @author Luna, Tian
 */
public class Commit implements Serializable {

    /** ArrayList of parents' Sha1 */
    ArrayList<String> parent = new ArrayList<>();

    /** files Hashmap, key is the file name, value is file's Sha1 */
    HashMap<String, String> fileMap;

    /** String of Commit's message */
    String message;

    /** Date, timestamp of commit */
    Date timestamp;

    /** initial commit*/
    Commit(Commit n, String m) {
        message = m;
        if (n == null) {
            timestamp = new Date(0);
            parent = null;
            fileMap = new HashMap<>();
        } else {
            timestamp = new Date();
            parent.add(n.getSha1());     //parent: sha1 of parent Node
            fileMap = n.fileMap;
        }
    }

    /** Commit after merge */
    Commit(Commit n, Commit n2, String m) {
        message = m;
        timestamp = new Date();
        parent.add(n.getSha1());     //parent: sha1 of parent Node
        parent.add(n2.getSha1());
        fileMap = n.fileMap;
    }


    public Commit setCommit() {
        List<String> addFile = Utils.plainFilenamesIn(STAGE_ADD);
        BlobHashMap blobH = readObject(Utils.join(BLOB_FILES, "blob_map"), BlobHashMap.class);

        for (int i = 0; i < addFile.size(); i++) {
            String addFileName = addFile.get(i);
            File addF = Utils.join(STAGE_ADD, addFileName);    //single add file
            String addSha1 = Utils.sha1(readContents(addF));      //add file's sha1
            this.fileMap.put(addFileName, addSha1);        //put this file in comNode
            blobH.insert(addSha1, addF);       //add fileName & sha1 in blobhash
            addF.delete();
        }

        List<String> removeFiles = Utils.plainFilenamesIn(STAGE_REMOVE);
        for (int j = 0; j < removeFiles.size(); j++) {
            String rmName = removeFiles.get(j);
            File rmFile = Utils.join(STAGE_REMOVE, rmName);    //single add file
            String rmSha1 = Utils.sha1(readContents(rmFile));      //add file's sha1
            this.fileMap.remove(rmName, rmSha1);
            blobH.insert(rmSha1, rmFile);
            rmFile.delete();
        }
        return this;
    }


    public String getSha1() {
        return Utils.sha1(serialize(this));
    }

    /**
     * safe Commit in COMMIT folder
     */
    public void safeCommit() {
        File committed = Utils.join(Repository.COMMIT, this.getSha1());
        Utils.writeObject(committed, this);
    }

}








