package tools;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class PosixZipRepack {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: PosixZipRepack <sourceDir> <outputZip>");
            System.exit(1);
        }

        File sourceDir = new File(args[0]);
        File zipFile = new File(args[1]);
        if (zipFile.exists()) {
            zipFile.delete();
        }

        Path srcPath = sourceDir.toPath();
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)))) {
            Files.walk(srcPath).filter(Files::isRegularFile).forEach(path -> {
                try {
                    String rel = srcPath.relativize(path).toString().replace('\\', '/');
                    byte[] data = Files.readAllBytes(path);
                    ZipEntry entry = new ZipEntry(rel);

                    if (rel.equals("resources.arsc")) {
                        entry.setMethod(ZipEntry.STORED);
                        entry.setSize(data.length);
                        entry.setCompressedSize(data.length);
                        CRC32 crc = new CRC32();
                        crc.update(data);
                        entry.setCrc(crc.getValue());
                    } else {
                        entry.setMethod(ZipEntry.DEFLATED);
                    }

                    zos.putNextEntry(entry);
                    zos.write(data);
                    zos.closeEntry();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        System.out.println("PosixZipRepack successfully created: " + zipFile.getAbsolutePath());
    }
}
