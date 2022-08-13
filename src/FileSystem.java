import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

enum NodeType {
    FILE, DIRECTORY
};

enum FileType {
    ASCII, BINARY
};

class Node {
    String name;
    Node parent;
    List<Node> children;
    NodeType type;
    boolean deleted;

    Node(String name, NodeType type) {
        this.name = name;
        this.type = type;
        this.deleted = false;

        if (type == NodeType.DIRECTORY) {
            this.children = new LinkedList<Node>();
        } else {
            this.children = null;
        }
    }

    Node addChild(String name, NodeType type) {
        for (Node node : this.children) {
            if (node.name.equals(name)) {
                return null;
            }
        }

        Node node = new Node(name, type);

        node.parent = this;
        this.children.add(node);
        return node;
    }

    NodeType getType() {
        return this.type;
    }

    boolean isRoot() {
        return this.parent == null;
    }

};

public class FileSystem {
    String basePath;
    Node root;
    String separator = "/";

    public FileSystem(String basePath, String root) {
        this.basePath = basePath;
        this.root = new Node(root, NodeType.DIRECTORY);
    }

    public boolean init() {
        try {
            this._readDirectoryContent(new File(this.root.name), this.root);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private void _readDirectoryContent(File dir, Node node) throws IOException {
        File[] files = dir.listFiles();

        for (File file : files) {
            if (file.isDirectory()) {
                this._readDirectoryContent(file, node.addChild(file.getName(), NodeType.DIRECTORY));
            } else {
                node.addChild(file.getName(), NodeType.FILE);
            }
        }
    }

    /*
     * TODO: maybe you should retun a message explaining the reason why operation
     * failed (in case of failure)
     */
    public boolean mkdir(String path, String name) {
        Node node = this._findNode(this.root, path);

        if (node == null) {
            return false;
        }

        synchronized (node) {
            if (node.deleted == true || node.type == NodeType.FILE) {
                return false;
            }

            File dir = new File(this.basePath + this.separator + path + this.separator + name);

            if (!dir.mkdir()) {
                return false;
            }

            return node.addChild(name, NodeType.DIRECTORY) != null ? true : false;
        }
    }

    public List<String> ls(String path) {
        Node node = this._findNode(this.root, path);

        if (node == null) {
            return null;
        }

        List<String> content = new LinkedList<String>();

        if (node.type == NodeType.FILE) {
            content.add(node.name);
        } else {
            for (Node n : node.children) {
                content.add(n.name);
            }
        }
        return content;
    }

    public boolean upload(String path, String name, FileType fileType, Closeable stream, boolean append) {
        Node node = this._findNode(this.root, path);

        if (node == null) {
            return false;
        }

        synchronized (node) {
            if (node.deleted == true || node.type == NodeType.FILE) {
                return false;
            }

            boolean exists = false;

            for (Node child : node.children) {
                if (child.name.equals(name)) {
                    exists = true;
                    break;
                }
            }

            if (exists == true && append == false) {
                /* file already exists */
                return false;
            }

            File file = new File(this.basePath + this.separator + path + this.separator + name);

            boolean result;

            if (fileType == FileType.BINARY) {
                result = this._storeBinaryFile(stream, file, append);
            } else {
                result = this._storeAsciiFile(stream, file, append);
            }

            if (result == false) {
                return false;
            }

            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (append == true && exists == true) {
                return true;
            }

            return node.addChild(name, NodeType.FILE) != null ? true : false;
        }
    }

    private boolean _storeBinaryFile(Closeable stream, File file, boolean append) {
        BufferedInputStream fin;
        BufferedOutputStream fout = null;

        try {
            fin = (BufferedInputStream) stream;
            fout = new BufferedOutputStream(new FileOutputStream(file));
            byte[] buf = new byte[1024];
            int l = 0;

            while ((l = fin.read(buf, 0, 1024)) != -1) {
                fout.write(buf, 0, l);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                fout.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    private boolean _storeAsciiFile(Closeable stream, File file, boolean append) {
        BufferedReader rin = null;
        PrintWriter rout = null;

        try {
            rin = (BufferedReader) stream;
            rout = new PrintWriter(new FileOutputStream(file, append), true);
            String s;

            while ((s = rin.readLine()) != null) {
                rout.println(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            rout.close();
        }

        return true;
    }

    public boolean download(String path, FileType fileType, Closeable stream) {
        Node node = this._findNode(this.root, path);

        if (node == null) {
            return false;
        }

        synchronized (node) {
            if (node.deleted == true || node.type == NodeType.DIRECTORY) {
                return false;
            }

            File file = new File(this.basePath + this.separator + path);

            boolean result;

            if (fileType == FileType.BINARY) {
                result = this._readBinaryFile(file, stream);
            } else {
                result = this._readAsciiFile(file, stream);
            }

            if (result == false) {
                return false;
            }

            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    private boolean _readBinaryFile(File file, Closeable stream) {
        BufferedOutputStream fout = null;
        BufferedInputStream fin = null;

        try {
            fout = (BufferedOutputStream) stream;
            fin = new BufferedInputStream(new FileInputStream(file));
            byte[] buf = new byte[1024];
            int l = 0;

            while ((l = fin.read(buf, 0, 1024)) != -1) {
                fout.write(buf, 0, l);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                fin.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    private boolean _readAsciiFile(File file, Closeable stream) {
        BufferedReader rin = null;
        PrintWriter rout = null;

        try {
            rout = (PrintWriter) stream;
            rin = new BufferedReader(new FileReader(file));
            String s;

            while ((s = rin.readLine()) != null) {
                rout.println(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                rin.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    public boolean rm(String path) {
        Node node = this._findNode(this.root, path);

        if (node == null) {
            return false;
        }

        synchronized (node) {
            if (node.deleted == true || node.type == NodeType.DIRECTORY) {
                return false;
            }

            File file = new File(this.basePath + this.separator + path);

            if (!file.delete()) {
                return false;
            }

            node.deleted = true;
            return node.parent.children.remove(node);
        }
    }

    private Node _findNode(Node node, String path) {
        if (node.name.equals(path)) { // when use the node, you acquire the lock and check if it was not marked as
                                      // deleted
            return node;
        }

        String newPath = path.substring(path.indexOf('/') + 1);
        int index = newPath.indexOf('/');
        String childName;

        if (index != -1) {
            childName = newPath.substring(0, newPath.indexOf('/'));
        } else {
            childName = newPath;
        }

        synchronized (node) {
            if (node.children != null) {
                for (Node child : node.children) {
                    if (child.name.equals(childName)) {
                        return this._findNode(child, newPath);
                    }
                }
            }
        }

        return null;
    }
};
