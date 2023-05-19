package jhi.pickamix.server;

import jakarta.ws.rs.ApplicationPath;
import jhi.pickamix.server.util.PropertyWatcher;
import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/api/")
public class PickAMix extends ResourceConfig
{
	public PickAMix()
	{
		PropertyWatcher.initialize();

		packages("jhi.pickamix.server");

//		register(MultiPartFeature.class);
	}
}
