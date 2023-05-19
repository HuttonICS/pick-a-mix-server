package jhi.pickamix.server.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jhi.pickamix.server.database.Database;
import jhi.pickamix.server.database.codegen.tables.pojos.*;
import jhi.pickamix.server.pojo.*;
import org.jooq.*;

import java.sql.*;
import java.util.List;

import static jhi.pickamix.server.database.codegen.tables.ViewTrialComponentMeasures.VIEW_TRIAL_COMPONENT_MEASURES;
import static jhi.pickamix.server.database.codegen.tables.ViewTrials.VIEW_TRIALS;

@Path("trial")
public class TrialResource extends BaseResource
{
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postTrialTable(PaginatedRequest request)
			throws SQLException
	{
		processRequest(request);
		try (Connection conn = Database.getConnection())
		{
			DSLContext context = Database.getContext(conn);
			SelectSelectStep<Record> select = context.select();

			if (previousCount == -1) select.hint("SQL_CALC_FOUND_ROWS");

			SelectJoinStep<Record> from = select.from(VIEW_TRIALS);

			// Filter here!
			filter(from, filters, true);

			List<ViewTrials> result = setPaginationAndOrderBy(from).fetch().into(ViewTrials.class);

			long count = previousCount == -1 ? context.fetchOne("SELECT FOUND_ROWS()").into(Long.class) : previousCount;

			return Response.ok(new PaginatedResult<>(result, count)).build();
		}
	}

	@Path("/{trialId:\\d+}/measure")
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTrialMeasures(@PathParam("trialId") Integer trialId)
			throws SQLException
	{
		try (Connection conn = Database.getConnection())
		{
			DSLContext context = Database.getContext(conn);

			return Response.ok(context.selectFrom(VIEW_TRIAL_COMPONENT_MEASURES)
									  .where(VIEW_TRIAL_COMPONENT_MEASURES.TRIAL_ID.eq(trialId))
									  .orderBy(VIEW_TRIAL_COMPONENT_MEASURES.MEASURE_NAME, VIEW_TRIAL_COMPONENT_MEASURES.PLOT_MEASUREMENT_TYPE)
									  .fetchInto(ViewTrialComponentMeasures.class))
						   .build();
		}
	}
}
