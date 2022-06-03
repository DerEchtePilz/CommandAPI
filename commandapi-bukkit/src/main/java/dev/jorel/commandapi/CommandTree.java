package dev.jorel.commandapi;

import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.arguments.Argument;

/**
 * This is the root node for creating a command as a tree
 */
public class CommandTree extends CommandTreeBase<CommandSender, Argument<?>> {

	public CommandTree(final String commandName) {
		super(commandName);
	}

}