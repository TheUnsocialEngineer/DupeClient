package com.dupeclient.client.core.session;

import com.dupeclient.client.DupeClient;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

final class ModJarScanner {
    private static final Set<String> SUSPICIOUS_FILES = Set.of(
        ".data", ".l_ignore", ".l1", ".la_gnita", "Updater.class"
    );
    private static final Set<String> SUSPICIOUS_DIRS = Set.of(
        "thezowi", "services", ".settings"
    );
    private static final Set<String> ALLOWED_METAINF = Set.of(
        "MANIFEST.MF", "sponge_plugins.json"
    );
    private static final Set<String> SUSPICIOUS_SUFFIXES = Set.of(
            ".data", ".l1", ".la_gnita", ".l_ignore"
    );
    private static final byte[] CLASS_MAGIC = {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE};

    private ModJarScanner() {
    }

    record ScanResult(boolean infected, List<String> reasons) {
        static ScanResult clean() {
            return new ScanResult(false, List.of());
        }
    }

    static ScanResult scanSelfJar() {
        ModContainer self = FabricLoader.getInstance().getModContainer(DupeClient.MOD_ID).orElse(null);
        if (self == null) {
            return ScanResult.clean();
        }
        List<Path> paths;
        try {
            paths = self.getOrigin().getPaths();
        } catch (UnsupportedOperationException ex) {
            return ScanResult.clean();
        }
        List<String> reasons = new ArrayList<>();
        for (Path path : paths) {
            File file = path.toFile();
            if (!file.isFile() || !file.getName().toLowerCase(Locale.ROOT).endsWith(".jar")) {
                continue;
            }
            ScanResult one = scanJar(file);
            if (one.infected()) {
                reasons.add(file.getName() + ": " + String.join("; ", one.reasons()));
            }
        }
        return reasons.isEmpty() ? ScanResult.clean() : new ScanResult(true, reasons);
    }

    static ScanResult scanJar(File jarFile) {
        List<String> reasons = new ArrayList<>();
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name == null || name.isBlank()) {
                    continue;
                }
                if (name.startsWith("<html><img src=")) {
                    reasons.add("html injection entry");
                    continue;
                }
                if (name.startsWith("META-INF/")) {
                    if (!isAllowedMetaInf(name)) {
                        reasons.add("unexpected META-INF entry: " + name);
                    }
                    continue;
                }
                int slash = name.indexOf('/');
                if (slash >= 0) {
                    String top = name.substring(0, slash);
                    if (SUSPICIOUS_DIRS.contains(top)) {
                        reasons.add("suspicious directory: " + top);
                    }
                }
                for (String suspicious : SUSPICIOUS_FILES) {
                    if (name.equalsIgnoreCase(suspicious) || name.endsWith("/" + suspicious)) {
                        reasons.add("suspicious file: " + name);
                    }
                }
                for (String suffix : SUSPICIOUS_SUFFIXES) {
                    if (name.endsWith(suffix) && !name.endsWith(".class")) {
                        reasons.add("suspicious suffix in: " + name);
                    }
                }
                if (name.endsWith(".class")) {
                    byte[] bytes = jar.getInputStream(entry).readAllBytes();
                    if (!isValidClassFile(bytes)) {
                        reasons.add("invalid class magic: " + name);
                    }
                }
            }
        } catch (IOException ex) {
            reasons.add("jar read failed: " + ex.getMessage());
        }
        return reasons.isEmpty() ? ScanResult.clean() : new ScanResult(true, reasons);
    }

    static String selfJarSha256() {
        Path jar = selfJarPath();
        if (jar == null) {
            return "";
        }
        try {
            return sha256File(jar.toFile());
        } catch (Exception ignored) {
            return "";
        }
    }

    static Path selfJarPath() {
        ModContainer self = FabricLoader.getInstance().getModContainer(DupeClient.MOD_ID).orElse(null);
        if (self == null) {
            return null;
        }
        try {
            for (Path path : self.getOrigin().getPaths()) {
                File file = path.toFile();
                if (file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(".jar")) {
                    return file.toPath();
                }
            }
        } catch (UnsupportedOperationException ignored) {
        }
        return null;
    }

    private static String sha256File(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(Files.readAllBytes(file.toPath()));
        StringBuilder out = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            int v = b & 0xFF;
            if (v < 16) out.append('0');
            out.append(Integer.toHexString(v));
        }
        return out.toString();
    }

    private static boolean isAllowedMetaInf(String entryName) {
        if ("META-INF/".equals(entryName)) {
            return true;
        }
        String rest = entryName.substring("META-INF/".length());
        if (!rest.contains("/")) {
            if (ALLOWED_METAINF.contains(rest)) {
                return true;
            }
            if (isLicenseOrNoticeLeaf(rest)) {
                return true;
            }
            return rest.endsWith(".SF") || rest.endsWith(".RSA") || rest.endsWith(".DSA") || rest.endsWith(".EC");
        }
        int slash = rest.indexOf('/');
        String top = slash >= 0 ? rest.substring(0, slash) : rest;
        if (isAllowedMetaInfPrefix(top)) {
            return true;
        }
        int lastSlash = rest.lastIndexOf('/');
        String leaf = lastSlash >= 0 ? rest.substring(lastSlash + 1) : rest;
        return isLicenseOrNoticeLeaf(leaf);
    }

    private static boolean isLicenseOrNoticeLeaf(String leaf) {
        return "LICENSE".equalsIgnoreCase(leaf)
                || "LICENSE.txt".equalsIgnoreCase(leaf)
                || "NOTICE".equalsIgnoreCase(leaf)
                || "NOTICE.txt".equalsIgnoreCase(leaf);
    }

    private static boolean isAllowedMetaInfPrefix(String top) {
        return switch (top.toLowerCase(Locale.ROOT)) {
            case "jars", "maven", "services", "versions", "linux", "linux64", "osx", "windows",
                 "windows64", "windows32", "darwin", "macos", "freebsd", "native" -> true;
            default -> false;
        };
    }

    private static boolean isValidClassFile(byte[] bytes) {
        if (bytes == null || bytes.length < 8) {
            return false;
        }
        for (int i = 0; i < CLASS_MAGIC.length; i++) {
            if (bytes[i] != CLASS_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }
}
