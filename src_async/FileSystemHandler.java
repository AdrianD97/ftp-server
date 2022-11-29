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

enum FileType {
    ASCII, BINARY
};

public class FileSystemHandler {
    String root;
    String separator = "/";

    public FileSystemHandler(String root) {
        this.root = root;
    }

    private String _createPath(String path, String name) {
        return this.root + this.separator + path + (name != null ? this.separator + name : "");
    }

    /*
     * TODO: maybe you should retun a message explaining the reason why operation
     * failed (in case of failure)
     */
    public boolean mkdir(String path, String name) {
        File dir = new File(this._createPath(path, name));

        if (!dir.mkdir()) {
            return false;
        }

        return true;
    }

    public String[] ls(String path) {
        File file = new File(this._createPath(path, null));

        if (file.exists() && file.isDirectory()) {
            return file.list();
        } else if (file.exists() && file.isFile()) {
            String[] allFiles = new String[1];

            allFiles[0] = file.getName();
            return allFiles;
        } else {
            return new String[0];
        }
    }

    public boolean upload(String path, String name, FileType fileType, Closeable stream, boolean append) {
        File file = new File(this._createPath(path, name));

        if (file.exists() && append == false) {
            return false;
        }

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

        return true;
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
        File file = new File(this._createPath(path, null));

        if (!file.exists()) {
            return false;
        }

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
        File file = new File(this._createPath(path, null));

        if (!file.exists()) {
            return false;
        }

        if (!file.delete()) {
            return false;
        }

        return true;
    }
};
