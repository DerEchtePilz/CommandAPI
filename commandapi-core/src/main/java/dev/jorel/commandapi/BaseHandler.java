package dev.jorel.commandapi;

import java.awt.Component;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;

import dev.jorel.commandapi.abstractions.AbstractCommandSender;
import dev.jorel.commandapi.abstractions.AbstractPlatform;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.ICustomProvidedArgument;
import dev.jorel.commandapi.arguments.IPreviewable;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.PreviewInfo;
import dev.jorel.commandapi.wrappers.PreviewableFunction;

// The "brains" behind the CommandAPI.
// Handles command registration

// For all intents and purposes (all platforms), we can use all of
// Brigadier's API (definitely the case for Spigot/Paper and Velocity).
//
// TODO: We can use the Adventure API on Paper and Velocity (NOT SPIGOT)
//  and I'm not sure if we can use the Adventure API on Fabric, so let's
//  assume we can't until we figure that out.

public class BaseHandler<Source> {
	// TODO: Figure out what here gets moved to the common implementation and what
	//  is platform-specific

	private final static VarHandle COMMANDCONTEXT_ARGUMENTS;

	// Compute all var handles all in one go so we don't do this during main server
	// runtime
	static {
		VarHandle commandContextArguments = null;
		try {
			commandContextArguments = MethodHandles.privateLookupIn(CommandContext.class, MethodHandles.lookup())
					.findVarHandle(CommandContext.class, "arguments", Map.class);
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
		}
		COMMANDCONTEXT_ARGUMENTS = commandContextArguments;
	}

	/**
	 * Returns the raw input for an argument for a given command context and its
	 * key. This effectively returns the string value that is currently typed for
	 * this argument
	 * 
	 * @param <CommandSource> the command source type
	 * @param cmdCtx               the command context which is used to run this
	 *                             command
	 * @param key                  the node name for the argument
	 * @return the raw input string for this argument
	 */
	public static <CommandSource> String getRawArgumentInput(CommandContext<CommandSource> cmdCtx,
			String key) {
		StringRange range = ((Map<String, ParsedArgument<CommandSource, ?>>) COMMANDCONTEXT_ARGUMENTS.get(cmdCtx))
				.get(key).getRange();
		return cmdCtx.getInput().substring(range.getStart(), range.getEnd());
	}

	// TODO: Need to ensure this can be safely "disposed of" when we're done (e.g. on reloads).
	// I hiiiiiiighly doubt we're storing class caches of classes that can be unloaded at runtime,
	// but this IS a generic class caching system and we don't want derpy memory leaks
	private static final Map<ClassCache, Field> FIELDS = new HashMap<>();

	final TreeMap<String, CommandPermission> PERMISSIONS_TO_FIX = new TreeMap<>();
	protected final AbstractPlatform<Source> platform; // Access strictly via getPlatform() method which can be overridden
	final List<RegisteredCommand> registeredCommands; // Keep track of what has been registered for type checking
	final Map<List<String>, IPreviewable<? extends Argument<?>, ?>> previewableArguments; // Arguments with previewable chat

	private static BaseHandler<?> instance;

	// TODO: Wait, how do we instantiate this?
	protected BaseHandler(AbstractPlatform<Source> platform) {
		this.platform = platform;
		this.registeredCommands = new ArrayList<>();
		this.previewableArguments = new HashMap<>();

		instance = this;
	}

	public void onLoad() {
		checkDependencies();
		platform.onLoad();
	}

	private void checkDependencies() {
		// Check for common dependencies
		try {
			Class.forName("com.mojang.brigadier.CommandDispatcher");
		} catch (ClassNotFoundException e) {
			new ClassNotFoundException("Could not hook into Brigadier (Are you running Minecraft 1.13 or above?)")
				.printStackTrace();
		}

		Class<?> nbtContainerClass = CommandAPI.getConfiguration().getNBTContainerClass();
		if (nbtContainerClass != null && CommandAPI.getConfiguration().getNBTContainerConstructor() != null) {
			CommandAPI.logNormal("Hooked into an NBT API with class " + nbtContainerClass.getName());
		} else {
			if (CommandAPI.getConfiguration().hasVerboseOutput()) {
				CommandAPI.logWarning(
					"Could not hook into the NBT API for NBT support. Download it from https://www.spigotmc.org/resources/nbt-api.7939/");
			}
		}
	}

	public void onEnable(Object plugin) {
		platform.onEnable(plugin);
	}

	public void onDisable() {
		platform.onDisable();
	}

	public static BaseHandler<?> getInstance() {
		return instance;
	}

	public AbstractPlatform<Source> getPlatform() {
		return this.platform;
	}

	/**
	 * Generates a command to be registered by the CommandAPI.
	 * 
	 * @param args       set of ordered argument pairs which contain the prompt text
	 *                   and their argument types
	 * @param executor   code to be ran when the command is executed
	 * @param converted  True if this command is being converted from another plugin, and false otherwise
	 * @return a brigadier command which is registered internally
	 * @throws CommandSyntaxException if an error occurs when the command is ran
	 */
	Command<Source> generateCommand(Argument<?>[] args,
			CustomCommandExecutor<? extends AbstractCommandSender<Source>> executor, boolean converted) throws CommandSyntaxException {

		// Generate our command from executor
		return (cmdCtx) -> {
			AbstractCommandSender<?> sender = platform.getSenderForCommand(cmdCtx, executor.isForceNative());
			if (converted) {
				Object[] argObjs = argsToObjectArr(cmdCtx, args);
				int resultValue = 0;

				// Return a String[] of arguments for converted commands
				String[] argsAndCmd = cmdCtx.getRange().get(cmdCtx.getInput()).split(" ");
				String[] result = new String[argsAndCmd.length - 1];
				System.arraycopy(argsAndCmd, 1, result, 0, argsAndCmd.length - 1);

				// As stupid as it sounds, it's more performant and safer to use
				// a List<?>[] instead of a List<List<?>>, due to NPEs and AIOOBEs.
				@SuppressWarnings("unchecked")
				List<String>[] entityNamesForArgs = new List[args.length];
				for (int i = 0; i < args.length; i++) {
					entityNamesForArgs[i] = args[i].getEntityNames(argObjs[i]);
				}
				List<List<String>> product = CartesianProduct.getDescartes(Arrays.asList(entityNamesForArgs));

				// These objects in obj are List<String>
				for (List<String> strings : product) {
					// We assume result.length == strings.size
					if (result.length == strings.size()) {
						for (int i = 0; i < result.length; i++) {
							if (strings.get(i) != null) {
								result[i] = strings.get(i);
							}
						}
					}
					resultValue += executor.execute(sender, result);
				}

				return resultValue;
			} else {
				return executor.execute(sender, argsToObjectArr(cmdCtx, args));
			}
		};
	}

	/**
	 * Converts the List&lt;Argument> into an Object[] for command execution
	 * 
	 * @param cmdCtx the command context that will execute this command
	 * @param args   the map of strings to arguments
	 * @return an Object[] which can be used in (sender, args) ->
	 * @throws CommandSyntaxException when an argument isn't formatted correctly
	 */
	Object[] argsToObjectArr(CommandContext<Source> cmdCtx, Argument<?>[] args)
			throws CommandSyntaxException {
		// Array for arguments for executor
		List<Object> argList = new ArrayList<>();

		// Populate array
		for (Argument<?> argument : args) {
			if (argument.isListed()) {
				argList.add(parseArgument(cmdCtx, argument.getNodeName(), argument, argList.toArray()));
			}
		}

		return argList.toArray();
	}

	/**
	 * Parses an argument and converts it into its object (as defined in platform.java)
	 *
	 * @param cmdCtx the command context
	 * @param key    the key (declared in arguments)
	 * @param value  the value (the argument declared in arguments)
	 * @return the standard Bukkit type
	 * @throws CommandSyntaxException when an argument isn't formatted correctly
	 */
	Object parseArgument(CommandContext<Source> cmdCtx, String key, Argument<?> value,
			Object[] previousArgs) throws CommandSyntaxException {
		if (value.isListed()) {
			return value.parseArgument(platform, cmdCtx, key, previousArgs);
		} else {
			return null;
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////
	// SECTION: Permissions //
	//////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * This permission generation setup ONLY works iff:
	 * <ul>
	 * <li>You register the parent permission node FIRST.</li>
	 * <li>Example:<br>
	 * /mycmd - permission node: <code>my.perm</code> <br>
	 * /mycmd &lt;arg> - permission node: <code>my.perm.other</code></li>
	 * </ul>
	 *
	 * The <code>my.perm.other</code> permission node is revoked for the COMMAND
	 * REGISTRATION, however:
	 * <ul>
	 * <li>The permission node IS REGISTERED.</li>
	 * <li>The permission node, if used for an argument (as in this case), will be
	 * used for suggestions for said argument</li>
	 * </ul>
	 * 
	 * @param requirements An arbitrary additional check to perform on the CommandSender
	 *                        after the permissions check
	 */
	Predicate<Source> generatePermissions(String commandName, CommandPermission permission,
			Predicate<AbstractCommandSender<?>> requirements) {
		// If we've already registered a permission, set it to the "parent" permission.
		if (PERMISSIONS_TO_FIX.containsKey(commandName.toLowerCase())) {
			if (!PERMISSIONS_TO_FIX.get(commandName.toLowerCase()).equals(permission)) {
				permission = PERMISSIONS_TO_FIX.get(commandName.toLowerCase());
			}
		} else {
			// Add permission to a list to fix conflicts with minecraft:permissions
			PERMISSIONS_TO_FIX.put(commandName.toLowerCase(), permission);
		}

		final CommandPermission finalPermission = permission;

		// Register it to the Bukkit permissions registry
		if (finalPermission.getPermission().isPresent()) {
			platform.registerPermission(finalPermission.getPermission().get());
		}

		return (Source css) -> permissionCheck(platform.getCommandSenderFromCommandSource(css), finalPermission,
				requirements);
	}

	/**
	 * Checks if a sender has a given permission.
	 * 
	 * @param sender     the sender to check permissions of
	 * @param permission the CommandAPI CommandPermission permission to check
	 * @return true if the sender satisfies the provided permission
	 */
	static boolean permissionCheck(AbstractCommandSender<?> sender, CommandPermission permission, Predicate<AbstractCommandSender<?>> requirements) {
		boolean satisfiesPermissions;
		if (sender == null) {
			satisfiesPermissions = true;
		} else {
			if (permission.equals(CommandPermission.NONE)) {
				satisfiesPermissions = true;
			} else if (permission.equals(CommandPermission.OP)) {
				satisfiesPermissions = sender.isOp();
			} else {
				satisfiesPermissions = permission.getPermission().isEmpty() || sender.hasPermission(permission.getPermission().get());
			}
		}
		if (permission.isNegated()) {
			satisfiesPermissions = !satisfiesPermissions;
		}
		return satisfiesPermissions && requirements.test(sender);
	}
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	// SECTION: Registration //
	//////////////////////////////////////////////////////////////////////////////////////////////////////

	/*
	 * Expands multiliteral arguments and registers all expansions of
	 * MultiLiteralArguments throughout the provided command. Returns true if
	 * multiliteral arguments were present (and expanded) and returns false if
	 * multiliteral arguments were not present.
	 */
	private boolean expandMultiLiterals(CommandMetaData meta, final Argument<?>[] args,
			CustomCommandExecutor<? extends AbstractCommandSender<Source>> executor, boolean converted)
			throws CommandSyntaxException, IOException {

		// "Expands" our MultiLiterals into Literals
		for (int index = 0; index < args.length; index++) {
			// Find the first multiLiteral in the for loop
			if (args[index] instanceof MultiLiteralArgument superArg) {

				// Add all of its entries
				for (int i = 0; i < superArg.getLiterals().length; i++) {
					LiteralArgument litArg = (LiteralArgument) new LiteralArgument(superArg.getLiterals()[i])
							.setListed(superArg.isListed()).withPermission(superArg.getArgumentPermission())
							.withRequirement(superArg.getRequirements());

					// Reconstruct the list of arguments and place in the new literals
					Argument<?>[] newArgs = Arrays.copyOf(args, args.length);
					newArgs[index] = litArg;
					register(meta, newArgs, executor, converted);
				}
				return true;
			}
		}
		return false;
	}

	// Prevent nodes of the same name but with different types:
	// allow /race invite<LiteralArgument> player<PlayerArgument>
	// disallow /race invite<LiteralArgument> player<EntitySelectorArgument>
	// Return true if conflict was present, otherwise return false
	private boolean hasCommandConflict(String commandName, Argument<?>[] args, String argumentsAsString) {
		List<String[]> regArgs = new ArrayList<>();
		for (RegisteredCommand rCommand : registeredCommands) {
			if (rCommand.commandName().equals(commandName)) {
				for (String str : rCommand.argsAsStr()) {
					regArgs.add(str.split(":"));
				}
				// We just find the first entry that causes a conflict. If this
				// were some industry-level code, we would probably generate a
				// list of all commands first, then check for command conflicts
				// all in one go so we can display EVERY command conflict for
				// all commands, but this works perfectly and isn't important.
				break;
			}
		}
		for (int i = 0; i < args.length; i++) {
			// Avoid IAOOBEs
			if (regArgs.size() == i && regArgs.size() < args.length) {
				break;
			}
			// We want to ensure all node names are the same
			if (!regArgs.get(i)[0].equals(args[i].getNodeName())) {
				break;
			}
			// This only applies to the last argument
			if (i == args.length - 1) {
				if (!regArgs.get(i)[1].equals(args[i].getClass().getSimpleName())) {
					// Command it conflicts with
					StringBuilder builder2 = new StringBuilder();
					for (String[] arg : regArgs) {
						builder2.append(arg[0]).append("<").append(arg[1]).append("> ");
					}

					CommandAPI.logError("""
							Failed to register command:

							  %s %s

							Because it conflicts with this previously registered command:

							  %s %s
							""".formatted(commandName, argumentsAsString, commandName, builder2.toString()));
					return true;
				}
			}
		}
		return false;
	}

	// Links arg -> Executor
	private ArgumentBuilder<Source, ?> generateInnerArgument(Command<Source> command,
			Argument<?>[] args) {
		Argument<?> innerArg = args[args.length - 1];

		// Handle Literal arguments
		if (innerArg instanceof LiteralArgument literalArgument) {
			return getLiteralArgumentBuilderArgument(literalArgument.getLiteral(), innerArg.getArgumentPermission(),
					innerArg.getRequirements()).executes(command);
		}

		// Handle arguments with built-in suggestion providers
		else if (innerArg instanceof ICustomProvidedArgument customProvidedArg
				&& innerArg.getOverriddenSuggestions().isEmpty()) {
			return getRequiredArgumentBuilderWithProvider(innerArg, args,
					platform.getSuggestionProvider(customProvidedArg.getSuggestionProvider())).executes(command);
		}

		// Handle every other type of argument
		else {
			return getRequiredArgumentBuilderDynamic(args, innerArg).executes(command);
		}
	}

	// Links arg1 -> arg2 -> ... argN -> innermostArgument
	private ArgumentBuilder<Source, ?> generateOuterArguments(
			ArgumentBuilder<Source, ?> innermostArgument, Argument<?>[] args) {
		ArgumentBuilder<Source, ?> outer = innermostArgument;
		for (int i = args.length - 2; i >= 0; i--) {
			Argument<?> outerArg = args[i];

			// Handle Literal arguments
			if (outerArg instanceof LiteralArgument literalArgument) {
				outer = getLiteralArgumentBuilderArgument(literalArgument.getLiteral(),
						outerArg.getArgumentPermission(), outerArg.getRequirements()).then(outer);
			}

			// Handle arguments with built-in suggestion providers
			else if (outerArg instanceof ICustomProvidedArgument customProvidedArg
					&& outerArg.getOverriddenSuggestions().isEmpty()) {
				outer = getRequiredArgumentBuilderWithProvider(outerArg, args,
						platform.getSuggestionProvider(customProvidedArg.getSuggestionProvider())).then(outer);
			}

			// Handle every other type of argument
			else {
				outer = getRequiredArgumentBuilderDynamic(args, outerArg).then(outer);
			}
		}
		return outer;
	}

	/**
	 * Handles previewable arguments. This stores the path to previewable arguments
	 * in {@link BaseHandler#previewableArguments} for runtime resolving
	 * 
	 * @param commandName the name of the command
	 * @param args        the declared arguments
	 * @param aliases     the command's aliases
	 */
	private void handlePreviewableArguments(String commandName, Argument<?>[] args, String[] aliases) {
		if (args.length > 0 && args[args.length - 1] instanceof IPreviewable<?, ?> previewable) {
			List<String> path = new ArrayList<>();

			path.add(commandName);
			for (Argument<?> arg : args) {
				path.add(arg.getNodeName());
			}
			previewableArguments.put(List.copyOf(path), previewable);

			// And aliases
			for (String alias : aliases) {
				path.set(0, alias);
				previewableArguments.put(List.copyOf(path), previewable);
			}
		}
	}

	// Builds our platform command using the given arguments for this method, then
	// registers it
	void register(CommandMetaData meta, final Argument<?>[] args,
			CustomCommandExecutor<? extends AbstractCommandSender<Source>> executor, boolean converted)
			throws CommandSyntaxException, IOException {

		// "Expands" our MultiLiterals into Literals
		if (expandMultiLiterals(meta, args, executor, converted)) {
			return;
		}

		// Create the human-readable command syntax of arguments
		final String humanReadableCommandArgSyntax;
		{
			StringBuilder builder = new StringBuilder();
			for (Argument<?> arg : args) {
				builder.append(arg.toString()).append(" ");
			}
			humanReadableCommandArgSyntax = builder.toString().trim();
		}

		// #312 Safeguard against duplicate node names. This only applies to
		// required arguments (i.e. not literal arguments)
		{
			Set<String> argumentNames = new HashSet<>();
			for (Argument<?> arg : args) {
				// We shouldn't find MultiLiteralArguments at this point, only
				// LiteralArguments
				if (!(arg instanceof LiteralArgument)) {
					if (argumentNames.contains(arg.getNodeName())) {
						CommandAPI.logError("""
								Failed to register command:

								  %s %s

								Because the following argument shares the same node name as another argument:

								  %s
								""".formatted(meta.commandName, humanReadableCommandArgSyntax, arg.toString()));
						return;
					} else {
						argumentNames.add(arg.getNodeName());
					}
				}
			}
		}

		// Expand metaData into named variables
		String commandName = meta.commandName;
		CommandPermission permission = meta.permission;
		String[] aliases = meta.aliases;
		Predicate<AbstractCommandSender<?>> requirements = meta.requirements;
		Optional<String> shortDescription = meta.shortDescription;
		Optional<String> fullDescription = meta.fullDescription;

		// Handle command conflicts
		boolean hasRegisteredCommand = false;
		for (RegisteredCommand rCommand : registeredCommands) {
			hasRegisteredCommand |= rCommand.commandName().equals(commandName);
		}
		if (hasRegisteredCommand && hasCommandConflict(commandName, args, humanReadableCommandArgSyntax)) {
			return;
		} else {
			List<String> argumentsString = new ArrayList<>();
			for (Argument<?> arg : args) {
				argumentsString.add(arg.getNodeName() + ":" + arg.getClass().getSimpleName());
			}
			registeredCommands.add(new RegisteredCommand(commandName, argumentsString, shortDescription,
					fullDescription, aliases, permission));
		}

		// Handle previewable arguments
		handlePreviewableArguments(commandName, args, aliases);

		// Warn if the command we're registering already exists in this plugin's
		// plugin.yml file
		// TODO: We might need a "pre-register" method call for something like this?
//		{
//			final PluginCommand pluginCommand = Bukkit.getPluginCommand(commandName);
//			if (pluginCommand != null) {
////				CommandAPI.logWarning(
////						"Plugin command /%s is registered by Bukkit (%s). Did you forget to remove this from your plugin.yml file?"
////								.formatted(commandName, pluginCommand.getPlugin().getName()));
//			}
//		}

		CommandAPI.logInfo("Registering command /" + commandName + " " + humanReadableCommandArgSyntax);

		// Generate the actual command
		Command<Source> command = generateCommand(args, executor, converted);

		/*
		 * The innermost argument needs to be connected to the executor. Then that
		 * argument needs to be connected to the previous argument etc. Then the first
		 * argument needs to be connected to the command name, so we get: CommandName ->
		 * Args1 -> Args2 -> ... -> ArgsN -> Executor
		 */
		LiteralCommandNode<Source> resultantNode;
		List<LiteralCommandNode<Source>> aliasNodes = new ArrayList<>();
		if (args.length == 0) {
			// Link command name to the executor
			resultantNode = platform.registerCommandNode(getLiteralArgumentBuilder(commandName)
					.requires(generatePermissions(commandName, permission, requirements)).executes(command));

			// Register aliases
			for (String alias : aliases) {
				CommandAPI.logInfo("Registering alias /" + alias + " -> " + resultantNode.getName());
				aliasNodes.add(platform.registerCommandNode(getLiteralArgumentBuilder(alias)
						.requires(generatePermissions(alias, permission, requirements)).executes(command)));
			}
		} else {

			// Generate all of the arguments, following each other and finally linking to
			// the executor
			ArgumentBuilder<Source, ?> commandArguments = generateOuterArguments(
					generateInnerArgument(command, args), args);

			// Link command name to first argument and register
			resultantNode = platform.registerCommandNode(getLiteralArgumentBuilder(commandName)
					.requires(generatePermissions(commandName, permission, requirements)).then(commandArguments));

			// Register aliases
			for (String alias : aliases) {
				if (CommandAPI.getConfiguration().hasVerboseOutput()) {
					CommandAPI.logInfo("Registering alias /" + alias + " -> " + resultantNode.getName());
				}

				aliasNodes.add(platform.registerCommandNode(getLiteralArgumentBuilder(alias)
						.requires(generatePermissions(alias, permission, requirements)).then(commandArguments)));
			}
		}
		
		platform.postCommandRegistration(resultantNode, aliasNodes);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////
	// SECTION: Argument Builders //
	//////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a literal for a given name.
	 * 
	 * @param commandName the name of the literal to create
	 * @return a brigadier LiteralArgumentBuilder representing a literal
	 */
	LiteralArgumentBuilder<Source> getLiteralArgumentBuilder(String commandName) {
		return LiteralArgumentBuilder.literal(commandName);
	}

	/**
	 * Creates a literal for a given name that requires a specified permission.
	 * 
	 * @param commandName the name fo the literal to create
	 * @param permission  the permission required to use this literal
	 * @return a brigadier LiteralArgumentBuilder representing a literal
	 */
	LiteralArgumentBuilder<Source> getLiteralArgumentBuilderArgument(String commandName,
			CommandPermission permission, Predicate<AbstractCommandSender<?>> requirements) {
		LiteralArgumentBuilder<Source> builder = LiteralArgumentBuilder.literal(commandName);
		return builder.requires((Source css) -> permissionCheck(platform.getCommandSenderFromCommandSource(css),
				permission, requirements));
	}

	// Gets a RequiredArgumentBuilder for a DynamicSuggestedStringArgument
	RequiredArgumentBuilder<Source, ?> getRequiredArgumentBuilderDynamic(final Argument<?>[] args,
			Argument<?> argument) {

		final SuggestionProvider<Source> suggestions;

		if (argument.getOverriddenSuggestions().isPresent()) {
			suggestions = toSuggestions(argument, args, true);
		} else if (argument.getIncludedSuggestions().isPresent()) {
			// TODO(#317): Merge the suggestions included here instead?
			suggestions = (cmdCtx, builder) -> argument.getRawType().listSuggestions(cmdCtx, builder);
		} else {
			suggestions = null;
		}

		return getRequiredArgumentBuilderWithProvider(argument, args, suggestions);
	}

	// Gets a RequiredArgumentBuilder for an argument, given a SuggestionProvider
	RequiredArgumentBuilder<Source, ?> getRequiredArgumentBuilderWithProvider(Argument<?> argument,
			Argument<?>[] args, SuggestionProvider<Source> provider) {
		SuggestionProvider<Source> newSuggestionsProvider = provider;

		// If we have suggestions to add, combine provider with the suggestions
		if (argument.getIncludedSuggestions().isPresent() && argument.getOverriddenSuggestions().isEmpty()) {
			SuggestionProvider<Source> addedSuggestions = toSuggestions(argument, args, false);

			newSuggestionsProvider = (cmdCtx, builder) -> {
				// Heavily inspired by CommandDispatcher#listSuggestions, with combining
				// multiple CompletableFuture<Suggestions> into one.

				CompletableFuture<Suggestions> addedSuggestionsFuture = addedSuggestions.getSuggestions(cmdCtx,
						builder);
				CompletableFuture<Suggestions> providerSuggestionsFuture = provider.getSuggestions(cmdCtx, builder);
				CompletableFuture<Suggestions> result = new CompletableFuture<>();
				CompletableFuture.allOf(addedSuggestionsFuture, providerSuggestionsFuture).thenRun(() -> {
					List<Suggestions> suggestions = new ArrayList<>();
					suggestions.add(addedSuggestionsFuture.join());
					suggestions.add(providerSuggestionsFuture.join());
					result.complete(Suggestions.merge(cmdCtx.getInput(), suggestions));
				});
				return result;
			};
		}

		RequiredArgumentBuilder<Source, ?> requiredArgumentBuilder = RequiredArgumentBuilder
				.argument(argument.getNodeName(), argument.getRawType());

		return requiredArgumentBuilder.requires(css -> permissionCheck(platform.getCommandSenderFromCommandSource(css),
				argument.getArgumentPermission(), argument.getRequirements())).suggests(newSuggestionsProvider);
	}

	Object[] generatePreviousArguments(CommandContext<Source> context, Argument<?>[] args, String nodeName)
			throws CommandSyntaxException {
		// Populate Object[], which is our previously filled arguments
		List<Object> previousArguments = new ArrayList<>();

		for (Argument<?> arg : args) {
			if (arg.getNodeName().equals(nodeName) && !(arg instanceof LiteralArgument)) {
				break;
			}

			Object result;
			try {
				result = parseArgument(context, arg.getNodeName(), arg, previousArguments.toArray());
			} catch (IllegalArgumentException e) {
				/*
				 * Redirected commands don't parse previous arguments properly. Simplest way to
				 * determine what we should do is simply set it to null, since there's nothing
				 * else we can do. I thought about letting this simply be an empty array, but
				 * then it's even more annoying to deal with - I wouldn't expect an array of
				 * size n to suddenly, randomly be 0, but I would expect random NPEs because
				 * let's be honest, this is Java we're dealing with.
				 */
				result = null;
			}
			if (arg.isListed()) {
				previousArguments.add(result);
			}
		}
		return previousArguments.toArray();
	}

	SuggestionProvider<Source> toSuggestions(Argument<?> theArgument, Argument<?>[] args,
			boolean overrideSuggestions) {
		return (CommandContext<Source> context, SuggestionsBuilder builder) -> {
			// Construct the suggestion info
			SuggestionInfo suggestionInfo = new SuggestionInfo(platform.getCommandSenderFromCommandSource(context.getSource()),
					generatePreviousArguments(context, args, theArgument.getNodeName()), builder.getInput(),
					builder.getRemaining());

			// Get the suggestions
			Optional<ArgumentSuggestions> suggestionsToAddOrOverride = overrideSuggestions
					? theArgument.getOverriddenSuggestions()
					: theArgument.getIncludedSuggestions();
			return suggestionsToAddOrOverride.orElse(ArgumentSuggestions.empty()).suggest(suggestionInfo, builder);
		};
	}

	/**
	 * Looks up the function to generate a chat preview for a path of nodes in the
	 * command tree. This is a method internal to the CommandAPI and isn't expected
	 * to be used by plugin developers (but you're more than welcome to use it as
	 * you see fit).
	 * 
	 * @param path a list of Strings representing the path (names of command nodes)
	 *             to (and including) the previewable argument
	 * @return a function that takes in a {@link PreviewInfo} and returns a
	 *         {@link Component}. If such a function is not available, this will
	 *         return a function that always returns null.
	 */
	public Optional<PreviewableFunction<?>> lookupPreviewable(List<String> path) {
		final IPreviewable<? extends Argument<?>, ?> previewable = previewableArguments.get(path);
		if (previewable != null && previewable.getPreview().isPresent()) {
			// Yeah, don't even question this logic of getting the value of an
			// optional and then wrapping it in an optional again. Java likes it
			// and complains if you don't do this. Not sure why.
			return Optional.of(previewable.getPreview().get());
		} else {
			return Optional.empty();
		}
	}

	/**
	 * 
	 * @param path a list of Strings representing the path (names of command nodes)
	 *             to (and including) the previewable argument
	 * @return Whether a previewable is legacy (non-Adventure) or not
	 */
	public boolean lookupPreviewableLegacyStatus(List<String> path) {
		final IPreviewable<? extends Argument<?>, ?> previewable = previewableArguments.get(path);
		if (previewable != null && previewable.getPreview().isPresent()) {
			return previewable.isLegacy();
		} else {
			return true;
		}
	}

	/////////////////////////
	// SECTION: Reflection //
	/////////////////////////

	/**
	 * Caches a field using reflection if it is not already cached, then return the
	 * field of a given class. This will also make the field accessible.
	 * 
	 * @param clazz the class where the field is declared
	 * @param name  the name of the field
	 * @return a Field reference
	 */
	public static Field getField(Class<?> clazz, String name) {
		ClassCache key = new ClassCache(clazz, name);
		if (FIELDS.containsKey(key)) {
			return FIELDS.get(key);
		} else {
			Field result;
			try {
				result = clazz.getDeclaredField(name);
			} catch (ReflectiveOperationException e) {
				e.printStackTrace();
				return null;
			}
			result.setAccessible(true);
			FIELDS.put(key, result);
			return result;
		}
	}

	//////////////////////////////
	// SECTION: Private classes //
	//////////////////////////////

	/**
	 * Class to store cached methods and fields
	 * 
	 * This is required because each key is made up of a class and a field or method
	 * name
	 */
	private record ClassCache(Class<?> clazz, String name) {
	}

	/**
	 * A class to compute the Cartesian product of a number of lists. Source:
	 * https://www.programmersought.com/article/86195393650/
	 */
	private static final class CartesianProduct {

		// Shouldn't be instantiated
		private CartesianProduct() {
		}

		/**
		 * Returns the Cartesian product of a list of lists
		 * 
		 * @param <T>  the underlying type of the list of lists
		 * @param list the list to calculate the Cartesian product of
		 * @return a List of lists which represents the Cartesian product of all
		 *         elements of the input
		 */
		public static <T> List<List<T>> getDescartes(List<List<T>> list) {
			List<List<T>> returnList = new ArrayList<>();
			descartesRecursive(list, 0, returnList, new ArrayList<T>());
			return returnList;
		}

		/**
		 * Recursive implementation Principle: traverse sequentially from 0 of the
		 * original list to the end
		 * 
		 * @param <T>          the underlying type of the list of lists
		 * @param originalList original list
		 * @param position     The position of the current recursion in the original
		 *                     list
		 * @param returnList   return result
		 * @param cacheList    temporarily saved list
		 */
		private static <T> void descartesRecursive(List<List<T>> originalList, int position,
				List<List<T>> returnList, List<T> cacheList) {
			List<T> originalItemList = originalList.get(position);
			for (int i = 0; i < originalItemList.size(); i++) {
				// The last one reuses cacheList to save memory
				List<T> childCacheList = (i == originalItemList.size() - 1) ? cacheList : new ArrayList<>(cacheList);
				childCacheList.add(originalItemList.get(i));
				if (position == originalList.size() - 1) {// Exit recursion to the end
					returnList.add(childCacheList);
					continue;
				}
				descartesRecursive(originalList, position + 1, returnList, childCacheList);
			}
		}

	}

}