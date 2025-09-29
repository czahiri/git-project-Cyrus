import java.io.File;

public class GitTester {

    public static boolean verifyRepo() {
        File repo = new File("git");
        File objects = new File(repo, "objects");
        File index = new File(repo, "index");
        File head = new File(repo, "HEAD");

        boolean result = false;

        if (repo.exists() && repo.isDirectory()) {
            if (objects.exists() && objects.isDirectory()) {
                if (index.exists() && index.isFile()) {
                    if (head.exists() && head.isFile()) {
                        result = true;
                    } else {
                        result = false;
                    }
                } else {
                    result = false;
                }
            } else {
                result = false;
            }
        } else {
            result = false;
        }

        return result;
    }

    public static void deleteRecursively(File f) {
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) {
                for (int i = 0; i < kids.length; i++) {
                    deleteRecursively(kids[i]);
                }
            }
        }
        f.delete();
    }

    public static void cleanup() {
        File repo = new File("git");
        if (repo.exists()) {
            deleteRecursively(repo);
        }
    }

    public static boolean cycleOnce() {
        cleanup();

        Git g1 = new Git();
        g1.initializeRepository();
        boolean ok1 = verifyRepo();
        if (ok1 == false) {
            return false;
        }

        Git g2 = new Git();
        g2.initializeRepository();
        boolean ok2 = verifyRepo();
        if (ok2 == false) {
            return false;
        }

        cleanup();
        File repo = new File("git");
        boolean ok3;
        if (repo.exists()) {
            ok3 = false;
        } else {
            ok3 = true;
        }

        if (ok1 == true && ok2 == true && ok3 == true) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean cycleMany(int n) {
        boolean allPass = true;
        for (int i = 0; i < n; i++) {
            boolean pass = cycleOnce();
            if (pass == true) {
                System.out.println("Cycle " + (i + 1) + ": PASS");
            } else {
                System.out.println("Cycle " + (i + 1) + ": FAIL");
                allPass = false;
            }
        }
        if (allPass == true) {
            return true;
        } else {
            return false;
        }
    }

    public static void main(String[] args) {
        //Tester should 0. Init git, 1. Make file, 2. Write to File, 3. Make Blob, compare SHA-1, 4. Add to index, 5. Delete git directory to reset, 6. Delete files that were programmatically created
        int times = 3;
        if (args != null) {
            if (args.length > 0) {
                try {
                    times = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    times = 3;
                }
            }
        }

        boolean result = cycleMany(times);
        if (result == true) {
            System.out.println("All cycles: PASS");
        } else {
            System.out.println("All cycles: FAIL");
        }
    }
}
