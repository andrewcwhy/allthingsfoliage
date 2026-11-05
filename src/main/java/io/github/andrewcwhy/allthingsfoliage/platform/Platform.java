package io.github.andrewcwhy.allthingsfoliage.platform;

public interface Platform {
    boolean isModLoaded(String modId);

    /** Where this loader keeps mod config files. */
    java.nio.file.Path configDir();

    ModLoader loader();

    /**
     * Every copy of a file that Minecraft and the mods carry at this path, such as a block tag each of them adds to: the
     * bytes of each. Read through the class loader, which sees every jar where the loader lays them out on it.
     */
    default java.util.List<byte[]> readAll(String path) {
        java.util.List<byte[]> found = new java.util.ArrayList<>();
        try {
            java.util.Enumeration<java.net.URL> urls = Platform.class
                .getClassLoader()
                .getResources(path);
            while (urls.hasMoreElements()) {
                try (java.io.InputStream in = urls.nextElement().openStream()) {
                    found.add(in.readAllBytes());
                }
            }
        } catch (java.io.IOException e) {
            // Whatever was read before stays.
        }
        return found;
    }

    enum ModLoader {
        FABRIC,
        NEOFORGE,
    }
}
