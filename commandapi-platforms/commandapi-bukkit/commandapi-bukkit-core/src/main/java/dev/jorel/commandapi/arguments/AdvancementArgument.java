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
package dev.jorel.commandapi.arguments;

import org.bukkit.advancement.Advancement;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import dev.jorel.commandapi.BukkitPlatform;
import dev.jorel.commandapi.abstractions.AbstractPlatform;

/**
 * An argument that represents the Bukkit Advancement object
 * @apiNote Returns an {@link Advancement} object
 */
public class AdvancementArgument extends SafeOverrideableArgument<Advancement, Advancement> implements ICustomProvidedArgument {
	
	/**
	 * Constructs an AdvancementArgument with a given node name
	 * @param nodeName the name of the node for argument
	 */
	public AdvancementArgument(String nodeName) {
		super(nodeName, BukkitPlatform.get()._ArgumentMinecraftKeyRegistered(), BukkitPlatform.fromKey(Advancement::getKey));
	}

	@Override
	public Class<Advancement> getPrimitiveType() {
		return Advancement.class;
	}
	
	@Override
	public CommandAPIArgumentType getArgumentType() {
		return CommandAPIArgumentType.ADVANCEMENT;
	}

	@Override
	public SuggestionProviders getSuggestionProvider() {
		return SuggestionProviders.ADVANCEMENTS;
	}

	@Override
	public <CommandSourceStack> Advancement parseArgument(AbstractPlatform<CommandSourceStack> platform,
			CommandContext<CommandSourceStack> cmdCtx, String key, Object[] previousArgs) throws CommandSyntaxException {
		return ((BukkitPlatform<CommandSourceStack>) platform).getAdvancement(cmdCtx, key);
	}

}