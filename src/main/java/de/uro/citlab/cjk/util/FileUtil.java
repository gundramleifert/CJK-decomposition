/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uro.citlab.cjk.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gundram
 */
public class FileUtil {

    public static final Charset CHARSET = StandardCharsets.UTF_8;
    public static final String CHARSET_NAME = CHARSET.name();
    public static final String[] IMAGE_SUFFIXES = new String[]{
        "png", "jpg", "jpeg", "jpe", "tif", "tiff",
        "PNG", "JPG", "JPEG", "JPE", "TIF", "TIFF"};
    private static Logger LOG = LoggerFactory.getLogger(FileUtil.class);

    public static List<String> readLines(File file) {
        try {
            return FileUtils.readLines(file, CHARSET);
        } catch (IOException ex) {
            throw new RuntimeException("cannot read file '" + file == null ? null : file.getAbsolutePath() + "'", ex);
        }
    }

    public static void writeLines(File file, List<String> lines) {
        writeLines(file, lines, false);
    }

    public static void writeLine(File file, String line) {
        writeLines(file, Arrays.asList(line), false);
    }

    public static void writeLines(File file, List<String> lines, boolean append) {
        try {
            FileUtils.writeLines(file, CHARSET_NAME, lines, append);
        } catch (IOException ex) {
            throw new RuntimeException("cannot write file '" + file == null ? null : file.getAbsolutePath() + "'", ex);
        }
    }

    public static void deleteFilesInPageFolder(Collection<File> files) {
        List<File> deleteList = new LinkedList<>();
        for (File file : files) {
            File parentFile = file.getParentFile();
            if (parentFile != null && parentFile.getName().equals("page")) {
                deleteList.add(file);
            }
        }
        files.removeAll(deleteList);
    }

    public static void deleteMetadataAndMetsFiles(Collection<File> files) {
        List<File> deleteList = new LinkedList<>();
        for (File file : files) {
            String name = file.getName();
            if (name.equals("mets.xml") || name.equals("metadata.xml") || name.equals("doc.xml")) {
                deleteList.add(file);
                LOG.warn("ignore file {} because it is probably no pageXML file.", file);
            }
        }
        files.removeAll(deleteList);
    }

    public static File getTgtFile(Path srcFolder, Path tgtFolder, File srcFile) {
        return new File(tgtFolder.toFile(), srcFolder.relativize(srcFile.toPath()).toString());
    }

    public static File getTgtFile(File srcFolder, File tgtFolder, File srcFile) {
        return getTgtFile(srcFolder.getAbsoluteFile().toPath(), tgtFolder.getAbsoluteFile().toPath(), srcFile.getAbsoluteFile());
    }

    public static String[] asStringList(Collection<File> lst) {
        if (lst == null) {
            return null;
        }
        String[] res = new String[lst.size()];
        int idx = 0;
        for (File file : lst) {
            res[idx++] = file.getAbsolutePath();
        }
        return res;
    }

    public static List<String> getStringList(Collection<File> lst) {
        if (lst == null) {
            return null;
        }
        List<String> res = new ArrayList<>(lst.size());
        for (File file : lst) {
            res.add(file.getAbsolutePath());
        }
        return res;
    }

    public static File getFile(File directory, final String name, boolean recursive) {
        Iterator<File> iterateFiles = FileUtils.iterateFiles(directory, null, recursive);
        while (iterateFiles.hasNext()) {
            File file = iterateFiles.next();
            if (file.getName().equals(name)) {
                return file;
            }
        }
        return null;
    }

    public static List<File> listFiles(File directory, String extension, boolean recursive) {
        return listFiles(directory, new String[]{extension}, recursive);

    }

    public static List<File> listFiles(
            File directory, String[] extensions, boolean recursive) {
        try {
            Collection<File> listFiles = FileUtils.listFiles(directory, extensions, recursive);
            LinkedList<File> res = new LinkedList<>(listFiles);
            Collections.sort(res);
            return res;
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("cannot find directory '" + directory + "'.", ex);
        }
    }

    public static File getSourceFolderListsOrFolders(String pathes, String[] suffixes, boolean recursive) {
        if (pathes.split(File.pathSeparator).length != 1) {
            throw new RuntimeException("path should be only one path or file, but is " + pathes + ".");
        }
        if (new File(pathes).isDirectory()) {
            return new File(pathes);
        }
        return new File(".");
    }

    public static List<File> getFilesListsOrFolders(String pathes, String[] suffixes, boolean recursive) {
        List<File> res = new ArrayList<>();
        for (String string : pathes.split(File.pathSeparator)) {
            if (new File(string).isDirectory()) {
                res.addAll(FileUtil.listFiles(new File(string), suffixes, recursive));
            } else {
                try {
                    List<String> readLines = FileUtils.readLines(new File(string));
                    for (String readLine : readLines) {
                        res.add(new File(readLine));
                    }
                } catch (IOException ex) {
                    throw new RuntimeException("cannot load file  or folder '" + new File(string).getAbsolutePath() + "'.", ex);
                }
            }
        }
        return res;

    }

    public static void copyFile(File src, File tgt) {
        try {
            FileUtils.copyFile(src, tgt);
        } catch (IOException ex) {
            throw new RuntimeException("cannot copy file '" + src == null ? "null" : src.getAbsolutePath() + "' to '" + tgt == null ? "null" : tgt.getAbsolutePath() + "'.", ex);
        }
    }

    public static Collection<File> listFilesAndDirs(
            File directory, IOFileFilter fileFilter, IOFileFilter dirFilter) {
        if (directory == null || !directory.isDirectory()) {
            throw new IllegalArgumentException("Directory '" + (directory == null ? "null" : directory.getAbsolutePath()) + "' is not a directory");
        }
        return FileUtils.listFilesAndDirs(directory, fileFilter, dirFilter);
    }

    public static List<File> getFoldersLeave(File parentFolder) {
        Collection<File> foldersExec;
        foldersExec = FileUtil.listFilesAndDirs(parentFolder, DirectoryFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        CollectionUtils.filter(foldersExec, new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                File file = (File) object;
                for (File listFile : file.listFiles()) {
                    if (listFile.isDirectory() && !listFile.getName().equals("page")) {
                        return false;
                    }
                }
                return !file.getName().equals("page");
            }
        });
        return new LinkedList<>(foldersExec);

    }

}
