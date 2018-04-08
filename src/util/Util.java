package util;
import java.io.File;
import java.io.FileFilter;

import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

public interface Util {
	
	String pathSeparator = "/";
	
	String carsDotDriven = "cars.driven";
	
	Callback<ListView<File>, ListCell<File>> fileNameViewFactory = new Callback<ListView<File>,ListCell<File>>(){
	    @Override
	    public ListCell<File> call(ListView<File> l){
	    	
	        return new ListCell<File>(){
	        	
	            @Override
	            protected void updateItem(File file,boolean empty) {
	            	
	                super.updateItem(file, empty);
	                
	                if (file == null || empty) {
	                	
	                    setGraphic(null);
	                    
	                } else {
	                	
	                    setText(file.getName());
	                    
	                }
	            }
	        } ;
	    }
	
	};
	FileFilter directoryFileFilter = new FileFilter() {

		@Override
		public boolean accept(File pathname) {
			return pathname.isDirectory();
		}
		
		@Override
		public String toString(){
			return "directory filter";
		}
	};

	
	FileFilter pngFileFilter = new FileFilter() {

		@Override
		public boolean accept(File pathname) {
			return pathname.getName().endsWith(".png");
		}
		
		@Override
		public String toString(){
			return "PNG file filter";
		}
	};
	
	
	FileFilter birdieFileFilter = new FileFilter() {

		@Override
		public boolean accept(File pathname) {
			return pathname.getName().contains("birdies");
		}
		
		@Override
		public String toString(){
			return "Birdie file filter";
		}
	};
	FileFilter unprocessedPointingFilter = new FileFilter() {

		@Override
		public boolean accept(File pointing) {
			
			if(!pointing.isDirectory()) return false;
			
			File[] utcs = pointing.listFiles(unProcessedUTCFilter);
			
			return (utcs.length > 0);
		}
		
		@Override
		public String toString(){
			return "Unprocessed Pointing Filter";
		}
	};
	
	
	FileFilter processedPointingFilter = new FileFilter() {

		@Override
		public boolean accept(File pointing) {
			
			if(!pointing.isDirectory()) return false;
			
			File[] utcs = pointing.listFiles(processedUTCFilter);
			
			return (utcs.length > 0);
		}
		@Override
		public String toString(){
			return "unprocessed Pointing filter";
		}
	};
	
	
	FileFilter processedUTCFilter = new FileFilter() {

		@Override
		public boolean accept(File utc) {
			
			if(!utc.isDirectory()) return false;
								
			if(new File(utc,carsDotDriven ).exists()) return true;
			
			return false;
		}
		
		@Override
		public String toString(){
			return "processed UTC filter";
		}
	};
	
	
	FileFilter unProcessedUTCFilter = new FileFilter() {

		@Override
		public boolean accept(File utc) {
			
			if(!utc.isDirectory()) return false;
								
			if(new File(utc, carsDotDriven ).exists()) return false;
			
			return true;
		}
		
		@Override
		public String toString(){
			return "unprocessed UTC filter";
		}
	};
	
	
	
}
