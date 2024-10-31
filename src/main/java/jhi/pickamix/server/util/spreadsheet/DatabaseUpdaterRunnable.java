package jhi.pickamix.server.util.spreadsheet;

public class DatabaseUpdaterRunnable implements Runnable
{
	@Override
	public void run()
	{
		DatabaseUpdater.fromSpreadsheet();
	}
}
