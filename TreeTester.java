public class TreeTester {
    public static void main(String[] args) {
        try {
            Tree t = new Tree();
            String hash = t.createTree("proj");
            System.out.println("Tree hash: " + hash);
            System.out.println("Tree content:");
            System.out.println(t.readObject(hash));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}