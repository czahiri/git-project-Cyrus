public class WrapperTester {
    public static void main(String args[]) throws Throwable {

        /* Your tester code goes here */
        GitWrapper gw = new GitWrapper();
        gw.init();
        gw.add("proj/Hello.txt");
        gw.add("proj/docs/World.txt");
        gw.commit("John Doe", "Initial commit");
        gw.checkout("1234567890");

    }
}
