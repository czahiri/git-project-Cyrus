import java.io.File;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Builds hierarchical tree objects from the git/index file,
 * replicating how Git constructs directory trees.
 */
public class WorkingList {

    /** Represents a single item in the working list — either a blob (file) or tree (directory). */
    private static class Item {
        String type; // "blob" or "tree"
        String sha;  // SHA-1 hash value
        String path; // file or directory path (always uses forward slashes)

        Item(String t, String s, String p) {
            this.type = t;
            this.sha = s;
            this.path = p;
        }
    }

    /**
     * Main entry point — builds all trees from the index, bottom-up.
     * Keeps collapsing directories until only the root tree remains.
     * returns SHA-1 hash of the root tree
     */
    public String build() throws Exception {
        ensureObjects();              // make sure git/objects exists
        ArrayList<Item> list = read(); // read index entries
        sort(list);                    // sort alphabetically by path

        // Loop until root tree is built
        while (true) {
            if (list.size() == 1) {
                Item only = list.get(0);
                if (only.type.equals("tree")) {
                    return only.sha; // finished — root tree built
                }
            }

            // Find the next deepest directory that can be turned into a tree
            String dir = leafDir(list);
            if (dir == null) {
                // If no more subdirectories remain, build root tree
                String rootSha = makeTree("", list);
                ArrayList<Item> one = new ArrayList<Item>();
                one.add(new Item("tree", rootSha, ""));
                list = one;
                return rootSha;
            }

            // Build tree for this directory, then replace its entries
            String dirSha = makeTree(dir, list);
            list = collapse(list, dir, dirSha);
        }
    }

    /**
     * Reads the git/index file and loads all blob entries into memory.
     * returns list of blob-type items from the index
     */
    private ArrayList<Item> read() throws Exception {
        ArrayList<Item> list = new ArrayList<Item>();
        File idx = new File("git" + File.separator + "index");
        if (!idx.exists()) {
            return list;
        }

        BufferedReader br = new BufferedReader(new FileReader(idx));
        String line = br.readLine();
        while (line != null) {
            String s = line.trim();
            if (s.length() > 0) {
                int sp = s.indexOf(' ');
                if (sp != -1) {
                    String sha = s.substring(0, sp);
                    String p = s.substring(sp + 1).replace('\\', '/');
                    list.add(new Item("blob", sha, p));
                }
            }
            line = br.readLine();
        }
        br.close();
        return list;
    }

    /**
     * Sorts the working list alphabetically by path.
     */
    private void sort(ArrayList<Item> list) {
        Collections.sort(list, new Comparator<Item>() {
            public int compare(Item a, Item b) {
                return a.path.compareTo(b.path);
            }
        });
    }

    /**
     * Finds the deepest directory that has all subtrees already built,
     * meaning it's ready to be collapsed into a tree.
     * returns directory path ready to build, or null if root
     */
    private String leafDir(ArrayList<Item> list) {
        String best = null;
        int bestDepth = -1;

        int i = 0;
        while (i < list.size()) {
            Item it = list.get(i);
            String parent = findParentDir(it.path);
            if (parent.length() > 0) {
                boolean deeper = needsChildBuild(list, parent);
                if (deeper == false) {
                    int d = depth(parent);
                    if (d > bestDepth) {
                        bestDepth = d;
                        best = parent;
                    }
                }
            }
            i = i + 1;
        }
        return best;
    }

    /**
     * Checks if the given directory still contains unbuilt child subdirectories.
     * returns true if more building is needed
     */
    private boolean needsChildBuild(ArrayList<Item> list, String dir) {
        String prefix = dir + "/";
        int i = 0;
        while (i < list.size()) {
            Item it = list.get(i);
            if (it.path.startsWith(prefix)) {
                String rest = it.path.substring(prefix.length());
                int slash = rest.indexOf('/');
                if (slash != -1) {
                    String childDir = dir + "/" + rest.substring(0, slash);
                    boolean childDone = hasTree(list, childDir);
                    if (childDone == false) {
                        return true;
                    }
                }
            }
            i = i + 1;
        }
        return false;
    }

    /**
     * Checks if a tree has already been created for the given directory.
     */
    private boolean hasTree(ArrayList<Item> list, String dir) {
        int i = 0;
        while (i < list.size()) {
            Item it = list.get(i);
            if (it.type.equals("tree")) {
                if (it.path.equals(dir)) {
                    return true;
                }
            }
            i = i + 1;
        }
        return false;
    }

    /**
     * Builds a tree file for a given directory.
     * Each entry is "blob <sha> <filename>" or "tree <sha> <dirname>".
     * returns SHA-1 hash of the created tree file
     */
    private String makeTree(String dir, ArrayList<Item> list) throws Exception {
        ArrayList<String> lines = new ArrayList<String>();

        int i = 0;
        while (i < list.size()) {
            Item it = list.get(i);
            String parent = findParentDir(it.path);
            if (parent.equals(dir)) {
                String name = base(it.path);
                if (it.type.equals("blob")) {
                    if (name.length() > 0 && name.indexOf('/') == -1) {
                        lines.add("blob " + it.sha + " " + name);
                    }
                } else {
                    lines.add("tree " + it.sha + " " + name);
                }
            }
            i = i + 1;
        }

        Collections.sort(lines);
        String data = join(lines);
        String sha = sha1(data);
        writeObj(sha, data);
        return sha;
    }

    /**
     * Collapses all entries in a directory into a single tree entry.
     * Replaces child blobs/trees with one "tree <sha> <dir>".
     */
    private ArrayList<Item> collapse(ArrayList<Item> list, String dir, String sha) {
        ArrayList<Item> out = new ArrayList<Item>();
        String prefix;
        if (dir.length() == 0) {
            prefix = "";
        } else {
            prefix = dir + "/";
        }

        int i = 0;
        while (i < list.size()) {
            Item it = list.get(i);
            boolean drop = false;

            if (dir.length() == 0) {
                String parent = findParentDir(it.path);
                if (parent.equals("")) {
                    drop = true;
                }
            } else {
                if (it.path.startsWith(prefix)) {
                    String rest = it.path.substring(prefix.length());
                    int slash = rest.indexOf('/');
                    if (slash == -1) {
                        drop = true;
                    } else {
                        String maybeChildDir = dir + "/" + rest.substring(0, slash);
                        if (it.type.equals("tree") && it.path.equals(maybeChildDir)) {
                            drop = true;
                        }
                    }
                }
            }

            if (drop == false) {
                out.add(it);
            }
            i = i + 1;
        }

        out.add(new Item("tree", sha, dir));
        sort(out);
        return out;
    }

    /**
     * Returns the parent directory path of a given file/directory path.
     */
    private String findParentDir(String path) {
        if (path == null) {
            return "";
        }
        String s = path.replace('\\', '/');
        int i = s.lastIndexOf('/');
        if (i == -1) {
            return "";
        } else {
            return s.substring(0, i);
        }
    }

    /**
     * Extracts the base name (last component) from a path.
     */
    private String base(String path) {
        if (path == null) {
            return "";
        }
        String s = path.replace('\\', '/');
        int i = s.lastIndexOf('/');
        if (i == -1) {
            return s;
        } else {
            return s.substring(i + 1);
        }
    }

    /**
     * Calculates directory depth (count of slashes + 1).
     */
    private int depth(String path) {
        if (path == null || path.length() == 0) {
            return 0;
        }
        int c = 1;
        int i = 0;
        while (i < path.length()) {
            if (path.charAt(i) == '/') {
                c = c + 1;
            }
            i = i + 1;
        }
        return c;
    }

    /**
     * Joins all lines with newline characters to form file content.
     */
    private String join(ArrayList<String> lines) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < lines.size()) {
            sb.append(lines.get(i));
            if (i < lines.size() - 1) {
                sb.append("\n");
            }
            i = i + 1;
        }
        return sb.toString();
    }

    /**
     * Ensures the "git/objects" folder exists before writing any trees.
     */
    private void ensureObjects() {
        File git = new File("git");
        if (!git.exists()) {
            git.mkdir();
        }
        File objects = new File(git, "objects");
        if (!objects.exists()) {
            objects.mkdir();
        }
    }

    /**
     * Writes a tree object to git/objects/<sha>.
     * If the file already exists, it’s not overwritten.
     */
    private void writeObj(String sha, String data) throws Exception {
        File out = new File("git" + File.separator + "objects", sha);
        if (!out.exists()) {
            FileWriter w = new FileWriter(out, false);
            w.write(data);
            w.flush();
            w.close();
        }
    }

    /**
     * Returns the SHA-1 hash of a given string.
     */
    private String sha1(String s) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 not available");
        }
        byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
        return toHex(dig);
    }

    /**
     * Converts byte array to hexadecimal string.
     */
    private String toHex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        int i = 0;
        while (i < b.length) {
            int v = b[i] & 0xff;
            String h = Integer.toHexString(v);
            if (h.length() == 1) {
                sb.append('0');
            }
            sb.append(h);
            i = i + 1;
        }
        return sb.toString();
    }
}
