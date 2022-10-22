package dev.jorel.commandapi;

import dev.jorel.commandapi.abstractions.AbstractCommandSender;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.*;
import org.bukkit.command.CommandSender;

public interface BukkitExecutable<Impl extends BukkitExecutable<Impl>> {
	CustomCommandExecutor<CommandSender, AbstractCommandSender<? extends CommandSender>> getExecutor();

	// Regular command executor

	/**
	 * Adds an executor to the current command builder
	 * @param executor A lambda of type <code>(CommandSender, Object[]) -&gt; ()</code> that will be executed when the command is run
	 * @param types A list of executor types to use this executes method for.
	 * @return this command builder
	 */
	default Impl executes(CommandExecutor executor, ExecutorType... types) {
		if(types == null || types.length == 0) {
			getExecutor().addNormalExecutor(executor);
		} else {
			for(ExecutorType type : types) {
				getExecutor().addNormalExecutor(new CommandExecutor() {

					@Override
					public void run(CommandSender sender, Object[] args) throws WrapperCommandSyntaxException {
						executor.executeWith(BukkitPlatform.get().wrapCommandSender(sender), args);
					}

					@Override
					public ExecutorType getType() {
						return type;
					}
				});
			}
		}
		return (Impl) this;
	}

	/**
	 * Adds an executor to the current command builder
	 * @param executor A lambda of type <code>(CommandSender, Object[]) -&gt; int</code> that will be executed when the command is run
	 * @param types A list of executor types to use this executes method for.
	 * @return this command builder
	 */
	default Impl executes(ResultingCommandExecutor executor, ExecutorType... types) {
		if(types == null || types.length == 0) {
			getExecutor().addResultingExecutor(executor);
		} else {
			for(ExecutorType type : types) {
				getExecutor().addResultingExecutor(new ResultingCommandExecutor() {

					@Override
					public int run(CommandSender sender, Object[] args) throws WrapperCommandSyntaxException {
						return executor.executeWith(BukkitPlatform.get().wrapCommandSender(sender), args);
					}

					@Override
					public ExecutorType getType() {
						return type;
					}
				});
			}
		}
		return (Impl) this;
	}

	// Player command executor

	/**
	 * Adds an executor to the current command builder
	 * @param executor A lambda of type <code>(Player, Object[]) -&gt; ()</code> that will be executed when the command is run
	 * @return this command builder
	 */
	default Impl executesPlayer(PlayerCommandExecutor executor) {
		getExecutor().addNormalExecutor(executor);
		return (Impl) this;
	}

	/**
	 * Adds an executor to the current command builder
	 * @param executor A lambda of type <code>(Player, Object[]) -&gt; int</code> that will be executed when the command is run
	 * @return this command builder
	 */
	default Impl executesPlayer(PlayerResultingCommandExecutor executor) {
		getExecutor().addResultingExecutor(executor);
		return (Impl) this;
	}

	// Entity command executor

	/**
	 * Adds an executor to the current command builder
	 * @param executor A lambda of type <code>(Entity, Object[]) -&gt; ()</code> that will be executed when the command is run
	 * @return this command builder
	 */
	default Impl executesEntity(EntityCommandExecutor executor) {
		getExecutor().addNormalExecutor(executor);
		return (Impl) this;
	}

	/**
	 * Adds an executor to the current command builder
	 * @param executor A lambda of type <code>(Entity, Object[]) -&gt; int</code> that will be executed when the command is run
	 * @return this command builder
	 */
	default Impl executesEntity(EntityResultingCommandExecutor executor) {
		getExecutor().addResultingExecutor(executor);
		return (Impl) this;
	}

	// Proxy command executor

	/**
	 * Adds an executor to the current command builder
	 * @param executor A lambda of type <code>(Entity, Object[]) -&gt; ()</code> that will be executed when the command is run
	 * @return this command builder
	 */
	default Impl executesProxy(ProxyCommandExecutor executor) {
		getExecutor().addNormalExecutor(executor);
		return (Impl) this;
	}

	/**
	 * Adds an executor to the current command builder
	 * @param executor A lambda of type <code>(Entity, Object[]) -&gt; int</code> that will be executed when the command is run
	 * @return this command builder
	 */
	default Impl executesProxy(ProxyResultingCommandExecutor executor) {
		getExecutor().addResultingExecutor(executor);
		return (Impl) this;
	}

	// Command block command sender

	/**
	 * Adds an executor to the current command builder
	 * @param executor A lambda of type <code>(BlockCommandSender, Object[]) -&gt; ()</code> that will be executed when the command is run
	 * @return this command builder
	 */
	default Impl executesCommandBlock(CommandBlockCommandExecutor executor) {
		getExecutor().addNormalExecutor(executor);
		return (Impl) this;
	}

	/**
	 * Adds an executor to the current command builder
	 * @param executor A lambda of type <code>(BlockCommandSender, Object[]) -&gt; int</code> that will be executed when the command is run
	 * @return this command builder
	 */
	default Impl executesCommandBlock(CommandBlockResultingCommandExecutor executor) {
		getExecutor().addResultingExecutor(executor);
		return (Impl) this;
	}

	// Console command sender

	/**
	 * Adds an executor to the current command builder
	 * @param executor A lambda of type <code>(BlockCommandSender, Object[]) -&gt; ()</code> that will be executed when the command is run
	 * @return this command builder
	 */
	default Impl executesConsole(ConsoleCommandExecutor executor) {
		getExecutor().addNormalExecutor(executor);
		return (Impl) this;
	}

	/**
	 * Adds an executor to the current command builder
	 * @param executor A lambda of type <code>(BlockCommandSender, Object[]) -&gt; int</code> that will be executed when the command is run
	 * @return this command builder
	 */
	default Impl executesConsole(ConsoleResultingCommandExecutor executor) {
		getExecutor().addResultingExecutor(executor);
		return (Impl) this;
	}

	/**
	 * Adds an executor to the current command builder
	 * @param executor A lambda of type <code>(NativeCommandExecutor, Object[]) -&gt; ()</code> that will be executed when the command is run
	 * @return this command builder
	 */
	default Impl executesNative(NativeCommandExecutor executor) {
		getExecutor().addNormalExecutor(executor);
		return (Impl) this;
	}

	/**
	 * Adds an executor to the current command builder
	 * @param executor A lambda of type <code>(NativeCommandExecutor, Object[]) -&gt; int</code> that will be executed when the command is run
	 * @return this command builder
	 */
	default Impl executesNative(NativeResultingCommandExecutor executor) {
		getExecutor().addResultingExecutor(executor);
		return (Impl) this;
	}

}