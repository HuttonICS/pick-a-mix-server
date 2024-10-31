package jhi.pickamix.server.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jhi.pickamix.server.util.spreadsheet.DatabaseUpdater;

@Path("form-update")
public class FormUpdateResource
{
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public void getFormUpdate()
	{
		// Fire off the spreadsheet importer
		new Thread(DatabaseUpdater::fromSpreadsheet).start();
	}
}
