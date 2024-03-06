package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import static gitlet.Repository.BLOB_FILES;

/**
 * In order to store all versions of committed files, under the "blob_files" folders
 * The stored format is a raw FILE, the blob_map to store
 */
public class BlobHashMap implements Serializable {
    private int initSize;    //capacity need revise
    private Collection<Blob>[] allblobs;
    private double loadfactor;
    private int itemSize;
    private Set<String> keySet = new HashSet<>();

    public BlobHashMap() {
        initSize = 31;
        allblobs = new Collection[31];
        loadfactor = 0.75;
    }

    private class Blob {
        String key;
        File value;

        Blob(String k, File v) {
            key = k;
            value = v;
        }
    }

    private Blob createBlob(String key, File value) {
        return new Blob(key, value);
    }

    private Collection<Blob> createBucket() {
        return new ArrayList<>();
    }


    public boolean containsKey(String key) {
        int bin = key.hashCode() % initSize;
        if (bin < 0) {
            bin = bin + initSize;
        }
        if (allblobs[bin] == null) {
            return false;
        }
        for (Blob e : allblobs[bin]) {
            if (e.key.equals(key)) {
                return true;
            }
        }
        return false;
    }


    /**
     * insert & add new blob in Hashfolder & hashmap
     * if exists, return
     */
    public void insert(String key, File value) {
        if (containsKey(key)) {
            return;
        }
        if (itemSize / initSize > loadfactor) {
            resize(initSize, 2);
        }
        int bin = key.hashCode() % initSize;
        if (bin < 0) {
            bin = bin + initSize;
        }
        if (allblobs[bin] == null) {
            allblobs[bin] = createBucket();
        }
        Blob newB = createBlob(key, value);
        allblobs[bin].add(newB);
        File blobF = Utils.join(BLOB_FILES, key);
        Utils.writeContents(blobF, Utils.readContents(value));
        itemSize += 1;
        keySet.add(key);
    }


    private void resize(int prevSize, int factor) {
        int capacity = prevSize * factor;
        Collection<Blob>[] copyB = new Collection[capacity];
        for (int i = 0; i < initSize; i++) {
            if (allblobs[i] == null) {
                continue;
            }
            for (Blob e : allblobs[i]) {
                int index = e.key.hashCode() % capacity;
                if (index < 0) {
                    index += capacity;
                }
                if (copyB[index] == null) {
                    copyB[index] = createBucket();
                }
                Blob add = new Blob(e.key, e.value);
                copyB[index].add(add);
            }
        }
        allblobs = copyB;
    }

}
