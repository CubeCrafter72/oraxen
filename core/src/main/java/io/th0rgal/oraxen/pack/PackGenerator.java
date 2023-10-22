package io.th0rgal.oraxen.pack;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenMeta;
import team.unnamed.creative.BuiltResourcePack;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackReader;
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackWriter;

public class PackGenerator {

    ResourcePack resourcePack;
    BuiltResourcePack builtPack;

    public PackGenerator() {
        PackDownloader.downloadDefaultPack();
        resourcePack = MinecraftResourcePackReader.minecraft().readFromDirectory(OraxenPlugin.get().packPath().toFile());
        OraxenPlugin.get().setResourcePack(resourcePack);
    }

    public void generatePack() {
        resourcePack.unknownFiles().remove("token.secret");
        resourcePack.unknownFiles().remove("DefaultPack.zip");
        resourcePack.unknownFiles().remove("pack.zip");
        MinecraftResourcePackWriter.minecraft().writeToZipFile(OraxenPlugin.get().packPath().resolve("pack.zip").toFile(), resourcePack);

        builtPack = MinecraftResourcePackWriter.minecraft().build(resourcePack);
    }

    public BuiltResourcePack getBuiltPack() {
        return builtPack;
    }

    private void addItemPackFiles() {
        for (ItemBuilder builder : OraxenItems.getItems()) {
            if (builder == null || !builder.hasOraxenMeta()) continue;
            OraxenMeta meta = builder.getOraxenMeta();
            if (meta.getCustomModelData() == 0) return;
        }
    }
}
