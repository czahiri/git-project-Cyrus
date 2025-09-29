import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Index {

    private final File repoDir;
    private final File objectsDir;
    private final File indexFile;

    public Index() {
        this.repoDir = new File("git");
        this.objectsDir = new File(this.repoDir, "objects");
        this.indexFile = new File(this.repoDir, "index");
    }

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

    public void add(String sourcePath) throws IOException {
        ensureStructure();
        String hash = Blob.createBlobFromPath(sourcePath);
        String filename = fileNameOnly(sourcePath);

        List<String> lines = readAll(indexFile.toPath());
        List<String> filtered = new ArrayList<String>();

        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i);
            String keep = line;

            boolean same = false;
            if (line != null) {
                int space = line.indexOf(' ');
                String right;
                if (space == -1) {
                    right = line;
                } else {
                    right = line.substring(space + 1);
                }
                if (right.equals(filename)) {
                    same = true;
                }
            }

            if (same == false) {
                filtered.add(keep);
            }
            i = i + 1;
        }

        filtered.add(hash + " " + filename);
        writeExact(indexFile.toPath(), filtered);
    }

    private static String fileNameOnly(String path) {
        Path p = Paths.get(path);
        Path name = p.getFileName();
        if (name == null) {
            return path;
        } else {
            return name.toString();
        }
    }

    private static List<String> readAll(Path p) throws IOException {
        if (Files.exists(p)) {
            return Files.readAllLines(p, StandardCharsets.UTF_8);
        } else {
            return new ArrayList<String>();
        }
    }

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
    public void resetObjects() {
        File objects = new File("git" + File.separator + "objects");
        if (objects.exists()) {
            deleteRecursively(objects);
        }
        File gitDir = new File("git");
        if (!gitDir.exists()) {
            gitDir.mkdir();
        }
        File newObjects = new File(gitDir, "objects");
        if (!newObjects.exists()) {
            newObjects.mkdir();
        }
    }

    public void clearIndex() {
        try {
            File idx = new File("git" + File.separator + "index");
            File parent = idx.getParentFile();
            if (parent != null) {
                if (!parent.exists()) {
                    parent.mkdirs();
                }
            }
            FileWriter w = new FileWriter(idx, false);
            w.write("");
            w.flush();
            w.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteNonJavaInCwd() {
        File cwd = new File(".");
        File[] items = cwd.listFiles();
        if (items != null) {
            int i = 0;
            while (i < items.length) {
                File f = items[i];
                boolean isJava = false;
                boolean isGit = false;
                if (f.getName().endsWith(".java")) {
                    isJava = true;
                }
                if (f.getName().equals("git")) {
                    isGit = true;
                }
                if (f.isFile()) {
                    if (isJava == false && isGit == false) {
                        f.delete();
                    }
                }
                i = i + 1;
            }
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
