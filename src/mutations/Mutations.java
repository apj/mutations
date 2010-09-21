package mutations;

import java.io.File;

import report2.JSONReportingProfileLoader;
import report2.ReportGenerator;
import report2.ReportingProfile;

public class Mutations
{
	public static void main(String[] args)
	{
		String reportingProfileFilePath = null;
		
		for(String arg : args)
		{
			if(arg.startsWith("-p"))
			{
				reportingProfileFilePath = arg.substring(3);
				break;
			}
		}
		
		ReportingProfile profile = null;
		
		if(reportingProfileFilePath != null)
		{
			File reportingProfileFile = new File(reportingProfileFilePath);
			
			if(reportingProfileFile.exists())
			{
				profile = loadProfileFromFile(reportingProfileFile);
			}
			else
			{
				// Throw an exception				
			}
		}
		else
		{
			System.out.println("No profile file specified");
		}
		
		// If the profile was loaded, run the reports
		if(profile != null)
			new ReportGenerator(profile).generateReports();
	}

	private static ReportingProfile loadProfileFromFile(File profileFile)
	{
		ReportingProfile profile = null;
		
		if(profileFile.getName().endsWith(".json"))
		{
			JSONReportingProfileLoader profileLoader = new JSONReportingProfileLoader();
			profile = profileLoader.loadReportingProfile(profileFile);
		}
		return profile;
	}
}