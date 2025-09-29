import java.io.File;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;

public class GitTester {
    public static void main(String[] args) {
        File repo = new File("git");
        if (repo.exists()) {
            deleteRecursively(repo);
        }

        // Initialize repository
        Git g = new Git();
        g.initializeRepository();
        boolean repoOk = verifyRepo();
        if (repoOk == true) {
            System.out.println("Repo initialized: PASS");
        } else {
            System.out.println("Repo initialized: FAIL");
            return;
        }

        // Make file
        boolean made = makeFile("tester_file.txt", "hello\nline2\n");
        if (made == true) {
            System.out.println("File created: PASS");
        } else {
            System.out.println("File created: FAIL");
            return;
        }

        // Compute SHA-1
        String expected;
        try {
            expected = Blob.sha1FromFile(new File("tester_file.txt"));
        } catch (Exception e) {
            System.out.println("SHA1 compute: FAIL");
            return;
        }
        System.out.println("SHA1 computed: " + expected);

        // Make blob and compare
        String created;
        try {
            created = Blob.createBlobFromPath("tester_file.txt");
        } catch (Exception e) {
            System.out.println("Blob creation: FAIL");
            return;
        }
        if (created.equals(expected)) {
            System.out.println("Blob creation: PASS");
        } else {
            System.out.println("Blob creation: FAIL");
            return;
        }

        // Add to index
        try {
            Index idx = new Index();
            idx.add("tester_file.txt");
        } catch (Exception e) {
            System.out.println("Add to index: FAIL");
            return;
        }
        boolean inIndex = indexHasEntry("tester_file.txt", created);
        if (inIndex == true) {
            System.out.println("Index updated: PASS");
        } else {
            System.out.println("Index updated: FAIL");
            return;
        }

        // Delete repo
        if (repo.exists()) {
            deleteRecursively(repo);
        }
        if (!repo.exists()) {
            System.out.println("Repo cleanup: PASS");
        } else {
            System.out.println("Repo cleanup: FAIL");
            return;
        }

        // Delete program files
        File f = new File("tester_file.txt");
        if (f.exists()) {
            f.delete();
        }
        if (!f.exists()) {
            System.out.println("File cleanup: PASS");
        } else {
            System.out.println("File cleanup: FAIL");
        }
    }

    private static boolean verifyRepo() {
        File repo = new File("git");
        File objects = new File(repo, "objects");
        File index = new File(repo, "index");
        File head = new File(repo, "HEAD");

        if (repo.exists() && repo.isDirectory()) {
            if (objects.exists() && objects.isDirectory()) {
                if (index.exists() && index.isFile()) {
                    if (head.exists() && head.isFile()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void deleteRecursively(File f) {
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

    private static boolean makeFile(String name, String content) {
        try {
            FileWriter w = new FileWriter(new File(name), false);
            w.write(content);
            w.flush();
            w.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean indexHasEntry(String fileName, String hash) {
        try {
            File idx = new File("git" + File.separator + "index");
            if (!idx.exists()) {
                return false;
            }
            BufferedReader br = new BufferedReader(new FileReader(idx));
            String line = br.readLine();
            while (line != null) {
                int space = line.indexOf(' ');
                if (space != -1) {
                    String left = line.substring(0, space);
                    String right = line.substring(space + 1);
                    if (left.equals(hash) && right.equals(fileName)) {
                        br.close();
                        return true;
                    }
                }
                line = br.readLine();
            }
            br.close();
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
