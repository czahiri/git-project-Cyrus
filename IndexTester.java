import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class IndexTester {

    public static void main(String[] args) {
        try {
            boolean prepOk = prepareSamples();
            if (prepOk == false) {
                System.out.println("Prepare samples: FAIL");
                return;
            } else {
                System.out.println("Prepare samples: PASS");
            }

            Index idx = new Index();
            idx.add("s1.txt");
            idx.add("s2.txt");
            idx.add("s3.txt");

            List<String> lines = readIndexLines();
            boolean check1 = verifyEntry(lines, "s1.txt");
            boolean check2 = verifyEntry(lines, "s2.txt");
            boolean check3 = verifyEntry(lines, "s3.txt");

            if (check1 == true) {
                System.out.println("s1.txt indexed: PASS");
            } else {
                System.out.println("s1.txt indexed: FAIL");
            }

            if (check2 == true) {
                System.out.println("s2.txt indexed: PASS");
            } else {
                System.out.println("s2.txt indexed: FAIL");
            }

            if (check3 == true) {
                System.out.println("s3.txt indexed: PASS");
            } else {
                System.out.println("s3.txt indexed: FAIL");
            }

            boolean all = false;
            if (check1 == true && check2 == true && check3 == true) {
                all = true;
            } else {
                all = false;
            }

            if (all == true) {
                System.out.println("All index checks: PASS");
            } else {
                System.out.println("All index checks: FAIL");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean prepareSamples() {
        try {
            boolean ok = makeFile("s1.txt", "alpha\nline2\n");
            if (ok == false) {
                return false;
            }
            ok = makeFile("s2.txt", "beta\nline2\nline3\n");
            if (ok == false) {
                return false;
            }
            ok = makeFile("s3.txt", "gamma\n");
            if (ok == false) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean makeFile(String name, String content) {
        try {
            File f = new File(name);
            FileWriter w = new FileWriter(f, false);
            w.write(content);
            w.flush();
            w.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static List<String> readIndexLines() {
        try {
            File idx = new File("git" + File.separator + "index");
            if (idx.exists()) {
                return Files.readAllLines(idx.toPath(), StandardCharsets.UTF_8);
            } else {
                return new ArrayList<String>();
            }
        } catch (Exception e) {
            return new ArrayList<String>();
        }
    }

    private static boolean verifyEntry(List<String> lines, String fileName) {
        try {
            String hashFromIndex = findHashFor(lines, fileName);
            if (hashFromIndex == null) {
                return false;
            }

            File src = new File(fileName);
            String recomputed = Blob.sha1FromFile(src);
            if (recomputed.equals(hashFromIndex)) {
                File obj = new File("git" + File.separator + "objects" + File.separator + hashFromIndex);
                if (obj.exists()) {
                    if (obj.isFile()) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static String findHashFor(List<String> lines, String fileName) {
        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i);
            if (line != null) {
                int space = line.indexOf(' ');
                if (space != -1) {
                    String left = line.substring(0, space);
                    String right = line.substring(space + 1);
                    if (right.equals(fileName)) {
                        return left;
                    }
                } else {
                    if (line.equals(fileName)) {
                        return "";
                    }
                }
            }
            i = i + 1;
        }
        return null;
    }
}
