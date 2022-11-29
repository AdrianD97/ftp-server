public class UserConnection {
    private enum UserStatus {
        NOTLOGGEDIN, ENTEREDUSERNAME, LOGGEDIN
    }

    private User user;
    private SocketChannel socketChannel;
    private UserStatus currentUserStatus = UserStatus.NOTLOGGEDIN;

    public UserConnection(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    /**
     * - should have an execute command method
     * for each transfer command will create async job (each job will open
     * a data connection do the task, and in the end will close the connection)
     * - it will avoid the overriding
     */

}
