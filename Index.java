import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Manages the git/index file — staging, reading, writing. */
public class Index {
    private final File repoDir;
    private final File objectsDir;
    private final File indexFile;

    /** Sets up file references for repo paths. */
    public Index() {
        this.repoDir = new File("git");
        this.objectsDir = new File(repoDir, "objects");
        this.indexFile = new File(repoDir, "index");
    }

    /** Ensures repo structure exists (git/, git/objects/, git/index). */
    public void ensureStructure() throws IOException {
        if (!repoDir.exists()) repoDir.mkdir();
        if (!objectsDir.exists()) objectsDir.mkdir();
        if (!indexFile.exists()) indexFile.createNewFile();
    }

    /** 
     * Adds a file to index: creates blob, updates or replaces entry.
     * Avoids duplicates if same file + same hash already staged.
     */
    public void add(String sourcePath) throws IOException {
        ensureStructure();
        String path = normalizePath(sourcePath);
        String hash = Blob.createBlobFromPath(path);

        List<String> oldLines = readAll(indexFile.toPath());
        List<String> newLines = new ArrayList<>();

        boolean alreadyExact = false;
        for (String line : oldLines) {
            if (line == null) continue;
            int space = line.indexOf(' ');
            if (space != -1) {
                String left = line.substring(0, space);
                String right = line.substring(space + 1);
                if (right.equals(path)) {
                    if (left.equals(hash)) alreadyExact = true;
                } else {
                    newLines.add(line);
                }
            }
        }

        if (!alreadyExact) newLines.add(hash + " " + path);
        writeExact(indexFile.toPath(), newLines);
    }

    /** Converts backslashes to slashes and removes "./". */
    private static String normalizePath(String p) {
        if (p == null) return "";
        String s = p.replace('\\', '/');
        if (s.startsWith("./")) s = s.substring(2);
        if (s.equals(".")) s = "";
        return s;
    }

    /** Returns only filename from full path. */
    public static String fileNameOnly(String path) {
        Path p = Paths.get(path);
        Path name = p.getFileName();
        return name == null ? path : name.toString();
    }

    /** Reads all lines from path if exists, else returns empty list. */
    private static List<String> readAll(Path p) throws IOException {
        return Files.exists(p) ? Files.readAllLines(p, StandardCharsets.UTF_8) : new ArrayList<>();
    }

    /** Writes all lines to file exactly (no trailing newline). */
    private static void writeExact(Path p, List<String> lines) throws IOException {
        FileWriter w = new FileWriter(p.toFile(), false);
        for (int i = 0; i < lines.size(); i++) {
            w.write(lines.get(i));
            if (i < lines.size() - 1) w.write("\n");
        }
        w.close();
    }

    /** Deletes and recreates git/objects folder. */
    public void resetObjects() {
        File objects = new File("git/objects");
        if (objects.exists()) deleteRecursively(objects);
        new File("git/objects").mkdirs();
    }

    /** Clears the index file contents. */
    public void clearIndex() {
        try (FileWriter w = new FileWriter("git/index", false)) {
            w.write("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Deletes all non-Java, non-git files in current working directory. */
    public void deleteNonJavaInCwd() {
        File[] items = new File(".").listFiles();
        if (items == null) return;
        for (File f : items) {
            if (f.isFile() && !f.getName().endsWith(".java") && !f.getName().equals("git")) {
                f.delete();
            }
        }
    }

    /** Recursively deletes a folder or file. */
    private void deleteRecursively(File f) {
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) for (File k : kids) deleteRecursively(k);
        }
        f.delete();
    }
}
