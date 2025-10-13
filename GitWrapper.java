import java.io.IOException;

public class GitWrapper {

    /**
     * Initializes a new Git repository.
     * This should create the necessary directory structure
     * and initial files required for a Git repository.
     * This should create the initial commit and update HEAD accordingly
     */
    public void init() {
        Git g = new Git();
        g.initializeRepository();
    };

    /**
     * Stages a file for the next commit.
     *
     * @param filePath The path to the file to be staged.
     * @throws IOException 
     */
    public void add(String filePath) throws IOException {
       Index i = new Index();
       i.add(filePath);
    };

    /**
     * Creates a commit with the given author and message.
     * It should capture the current state of the repository,
     * update the HEAD, and return the commit hash.
     *
     * @param author  The name of the author making the commit.
     * @param message The commit message describing the changes.
     * @return The SHA1 hash of the new commit.
     * @throws Throwable
     */
    public String commit(String author, String message) throws Throwable {
        return Git.writeInCommit(author, message);
    };

    /**
     * EXTRA CREDIT: Checks out a specific commit given its hash.
     * This should update the working directory to match the
     * state of the repository at that commit.
     *
     * @param commitHash The SHA1 hash of the commit to check out.
     */
    public void checkout(String commitHash) {
        // to-do: implement functionality here

    };
}