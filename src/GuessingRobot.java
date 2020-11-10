import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import exceptions.InvalidFileException;
import sun.net.www.content.image.png;

public class GuessingRobot {

	static PulsarGuesser guesser = new PulsarGuesser();
	public static void main(String[] args) throws IOException {
		String root = "/nfs/smirf/results";
		BufferedWriter bw = new BufferedWriter(new FileWriter("/home/vivek/guessed.list.withsnr.dm.cand"));
		File[] pointings = new File(root).listFiles(new FileFilter() {
			
			@Override
			public boolean accept(File pathname) {
				
				return pathname.isDirectory();
			}
		});
		
		
		for(File pointingDir: pointings) {
			
			System.err.println("Considering " + pointingDir.getName());
			
			File[] utcs = pointingDir.listFiles(new FileFilter() {
				
				@Override
				public boolean accept(File pathname) {
					
					return pathname.isDirectory() && pathname.getName().startsWith("201");
				}
			});
			
			for(File utcDir: utcs) {
				
				File carsDir = new File(utcDir, "cars");
				
				if(carsDir.exists()) {
					
					File pdmpPosn = new File(carsDir,"pdmp.posn");
					
					if(pdmpPosn.exists()) {
						
						Stream<String> pdmpPosnStream = null;
						try {
							pdmpPosnStream = Files.lines(Paths.get(pdmpPosn.getAbsolutePath()));
						} catch (IOException e1) {
							e1.printStackTrace();
							return;
						}
						
						pdmpPosnStream.forEach(f -> {
							try {
								PDMP pdmp = new PDMP(f);
								String name = pdmp.getFileName().replaceAll(".car", ".png");

								File pngFile = new File(carsDir,name);
								
								String guess = PulsarGuesser.guessPulsar(pdmp);
								
								if(!guess.equals("None.") && pngFile.exists()) {
									
									String utc = utcDir.getName();
									String chunks[] = pdmp.getFileName().replaceAll(".car", "").split("_");
									String shortlistFile=utc+".shortlisted."+chunks[1];
									Integer lineNUmber = Integer.parseInt(chunks[2]);
									String fftLine = Files.readAllLines(Paths.get(utcDir.getAbsolutePath() + "/" + shortlistFile)).get(lineNUmber+1);
									
									String s = pointingDir.getName() + " " + utcDir.getName() + " " + pdmp.getFileName() + " "+ pdmp.getBestSNR() + " " +  guess + " " + fftLine + "  " + pngFile.getAbsolutePath() + "\n";
									bw.write(s);
									System.err.println(s);
								}
								
								
							} catch (InvalidFileException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
						});
						pdmpPosnStream.close();
						
						
					}//if(pdmpPosn.exists())
					
				}//if(carsDir.exists())
				
			} //for(File utcDir: utcs) 
			bw.flush();
		}// for(File pointingDir: pointings)
		bw.close();
	} //main
} // class
