package org.aequoreus.hellofabric;

import net.fabricmc.loader.api.metadata.*;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.metadata.BuiltinModMetadata;
import net.fabricmc.loader.impl.util.Arguments;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class HMCLProvider implements GameProvider {
    private final GameTransformer transformer = new GameTransformer(new EntrypointPatch());

    private Arguments arguments;
    private List<Path> classPath;
    private String entrypoint;
    private String version;

    @Override
    public String getGameId() {
        return "hmcl";
    }

    @Override
    public String getGameName() {
        return "Hello! Minecraft Launcher";
    }

    @Override
    public String getRawGameVersion() {
        return version;
    }

    @Override
    public String getNormalizedGameVersion() {
        return version;
    }

    @Override
    public Collection<BuiltinMod> getBuiltinMods() {
        BuiltinModMetadata.Builder metadata = new BuiltinModMetadata.Builder(getGameId(), getNormalizedGameVersion())
                .setName(getGameName());

        return List.of(
                new BuiltinMod(classPath, metadata.build())
        );
    }

    @Override
    public String getEntrypoint() {
        return this.entrypoint;
    }

    @Override
    public Path getLaunchDirectory() {
        try {
            return Paths.get(".").toRealPath();
        } catch (IOException e) {
            throw new RuntimeException("Failed to resolve launch dir", e);
        }
    }

    @Override
    public boolean requiresUrlClassLoader() {
        return false;
    }

    @Override
    public Set<BuiltinTransform> getBuiltinTransforms(String className) {
        return Set.of(BuiltinTransform.CLASS_TWEAKS);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean locateGame(FabricLauncher fabricLauncher, String[] args) {
        this.arguments = new Arguments();
        arguments.parse(args);

        Path launchDir = getLaunchDirectory();
        Pattern filenamePattern = Pattern.compile("HMCL-\\d+\\.\\d+\\.\\d+\\.jar$|HMCL-\\d+\\.\\d+\\.\\d+\\.\\d+\\.jar$");
        Path gameJar;
        if (arguments.containsKey("gameDir")) {
            gameJar = launchDir.resolve(arguments.get("gameDir"));
        } else try (Stream<Path> paths = Files.find(launchDir, 1, (path, attrs)
                -> filenamePattern.matcher(path.getFileName().toString()).matches() && attrs.isRegularFile())) {
            gameJar = paths.findFirst().orElseThrow(() -> new RuntimeException("Failed to find HMCL jar"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        entrypoint = "com.jackhuang.hmcl.Main";
        version = "999.999.999";
        try (JarFile jarFile = new JarFile(gameJar.toFile())) {
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                Attributes attrs = manifest.getMainAttributes();
                entrypoint = attrs.getValue(Attributes.Name.MAIN_CLASS);
                version = attrs.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            }
        } catch (IOException ignored) {}

        classPath = List.of(gameJar);

        return true;
    }

    @Override
    public void initialize(FabricLauncher fabricLauncher) {
        List<Path> parentClassPath = Stream.of(System.getProperty("java.class.path").split(File.pathSeparator))
                .map(Path::of)
                .map((path) -> {
                    try {
                        return path.toRealPath();
                    } catch(IOException e) {
                        throw new RuntimeException("Failed to get real path of " + path, e);
                    }
                })
                .filter((path) -> !classPath.contains(path))
                .toList();

        fabricLauncher.setValidParentClassPath(parentClassPath);

        transformer.locateEntrypoints(fabricLauncher, classPath);
    }

    @Override
    public GameTransformer getEntrypointTransformer() {
        return transformer;
    }

    @Override
    public void unlockClassPath(FabricLauncher fabricLauncher) {
        classPath.forEach(fabricLauncher::addToClassPath);
    }

    @Override
    public void launch(ClassLoader loader) {
        MethodHandle invoker;
        try {
            Class<?> target = loader.loadClass(getEntrypoint());
            invoker = MethodHandles.lookup().findStatic(target, "main", MethodType.methodType(void.class, String[].class));
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Failed to find entry point", e);
        }

        try {
            //noinspection ConfusingArgumentToVarargsMethod
            invoker.invokeExact(arguments.toArray());
        } catch (Throwable e) {
            throw new RuntimeException("Failed to launch", e);
        }
    }

    @Override
    public Arguments getArguments() {
        return this.arguments;
    }

    @Override
    public String[] getLaunchArguments(boolean sanitize) {
        return getArguments().toArray();
    }
}
