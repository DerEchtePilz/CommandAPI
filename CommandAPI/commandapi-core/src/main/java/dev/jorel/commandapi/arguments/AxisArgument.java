package dev.jorel.commandapi.arguments;

import java.util.EnumSet;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.bukkit.Axis;
import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPIHandler;

/**
 * An argument that represents x, y and z axes as an EnumSet of Axis
 */
public class AxisArgument extends Argument implements ISafeOverrideableSuggestions<EnumSet<Axis>> {
	
	/**
	 * An Axis argument. Represents the axes x, y and z
	 */
	public AxisArgument() {
		super(CommandAPIHandler.getNMS()._ArgumentAxis());
	}

	@Override
	public Class<?> getPrimitiveType() {
		return EnumSet.class;
	}
	
	@Override
	public CommandAPIArgumentType getArgumentType() {
		return CommandAPIArgumentType.AXIS;
	}

	@SafeVarargs
	@Override
	public final Argument safeOverrideSuggestions(EnumSet<Axis>... suggestions) {
		return super.overrideSuggestions(sMap0(e -> e.stream().map(Axis::name).map(String::toLowerCase).reduce(String::concat).get(), suggestions));
	}

	@Override
	public Argument safeOverrideSuggestions(Function<CommandSender, EnumSet<Axis>[]> suggestions) {
		return super.overrideSuggestions(sMap1(e -> e.stream().map(Axis::name).map(String::toLowerCase).reduce(String::concat).get(), suggestions));
	}

	@Override
	public Argument safeOverrideSuggestions(BiFunction<CommandSender, Object[], EnumSet<Axis>[]> suggestions) {
		return super.overrideSuggestions(sMap2(e -> e.stream().map(Axis::name).map(String::toLowerCase).reduce(String::concat).get(), suggestions));
	}
}