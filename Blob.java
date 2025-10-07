import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Handles creating and hashing blob files. */
public class Blob {

    /** Toggle for compression (stretch goal). */
    public static boolean COMPRESS = false;

    /** Generates SHA-1 from file contents. */
    public static String sha1FromFile(File file) throws IOException {
        if (file == null) throw new IOException("File is null");
        if (!file.exists()) throw new IOException("File not found: " + file.getPath());
        if (!file.isFile()) throw new IOException("Not a file: " + file.getPath());

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 not available");
        }

        FileInputStream in = new FileInputStream(file);
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            md.update(buffer, 0, read);
        }
        in.close();

        return toHex(md.digest());
    }

    /** 
     * Creates a blob file for given source path, compressed if COMPRESS is true.
     * Returns the blob SHA-1 hash.
     */
    public static String createBlobFromPath(String sourcePath) throws IOException {
        File source = new File(sourcePath);

        File gitDir = new File("git");
        if (!gitDir.exists()) gitDir.mkdir();

        File objectsDir = new File(gitDir, "objects");
        if (!objectsDir.exists()) objectsDir.mkdir();

        String hash;

        if (COMPRESS) {
            // Compress + save blob
            byte[] raw = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(sourcePath));
            java.util.zip.Deflater deflater = new java.util.zip.Deflater();
            deflater.setInput(raw);
            deflater.finish();

            byte[] buffer = new byte[1024];
            java.io.ByteArrayOutputStream outBytes = new java.io.ByteArrayOutputStream();
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                outBytes.write(buffer, 0, count);
            }
            deflater.end();

            byte[] compressed = outBytes.toByteArray();
            hash = sha1OfBytes(compressed);
            File blobFile = new File(objectsDir, hash);
            if (!blobFile.exists()) {
                FileOutputStream out = new FileOutputStream(blobFile);
                out.write(compressed);
                out.close();
            }
        } else {
            // Normal (uncompressed)
            hash = sha1FromFile(source);
            File blobFile = new File(objectsDir, hash);
            if (!blobFile.exists()) writeRawCopy(source, blobFile);
        }

        return hash;
    }

    /** Writes file bytes exactly as-is to new target blob file. */
    private static void writeRawCopy(File source, File target) throws IOException {
        FileInputStream in = new FileInputStream(source);
        FileOutputStream out = new FileOutputStream(target);
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        out.close();
        in.close();
    }

    /** Computes SHA-1 of raw byte array. */
    private static String sha1OfBytes(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(data);
            return toHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 not available");
        }
    }

    /** Converts bytes to hex string. */
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String h = Integer.toHexString(b & 0xff);
            if (h.length() == 1) sb.append('0');
            sb.append(h);
        }
        return sb.toString();
    }
}
