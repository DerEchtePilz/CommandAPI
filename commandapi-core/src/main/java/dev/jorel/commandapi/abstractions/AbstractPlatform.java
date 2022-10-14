package dev.jorel.commandapi.abstractions;

import java.util.List;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;

import dev.jorel.commandapi.arguments.SuggestionProviders;

public abstract class AbstractPlatform<Source> {
	// TODO: Add methods that need platform-specific implementations
	// All methods in bukkit NMS will probably also need to be here
	
	// ^ I don't think all bukkit NMS methods will have to be in here.
	// Almost all Bukkit NMS methods should be implemented in Bukkit's
	// AbstractPlatform implementation. The only things in here are going
	// to be supppppppppper low-level stuff

	public abstract void onLoad();

	public abstract void onEnable(Object plugin);

	public abstract void onDisable();


	// "Source" in this case (for CommandContext<Source>) is something like a
	// CommandListenerWrapper (Spigot mappings) or CommandSourceStack (Mojang mappings).
	// 
	// This method returns AbstractCommandSender<? extends Source>, except what we actually want to return
	// is just a plain ol' AbstractCommandSender<?> - we don't care what the abstract command sender is parameterized
	// over
	public abstract AbstractCommandSender<?> getSenderForCommand(CommandContext<Source> cmdCtx, boolean forceNative);

	// Converts a command source into its source. For Bukkit, this 
	// is implemented in NMS. TODO: For Velocity, I have no idea what
	// a command source is or consists of
	public abstract AbstractCommandSender<?> getCommandSenderFromCommandSource(Source cs);

	// Registers a permission. Bukkit's permission system requires permissions to be "registered"
	// before they can be used.
	public abstract void registerPermission(String string);

	// Some commands have existing suggestion providers. TODO: We can PROBABLY avoid this by
	// implementing the relevant NMS SuggestionProviders implementation on the platform-specific
	// argument, but I CBA to think about that now so I'll dump it here
	public abstract SuggestionProvider<Source> getSuggestionProvider(SuggestionProviders suggestionProvider);
	

	
	/**
	 * Stuff to run after a command has been generated. For Bukkit, this involves
	 * finding command ambiguities for logging and generating the command JSON
	 * dispatcher file. If we're being fancy, we could also create a "registered
	 * a command" event (which can't be cancelled)
	 * @param aliasNodes any alias nodes that were also registered as a part of this registration process
	 * @param resultantNode the node that was registered
	 */
	public abstract void postCommandRegistration(LiteralCommandNode<Source> resultantNode , List<LiteralCommandNode<Source>> aliasNodes);

	/**
	 * Registers a Brigadier command node. For Bukkit, this requires using reflection to
	 * access the CommandDispatcher (DISPATCHER), then registering it directly using
	 * DISPATCHER.register. For Velocity, this is as simple as commandManager.register(new BrigadierCommand( node ))
	 */
	public abstract LiteralCommandNode<Source>  registerCommandNode(LiteralArgumentBuilder<Source> node);

	// We probabbbbbbbbly need to register some form of help for commands? I'm not
	// sure if 
	public abstract void registerHelp();
	


	/**
	 * Unregisters a command. For Bukkit, this is as complex as unregistering it from
	 * the CommandDispatcher. For Velocity, this is as simple as commandManager.unregister(commandName)
	 */
	public abstract void unregister(String commandName);
	


	// For Bukkit, chat preview has to be unhooked
	// Wait why does this appear here? This shouldn't!
	// public abstract void onDisable();
}