package redgear.core.render;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;

public abstract class SimpleBlockRenderingHandler implements ISimpleBlockRenderingHandler {

	private final int renderId;

	public SimpleBlockRenderingHandler() {
		renderId = RenderingRegistry.getNextAvailableRenderId();
		RenderingRegistry.registerBlockHandler(renderId, this);
	}

	@Override
	public final int getRenderId() {
		return renderId;
	}

}
