package jhi.pickamix.server.database.binding;

import com.google.gson.Gson;
import jhi.pickamix.server.pojo.TrialMeasure;
import org.jooq.*;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;

import java.sql.*;
import java.util.Objects;

/**
 * @author Sebastian Raubach
 */
public class TrialMeasureBinding implements Binding<JSON, TrialMeasure[]>
{
	@Override
	public Converter<JSON, TrialMeasure[]> converter()
	{
		Gson gson = new Gson();
		return new Converter<>()
		{
			@Override
			public TrialMeasure[] from(JSON o)
			{
				return o == null ? null : gson.fromJson(Objects.toString(o), TrialMeasure[].class);
			}

			@Override
			public JSON to(TrialMeasure[] o)
			{
				return o == null ? null : JSON.json(gson.toJson(o));
			}

			@Override
			public Class<JSON> fromType()
			{
				return JSON.class;
			}

			@Override
			public Class<TrialMeasure[]> toType()
			{
				return TrialMeasure[].class;
			}
		};
	}

	@Override
	public void sql(BindingSQLContext<TrialMeasure[]> ctx)
		throws SQLException
	{
		// Depending on how you generate your SQL, you may need to explicitly distinguish
		// between jOOQ generating bind variables or inlined literals.
		if (ctx.render().paramType() == ParamType.INLINED)
			ctx.render().visit(DSL.inline(ctx.convert(converter()).value())).sql("");
		else
			ctx.render().sql("?");
	}

	@Override
	public void register(BindingRegisterContext<TrialMeasure[]> ctx)
		throws SQLException
	{
		ctx.statement().registerOutParameter(ctx.index(), Types.VARCHAR);
	}

	@Override
	public void set(BindingSetStatementContext<TrialMeasure[]> ctx)
		throws SQLException
	{
		ctx.statement().setString(ctx.index(), Objects.toString(ctx.convert(converter()).value(), null));
	}

	@Override
	public void set(BindingSetSQLOutputContext<TrialMeasure[]> ctx)
		throws SQLException
	{
		throw new SQLFeatureNotSupportedException();
	}

	@Override
	public void get(BindingGetResultSetContext<TrialMeasure[]> ctx)
		throws SQLException
	{
		ctx.convert(converter()).value(JSON.json(ctx.resultSet().getString(ctx.index())));
	}

	@Override
	public void get(BindingGetStatementContext<TrialMeasure[]> ctx)
		throws SQLException
	{
		ctx.convert(converter()).value(JSON.json(ctx.statement().getString(ctx.index())));
	}

	@Override
	public void get(BindingGetSQLInputContext<TrialMeasure[]> ctx)
		throws SQLException
	{
		throw new SQLFeatureNotSupportedException();
	}
}
