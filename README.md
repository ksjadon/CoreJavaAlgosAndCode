# To Run this program you need to set the JVM memory first by setting -Xms200m and -Xmx200M parameter. 
# Your in memory processing of file depends on the Xms and Xmx parameter. 
# For example if you set the initial and max memory to 200MB then this program will split the input file(which i supposed to big) into 45 to 50 MB each and apply sorting on it. 
# Once the input files splitted,sorted and saved in temp directory, next program will merge all these files and during merging it's use PriorityQueue & BinaryFileBuffer to sort the files.

// Calculate the available memory by using below function.
public static double currentAvailableMemory() {
		System.gc();
		Runtime r = Runtime.getRuntime();		
		long allocatedMemory = r.totalMemory() - r.freeMemory();
		float presFreeMemory = (float) ((r.maxMemory() - allocatedMemory) / TO_GB);
		return presFreeMemory;
	}
  
  
  // Below method will return the size of read line from file.
  public static long sizeOfTheReadLine(String s) {

		int objHeader = 16;
		int arrayHeader = 24;
		int objRef = 8;
		int initFields = 12;
		return (s.length() * 2) + objHeader + arrayHeader + objRef + initFields;
	}
