import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

// Manages the git/index file — staging, reading, writing
public class Index {
    private final File repoDir;
    private final File objectsDir;
    private final File indexFile;

    // Sets up file references for repo paths
    public Index() {
        this.repoDir = new File("git");
        this.objectsDir = new File(repoDir, "objects");
        this.indexFile = new File(repoDir, "index");
    }

    // Ensures repo structure exists (git/, git/objects/, git/index)
    public void ensureStructure() throws IOException {
        if (!repoDir.exists()) {
            repoDir.mkdir();
        }
        if (!objectsDir.exists()) {
            objectsDir.mkdir();
        }
        if (!indexFile.exists()) {
            indexFile.createNewFile();
        }
    }

    // Adds a file to index: creates blob, updates or replaces entry; no-op if unchanged
    public void add(String sourcePath) throws IOException {
        ensureStructure();

        String path = normalizePath(sourcePath);
        String hash = Blob.createBlobFromPath(path);

        List<String> oldLines = readAll(indexFile.toPath());
        List<String> newLines = new ArrayList<String>();

        boolean alreadyExact = false;
        int i = 0;
        while (i < oldLines.size()) {
            String line = oldLines.get(i);
            if (line == null) {
                i = i + 1;
                continue;
            }
            int space = line.indexOf(' ');
            if (space != -1) {
                String left = line.substring(0, space);
                String right = line.substring(space + 1);
                if (right.equals(path)) {
                    if (left.equals(hash)) {
                        alreadyExact = true;
                        newLines.add(line); // keep as-is
                    }
                    // else: drop old line, will write new one below
                } else {
                    newLines.add(line); // different path, keep
                }
            } else {
                newLines.add(line);
            }
            i = i + 1;
        }

        if (!alreadyExact) {
            newLines.add(hash + " " + path);
        }

        writeExact(indexFile.toPath(), newLines);
    }

    // Converts backslashes to slashes and removes "./"
    private static String normalizePath(String p) {
        if (p == null) {
            return "";
        }
        String s = p.replace('\\', '/');
        if (s.startsWith("./")) {
            s = s.substring(2);
        }
        if (s.equals(".")) {
            s = "";
        }
        return s;
    }

    // Returns only filename from full path
    public static String fileNameOnly(String path) {
        Path p = Paths.get(path);
        Path name = p.getFileName();
        if (name == null) {
            return path;
        } else {
            return name.toString();
        }
    }

    // Reads all lines from path if exists, else returns empty list
    private static List<String> readAll(Path p) throws IOException {
        if (Files.exists(p)) {
            return Files.readAllLines(p, StandardCharsets.UTF_8);
        } else {
            return new ArrayList<String>();
        }
    }

    // Writes all lines to file exactly (no trailing newline)
    private static void writeExact(Path p, List<String> lines) throws IOException {
        FileWriter w = new FileWriter(p.toFile(), false);
        int i = 0;
        while (i < lines.size()) {
            w.write(lines.get(i));
            if (i < lines.size() - 1) {
                w.write("\n");
            }
            i = i + 1;
        }
        w.flush();
        w.close();
    }

    // Utility helpers used elsewhere (not needed for rubric, kept simple)
    public void resetObjects() {
        File objects = new File("git/objects");
        if (objects.exists()) {
            deleteRecursively(objects);
        }
        new File("git/objects").mkdirs();
    }

    public void clearIndex() {
        try {
            FileWriter w = new FileWriter("git/index", false);
            w.write("");
            w.flush();
            w.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteRecursively(File f) {
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
