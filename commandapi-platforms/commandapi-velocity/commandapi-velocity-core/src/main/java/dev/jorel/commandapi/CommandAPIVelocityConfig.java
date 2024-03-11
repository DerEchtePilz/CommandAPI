package dev.jorel.commandapi;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.Optional;

/**
 * A class that contains information needed to configure the CommandAPI on Velocity-based servers.
 */
public class CommandAPIVelocityConfig extends CommandAPIConfig<CommandAPIVelocityConfig> {
	ProxyServer server;
	Object plugin;

	/**
	 * Creates a new CommandAPIVelocityConfig object. Variables in this
	 * constructor are required to load the CommandAPI on Velocity properly.
	 *
	 * @param server The {@link ProxyServer} that the CommandAPI is running on.
	 */
	public CommandAPIVelocityConfig(ProxyServer server) {
		this(server, null);
	}

	/**
	 * Creates a new CommandAPIVelocityConfig object. Variables in this
	 * constructor are required to load the CommandAPI on Velocity properly.
	 *
	 * @param server The {@link ProxyServer} that the CommandAPI is running on.
	 * @param plugin The plugin that is loading the CommandAPI.
	 */
	public CommandAPIVelocityConfig(ProxyServer server, Object plugin) {
		this.server = server;
		this.plugin = plugin;
		super.setNamespace("");
	}

	/**
	 * @return this CommandAPIVelocityConfig
	 */
	@Override
	public CommandAPIVelocityConfig usePluginNamespace() {
		if (plugin == null) {
			CommandAPI.logNormal("Cannot use plugin namespace because plugin was not set or null. The currently set namespace wasn't changed.");
			return instance();
		}
		Optional<PluginContainer> pluginContainerOptional = server.getPluginManager().fromInstance(plugin);
		if (pluginContainerOptional.isEmpty()) {
			CommandAPI.logInfo("Using the default namespace to register commands since the given Object is not a Velocity plugin!");
			return instance();
		}
		super.setNamespace(pluginContainerOptional.get().getDescription().getId());
		super.usePluginNamespace = true;
		return instance();
	}

	@Override
	public CommandAPIVelocityConfig setNamespace(String namespace) {
		if (namespace == null) {
			throw new NullPointerException("Default namespace can't be null!");
		}
		if (!CommandAPIHandler.NAMESPACE_PATTERN.matcher(namespace).matches()) {
			CommandAPI.logNormal("Did not set namespace to the provided '" + namespace + "' namespace because only 0-9, a-z, underscores, periods and hyphens are allowed!");
			return this;
		}
		return super.setNamespace(namespace);
	}

	@Override
	public CommandAPIVelocityConfig instance() {
		return this;
	}
}
