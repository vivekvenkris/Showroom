import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import exceptions.InvalidFileException;
import service.EphemService;

public class Consolidator {
public static void main(String[] args) {
	File root = new File("/nfs/smirf/results/");
	
	File[] pointings = root.listFiles(new FilenameFilter() {
		
		@Override
		public boolean accept(File dir, String name) {
			return dir.isDirectory() && (name.startsWith("SMIRF_") || name.startsWith("J"));
		}
	});
	String out = "";

	for(File pointing: pointings){
		
		//System.err.println("Considering " + pointing.getName());
		File[] utcs = pointing.listFiles();

		for(File utcDir: utcs){
			if(utcDir.isDirectory()){
				String utc = utcDir.getName();
				if(EphemService.getMJDForUTC(utc + ".000") < EphemService.getMJDForUTC("2017-06-15-00:00:00.000")){
					File carsDir = new File(utcDir,"cars");
					File pdmpPosn = new File(carsDir, "pdmp.posn");
					if(!pdmpPosn.exists()) System.err.println("Skipping " + pointing.getName() + " " + utc );
					if(pdmpPosn.exists()){
						Stream<String> pdmpPosnStream = null;
						try {
							pdmpPosnStream = Files.lines(Paths.get(pdmpPosn.getAbsolutePath()));
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
							return;
						}
						List<PDMP> pdmpList = new ArrayList<>();

						
						pdmpList = pdmpPosnStream.map(f -> {
							try {
								PDMP p = new PDMP(f);
								String pulsar = PulsarGuesser.guessPulsar(p);
								if(pulsar.contains("None")) return null;
								p.setGuess(pulsar);
								return p;
								
							} catch (InvalidFileException e) {
								e.printStackTrace();
								return null;
							}
						}).filter(f -> f!=null).collect(Collectors.toList());
						//System.err.println("****************************");
						//System.err.println(utc + ":");
						System.err.println(pointing.getName() + " " + pdmpList.size());
						for (PDMP pdmp : pdmpList) {
							out +=(utcDir.getAbsolutePath() + "/cars/"+ pdmp.getFileName() + " " + pdmp.getBestSNR() + " " + pdmp.getGuess()) + "\n" ;
						}
						
						
						
						
						
						pdmpPosnStream.close();
						
						
						
					}
				}
			}
		}
		
	}
	
	//System.err.println("out:" + out);
	try {
		Files.write(Paths.get("/home/vivek/smirf0.cands"), out.getBytes());
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}
}
