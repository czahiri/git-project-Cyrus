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

// Builds hierarchical tree objects from the git/index file
public class WorkingList {

    // Represents a single item in the working list
    private static class Item {
        String type; // "blob" or "tree"
        String sha;  // SHA-1 hash value
        String path; // file or directory path (with forward slashes)

        Item(String t, String s, String p) {
            this.type = t;
            this.sha = s;
            this.path = p;
        }
    }

    // Builds all trees from index, bottom-up, returns root tree SHA
    public String build() throws Exception {
        ensureObjects();

        ArrayList<Item> list = read();
        sort(list);

        // Special case: empty index -> empty root tree
        if (list.size() == 0) {
            String emptyContent = "";
            String emptySha = sha1(emptyContent);
            writeObj(emptySha, emptyContent);
            return emptySha;
        }

        while (true) {
            if (list.size() == 1) {
                Item only = list.get(0);
                if ("tree".equals(only.type)) {
                    return only.sha;
                }
            }

            String dir = leafDir(list);
            if (dir == null) {
                // Cyrus Fix: if no subdirs remain, collapse all top-level items into a root tree
                ArrayList<String> trLines = new ArrayList<String>();
                int iTop = 0;
                while (iTop < list.size()) {
                    Item itemTop = list.get(iTop);
                    if (itemTop.path.indexOf('/') == -1) {
                        trLines.add(itemTop.type + " " + itemTop.sha + " " + itemTop.path);
                    }
                    iTop = iTop + 1;
                }
                Collections.sort(trLines);
                String content;
                if (trLines.size() == 0) {
                    content = "";
                } else {
                    StringBuilder sb = new StringBuilder();
                    int j = 0;
                    while (j < trLines.size()) {
                        sb.append(trLines.get(j));
                        if (j < trLines.size() - 1) {
                            sb.append("\n");
                        }
                        j = j + 1;
                    }
                    content = sb.toString() + "\n";
                }
                String rootSha = sha1(content);
                writeObj(rootSha, content);

                ArrayList<Item> next = new ArrayList<Item>();
                next.add(new Item("tree", rootSha, ""));
                int k = 0;
                while (k < list.size()) {
                    Item itKeep = list.get(k);
                    if (itKeep.path.indexOf('/') != -1) {
                        next.add(itKeep);
                    }
                    k = k + 1;
                }
                list = next;
                // continue loop; will collapse remaining subtrees to reach a single root tree
            } else {
                String dirSha = makeTree(dir, list);
                list = collapse(list, dir, dirSha);
            }
        }
    }

    // Reads git/index and loads blob entries
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

    // Sort by path
    private void sort(ArrayList<Item> list) {
        Collections.sort(list, new Comparator<Item>() {
            public int compare(Item a, Item b) {
                return a.path.compareTo(b.path);
            }
        });
    }

    // Finds deepest directory ready to build; returns null if none
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

    // Checks if a directory still has unbuilt child subdirectories
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

    // True if a tree already exists for dir
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

    // Builds a tree file for a given directory, returns its SHA
    private String makeTree(String dir, ArrayList<Item> list) throws Exception {
        ArrayList<String> lines = new ArrayList<String>();

        int i = 0;
        while (i < list.size()) {
            Item it = list.get(i);
            String parent = findParentDir(it.path);
            if (parent.equals(dir)) {
                String name = base(it.path);
                if (it.type.equals("blob")) {
                    if (name.length() > 0) {
                        if (name.indexOf('/') == -1) {
                            lines.add("blob " + it.sha + " " + name);
                        }
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

    // Replaces children of dir with single "tree <sha> <dir>"
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
                        if (it.type.equals("tree")) {
                            if (it.path.equals(maybeChildDir)) {
                                drop = true;
                            }
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

    // Parent directory path of a path
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

    // Basename of a path
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

    // Depth of a path (slashes + 1)
    private int depth(String path) {
        if (path == null) {
            return 0;
        }
        if (path.length() == 0) {
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

    // Joins lines with newline
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

    // Ensure git/objects exists
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

    // Write object to git/objects/<sha> if not present
    private void writeObj(String sha, String data) throws Exception {
        File out = new File("git" + File.separator + "objects", sha);
        if (!out.exists()) {
            FileWriter w = new FileWriter(out, false);
            w.write(data);
            w.flush();
            w.close();
        }
    }

    // SHA-1 of string
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

    // Hex helper
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
