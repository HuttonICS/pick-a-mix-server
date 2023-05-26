package jhi.pickamix.server.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jhi.pickamix.server.database.Database;
import jhi.pickamix.server.database.codegen.tables.pojos.*;
import org.jooq.DSLContext;

import java.sql.*;

import static jhi.pickamix.server.database.codegen.tables.Measures.MEASURES;
import static jhi.pickamix.server.database.codegen.tables.ViewTrialComponentMeasures.VIEW_TRIAL_COMPONENT_MEASURES;

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

			return Response.ok(context.selectFrom(MEASURES)
									  .orderBy(MEASURES.NAME.sortAsc("Crop purpose", "Tillage", "Sowing date", "Sowing rate (kg/ha)", "Sowing method", "Fertiliser", "Disease control", "Insect control", "Weed control", "Harvest date", "Yield (t/ha)"))
									  .fetchInto(Measures.class)).build();
		}
	}

	@GET
	@Path("/{measureId:\\d+}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMeasureValues(@PathParam("measureId") Integer measureId)
			throws SQLException
	{
		if (measureId == null)
			return Response.status(Response.Status.BAD_REQUEST).build();

		try (Connection conn = Database.getConnection())
		{
			DSLContext context = Database.getContext(conn);

			return Response.ok(context.selectFrom(VIEW_TRIAL_COMPONENT_MEASURES)
									  .where(VIEW_TRIAL_COMPONENT_MEASURES.MEASURE_ID.eq(measureId))
									  .fetchInto(ViewTrialComponentMeasures.class))
						   .build();
		}
	}
}
