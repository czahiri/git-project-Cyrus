import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Blob {

    public static boolean COMPRESS = false;

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
        int read;
        while (true) {
            read = in.read(buffer);
            if (read == -1) {
                break;
            }
            md.update(buffer, 0, read);
        }
        in.close();

        byte[] digest = md.digest();
        String hex = toHex(digest);
        return hex;
    }

    public static String createBlobFromPath(String sourcePath) throws IOException {
        File source = new File(sourcePath);
        String hash = sha1FromFile(source);

        File gitDir = new File("git");
        if (!gitDir.exists()) {
            gitDir.mkdir();
        }

        File objectsDir = new File(gitDir, "objects");
        if (!objectsDir.exists()) {
            objectsDir.mkdir();
        }

        File blobFile = new File(objectsDir, hash);
        if (!blobFile.exists()) {
            if (COMPRESS == true) {
                writeCompressedCopy(source, blobFile);
            } else {
                writeRawCopy(source, blobFile);
            }
        }
        return hash;
    }

    private static void writeRawCopy(File source, File target) throws IOException {
        FileInputStream in = new FileInputStream(source);
        FileOutputStream out = new FileOutputStream(target);
        byte[] buffer = new byte[8192];
        int read;
        while (true) {
            read = in.read(buffer);
            if (read == -1) {
                break;
            }
            out.write(buffer, 0, read);
        }
        out.flush();
        out.close();
        in.close();
    }

    private static void writeCompressedCopy(File source, File target) throws IOException {
        java.util.zip.DeflaterOutputStream out = new java.util.zip.DeflaterOutputStream(new FileOutputStream(target));
        FileInputStream in = new FileInputStream(source);
        byte[] buffer = new byte[8192];
        int read;
        while (true) {
            read = in.read(buffer);
            if (read == -1) {
                break;
            }
            out.write(buffer, 0, read);
        }
        out.finish();
        out.close();
        in.close();
    }

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
