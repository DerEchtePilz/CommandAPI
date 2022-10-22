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

import dev.jorel.commandapi.BukkitExecutable;
import org.bukkit.command.CommandSender;

/**
 * An argument that represents primitive Java longs
 */
public class LongArgument extends AbstractLongArgument<LongArgument, CommandSender> implements BukkitExecutable<LongArgument> {
	/**
	 * A long argument
	 *
	 * @param nodeName the name of the node for this argument
	 */
	public LongArgument(String nodeName) {
		super(nodeName);
	}

	/**
	 * A long argument with a minimum value
	 *
	 * @param nodeName the name of the node for this argument
	 * @param value    The minimum value this argument can take (inclusive)
	 */
	public LongArgument(String nodeName, long value) {
		super(nodeName, value);
	}

	/**
	 * A long argument with a minimum and maximum value
	 *
	 * @param nodeName the name of the node for this argument
	 * @param min      The minimum value this argument can take (inclusive)
	 * @param max      The maximum value this argument can take (inclusive)
	 */
	public LongArgument(String nodeName, long min, long max) {
		super(nodeName, min, max);
	}
}