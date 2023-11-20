package io.th0rgal.oraxen;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.impl.ServerImplementation;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent;
import io.th0rgal.oraxen.commands.CommandsManager;
import io.th0rgal.oraxen.compatibilities.CompatibilitiesManager;
import io.th0rgal.oraxen.config.ConfigsManager;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.config.SettingsUpdater;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.font.packets.InventoryPacketListener;
import io.th0rgal.oraxen.font.packets.TitlePacketListener;
import io.th0rgal.oraxen.hud.HudManager;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.new_pack.PackGenerator;
import io.th0rgal.oraxen.new_pack.PackServer;
import io.th0rgal.oraxen.recipes.RecipesManager;
import io.th0rgal.oraxen.sound.SoundManager;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.NoticeUtils;
import io.th0rgal.oraxen.utils.OS;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.actions.ClickActionManager;
import io.th0rgal.oraxen.utils.armorequipevent.ArmorEquipEvent;
import io.th0rgal.oraxen.utils.customarmor.CustomArmorListener;
import io.th0rgal.oraxen.utils.inventories.InvManager;
import io.th0rgal.oraxen.utils.logs.Logs;
import io.th0rgal.protectionlib.ProtectionLib;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import team.unnamed.creative.ResourcePack;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.jar.JarFile;

public class OraxenPlugin extends JavaPlugin {

    private static OraxenPlugin oraxen;
    private ConfigsManager configsManager;
    private BukkitAudiences audience;
    private FontManager fontManager;
    private HudManager hudManager;
    private SoundManager soundManager;
    private InvManager invManager;
    private ResourcePack resourcePack;
    private PackGenerator packGenerator;
    private PackServer packServer;
    private ClickActionManager clickActionManager;
    private ProtocolManager protocolManager;
    public static FoliaLib foliaLib;
    public static boolean supportsDisplayEntities;

    public OraxenPlugin() {
        oraxen = this;
    }

    public static OraxenPlugin get() {
        return oraxen;
    }

    @Nullable
    public static JarFile getJarFile() {
        try {
            return new JarFile(oraxen.getFile());
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this).silentLogs(true));
    }

    @Override
    public void onEnable() {
        VersionUtil.isPremium();
        foliaLib = new FoliaLib(this);
        CommandAPI.onEnable();
        ProtectionLib.init(this);
        audience = BukkitAudiences.create(this);
        reloadConfigs();
        clickActionManager = new ClickActionManager(this);
        supportsDisplayEntities = VersionUtil.isSupportedVersionOrNewer("1.19.4");
        hudManager = new HudManager(configsManager);
        fontManager = new FontManager(configsManager);
        soundManager = new SoundManager(configsManager.getSound());

        if (Settings.KEEP_UP_TO_DATE.toBool())
            new SettingsUpdater().handleSettingsUpdate();
        final PluginManager pluginManager = Bukkit.getPluginManager();
        if (pluginManager.isPluginEnabled("ProtocolLib")) {
            protocolManager = ProtocolLibrary.getProtocolManager();
            // Swapped for individual BlockDamageListener's
            /*if (!VersionUtil.isFoliaServer()) new BreakerSystem().registerListener();
            else Logs.logError("BreakerSystem is not supported on Folia servers yet");*/
            if (Settings.FORMAT_INVENTORY_TITLES.toBool())
                protocolManager.addPacketListener(new InventoryPacketListener());
            protocolManager.addPacketListener(new TitlePacketListener());
        } else Logs.logWarning("ProtocolLib is not on your server, some features will not work");
        pluginManager.registerEvents(new CustomArmorListener(), this);
        NMSHandlers.setup();
        packGenerator = new PackGenerator();

        MechanicsManager.registerNativeMechanics();
        OraxenItems.loadItems();
        fontManager.registerEvents();
        fontManager.verifyRequired(); // Verify the required glyph is there
        hudManager.registerEvents();
        hudManager.registerTask();
        hudManager.parsedHudDisplays = hudManager.generateHudDisplays();
        pluginManager.registerEvents(new ItemUpdater(), this);
        RecipesManager.load(this);
        invManager = new InvManager();
        ArmorEquipEvent.registerListener(this);
        new CommandsManager().loadCommands();

        packGenerator.generatePack();
        packServer = new PackServer();
        postLoading();

        CompatibilitiesManager.enableNativeCompatibilities();
        if (VersionUtil.isCompiled()) NoticeUtils.compileNotice();
        if (VersionUtil.isLeaked()) NoticeUtils.leakNotice();
        if (VersionUtil.isFoliaServer()) NoticeUtils.foliaNotice();
    }

    private void postLoading() {
        new Metrics(this, 5371);
        foliaLib.getImpl().runNextTick((w) -> Bukkit.getPluginManager().callEvent(new OraxenItemsLoadedEvent()));
    }

    @Override
    public void onDisable() {
        if (packServer != null) packServer.stop();
        unregisterListeners();
        foliaLib.getImpl().cancelAllTasks();
        for (Player player : Bukkit.getOnlinePlayers())
            if (NMSHandlers.getHandler() != null && Settings.NMS_GLYPHS.toBool())
                NMSHandlers.getHandler().uninject(player);

        CompatibilitiesManager.disableCompatibilities();
        Message.PLUGIN_UNLOADED.log();
    }

    private void unregisterListeners() {
        fontManager.unregisterEvents();
        hudManager.unregisterEvents();
        MechanicsManager.unloadListeners();
        HandlerList.unregisterAll(this);
    }

    public Path packPath() {
        return getDataFolder().toPath().resolve("pack");
    }

    public ProtocolManager protocolManager() {
        return protocolManager;
    }

    public BukkitAudiences audience() {
        return audience;
    }

    public void reloadConfigs() {
        configsManager = new ConfigsManager(this);
        configsManager.validatesConfig();
    }

    public ConfigsManager configsManager() {
        return configsManager;
    }

    public FontManager fontManager() {
        return fontManager;
    }

    public void fontManager(final FontManager fontManager) {
        this.fontManager.unregisterEvents();
        this.fontManager = fontManager;
        fontManager.registerEvents();
    }

    public HudManager hudManager() {
        return hudManager;
    }

    public void hudManager(final HudManager hudManager) {
        this.hudManager.unregisterEvents();
        this.hudManager = hudManager;
        hudManager.registerEvents();
    }

    public SoundManager soundManager() {
        return soundManager;
    }

    public void soundManager(final SoundManager soundManager) {
        this.soundManager = soundManager;
    }

    public InvManager invManager() {
        return invManager;
    }

    public ResourcePack resourcePack() {
        if (resourcePack == null) resourcePack = ResourcePack.resourcePack();
        return resourcePack;
    }

    public void resourcePack(ResourcePack resourcePack) {
        this.resourcePack = resourcePack;
    }

    public PackGenerator packGenerator() {
        return packGenerator;
    }

    public PackServer packServer() {
        return packServer;
    }

    public ClickActionManager clickActionManager() {
        return clickActionManager;
    }
}
