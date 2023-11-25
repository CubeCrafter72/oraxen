package io.th0rgal.oraxen.new_commands;

import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.audience.Audience;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public class ItemInfoCommand {

    public static Command.Builder<CommandSender> itemInfoCommand(Command.Builder<CommandSender> builder) {
        return builder.literal("iteminfo")
                .permission("oraxen.command.iteminfo")
                .argument(StringArgument.of("itemid"))
                .handler(context -> {
                    String argument = context.get("itemid");
                    Audience audience = OraxenPlugin.get().audience().sender(context.getSender());
                    if (argument.equals("all")) {
                        for (Map.Entry<String, ItemBuilder> entry : OraxenItems.getEntries()) {
                            sendItemInfo(audience, entry.getValue(), entry.getKey());
                        }
                    } else {
                        ItemBuilder itemBuilder = OraxenItems.getItemById(argument);
                        if (itemBuilder == null)
                            audience.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<red>No item found with ID</red> <dark_red>" + argument));
                        else sendItemInfo(audience, itemBuilder, argument);
                    }
                });
    }

    private static void sendItemInfo(Audience sender, ItemBuilder builder, String itemId) {
        sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_aqua>ItemID: <aqua>" + itemId));
        sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_green>CustomModelData: <green>" + builder.getOraxenMeta().customModelData()));
        sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_green>Material: <green>" + builder.getReferenceClone().getType()));
        sender.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_green>Model Name: <green>" + builder.getOraxenMeta().modelKey().asString()));
        Logs.newline();
    }
}
