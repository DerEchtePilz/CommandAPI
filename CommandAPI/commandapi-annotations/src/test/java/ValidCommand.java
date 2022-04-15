/*******************************************************************************
 * Copyright 2018, 2020 Jorel Ali (Skepter) - MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.annotations.annotations.Command;
import dev.jorel.commandapi.annotations.annotations.Default;
import dev.jorel.commandapi.annotations.annotations.Executors;
import dev.jorel.commandapi.annotations.annotations.Help;
import dev.jorel.commandapi.annotations.annotations.Permission;
import dev.jorel.commandapi.annotations.annotations.Subcommand;
import dev.jorel.commandapi.annotations.annotations.Suggestion;
import dev.jorel.commandapi.annotations.annotations.Suggests;
import dev.jorel.commandapi.annotations.arguments.APlayerArgument;
import dev.jorel.commandapi.annotations.arguments.AStringArgument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.SafeSuggestions;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.ExecutorType;

@Command("warp")	
@Help(value = "Manages all warps on the server", shortDescription = "Manages warps")
public class ValidCommand {
	
	// List of warp names and their locations
	final Map<String, Location> warps;
	
	public ValidCommand() {
		warps = new HashMap<>();
	}
	
	// /warp
	@Subcommand
	public void warp(CommandSender sender) {
		sender.sendMessage("--- Warp help ---");
		sender.sendMessage("/warp - Show this help");
		sender.sendMessage("/warp <warp> - Teleport to <warp>");
		sender.sendMessage("/warp create <warpname> - Creates a warp at your current location");
		sender.sendMessage("/warp tp <player> <warpname> - Teleports a player to a warp");
	}
	
	// /warp <warpname> 
	@Subcommand
	public void warp(Player player, @AStringArgument String warpName) {
		player.teleport(warps.get(warpName));
	}
	
	@Suggestion("blah")
	public ArgumentSuggestions aaaaa() {
		return ArgumentSuggestions.strings("hi", "bye");
	}
	
	public SafeSuggestions<Location> aaaaaaaaaaa() {
		return SafeSuggestions.suggest(new Location(null, 1, 2, 3));
	}
	
	@Subcommand("create")
	@Permission("warps.create")
	@Executors({ExecutorType.ENTITY, ExecutorType.PLAYER})
	public void createWarp(CommandSender sender, @Suggests("blah") @AStringArgument String warpName) throws WrapperCommandSyntaxException {
		warps.put(warpName, ((LivingEntity) sender).getLocation());
		throw CommandAPI.fail("");
	}
	
	@Subcommand("create")
	@Permission("warps.create")
	public void tpWarp(CommandSender sender, @APlayerArgument OfflinePlayer target, @AStringArgument String warpName) {
		if(target.isOnline() && target instanceof Player onlineTarget) {
			onlineTarget.teleport(warps.get(warpName));
		}
	}
	
	
}