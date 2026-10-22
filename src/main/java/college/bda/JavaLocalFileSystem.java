package college.bda;

import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.permission.FsPermission;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/** Local Windows demo adapter. Uses Java NIO instead of external winutils binaries.
 * HDFS URIs continue to use Hadoop's normal DistributedFileSystem implementation.
 */
public final class JavaLocalFileSystem extends RawLocalFileSystem {
    @Override public boolean mkdirs(Path path, FsPermission permission) throws IOException {
        Files.createDirectories(pathToFile(path).toPath()); return true;
    }
    @Override public void setPermission(Path path, FsPermission permission) { /* NTFS uses Windows ACLs. */ }
    @Override public FileStatus getFileStatus(Path path) throws IOException {
        java.nio.file.Path local = pathToFile(path).toPath();
        if (!Files.exists(local)) throw new FileNotFoundException(path.toString());
        BasicFileAttributes attrs = Files.readAttributes(local, BasicFileAttributes.class);
        return new FileStatus(attrs.size(), attrs.isDirectory(), 1, getDefaultBlockSize(path),
                attrs.lastModifiedTime().toMillis(), attrs.lastAccessTime().toMillis(),
                new FsPermission((short)0777), System.getProperty("user.name"), "", makeQualified(path));
    }
    @Override public FileStatus[] listStatus(Path path) throws IOException {
        java.nio.file.Path local = pathToFile(path).toPath();
        if (!Files.isDirectory(local)) return new FileStatus[]{getFileStatus(path)};
        List<FileStatus> result = new ArrayList<>();
        try (var files = Files.list(local)) {
            for (java.nio.file.Path item : files.toList())
                result.add(getFileStatus(new Path(path, item.getFileName().toString())));
        }
        return result.toArray(FileStatus[]::new);
    }
    @Override public boolean delete(Path path, boolean recursive) throws IOException {
        java.nio.file.Path local = pathToFile(path).toPath().toAbsolutePath().normalize();
        if (local.getParent() == null) throw new IOException("Refusing to delete a filesystem root.");
        if (!Files.exists(local)) return false;
        if (!recursive || !Files.isDirectory(local)) { Files.delete(local); return true; }
        try (var entries = Files.walk(local)) {
            for (java.nio.file.Path entry : entries.sorted(Comparator.reverseOrder()).toList()) Files.delete(entry);
        }
        return true;
    }
}
