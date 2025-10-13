import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Date;

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

        if (repoOk && objectsOk && indexOk && headOk) { // updated
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

        byte[] hashBytes = md.digest();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hashBytes.length; i++) {
            String hex = Integer.toHexString(hashBytes[i] & 0xff);
            if (hex.length() == 1) {
                sb.append("0");
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    public static String writeInCommit(String author, String message) throws Throwable {

        String finalAdd = "";
        // get the tree
        WorkingList workingList = new WorkingList();
        String shaOfRoot = workingList.build();
        finalAdd += "tree: " + shaOfRoot + "\n";

        // get the parent
        File commitFile = new File("Commit");
        File headFile = new File("git", "HEAD");
        String parent = getParentHelper(headFile);
        finalAdd += "parent: " + parent + "\n";

        // easy stuff
        finalAdd += "author: " + author + "\n";
        finalAdd += dateLineHelper() + "\n";
        finalAdd += "message: " + message;

        String hashName = generateSHA1HashHelper(finalAdd);

        commitFile = new File("git/objects", hashName); // maybe git/obj
        commitFile.createNewFile();

        try {
            Files.write(Paths.get(commitFile.getAbsolutePath()), finalAdd.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Files.write(Paths.get("git/" + "HEAD"), hashName.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return hashName;

    }

    public static String dateLineHelper() {
        // makes date string
        Date now = new Date();
        String hours = "" + now.getHours();
        String mins = "" + now.getMinutes();
        String secs = "" + now.getSeconds();

        LocalDate date = LocalDate.now();
        String dateStr = date.toString(); // gives numbers
        String toWriteInCommit = "";
        String year = dateStr.substring(0, 4);
        String month = dateStr.substring(5, 7);
        String day = dateStr.substring(8);
        if (month.equals("01")) {
            toWriteInCommit += "Jan ";
        }
        if (month.equals("02")) {
            toWriteInCommit += "Feb ";
        }
        if (month.equals("03")) {
            toWriteInCommit += "March ";
        }
        if (month.equals("04")) {
            toWriteInCommit += "April ";
        }
        if (month.equals("05")) {
            toWriteInCommit += "May ";
        }
        if (month.equals("06")) {
            toWriteInCommit += "June ";
        }
        if (month.equals("07")) {
            toWriteInCommit += "July ";
        }
        if (month.equals("08")) {
            toWriteInCommit += "Aug ";
        }
        if (month.equals("09")) {
            toWriteInCommit += "Sept ";
        }
        if (month.equals("10")) {
            toWriteInCommit += "Oct ";
        }
        if (month.equals("11")) {
            toWriteInCommit += "Nov ";
        }
        if (month.equals("12")) {
            toWriteInCommit += "Dec ";
        }

        toWriteInCommit += day + ", ";
        toWriteInCommit += year;
        toWriteInCommit = "date: " + toWriteInCommit + " hour: " + hours + " min: " + mins + " secs: " + secs;
        return toWriteInCommit;
    }

    public static String generateSHA1HashHelper(String input) {
        try {
            // Get an instance of the SHA-1 MessageDigest
            MessageDigest md = MessageDigest.getInstance("SHA-1");

            // Convert the input string to bytes and digest them
            byte[] messageDigest = md.digest(input.getBytes());

            // Convert the byte array into a hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                // Convert each byte to its hexadecimal representation
                String hex = Integer.toHexString(0xff & b);
                // Prepend a '0' if the hex value is a single digit
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            // Handle the case where SHA-1 algorithm is not available
            throw new RuntimeException("SHA-1 algorithm not found.", e);
        }
    }

    public static String getParentHelper(File headFile) throws Throwable {
        File git = new File("git");
        Path filePath = git.toPath().resolve(headFile.getName());
        byte[] fileBytes = Files.readAllBytes(filePath);
        String parent = new String(fileBytes);
        return parent;
    }
}