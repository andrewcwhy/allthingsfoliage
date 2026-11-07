package io.github.andrewcwhy.allthingsfoliage.gpu.compat;

import io.github.andrewcwhy.allthingsfoliage.AllThingsFoliage;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Polytone's coloured lights ("Polylights"), from Polytone 6.8 on 26.1.2 and 6.9 on 26.2. Polytone rewrites the
 * game's and Sodium's chunk shaders as it loads them, so every read of the light map also adds the colour of the
 * lights around, and hands each program that reads its light volume the data it needs as the program is set on a
 * pass, whoever built it. The copy of Sodium's chunk shaders the renderer compiles itself never goes through that
 * loading, so it is handed to the same rewriting here; the data then reaches it as it reaches Sodium's own.
 * <p>
 * Polytone also writes the face a vertex looks along above its light level (bits 8 to 10) in every buffer built in
 * the game's block format, the renderer's mesh among them; the renderer's shaders keep the light level to its own
 * eight bits, and the Sodium copy hands the face on where Sodium's mesh carries it.
 * <p>
 * Reached by name: Polytone is never compiled against for this. Missing or changed, the foliage keeps the game's
 * light.
 */
final class PolytoneColoredLight {

    private static final String PATCHER =
        "net.mehvahdjukaar.polytone.content.shaders.light.LightShaderPatcher";
    /** What Polytone's rewriting leaves in a shader, which says it tints the light while the game draws the chunks. */
    static final String TINTED_READ = "_polyColoredLightmap";

    private static final MethodHandle PATCH_SODIUM = find(
        "patchSodiumChunkShader"
    );
    private static boolean failed;

    private PolytoneColoredLight() {}

    private static MethodHandle find(String name) {
        if (!AllThingsFoliage.xplat().isModLoaded("polytone")) {
            return null;
        }
        try {
            Class<?> patcher = Class.forName(
                PATCHER,
                false,
                PolytoneColoredLight.class.getClassLoader()
            );
            return MethodHandles.publicLookup().findStatic(
                patcher,
                name,
                MethodType.methodType(String.class, String.class)
            );
        } catch (ReflectiveOperationException | LinkageError e) {
            // A Polytone without coloured lights, or one that changed them.
            return null;
        }
    }

    /**
     * The copy of Sodium's chunk vertex shader with Polytone's coloured light in it, while Polytone tints the light of
     * the chunks -- which shows in the game's terrain shader as Polytone rewrote it -- or the copy as it was.
     */
    static String tintSodiumVertex(
        String vertex,
        String terrainVertex,
        String globalsBlock
    ) {
        if (
            PATCH_SODIUM == null ||
            failed ||
            vertex == null ||
            terrainVertex == null ||
            !terrainVertex.contains(TINTED_READ)
        ) {
            return vertex;
        }
        try {
            return shareGlobals(
                vertex,
                (String) PATCH_SODIUM.invokeExact(vertex),
                globalsBlock
            );
        } catch (Throwable e) {
            failed = true;
            AllThingsFoliage.logger.warn(
                "Polytone's coloured lights could not be added to the GPU foliage's shaders; the foliage " +
                    "near the player keeps the game's light",
                e
            );
            return vertex;
        }
    }

    private static final java.util.regex.Pattern VERSION =
        java.util.regex.Pattern.compile(
            "^\\s*#version[^\\n]*\\n",
            java.util.regex.Pattern.MULTILINE
        );
    private static final java.util.regex.Pattern GLOBALS =
        java.util.regex.Pattern.compile(
            "layout\\s*\\(\\s*std140\\s*\\)\\s*uniform\\s+Globals\\s*\\{([^}]*)\\}\\s*;"
        );
    private static final java.util.regex.Pattern MEMBER =
        java.util.regex.Pattern.compile("\\b\\w+\\s+(\\w+)\\s*;");

    /**
     * Polytone puts its light code right after the version line, with the game's block of globals in it under the
     * game's names. The copy declares that block itself, further down, under names of its own -- Sodium's shaders use
     * some of the game's names for functions of their own -- and a program may not hold the block twice. So the copy's
     * declaration is moved up in place of Polytone's, and the game's names lead to the copy's from there on -- left so,
     * since Polytone's macros read them where they are used; Sodium's code reads none of them.
     * Where Polytone's code is not shaped that way the rewritten copy is handed back as it is, and if that does not
     * compile the foliage is drawn with the mod's own shaders.
     */
    private static String shareGlobals(
        String original,
        String tinted,
        String globalsBlock
    ) {
        java.util.regex.Matcher version = VERSION.matcher(original);
        if (!version.find()) {
            return tinted;
        }
        int afterVersion = version.end();
        String following = original.substring(
            afterVersion,
            Math.min(original.length(), afterVersion + 400)
        );
        int copyStarts = tinted.indexOf(following, afterVersion);
        int ownBlock =
            copyStarts < 0 ? -1 : tinted.indexOf(globalsBlock, copyStarts);
        if (ownBlock < 0) {
            return tinted;
        }
        String polytone = tinted.substring(afterVersion, copyStarts);
        java.util.regex.Matcher block = GLOBALS.matcher(polytone);
        if (!block.find()) {
            return tinted;
        }
        StringBuilder renames = new StringBuilder();
        java.util.regex.Matcher member = MEMBER.matcher(block.group(1));
        while (member.find()) {
            String name = member.group(1);
            if (!globalsBlock.contains(" mc2_" + name + ";")) {
                return tinted;
            }
            renames
                .append("#define ")
                .append(name)
                .append(" mc2_")
                .append(name)
                .append('\n');
        }
        String rest = tinted.substring(copyStarts);
        int inRest = ownBlock - copyStarts;
        return (
            tinted.substring(0, afterVersion) +
            globalsBlock +
            renames +
            polytone.substring(0, block.start()) +
            polytone.substring(block.end()) +
            "\n" +
            rest.substring(0, inRest) +
            rest.substring(inRest + globalsBlock.length())
        );
    }
}
