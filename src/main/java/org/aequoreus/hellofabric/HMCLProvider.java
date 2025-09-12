package org.aequoreus.hellofabric;

import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.util.Arguments;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class HMCLProvider implements GameProvider {

    private Path gameDir;
    private Arguments arguments;
    private List<Path> jars = new ArrayList<>();

    @Override
    public String getGameId() {
        return "hmcl";
    }

    @Override
    public String getGameName() {
        return "HMCL";
    }

    @Override
    public String getRawGameVersion() {
        return "3.6.17";
    }

    @Override
    public String getNormalizedGameVersion() {
        return "3.6.17";
    }

    @Override
    public Collection<BuiltinMod> getBuiltinMods() {
        return Collections.emptyList();
    }

    @Override
    public String getEntrypoint() {
        return "";
    }

    @Override
    public Path getLaunchDirectory() {
        return this.gameDir;
    }

    @Override
    public boolean isObfuscated() {
        return false;
    }

    @Override
    public boolean requiresUrlClassLoader() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean locateGame(FabricLauncher fabricLauncher, String[] args) {
        Arguments arguments = new Arguments();
        arguments.parse(args);
        this.arguments = arguments;

        return false;
    }

    @Override
    public void initialize(FabricLauncher fabricLauncher) {

    }

    @Override
    public GameTransformer getEntrypointTransformer() {
        return null;
    }

    @Override
    public void unlockClassPath(FabricLauncher fabricLauncher) {
        for (Path jar : this.jars) {
            fabricLauncher.addToClassPath(jar);
        }
    }

    @Override
    public void launch(ClassLoader classLoader) {
        try {
            Class main = classLoader.loadClass(this.getEntrypoint());
            Method method = main.getMethod("main", String[].class);

            method.invoke(null, (Object) getLaunchArguments(true));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Arguments getArguments() {
        return this.arguments;
    }

    @Override
    public String[] getLaunchArguments(boolean b) {
        return getArguments().toArray();
    }
}
