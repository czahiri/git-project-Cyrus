import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.*;

/** Creates tree objects that reference blobs and subtrees. */
public class Tree {

    /** Builds a tree file recursively for the given directory path. */
    public String createTree(String dirPath) throws IOException {
        File dir = new File(dirPath);
        if (!dir.exists()) throw new IOException("Directory does not exist: " + dirPath);
        if (!dir.isDirectory()) throw new IOException("Not a directory: " + dirPath);

        StringBuilder sb = new StringBuilder();
        File[] items = dir.listFiles();
        if (items != null) {
            for (File f : items) {
                if (f.isFile()) {
                    String blobHash = Blob.createBlobFromPath(f.getPath());
                    sb.append("blob ").append(blobHash).append(" ").append(f.getPath()).append("\n");
                } else if (f.isDirectory()) {
                    String childHash = createTree(f.getPath());
                    sb.append("tree ").append(childHash).append(" ").append(f.getPath()).append("\n");
                }
            }
        }

        String data = trimOne(sb.toString());
        String treeHash = sha1OfString(data);
        ensureObjects();
        File out = new File("git/objects", treeHash);
        if (!out.exists()) try (FileWriter w = new FileWriter(out)) { w.write(data); }
        return treeHash;
    }

    /** Reads and returns contents of tree object. */
    public String readObject(String hash) {
        try {
            File f = new File("git/objects", hash);
            byte[] b = Files.readAllBytes(f.toPath());
            return new String(b, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    /** Checks if a tree object exists. */
    public boolean objectExists(String hash) {
        File f = new File("git/objects", hash);
        return f.exists() && f.isFile();
    }

    /** Searches for an expected line in the given tree content. */
    public boolean linePresent(String content, String expected) {
        if (content == null) return false;
        for (String line : content.split("\n")) {
            if (line.equals(expected)) return true;
        }
        return false;
    }

    /** Ensures git/objects folder exists. */
    private void ensureObjects() {
        File git = new File("git");
        if (!git.exists()) git.mkdir();
        File objects = new File(git, "objects");
        if (!objects.exists()) objects.mkdir();
    }

    /** Removes last newline from string if present. */
    private String trimOne(String s) {
        return (s != null && s.endsWith("\n")) ? s.substring(0, s.length() - 1) : (s == null ? "" : s);
    }

    /** Returns SHA-1 hash of a string’s bytes. */
    private String sha1OfString(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return toHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 not available");
        }
    }

    /** Converts bytes to hex string. */
    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String h = Integer.toHexString(b & 0xff);
            if (h.length() == 1) sb.append('0');
            sb.append(h);
        }
        return sb.toString();
    }
}
