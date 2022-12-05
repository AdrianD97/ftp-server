public class Debug {
    private boolean debugMode;

    public Debug(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public void out() {
        if (this.debugMode) {
            System.out.println("Thread " + Thread.currentThread().getId() + ": " + msg);
        }
    }
}
