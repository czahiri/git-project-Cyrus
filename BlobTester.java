import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class BlobTester {

    public static boolean verifyExists(String hash) {
        File f = new File("git" + File.separator + "objects" + File.separator + hash);
        if (f.exists()) {
            if (f.isFile()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static void resetObjects() {
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

    public static void deleteRecursively(File f) {
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

    public static void main(String[] args) {
        try {
            File src = new File("blob_source.txt");
            if (!src.exists()) {
                FileWriter w = new FileWriter(src);
                w.write("This is blob data.\nLine 2.\n");
                w.flush();
                w.close();
            }

            String hash = Blob.createBlobFromPath(src.getPath());
            boolean ok1 = verifyExists(hash);
            if (ok1 == true) {
                System.out.println("Verify after create: PASS");
            } else {
                System.out.println("Verify after create: FAIL");
            }

            resetObjects();
            boolean ok2 = verifyExists(hash);
            if (ok2 == false) {
                System.out.println("Verify after reset: PASS");
            } else {
                System.out.println("Verify after reset: FAIL");
            }

            String hash2 = Blob.createBlobFromPath(src.getPath());
            boolean ok3 = verifyExists(hash2);
            if (ok3 == true) {
                System.out.println("Verify after recreate: PASS");
            } else {
                System.out.println("Verify after recreate: FAIL");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}