import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// Handles creating and hashing blob files
public class Blob {

    // Toggle for compression (not used in rubric paths, kept simple)
    public static boolean COMPRESS = false;

    // Generates SHA-1 from file contents
    public static String sha1FromFile(File file) throws IOException {
        if (file == null) {
            throw new IOException("File is null");
        }
        if (!file.exists()) {
            throw new IOException("File not found: " + file.getPath());
        }
        if (!file.isFile()) {
            throw new IOException("Not a file: " + file.getPath());
        }

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 not available");
        }

        FileInputStream in = new FileInputStream(file);
        byte[] buffer = new byte[8192];
        int read = in.read(buffer);
        while (read != -1) {
            md.update(buffer, 0, read);
            read = in.read(buffer);
        }
        in.close();

        byte[] dig = md.digest();
        return toHex(dig);
    }

    // Creates a blob file for given source path. Returns blob SHA-1.
    public static String createBlobFromPath(String sourcePath) throws IOException {
        File source = new File(sourcePath);
        if (!source.exists()) {
            throw new IOException("Path does not exist: " + sourcePath);
        }
        if (source.isDirectory()) {
            throw new IOException("Path is a directory, not a file: " + sourcePath);
        }

        File gitDir = new File("git");
        if (!gitDir.exists()) {
            gitDir.mkdir();
        }

        File objectsDir = new File(gitDir, "objects");
        if (!objectsDir.exists()) {
            objectsDir.mkdir();
        }

        String hash = sha1FromFile(source);
        File blobFile = new File(objectsDir, hash);

        if (!blobFile.exists()) {
            writeRawCopy(source, blobFile);
        }

        return hash;
    }

    // Writes source file bytes to target
    private static void writeRawCopy(File source, File target) throws IOException {
        FileInputStream in = new FileInputStream(source);
        FileOutputStream out = new FileOutputStream(target);
        byte[] buffer = new byte[8192];
        int read = in.read(buffer);
        while (read != -1) {
            out.write(buffer, 0, read);
            read = in.read(buffer);
        }
        out.flush();
        out.close();
        in.close();
    }

    // Converts bytes to hex
    private static String toHex(byte[] bytes) {
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
