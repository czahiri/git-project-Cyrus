import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class WorkingListTester {
    public static void main(String[] args) {
        try {
            // make directories
            new File("git").mkdirs();
            new File("git/objects").mkdirs();
            new File("proj/docs").mkdirs();

            // make some files
            File f1 = new File("proj/README.md");
            File f2 = new File("proj/docs/Hello.txt");
            File f3 = new File("proj/docs/World.txt");

            write(f1, "readme\n");
            write(f2, "hello\n");
            write(f3, "world\n");

            // blob them
            String h1 = Blob.createBlobFromPath(f1.getPath());
            String h2 = Blob.createBlobFromPath(f2.getPath());
            String h3 = Blob.createBlobFromPath(f3.getPath());

            // write index
            File idx = new File("git/index");
            FileWriter w = new FileWriter(idx, false);
            w.write(h1 + " " + f1.getPath() + "\n");
            w.write(h2 + " " + f2.getPath() + "\n");
            w.write(h3 + " " + f3.getPath());
            w.close();

            // build tree
            WorkingList wl = new WorkingList();
            String root = wl.build();

            // print results
            System.out.println("Root tree: " + root);
            System.out.println("Contents:");
            System.out.println(readObj(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void write(File f, String s) throws Exception {
        f.getParentFile().mkdirs();
        FileWriter w = new FileWriter(f, false);
        w.write(s);
        w.close();
    }

    private static String readObj(String sha) throws Exception {
        File obj = new File("git/objects", sha);
        byte[] b = Files.readAllBytes(obj.toPath());
        return new String(b, StandardCharsets.UTF_8);
    }
}
