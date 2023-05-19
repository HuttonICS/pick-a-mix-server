package jhi.pickamix.server.database;

import org.jooq.codegen.*;
import org.jooq.meta.*;

/**
 * @author Sebastian Raubach
 */
public class CustomCodegen extends JavaGenerator
{
	@Override
	protected void generatePojoGetter(TypedElementDefinition<?> column, int index, JavaWriter out)
	{
		// Don't do anything here, we're using Lombok
	}

	@Override
	protected void generatePojoSetter(TypedElementDefinition<?> column, int index, JavaWriter out)
	{
		// Don't do anything here, we're using Lombok
	}

	@Override
	public boolean generateIndexes()
	{
		return false;
	}

	@Override
	protected void generateSchemaClassJavadoc(SchemaDefinition schema, JavaWriter out)
	{
		out.println("// @formatter:off");

		super.generateSchemaClassJavadoc(schema, out);
	}

	@Override
	protected void generateSchemaClassFooter(SchemaDefinition schema, JavaWriter out)
	{
		super.generateSchemaClassFooter(schema, out);

		out.println("// @formatter:on");
	}

	@Override
	protected void generateCatalogClassJavadoc(CatalogDefinition catalog, JavaWriter out)
	{
		out.println("// @formatter:off");

		super.generateCatalogClassJavadoc(catalog, out);
	}

	@Override
	protected void generateCatalogClassFooter(CatalogDefinition schema, JavaWriter out)
	{
		super.generateCatalogClassFooter(schema, out);

		out.println("// @formatter:on");
	}

	@Override
	protected void generateEnumClassJavadoc(EnumDefinition e, JavaWriter out)
	{
		out.println("// @formatter:off");

		super.generateEnumClassJavadoc(e, out);
	}

	@Override
	protected void generateEnumClassFooter(EnumDefinition e, JavaWriter out)
	{
		super.generateEnumClassFooter(e, out);

		out.println("// @formatter:on");
	}



	@Override
	protected void generateTableClassJavadoc(TableDefinition table, JavaWriter out)
	{
		out.println("// @formatter:off");

		super.generateTableClassJavadoc(table, out);
	}

	@Override
	protected void generateTableClassFooter(TableDefinition table, JavaWriter out)
	{
		super.generateTableClassFooter(table, out);

		out.println("// @formatter:on");
	}

	@Override
	protected void generateRecordClassJavadoc(TableDefinition table, JavaWriter out)
	{
		out.println("// @formatter:off");

		super.generateRecordClassJavadoc(table, out);
	}

	@Override
	protected void generateRecordClassFooter(TableDefinition table, JavaWriter out)
	{
		super.generateRecordClassFooter(table, out);

		out.println("// @formatter:on");
	}

	@Override
	protected void generatePojoClassJavadoc(TableDefinition table, JavaWriter out)
	{
		out.println("import lombok.*;");
		out.println("import lombok.experimental.Accessors;");
		out.println("");
		out.println("// @formatter:off");

		super.generatePojoClassJavadoc(table, out);

		out.println("@Getter");
		out.println("@Setter");
		out.println("@Accessors(chain = true)");
	}

	@Override
	protected void generatePojoClassFooter(TableDefinition table, JavaWriter out)
	{
		super.generatePojoClassFooter(table, out);

		out.println("// @formatter:on");
	}
}
