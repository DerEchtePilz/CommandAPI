package dev.jorel.commandapi.exceptions;

public class OptionalArgumentException extends RuntimeException {

	public OptionalArgumentException(String commandName) {
		super("Failed to register command /" + commandName + " because an unlinked required argument cannot follow an optional argument!");
	}

}
