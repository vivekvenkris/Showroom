import java.util.ArrayList;
import java.util.List;

import org.junit.experimental.max.MaxHistory;

import bean.Angle;
import bean.TBSourceTO;
import manager.PSRCATManager;
import util.Constants;
import util.Utilities;

public class PulsarGuesser {
	
	static Integer maxHarmonics = 16;
	
	public static String guessPulsar(PDMP pdmp){
		
		List<TBSourceTO> pulsars = PSRCATManager.getTbSources();
		
		List<String> shortlisted = new ArrayList<>();
		

		
		pulsars.stream().forEach(c -> {
			TBSourceTO f = (TBSourceTO)c;
			Double period = f.getP0() * 1000;
			Double DM = f.getDM();
			Angle ra = f.getAngleRA();
			Angle dec = f.getAngleDEC();
			
			if(period == null || DM == null) return; 
			
			boolean closePeriod = false;
			
			String s = f.getPsrName() + " ";
			
			boolean sub = pdmp.getBarycentricPeriod() < period;
			
			double ratio = sub? period/pdmp.getBarycentricPeriod():pdmp.getBarycentricPeriod()/period;
			int intRatio = (int)Math.round(ratio);
			if( intRatio < maxHarmonics  && Math.abs(Math.abs(ratio - intRatio)/ratio) * 100 < 0.01) {
				
				double fundamental = pdmp.getBarycentricPeriod() ;
				
				double errFundamental = pdmp.getErrBarycentricPeriod();
				
				if( intRatio > 1) {
					
					 fundamental = sub? pdmp.getBarycentricPeriod() * ratio :  pdmp.getBarycentricPeriod() / ratio;
					
					 errFundamental = sub? pdmp.getErrBarycentricPeriod() * ratio : pdmp.getErrBarycentricPeriod() / ratio;
					 
					 //System.err.println("harmonic. changing fundamental to " + fundamental);
					
				}
				
				
					//s += ( " NH = " + (sub? "1/":"") + String.format(" %.4f", ratio)); 
					s += (  (sub? "1/":"") + String.format("%.4f", ratio));
					closePeriod = true;
				
				
				
			}
			
			boolean closeInDM = false;
			
			if( Math.abs(Math.abs(DM - pdmp.getBestDM())/pdmp.getErrBestDM()) < 5  || Math.abs(DM - pdmp.getBestDM()) < 100 ){
				//s += " DM:" + String.format("(%.3f - %.3f = %.3f)/%.3f = %.3f sigma", DM, pdmp.getBestDM(), Math.abs(DM - pdmp.getBestDM()), pdmp.getErrBestDM(),Math.abs(DM - pdmp.getBestDM())/pdmp.getErrBestDM());
				s += " " + String.format("%.2f %.2f %.2f %.2f",DM, pdmp.getBestDM(),pdmp.getErrBestDM(),Math.abs(DM - pdmp.getBestDM())/pdmp.getErrBestDM());
				closeInDM = true;
			}
			
			boolean  closeInSpace = false;
			
			if(Math.sqrt(Utilities.equatorialDistance(pdmp.getAngleRA().getRadianValue(), 
					pdmp.getAngleDEC().getRadianValue(), ra.getRadianValue(), dec.getRadianValue())) < 5.0 * Constants.deg2Rad) {
				closeInSpace = true; 
			}
				
			if(closePeriod && closeInDM && closeInSpace)shortlisted.add(s + " ");
			
			
			
			
		});
		
		String guess = "";
		for(String s: shortlisted)  guess += s;
		
		if(guess.equals("")) guess = "None.";
		
		return guess;
		
	}
	
	

}
