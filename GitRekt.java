import java.io.*;

public class GitRekt {

    // Initializes repository folders and files
    public void init() {
        Git g = new Git();
        g.initializeRepository();
    }

    // Stages a file (creates blob and updates index)
    public void add(String filePath) throws IOException {
        Index idx = new Index();
        idx.add(filePath);
    }

    // Helper to read HEAD (not used by this tester, but handy)
    public String head() throws IOException {
        File h = new File("git/HEAD");
        if (!h.exists()) {
            return null;
        }
        BufferedReader br = new BufferedReader(new FileReader(h));
        String s = br.readLine();
        br.close();
        if (s == null) {
            return null;
        }
        s = s.trim();
        if (s.length() == 0) {
            return null;
        }
        return s;
    }
}
