# BlockState arguments

![](./images/arguments/blockstate.png)

The `BlockStateArgument` is used to represent data about blocks in the world. These refer to any blocks that have data or states, such as dispensers, signs, doors and pistons. The `BlockStateArgument` creates a Bukkit `BlockData` object when used.

> **Developer's Note:**
>
> Make sure to not confuse the cast type with `BlockState`. The naming of this argument refers to the internal Minecraft vanilla argument naming convention - **this argument casts to `BlockData` and NOT `BlockState`**.

<div class="example">

### Example - Setting a block

Say we want a simple command to set the block that you're looking at. We'll use the following command structure:

```
/set <block>
```

And then we can simply set our block using `setBlockData()`:

```java
LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
arguments.put("block", new BlockStateArgument());

new CommandAPICommand("set")
	.withArguments(arguments)
	.executesPlayer((player, args) -> {
		BlockData blockdata = (BlockData) args[0];
		Block targetBlock = player.getTargetBlockExact(256);
		
        // Set the block, along with its data
		targetBlock.setType(blockdata.getMaterial());
		targetBlock.getState().setBlockData(blockdata);
	})
	.register();
```

</div>
