import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import standalones.Point;

public class Candidate {
	
	String ra;
	String dec;
	String pngFileName;
	String pointLine; 
	String candidateLine;
	String bs;
	PDMP pmdp;
	
	boolean init;
	
	Point point;
	
	static Map<String, List<String> > pointsMap = new HashMap<>();
	
	public Candidate(){}
	
	
	
	public Candidate(String pngFileName, PDMP pdmp){
		
		this.pmdp = pdmp;
		this.pngFileName = pngFileName;
		
		System.err.println("Loading candidate: " + pngFileName);
		
		if(!pngFileName.contains(".")) return;
		
		String chunks[] = pngFileName.split("\\.")[0].split("_");
		
		if(chunks.length!=3) return;
		
		bs = chunks[1];
		
		Integer lineNumber = Integer.parseInt(chunks[2]);
		
		/**
		 * The string should be of the form HH:MM:SS+/-DD:MM:SS
		 */
		
		char sign = pngFileName.contains("+")? '+' : '-';
		
		String raStr = pngFileName.substring(0,pngFileName.indexOf(sign));
		String decStr = pngFileName.substring(pngFileName.indexOf(sign),pngFileName.indexOf("_"));
		
		String ra = raStr.substring(0,2) + ":" + raStr.substring(2,4) + ":" + raStr.substring(4,raStr.length());
		String dec = decStr.substring(0,3) + ":" + decStr.substring(3,5) 
						+ ":" + decStr.substring(5, decStr.length());

		if(dec.contains("_")){
			dec = dec.substring(0, dec.indexOf("_"));
		}

		System.err.println("Getting point for ra = " +ra + " dec = " + dec);
		
		candidateLine = null; 
		
		for(String s: pointsMap.get(bs)){
			
			if( s.contains(ra) && s.contains(dec) ) candidateLine = s;
		}
		
		if(candidateLine == null){
			System.err.println(" *******************Warning invalid PNG: " + pngFileName + "**********************");
			return;
		}
		
		System.err.println("candidate line: " + candidateLine);
		
		point = new Point(candidateLine);
		
		init=true;
	
	}

	public static void loadMap(File utcDir){
		
		pointsMap.clear();
		
		File pointsFiles[] = utcDir.listFiles( new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				
				return name.startsWith(dir.getName()) && name.endsWith(".pts");
			}
		});
		
		for(File f: pointsFiles){
			
			String bs = f.getName().split("\\.")[1];
			
			try {
				List<String> list = Files.readAllLines(f.toPath(), Charset.defaultCharset() );
				
				pointsMap.put(bs, list);
				
			} catch (IOException e) {
				e.printStackTrace();
			}

			
			
		}
		
		
	}

	public String getRa() {
		return ra;
	}

	public void setRa(String ra) {
		this.ra = ra;
	}

	public String getDec() {
		return dec;
	}

	public void setDec(String dec) {
		this.dec = dec;
	}

	public String getPngFileName() {
		return pngFileName;
	}

	public void setPngFileName(String pngFileName) {
		this.pngFileName = pngFileName;
	}

	public String getPointLine() {
		return pointLine;
	}

	public void setPointLine(String pointLine) {
		this.pointLine = pointLine;
	}

	public String getCandidateLine() {
		return candidateLine;
	}

	public void setCandidateLine(String candidateLine) {
		this.candidateLine = candidateLine;
	}

	public String getBs() {
		return bs;
	}

	public void setBs(String bs) {
		this.bs = bs;
	}

	public boolean isInit() {
		return init;
	}

	public void setInit(boolean init) {
		this.init = init;
	}

	public Point getPoint() {
		return point;
	}

	public void setPoint(Point point) {
		this.point = point;
	}

	public static Map<String, List<String>> getPointsMap() {
		return pointsMap;
	}

	public static void setPointsMap(Map<String, List<String>> pointsMap) {
		Candidate.pointsMap = pointsMap;
	}



	public PDMP getPmdp() {
		return pmdp;
	}



	public void setPmdp(PDMP pmdp) {
		this.pmdp = pmdp;
	}
	
	
}
