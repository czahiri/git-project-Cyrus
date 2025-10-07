import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Git {
    private final File repo;

    /** Constructor sets repo directory to "git". */
    public Git() {
        this.repo = new File("git");
    }

    /** 
     * Initializes a basic Git-like repository with folders:
     * git/, git/objects/, git/index, git/HEAD 
     */
    public void initializeRepository() {
        File objects = new File(repo, "objects");
        File index = new File(repo, "index");
        File head = new File(repo, "HEAD");

        boolean repoOk = repo.exists() && repo.isDirectory();
        boolean objectsOk = objects.exists() && objects.isDirectory();
        boolean indexOk = index.exists() && index.isFile();
        boolean headOk = head.exists() && head.isFile();

        // Check if repo already exists
        if (repoOk && objectsOk && indexOk && headOk) {
            System.out.println("Git Repository Already Exists");
            return;
        }

        // Otherwise create missing items
        if (!repoOk) repo.mkdir();
        if (!objectsOk) objects.mkdir();

        try {
            if (!indexOk) index.createNewFile();
            if (!headOk) head.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Git Repository Created");
    }

    /**
     * Calculates SHA-1 hash of a file’s contents.
     * @param file File to hash
     * @return 40-character hex SHA-1 string
     */
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
        int bytesRead;
        while ((bytesRead = fis.read(buffer)) != -1) {
            md.update(buffer, 0, bytesRead);
        }
        fis.close();

        // Convert to hex
        byte[] hashBytes = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(b & 0xff);
            if (hex.length() == 1) sb.append("0");
            sb.append(hex);
        }
        return sb.toString();
    }
}
