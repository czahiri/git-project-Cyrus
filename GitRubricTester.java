import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;

public class GitRubricTester {

    public static void main(String[] args) throws Exception {
        // reset
        deleteRecursively(new File("git"));
        deleteRecursively(new File("proj"));

        GitRekt gw = new GitRekt();

        // INIT
        gw.init();
        gw.init(); // should be fine if run again
        mustDir("git");
        mustDir("git/objects");
        mustFile("git/index");
        mustFile("git/HEAD");
        System.out.println("init OK");

        // EMPTY INDEX -> EMPTY TREE
        WorkingList wlEmpty = new WorkingList();
        String emptyRoot = wlEmpty.build();
        mustFile("git/objects/" + emptyRoot);
        String emptyContent = readObject(emptyRoot);
        if (emptyContent == null) emptyContent = "";
        if (!emptyContent.equals("")) throw new RuntimeException("empty index should create empty tree");
        System.out.println("empty tree OK");

        // ADD
        writeText("proj/a.txt", "alpha");
        writeText("proj/b.txt", "bravo");
        new File("proj/sub").mkdirs();
        writeText("proj/sub/c.txt", "charlie");

        // reject missing
        try {
            gw.add("proj/missing.txt");
            throw new RuntimeException("should reject missing path");
        } catch (IOException e) {
            // expected
        }

        // reject directory
        try {
            gw.add("proj/sub");
            throw new RuntimeException("should reject directory path");
        } catch (IOException e) {
            // expected
        }

        gw.add("proj/a.txt");
        gw.add("proj/b.txt");
        gw.add("proj/sub/c.txt");

        // blobs match bytes
        ArrayList<String> idx1 = readLines(new File("git/index"));
        int i = 0;
        while (i < idx1.size()) {
            String line = idx1.get(i);
            int sp = line.indexOf(' ');
            if (sp != -1) {
                String sha = line.substring(0, sp);
                String path = line.substring(sp + 1);
                byte[] src = Files.readAllBytes(new File(path).toPath());
                byte[] blob = Files.readAllBytes(new File("git/objects", sha).toPath());
                if (!sameBytes(src, blob)) throw new RuntimeException("blob mismatch for " + path);
            }
            i = i + 1;
        }
        System.out.println("add/blobs OK");

        // no-op re-add
        int before = idx1.size();
        gw.add("proj/a.txt");
        gw.add("proj/b.txt");
        gw.add("proj/sub/c.txt");
        ArrayList<String> idx2 = readLines(new File("git/index"));
        if (idx2.size() != before) throw new RuntimeException("index changed on no-op");
        System.out.println("no-op OK");

        // update entry after modify
        writeText("proj/a.txt", "alpha v2");
        gw.add("proj/a.txt");
        ArrayList<String> idx3 = readLines(new File("git/index"));
        String shaA2 = shaFor(idx3, "proj/a.txt");
        if (shaA2 == null) throw new RuntimeException("missing updated line for proj/a.txt");
        byte[] a2 = Files.readAllBytes(new File("proj/a.txt").toPath());
        byte[] a2Blob = Files.readAllBytes(new File("git/objects", shaA2).toPath());
        if (!sameBytes(a2, a2Blob)) throw new RuntimeException("updated blob mismatch");
        System.out.println("update OK");

        // TREE (from index)
        WorkingList wl1 = new WorkingList();
        String root1 = wl1.build();
        mustFile("git/objects/" + root1);

        WorkingList wl2 = new WorkingList();
        String root2 = wl2.build();
        if (!root2.equals(root1)) throw new RuntimeException("tree should be deterministic");
        System.out.println("tree deterministic OK");

        writeText("proj/b.txt", "bravo v2");
        gw.add("proj/b.txt");
        WorkingList wl3 = new WorkingList();
        String root3 = wl3.build();
        if (root3.equals(root2)) throw new RuntimeException("root tree should change after content change");
        System.out.println("tree change OK");

        System.out.println("DONE");
    }

    // helpers

    private static void writeText(String path, String s) throws IOException {
        File f = new File(path);
        File p = f.getParentFile();
        if (p != null && !p.exists()) p.mkdirs();
        BufferedWriter w = new BufferedWriter(new FileWriter(f, false));
        w.write(s);
        w.close();
    }

    private static ArrayList<String> readLines(File f) throws IOException {
        ArrayList<String> out = new ArrayList<String>();
        if (!f.exists()) return out;
        BufferedReader br = new BufferedReader(new FileReader(f));
        String ln = br.readLine();
        while (ln != null) {
            if (ln.trim().length() > 0) out.add(ln);
            ln = br.readLine();
        }
        br.close();
        return out;
    }

    private static boolean sameBytes(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int i = 0;
        while (i < a.length) {
            if (a[i] != b[i]) return false;
            i = i + 1;
        }
        return true;
    }

    private static String shaFor(ArrayList<String> lines, String path) {
        int i = 0;
        while (i < lines.size()) {
            String ln = lines.get(i);
            int sp = ln.indexOf(' ');
            if (sp != -1) {
                String sha = ln.substring(0, sp);
                String p = ln.substring(sp + 1);
                if (p.equals(path)) return sha;
            }
            i = i + 1;
        }
        return null;
    }

    private static String readObject(String sha) throws IOException {
        File f = new File("git/objects", sha);
        if (!f.exists() || !f.isFile()) return null;
        byte[] b = Files.readAllBytes(f.toPath());
        return new String(b, StandardCharsets.UTF_8);
    }

    private static void mustDir(String p) {
        File d = new File(p);
        if (!d.exists() || !d.isDirectory()) throw new RuntimeException("missing dir: " + p);
    }

    private static void mustFile(String p) {
        File f = new File(p);
        if (!f.exists() || !f.isFile()) throw new RuntimeException("missing file: " + p);
    }

    private static void deleteRecursively(File f) {
        if (!f.exists()) return;
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) {
                int i = 0;
                while (i < kids.length) {
                    deleteRecursively(kids[i]);
                    i = i + 1;
                }
            }
        }
        f.delete();
    }
}
