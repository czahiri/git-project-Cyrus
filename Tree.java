import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Tree {

    public String createTree(String dirPath) throws IOException {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            throw new IOException("Directory does not exist: " + dirPath);
        }
        if (!dir.isDirectory()) {
            throw new IOException("Not a directory: " + dirPath);
        }

        StringBuilder sb = new StringBuilder();
        File[] items = dir.listFiles();
        if (items != null) {
            int i = 0;
            while (i < items.length) {
                File f = items[i];
                if (f.isFile()) {
                    String blobHash = Blob.createBlobFromPath(f.getPath());
                    sb.append("blob ").append(blobHash).append(" ").append(f.getPath()).append("\n");
                } else {
                    if (f.isDirectory()) {
                        String childHash = createTree(f.getPath());
                        sb.append("tree ").append(childHash).append(" ").append(f.getPath()).append("\n");
                    }
                }
                i = i + 1;
            }
        }

        String data = trimOne(sb.toString());
        String treeHash = sha1OfString(data);
        ensureObjects();
        File out = new File("git" + File.separator + "objects", treeHash);
        if (!out.exists()) {
            FileWriter w = new FileWriter(out, false);
            w.write(data);
            w.flush();
            w.close();
        }
        return treeHash;
    }

    public String readObject(String hash) {
        try {
            File f = new File("git" + File.separator + "objects", hash);
            byte[] b = Files.readAllBytes(f.toPath());
            return new String(b, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean objectExists(String hash) {
        File f = new File("git" + File.separator + "objects", hash);
        if (f.exists()) {
            if (f.isFile()) {
                return true;
            }
        }
        return false;
    }

    public boolean linePresent(String content, String expected) {
        if (content == null) {
            return false;
        }
        int start = 0;
        while (true) {
            int nl = content.indexOf('\n', start);
            String line;
            if (nl == -1) {
                line = content.substring(start);
            } else {
                line = content.substring(start, nl);
            }
            if (line.equals(expected)) {
                return true;
            }
            if (nl == -1) {
                break;
            }
            start = nl + 1;
        }
        return false;
    }

    private void ensureObjects() {
        File git = new File("git");
        if (!git.exists()) {
            git.mkdir();
        }
        File objects = new File(git, "objects");
        if (!objects.exists()) {
            objects.mkdir();
        }
    }

    private String trimOne(String s) {
        if (s == null) {
            return "";
        }
        if (s.endsWith("\n")) {
            return s.substring(0, s.length() - 1);
        } else {
            return s;
        }
    }

    private String sha1OfString(String input) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 not available");
        }
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        return toHex(digest);
        }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        int i = 0;
        while (i < bytes.length) {
            int v = bytes[i] & 0xff;
            String h = Integer.toHexString(v);
            if (h.length() == 1) {
                sb.append('0');
            }
            sb.append(h);
            i = i + 1;
        }
        return sb.toString();
    }
}
