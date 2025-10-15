import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Git {
    private final File repo;

    // Constructor sets repo directory to "git"
    public Git() {
        this.repo = new File("git");
    }

    // Initializes git/, git/objects/, git/index, git/HEAD (idempotent)
    public void initializeRepository() {
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

        try {
            if (!indexOk) {
                index.createNewFile();
            }
            if (!headOk) {
                head.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Git Repository Created");
    }

    // Calculates SHA-1 hash of a file’s contents and returns 40-char hex string
    public static String sha1FromFile(File file) throws IOException {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IOException("File not found: " + file);
        }

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available");
        }

        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[8192];
        int bytesRead = fis.read(buffer);
        while (bytesRead != -1) {
            md.update(buffer, 0, bytesRead);
            bytesRead = fis.read(buffer);
        }
        fis.close();

        byte[] hashBytes = md.digest();
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < hashBytes.length) {
            int v = hashBytes[i] & 0xff;
            String hex = Integer.toHexString(v);
            if (hex.length() == 1) {
                sb.append("0");
            }
            sb.append(hex);
            i = i + 1;
        }
        return sb.toString();
    }
}
