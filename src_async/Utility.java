public class Utility {
    private static boolean debugMode;

    public static void activateDebugMode() {
        Utility.debugMode = true;
    }

    public static void deactivateDebugMode() {
        Utility.debugMode = false;
    }

    public static void debugOutput() {
        if (Utility.debugMode) {
            System.out.println("Thread " + Thread.currentThread().getId() + ": " + msg);
        }
    }
}
