/*******************************************************************************
 * Copyright 2022 Jorel Ali (Skepter) - MIT License
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import dev.jorel.commandapi.IStringTooltip;
import dev.jorel.commandapi.StringTooltip;
import dev.jorel.commandapi.SuggestionInfoBase;
import dev.jorel.commandapi.nms.NMS;

/**
 * An argument that accepts a list of objects
 * 
 * @param <T> the type that this list argument generates a list of.
 */
@SuppressWarnings("rawtypes")
public interface ListArgumentBase<T, ImplementedSender>
		extends IGreedyArgument, IArgumentBase<List, ImplementedSender> {

	public default ArgumentSuggestions<ImplementedSender> applySuggestions(String delimiter, boolean allowDuplicates,
			Function<ImplementedSender, Collection<T>> supplier, Function<T, IStringTooltip> mapper) {
		return ArgumentSuggestions.stringsWithTooltips((SuggestionInfoBase<ImplementedSender> info) -> {
			String currentArg = info.currentArg();

			// This need not be a sorted map because entries in suggestions are
			// automatically sorted anyway
			Set<IStringTooltip> values = new HashSet<>();
			for (T object : supplier.apply(info.sender())) {
				values.add(mapper.apply(object));
			}

			List<String> currentArgList = new ArrayList<>();
			for (String str : currentArg.split(Pattern.quote(delimiter))) {
				currentArgList.add(str);
			}

			if (!allowDuplicates) {
				for (String str : currentArgList) {
					IStringTooltip valueToRemove = null;
					for (IStringTooltip value : values) {
						if (value.getSuggestion().equals(str)) {
							valueToRemove = value;
							break;
						}
					}
					if (valueToRemove != null) {
						values.remove(valueToRemove);
					}
				}
			}

			// If we end with the specified delimiter, we prompt for the next entry
			if (currentArg.endsWith(delimiter)) {
				// 'values' now contains a set of all objects that are NOT in
				// the current list that the user is typing. We want to return
				// a list of the current argument + each value that isn't
				// in the list (i.e. each key in 'values')
				IStringTooltip[] returnValues = new IStringTooltip[values.size()];
				int i = 0;
				for (IStringTooltip str : values) {
					returnValues[i] = StringTooltip.of(currentArg + str.getSuggestion(), str.getTooltip());
					i++;
				}
				return returnValues;
			} else {
				// Auto-complete the current value that the user is typing
				// Remove the last argument and turn it into a string as the base for
				// suggestions
				String valueStart = currentArgList.remove(currentArgList.size() - 1);
				String suggestionBase = currentArgList.isEmpty() ? ""
						: String.join(delimiter, currentArgList) + delimiter;

				List<IStringTooltip> returnValues = new ArrayList<>();
				for (IStringTooltip str : values) {
					if (str.getSuggestion().startsWith(valueStart)) {
						returnValues.add(StringTooltip.of(suggestionBase + str.getSuggestion(), str.getTooltip()));
					}
				}
				return returnValues.toArray(new IStringTooltip[0]);
			}
		});
	}

	@Override
	public default Class<List> getPrimitiveType() {
		return List.class;
	}

	@Override
	public default CommandAPIArgumentType getArgumentType() {
		return CommandAPIArgumentType.LIST;
	}

	public default <CommandListenerWrapper> List<T> parseArgument(NMS<CommandListenerWrapper, ImplementedSender> nms,
			CommandContext<CommandListenerWrapper> cmdCtx, String key, String delimiter, boolean allowDuplicates,
			Function<ImplementedSender, Collection<T>> supplier, Function<T, IStringTooltip> mapper)
			throws CommandSyntaxException {
		// Get the list of values which this can take
		Map<IStringTooltip, T> values = new HashMap<>();
		for (T object : supplier.apply(nms.getImplementedSenderFromCSS(cmdCtx.getSource()))) {
			values.put(mapper.apply(object), object);
		}

		// If the argument argument's value is in the list of values, include it
		List<T> list = new ArrayList<>();
		String[] strArr = cmdCtx.getArgument(key, String.class).split(Pattern.quote(delimiter));
		for (String str : strArr) {
			// Yes, this isn't an instant lookup HashMap, but this is the best we can do
			for (IStringTooltip value : values.keySet()) {
				if (value.getSuggestion().equals(str)) {
					if (allowDuplicates) {
						list.add(values.get(value));
					} else if (!list.contains(values.get(value))) {
						list.add(values.get(value));
					}
				}
			}
		}
		return list;
	}
}