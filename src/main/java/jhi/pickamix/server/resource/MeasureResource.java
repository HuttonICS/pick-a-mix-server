package jhi.pickamix.server.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jhi.pickamix.server.database.Database;
import jhi.pickamix.server.database.codegen.tables.pojos.Measures;
import org.jooq.DSLContext;

import java.sql.*;

import static jhi.pickamix.server.database.codegen.tables.Measures.*;

@Path("measure")
public class MeasureResource
{
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMeasures()
		throws SQLException
	{
		try (Connection conn = Database.getConnection())
		{
			DSLContext context = Database.getContext(conn);

			return Response.ok(context.selectFrom(MEASURES).orderBy(MEASURES.NAME).fetchInto(Measures.class)).build();
		}
	}
}
