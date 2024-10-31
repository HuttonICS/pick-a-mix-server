package jhi.pickamix.server.importer;

import jhi.pickamix.server.database.Database;
import jhi.pickamix.server.database.codegen.enums.PlotsMeasurementType;
import jhi.pickamix.server.database.codegen.tables.records.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jooq.DSLContext;
import org.jooq.tools.StringUtils;

import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.*;
import java.util.logging.Logger;

import static jhi.pickamix.server.database.codegen.tables.Components.COMPONENTS;
import static jhi.pickamix.server.database.codegen.tables.Measures.MEASURES;
import static jhi.pickamix.server.database.codegen.tables.PlotComponents.PLOT_COMPONENTS;
import static jhi.pickamix.server.database.codegen.tables.PlotMeasures.PLOT_MEASURES;
import static jhi.pickamix.server.database.codegen.tables.Plots.PLOTS;
import static jhi.pickamix.server.database.codegen.tables.TrialMeasures.TRIAL_MEASURES;
import static jhi.pickamix.server.database.codegen.tables.Trials.TRIALS;

public class SpreadsheetImporter
{
	public static final String TRIAL_IDENTIFIER_DATASET_NAME = "Trial Identifier/dataset name";
	public static final String INFO_SHARED                   = "This information will not be shared.";
	public static final String TRIAL_LONGITUDE               = "Where is the trial - Longitude";
	public static final String TRIAL_LATITUDE                = "Where is the trial - Latitude";
	public static final String TRIAL_POSTCODE                = "Trial site - please input the first half of your postcode (e.g. DD2)";
	public static final String TIMESTAMP                     = "Timestamp";
	public static final String TRIAL_FARM_MANAGEMENT         = "Describe your farm management system.";
	public static final String TRIAL_WEED_INCIDENCE          = "If you’ve recorded weed incidence please note your observations here.";
	public static final String TRIAL_DISEASE_INCIDENCE       = "If you’ve recorded disease incidence please note your observations here.";
	public static final String TRIAL_PEST_INCIDENCE          = "If you’ve recorded pests incidence please note your observations here.";
	public static final String TRIAL_SOIL_HEALTH             = "Soil Health - please describe any observations.";
	public static final String TRIAL_BIODIVERSITY            = "Bio-diversity - if you have recorded bio-diversity please record your observations here.";
	public static final String TRIAL_COMMENTS                = "If you'd like at add anything further please note it here, or email to:  Ali.Karley@hutton.ac.uk";
	public static final String TRIAL_MIXTURE_COUNT           = "In the next section you will be inputting information about your plant mixtures. How many components are in your mixture?";
	public static final String HARVEST_DATE                  = "Harvest date";
	public static final String CROP_PURPOSE                  = "Crop purpose";
	public static final String TILLAGE                       = "Tillage";
	public static final String FERTILISER                    = "Fertiliser";
	public static final String WEED_CONTROL                  = "Weed control";
	public static final String INSECT_CONTROL                = "Insect control";
	public static final String DISEASE_CONTROL               = "Disease control";
	public static final String YIELD                         = "Yield (t/ha)";
	public static final String SOWING_DATE                   = "Sowing date";
	public static final String SOWING_RATE                   = "Sowing rate (kg/ha)";
	public static final String SOWING_METHOD                 = "Sowing method";

	private SimpleDateFormat sdfDateTime = new SimpleDateFormat("M/d/yyyy H:mm:ss");
	private SimpleDateFormat sdfDate     = new SimpleDateFormat("dd/MM/yyyy");
	private SimpleDateFormat sdfDatabase = new SimpleDateFormat("yyyy-MM-dd");

	private File input;

	public static void main(String[] args)
			throws SQLException, IOException
	{
		Database.init("localhost", "pick_a_mix", null, "root", null, false);

//		for (int i = 0; i < 100; i++)
		new SpreadsheetImporter(new File("C:/Users/sr41756/Downloads/SEAMS V3 - Germinate (Responses) (1).xlsx")).importFile();
	}

	public SpreadsheetImporter(File input)
	{
		this.input = input;
	}

	public void importFile()
			throws IOException, SQLException
	{
		Map<String, ComponentsRecord> dbComponents = new HashMap<>();
		Map<String, MeasuresRecord> dbMeasures;

		try (Connection conn = Database.getConnection())
		{
			DSLContext context = Database.getContext(conn);
			context.selectFrom(COMPONENTS).forEach(c -> dbComponents.put(c.getCrop() + "|" + c.getVariety(), c));
			dbMeasures = context.selectFrom(MEASURES).fetchMap(MeasuresRecord::getName);

			try (FileInputStream file = new FileInputStream(input);
				 Workbook wb = new XSSFWorkbook(file))
			{
				Sheet data = wb.getSheetAt(0);

				Row headers = data.getRow(0);

				Map<String, Integer> hm = new HashMap<>();
				for (Cell cell : headers)
					hm.put(cell.getStringCellValue().replace('\u00A0', ' ').replace('\u2007', ' ').replace('\u202F', ' ').replace(' ', ' ').trim(), cell.getColumnIndex());

				for (int i = 1; i < data.getPhysicalNumberOfRows(); i++)
				{
					Row row = data.getRow(i);
					Integer id = row.getRowNum(); // Row number as external identifier
					String trialName = getOptional(row.getCell(hm.get(TRIAL_IDENTIFIER_DATASET_NAME)));

					if (StringUtils.isEmpty(trialName))
					{
						Logger.getLogger("").warning("Missing trial name found, skipping row: " + id);
						continue;
					}

					String trialIdentifier = id + "-" + trialName;
					String email = getOptional(row.getCell(hm.get(INFO_SHARED)));
					Double longitude = getDouble(row.getCell(hm.get(TRIAL_LONGITUDE)));
					Double latitude = getDouble(row.getCell(hm.get(TRIAL_LATITUDE)));
					String postcode = getOptional(row.getCell(hm.get(TRIAL_POSTCODE)));
					Date submissionDate = getDateTime(row.getCell(hm.get(TIMESTAMP)));
					if (submissionDate == null)
						submissionDate = new Date();
					String farmManagement = getOptional(row.getCell(hm.get(TRIAL_FARM_MANAGEMENT)));
					String weedIncidence = getOptional(row.getCell(hm.get(TRIAL_WEED_INCIDENCE)));
					String diseaseIncidence = getOptional(row.getCell(hm.get(TRIAL_DISEASE_INCIDENCE)));
					String pestIncidence = getOptional(row.getCell(hm.get(TRIAL_PEST_INCIDENCE)));
					String soilHealth = getOptional(row.getCell(hm.get(TRIAL_SOIL_HEALTH)));
					String biodiversity = getOptional(row.getCell(hm.get(TRIAL_BIODIVERSITY)));

					String comment = getOptional(row.getCell(hm.get(TRIAL_COMMENTS)));

					Integer componentCount = getInteger(row.getCell(hm.get(TRIAL_MIXTURE_COUNT)));
					if (componentCount == null)
						componentCount = 0;

					TrialsRecord trial = context.selectFrom(TRIALS).where(TRIALS.NAME.eq(trialIdentifier)).fetchAny();

					if (trial != null)
					{
                        Logger.getLogger("").warning("Duplicate trial found, skipping row: " + id);
					}
					else
					{
						trial = context.newRecord(TRIALS);
						trial.setName(trialIdentifier);
						trial.setLatitude(latitude);
						trial.setLongitude(longitude);
						trial.setContactEmail(email);
						trial.setPostcode(postcode);
						trial.setFarmManagement(farmManagement);
						trial.setWeedIncidence(weedIncidence);
						trial.setPestIncidence(pestIncidence);
						trial.setDiseaseIncidence(diseaseIncidence);
						trial.setSoilHealth(soilHealth);
						trial.setBiodiversity(biodiversity);
						trial.setNotes(comment);
						trial.setCreatedOn(new Timestamp(submissionDate.getTime()));
						trial.store();

						Date harvestDate = getDate(row.getCell(hm.get("Harvest Date - mixture " + componentCount + " components")));
						String cropPurpose = getOptional(row.getCell(hm.get("What are you growing this crop for? Mixture " + componentCount + " components")));
						String tillage = getOptional(row.getCell(hm.get("Tillage - mixture " + componentCount + " components")));
						String fertiliser = getOptional(row.getCell(hm.get("Fertiliser, please input details at the end of the form - mixture " + componentCount + " components.")));
						String weedControl = getOptional(row.getCell(hm.get("Have you applied chemicals for Weed Control - mixture " + componentCount + " components.")));
						String insectControl = getOptional(row.getCell(hm.get("Have you applied chemicals for Insect Control - mixture " + componentCount + " components.")));
						String diseaseControl = getOptional(row.getCell(hm.get("Have you applied chemicals for Disease Control - mixture " + componentCount + " components.")));
						Double yield = getDouble(row.getCell(hm.get("Total mixture yield t/ha - " + componentCount + " components")));

						writeTrialMeasureDate(context, trial, dbMeasures.get(HARVEST_DATE), harvestDate);
						writeTrialMeasure(context, trial, dbMeasures.get(CROP_PURPOSE), cropPurpose);
						writeTrialMeasure(context, trial, dbMeasures.get(TILLAGE), tillage);
						writeTrialMeasure(context, trial, dbMeasures.get(FERTILISER), fertiliser);
						writeTrialMeasure(context, trial, dbMeasures.get(WEED_CONTROL), weedControl);
						writeTrialMeasure(context, trial, dbMeasures.get(INSECT_CONTROL), insectControl);
						writeTrialMeasure(context, trial, dbMeasures.get(DISEASE_CONTROL), diseaseControl);
						writeTrialMeasureDouble(context, trial, dbMeasures.get(YIELD), yield);

						List<CPRData> cprData = new ArrayList<>();

						for (int comp = 1; comp <= componentCount; comp++)
						{
							String crop = getOptional(row.getCell(hm.get("Component " + comp + " of " + componentCount)));
							String variety = getOptional(row.getCell(hm.get("Component " + comp + " of " + componentCount + " - Variety")));

							ComponentsRecord componentsRecord = dbComponents.get(crop + "|" + variety);

							if (componentsRecord == null)
							{
								componentsRecord = context.newRecord(COMPONENTS);
								componentsRecord.setCrop(crop);
								componentsRecord.setVariety(variety);
								componentsRecord.setCreatedOn(new Timestamp(submissionDate.getTime()));
								componentsRecord.store();

								dbComponents.put(crop + "|" + variety, componentsRecord);
							}

							// Add the mono-crops
							PlotsRecord monoPlot = context.newRecord(PLOTS);
							monoPlot.setTrialId(trial.getId());
							monoPlot.setMeasurementType(PlotsMeasurementType.mono);
							monoPlot.setCreatedOn(new Timestamp(submissionDate.getTime()));
							monoPlot.store();
							PlotComponentsRecord pc = context.newRecord(PLOT_COMPONENTS);
							pc.setPlotId(monoPlot.getId());
							pc.setComponentId(componentsRecord.getId());
							pc.store();

							// Add the mix-crops individual
							PlotsRecord mixPlot = context.newRecord(PLOTS);
							mixPlot.setTrialId(trial.getId());
							mixPlot.setMeasurementType(PlotsMeasurementType.mix);
							mixPlot.setCreatedOn(new Timestamp(submissionDate.getTime()));
							mixPlot.store();
							pc = context.newRecord(PLOT_COMPONENTS);
							pc.setPlotId(mixPlot.getId());
							pc.setComponentId(componentsRecord.getId());
							pc.store();

							Date sowingDateMono = getDate(row.getCell(hm.get("Sowing Date - component " + comp + " of " + componentCount)));
							Date harvestDateMono = getDate(row.getCell(hm.get("Harvest Date - component " + comp + " of " + componentCount)));
							cropPurpose = getOptional(row.getCell(hm.get("What are you growing this crop for? Component " + comp + " of " + componentCount)));
							Date sowingDateMix = getDate(row.getCell(hm.get("Sowing Date - mixture - " + comp + " of " + componentCount + " components")));
							Double sowingRateMono = getDouble(row.getCell(hm.get("Monoculture - sowing rate kg/ha - component " + comp + " of " + componentCount)));
							Double sowingRateMix = getDouble(row.getCell(hm.get("Mixture - sowing rate kg/ha - component " + comp + " of " + componentCount)));
							String tillageMono = getOptional(row.getCell(hm.get("Tillage - component " + comp + " of " + componentCount + ".")));
							String sowingMethodMono = getOptional(row.getCell(hm.get("Sowing Method - component " + comp + " of " + componentCount + ".")));
							String sowingMethodMix = getOptional(row.getCell(hm.get("Sowing Method - mixture - " + comp + " of " + componentCount + " components.")));
							String fertiliserMono = getOptional(row.getCell(hm.get("Fertiliser, please input details at the end of the form - component " + comp + " of " + componentCount + ".")));
							String weedControlMono = getOptional(row.getCell(hm.get("Have you applied chemicals for Weed Control - component " + comp + " of " + componentCount + ".")));
							String insectControlMono = getOptional(row.getCell(hm.get("Have you applied chemicals for Insect Control - component " + comp + " of " + componentCount + ".")));
							String diseaseControlMono = getOptional(row.getCell(hm.get("Have you applied chemicals for Disease Control - component " + comp + " of " + componentCount + ".")));
							Double yieldMono = getDouble(row.getCell(hm.get("Monoculture yield t/ha - component " + comp + " of " + componentCount)));
							Double yieldMix = getDouble(row.getCell(hm.get("Mixture yield - t/ha- component " + comp + " of " + componentCount)));

							if (sowingRateMix != null && sowingRateMono != null && yieldMono != null)
							{
								cprData.add(new CPRData()
													.setSowingRateMono(sowingRateMono)
													.setSowingRateMix(sowingRateMix)
													.setYieldMono(yieldMono)
													.setYieldMix(yieldMix));
							}

							writePlotMeasureDate(context, monoPlot, dbMeasures.get(SOWING_DATE), sowingDateMono);
							writePlotMeasureDate(context, monoPlot, dbMeasures.get(HARVEST_DATE), harvestDateMono);
							writePlotMeasure(context, monoPlot, dbMeasures.get(CROP_PURPOSE), cropPurpose);
							writePlotMeasureDate(context, mixPlot, dbMeasures.get(SOWING_DATE), sowingDateMix);
							writePlotMeasureDouble(context, monoPlot, dbMeasures.get(SOWING_RATE), sowingRateMono);
							writePlotMeasureDouble(context, mixPlot, dbMeasures.get(SOWING_RATE), sowingRateMix);
							writePlotMeasure(context, monoPlot, dbMeasures.get(TILLAGE), tillageMono);
							writePlotMeasure(context, monoPlot, dbMeasures.get(SOWING_METHOD), sowingMethodMono);
							writePlotMeasure(context, mixPlot, dbMeasures.get(SOWING_METHOD), sowingMethodMix);
							writePlotMeasure(context, monoPlot, dbMeasures.get(FERTILISER), fertiliserMono);
							writePlotMeasure(context, monoPlot, dbMeasures.get(WEED_CONTROL), weedControlMono);
							writePlotMeasure(context, monoPlot, dbMeasures.get(INSECT_CONTROL), insectControlMono);
							writePlotMeasure(context, monoPlot, dbMeasures.get(DISEASE_CONTROL), diseaseControlMono);
							writePlotMeasureDouble(context, monoPlot, dbMeasures.get(YIELD), yieldMono);
							writePlotMeasureDouble(context, mixPlot, dbMeasures.get(YIELD), yieldMix);
						}

						if (cprData.size() == componentCount)
						{
							Double numerator = 0d;
							double denominator = 0;
							boolean isIndividualYield = true;

							double sowingRateMixTotal = 0;
							for (CPRData d : cprData)
							{
								sowingRateMixTotal += d.getSowingRateMix();
							}

							for (CPRData d : cprData)
							{
								if (d.yieldMix != null && isIndividualYield)
									numerator += d.yieldMix;
								else
								{
									isIndividualYield = false;
									numerator = yield;
								}

								denominator += (d.sowingRateMix / sowingRateMixTotal) * d.yieldMono;
							}

							if (numerator != null && numerator != 0 && denominator != 0)
							{
								trial.setCpr(numerator / denominator);
								trial.store(TRIALS.CPR);
							}
						}
					}
				}
			}
		}
	}

	private void writePlotMeasureDouble(DSLContext context, PlotsRecord plot, MeasuresRecord measure, Double value)
	{
		if (value != null)
		{
			PlotMeasuresRecord pm = context.newRecord(PLOT_MEASURES);
			pm.setPlotId(plot.getId());
			pm.setMeasureId(measure.getId());
			pm.setValue(Double.toString(value));
			pm.setCreatedOn(new Timestamp(System.currentTimeMillis()));
			pm.store();
		}
	}

	private void writeTrialMeasureDouble(DSLContext context, TrialsRecord trial, MeasuresRecord measure, Double value)
	{
		if (value != null)
		{
			TrialMeasuresRecord pm = context.newRecord(TRIAL_MEASURES);
			pm.setTrialId(trial.getId());
			pm.setMeasureId(measure.getId());
			pm.setValue(Double.toString(value));
			pm.setCreatedOn(new Timestamp(System.currentTimeMillis()));
			pm.store();
		}
	}

	private void writePlotMeasure(DSLContext context, PlotsRecord plot, MeasuresRecord measure, String value)
	{
		if (!StringUtils.isBlank(value))
		{
			PlotMeasuresRecord pm = context.newRecord(PLOT_MEASURES);
			pm.setPlotId(plot.getId());
			pm.setMeasureId(measure.getId());
			pm.setValue(value);
			pm.setCreatedOn(new Timestamp(System.currentTimeMillis()));
			pm.store();
		}
	}

	private void writeTrialMeasure(DSLContext context, TrialsRecord trial, MeasuresRecord measure, String value)
	{
		if (!StringUtils.isBlank(value))
		{
			TrialMeasuresRecord pm = context.newRecord(TRIAL_MEASURES);
			pm.setTrialId(trial.getId());
			pm.setMeasureId(measure.getId());
			pm.setValue(value);
			pm.setCreatedOn(new Timestamp(System.currentTimeMillis()));
			pm.store();
		}
	}

	private void writePlotMeasureDate(DSLContext context, PlotsRecord plot, MeasuresRecord measure, Date value)
	{
		if (value != null)
		{
			PlotMeasuresRecord pm = context.newRecord(PLOT_MEASURES);
			pm.setPlotId(plot.getId());
			pm.setMeasureId(measure.getId());
			pm.setValue(sdfDatabase.format(value));
			pm.setCreatedOn(new Timestamp(System.currentTimeMillis()));
			pm.store();
		}
	}

	private void writeTrialMeasureDate(DSLContext context, TrialsRecord trial, MeasuresRecord measure, Date value)
	{
		if (value != null)
		{
			TrialMeasuresRecord pm = context.newRecord(TRIAL_MEASURES);
			pm.setTrialId(trial.getId());
			pm.setMeasureId(measure.getId());
			pm.setValue(sdfDatabase.format(value));
			pm.setCreatedOn(new Timestamp(System.currentTimeMillis()));
			pm.store();
		}
	}

	private Double getDouble(Cell cell)
	{
		try
		{
			if (cell.getCellType() == CellType.BLANK)
				return null;
			return cell.getNumericCellValue();
		}
		catch (Exception e)
		{
			try
			{
				String value = cell.getStringCellValue();
				return Double.parseDouble(value);
			}
			catch (Exception e1)
			{
				return null;
			}
		}
	}

	private Integer getInteger(Cell cell)
	{
		try
		{
			if (cell.getCellType() == CellType.BLANK)
				return null;
			return (int) cell.getNumericCellValue();
		}
		catch (Exception e)
		{
			try
			{
				String value = cell.getStringCellValue();
				return Integer.parseInt(value);
			}
			catch (Exception e1)
			{
				return null;
			}
		}
	}

	private Date getDateTime(Cell cell)
	{
		try
		{
			if (cell.getCellType() == CellType.BLANK)
				return null;
			return cell.getDateCellValue();
		}
		catch (Exception e)
		{
			try
			{
				return sdfDateTime.parse(cell.getStringCellValue());
			}
			catch (Exception e1)
			{
				return null;
			}
		}
	}

	private Date getDate(Cell cell)
	{
		try
		{
			if (cell.getCellType() == CellType.BLANK)
				return null;
			return cell.getDateCellValue();
		}
		catch (Exception e)
		{
			try
			{
				return sdfDate.parse(cell.getStringCellValue());
			}
			catch (Exception e1)
			{
				return null;
			}
		}
	}

	private String getOptional(Cell cell)
	{
		try
		{
			String value = cell.getStringCellValue();

			if (StringUtils.isBlank(value) || value.equalsIgnoreCase("not recorded") || value.equalsIgnoreCase("none") || value.equalsIgnoreCase("no data") || value.equalsIgnoreCase("no"))
			{
				return null;
			}
			else
			{
				return value.trim();
			}
		}
		catch (Exception e)
		{
			return null;
		}
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@Accessors(chain = true)
	@ToString
	private static class CPRData
	{
		private Double sowingRateMono;
		private Double sowingRateMix;
		private Double yieldMono;
		private Double yieldMix;
	}
}
