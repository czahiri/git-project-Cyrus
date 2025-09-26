import java.io.File;
import java.io.IOException;

public class Git {
    private final File repo;

    public Git() {
        this.repo = new File("git");
    }

    public void initializeRepository() {
        // Code to initialize a Git repository
        File objects = new File(repo, "objects");
        File index = new File(repo, "index");
        File head = new File(repo, "HEAD");

        boolean repoOk = repo.exists() && repo.isDirectory();
        boolean objectsOk = objects.exists() && objects.isDirectory();
        boolean indexOk = index.exists() && index.isFile();
        boolean headOk = head.exists() && head.isFile();

        if (repoOk && objectsOk && indexOk && headOk) {
            System.out.println("Git Repository Already Exists");
            return;
        }

        if (!repoOk) {
            repo.mkdir();
        }

        if (!objectsOk) {
            objects.mkdir();
        }

        if (!indexOk) {
            try {
                index.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (!headOk) {
            try {
                head.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Git Repository Created");
    }
}