package org.aequoreus.hellofabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.impl.FabricLoaderImpl;

import java.nio.file.Path;
import java.nio.file.Paths;

@SuppressWarnings("unused")
public class Hooks {
    public static final String INTERNAL_NAME = Hooks.class.getName().replace('.', '/');

    public static void init() {
        Path runDir = Paths.get(".");

        FabricLoaderImpl loader = FabricLoaderImpl.INSTANCE;
        loader.prepareModInit(runDir, FabricLoaderImpl.INSTANCE.getGameInstance());
        loader.invokeEntrypoints("init", ModInitializer.class, ModInitializer::onInitialize);
    }
}
