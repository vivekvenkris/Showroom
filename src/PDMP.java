import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.jastronomy.jsofa.JSOFA;
import org.jastronomy.jsofa.JSOFA.SphericalCoordinate;

import bean.Angle;
import exceptions.InvalidFileException;

public class PDMP{

	Double barycentricMJD;
	Double barycentricPeriod;
	Double errBarycentricPeriod;
	Double bestDM;
	Double errBestDM;
	Double bestPulseWidthinMilliSeconds;
	Double bestSNR;
	
	String fileName;
	String sourceName;

	Angle angleLAT;
	Angle angleLON;
	Angle angleRA;
	Angle angleDEC;
	
	String line;
	
	private PDMP(){}
	
	public static PDMP dummy(String file){
		PDMP dummy = new PDMP();
		dummy.setFileName(file);
		return dummy;
	}
	
	public PDMP(String line) throws InvalidFileException {
		
		String[] chunks = line.trim().split("\\s+");
		
		if(chunks.length < 10) { 
			throw new InvalidFileException("Invalid pdmp.posn file given. length needed >= 10, got = " +  chunks.length + " line = " + line + ""); 
		}
		
		this.line = line.trim();
		this.sourceName = chunks[0]	;
		
		this.angleLAT = new Angle(chunks[1], Angle.DEG);
		this.angleLON = new Angle(chunks[2], Angle.DEG);
		
		SphericalCoordinate sc = JSOFA.jauG2icrs(angleLON.getRadianValue(), angleLAT.getRadianValue());
		this.angleRA = new Angle(sc.alpha,Angle.HHMMSS);
		this.angleDEC = new Angle(sc.delta,Angle.DDMMSS);
		
		this.bestSNR = chunks[3].contains("-nan")? Double.NaN : Double.parseDouble(chunks[3]);
		this.barycentricMJD = chunks[4].contains("-nan")? Double.NaN : Double.parseDouble(chunks[4]);
		this.barycentricPeriod = chunks[5].contains("-nan")? Double.NaN : Double.parseDouble(chunks[5]);
		this.errBarycentricPeriod = chunks[6].contains("-nan")? Double.NaN : Double.parseDouble(chunks[6]);
		this.bestDM = chunks[7].contains("-nan")? Double.NaN : Double.parseDouble(chunks[7]);
		this.errBestDM = chunks[8].contains("-nan")? Double.NaN : Double.parseDouble(chunks[8]);
		
		this.fileName = chunks[9];
				
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( obj instanceof PDMP && ((PDMP)obj).getFileName().equals(this.fileName)) return true;
		return false;
	}
	
	public static void main(String[] args) throws IOException {
		Files.lines(Paths.get("/nfs/smirf/results/SMIRF_1826-1447/2017-08-18-10:28:12/cars/pdmp.posn"))
		.forEach(f -> {
			try {
				new PDMP(f);
			} catch (InvalidFileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}

	public Double getBarycentricMJD() {
		return barycentricMJD;
	}

	public void setBarycentricMJD(Double barycentricMJD) {
		this.barycentricMJD = barycentricMJD;
	}

	public Double getBarycentricPeriod() {
		return barycentricPeriod;
	}

	public void setBarycentricPeriod(Double barycentricPeriod) {
		this.barycentricPeriod = barycentricPeriod;
	}

	public Double getErrBarycentricPeriod() {
		return errBarycentricPeriod;
	}

	public void setErrBarycentricPeriod(Double errBarycentricPeriod) {
		this.errBarycentricPeriod = errBarycentricPeriod;
	}

	public Double getBestDM() {
		return bestDM;
	}

	public void setBestDM(Double bestDM) {
		this.bestDM = bestDM;
	}

	public Double getErrBestDM() {
		return errBestDM;
	}

	public void setErrBestDM(Double errBestDM) {
		this.errBestDM = errBestDM;
	}

	public Double getBestPulseWidthinMilliSeconds() {
		return bestPulseWidthinMilliSeconds;
	}

	public void setBestPulseWidthinMilliSeconds(Double bestPulseWidthinMilliSeconds) {
		this.bestPulseWidthinMilliSeconds = bestPulseWidthinMilliSeconds;
	}

	public Double getBestSNR() {
		return bestSNR;
	}

	public void setBestSNR(Double bestSNR) {
		this.bestSNR = bestSNR;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getSourceName() {
		return sourceName;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	public Angle getAngleLAT() {
		return angleLAT;
	}

	public void setAngleLAT(Angle angleLAT) {
		this.angleLAT = angleLAT;
	}

	public Angle getAngleLON() {
		return angleLON;
	}

	public void setAngleLON(Angle angleLON) {
		this.angleLON = angleLON;
	}

	public Angle getAngleRA() {
		return angleRA;
	}

	public void setAngleRA(Angle angleRA) {
		this.angleRA = angleRA;
	}

	public Angle getAngleDEC() {
		return angleDEC;
	}

	public void setAngleDEC(Angle angleDEC) {
		this.angleDEC = angleDEC;
	}

	public String getLine() {
		return line;
	}

	public void setLine(String line) {
		this.line = line;
	}
	
	
	
	
}
