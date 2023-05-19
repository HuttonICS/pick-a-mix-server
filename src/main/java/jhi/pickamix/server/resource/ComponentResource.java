package jhi.pickamix.server.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jhi.pickamix.server.database.Database;
import org.jooq.DSLContext;

import java.sql.*;

import static jhi.pickamix.server.database.codegen.tables.Components.COMPONENTS;

@Path("component")
public class ComponentResource
{
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getComponents()
			throws SQLException
	{
		try (Connection conn = Database.getConnection())
		{
			DSLContext context = Database.getContext(conn);

			return Response.ok(context.selectDistinct(COMPONENTS.CROP)
									  .from(COMPONENTS)
									  .orderBy(COMPONENTS.CROP)
									  .fetchInto(String.class))
						   .build();
		}
	}
}
