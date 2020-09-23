package dev.jorel.commandapi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;

/**
 * 'Simple' conversion of Plugin commands
 */
public abstract class Converter {
	
	private static final List<Argument> PLAIN_ARGUMENTS;
	
	static {
		PLAIN_ARGUMENTS = new ArrayList<>();
		PLAIN_ARGUMENTS.add(new GreedyStringArgument("args"));
	}

	/**
	 * Convert all commands stated in Plugin's plugin.yml file into CommandAPI-compatible commands
	 * @param plugin The plugin which commands are to be converted
	 */
	public static void convert(Plugin plugin) {
		CommandAPI.getLog().info("Converting commands for " + plugin.getName() + ":");
		plugin.getDescription().getCommands().keySet().forEach(commandName -> convertPluginCommand((JavaPlugin) plugin, commandName, PLAIN_ARGUMENTS));
	}
	
	/**
	 * Convert a command stated in Plugin's plugin.yml file into CommandAPI-compatible commands
	 * @param plugin The plugin where the command is registered
	 * @param cmdName The command to convert
	 */
	public static void convert(Plugin plugin, String cmdName) {
		convertPluginCommand((JavaPlugin) plugin, cmdName, PLAIN_ARGUMENTS);
	}
	
	/**
	 * Convert a command stated in Plugin's plugin.yml file into CommandAPI-compatible commands
	 * @param plugin The plugin where the command is registered
	 * @param cmdName The command to convert
	 * @param arguments The arguments that should be used to parse this command
	 */
	public static void convert(Plugin plugin, String cmdName, List<Argument> arguments) {
		convertPluginCommand((JavaPlugin) plugin, cmdName, arguments);
	}
	
	private static void convertPluginCommand(JavaPlugin plugin, String commandName, List<Argument> arguments) {
		CommandAPI.getLog().info("Converting " + plugin.getName() + " command /" + commandName);
		
		/* Parse the commands */
		Map<String, Object> cmdData = plugin.getDescription().getCommands().get(commandName);
		
		if(cmdData == null) {
			CommandAPI.getLog().severe("Couldn't find /" + commandName + " in " + plugin.getName() + "'s plugin.yml. Are you sure you're not confusing it with an alias?");
			return;
		}

		//Convert stupid YAML aliases to a String[] for CommandAPI
		String[] aliases = null;
		Object aliasObj = cmdData.get("aliases");
		if(aliasObj == null) {
			aliases = new String[0];
		} else if(aliasObj instanceof String) {
			aliases = new String[] {(String) aliasObj};
		} else if(aliasObj instanceof List) {
			@SuppressWarnings("unchecked")
			List<String> list = (List<String>) aliasObj;
			aliases = list.toArray(new String[0]);
		}
		if(aliases.length != 0) {
			CommandAPI.getLog().info("Aliases for command /" + commandName + " found. Using aliases " + Arrays.deepToString(aliases));
		}
		 
		//Convert YAML to CommandPermission
		CommandPermission permissionNode = null;
		String permission = (String) cmdData.get("permission");
		if(permission == null) {
			permissionNode = CommandPermission.NONE;
		} else {
			CommandAPI.getLog().info("Permission for command /" + commandName + " found. Using " + permission);
			permissionNode = CommandPermission.fromString(permission);
		}
		
		//No arguments
		new CommandAPICommand(commandName)
			.withPermission(permissionNode)
			.withAliases(aliases)
			.executesNative((sender, args) -> { plugin.getCommand(commandName).execute(of(sender), commandName, new String[0]); })
			.register();
		
		//Multiple arguments		
		CommandAPICommand.convertedOf(commandName)
			.withPermission(permissionNode)
			.withAliases(aliases)
			.withArguments(arguments)
			.executesNative((sender, args) -> { 
				if(arguments.equals(PLAIN_ARGUMENTS)) {
					plugin.getCommand(commandName).execute(of(sender), commandName, ((String) args[0]).split(" "));
				} else {
					plugin.getCommand(commandName).execute(of(sender), commandName, (String[]) args);
				}
			})
			.register();
	}
	
	private static CommandSender of(ProxiedCommandSender proxy) {
		CommandSender sender = (CommandSender) Proxy.newProxyInstance(CommandSender.class.getClassLoader(),
				proxy.getCallee().getClass().getInterfaces(), new CommandSenderHandler(proxy));
		
		return sender;
	}
	
	static class CommandSenderHandler implements InvocationHandler {
		
        private final ProxiedCommandSender proxiedCommandSender;
        
        public CommandSenderHandler(ProxiedCommandSender proxiedCommandSender) {
            this.proxiedCommandSender = proxiedCommandSender;
        }

		public Object invoke(Object proxy, Method method, Object[] args)
				throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			
			switch(method.getName()) {
			case "isPermissionSet":
			case "hasPermission":
			case "addAttachment":
			case "removeAttachment":
			case "recalculatePermissions":
			case "getEffectivePermissions":
			case "isOp":
			case "setOp":
				return method.invoke(proxiedCommandSender.getCaller(), args);
			default:
				return method.invoke(proxiedCommandSender.getCallee(), args);
			}
        }
    }
	
}
