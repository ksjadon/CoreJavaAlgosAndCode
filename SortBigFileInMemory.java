package com.mani.bigfile.sorting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

public class SortBigFileInMemory {
	static Double TO_GB = (double) (1024 * 1024 * 1024);
	static int MAX_TEMP_FILE = 10;
	static long MAX_MEMORY = Runtime.getRuntime().maxMemory();

	public static void main(String[] args) {

		// System.out.println("FreeBefor>>:" + currentAvailableMemory());
		try {

			File inputFile;
			// Write the sorted data in that file.
			File outFile = new File("c:\\big\\a.txt");
			if (!outFile.exists()) {
				System.out.println("Creating new File");
				outFile.createNewFile();
			}

			inputFile = new File("C:/big/BigFile_3.txt");

			BufferedReader fbr = new BufferedReader(new FileReader(inputFile));
			long blockSize = bestSizeOfMemoryBlock(inputFile.length());
			System.out.println(blockSize);
			List<File> files = new ArrayList<>();
			// Temporary Directory to save to individual sorted files
			File tempDirectory = new File("c:/big/split/");

			try {
				List<String> tmplist = new ArrayList<>();
				String line = "";
				try {

					while (line != null) {
						long currentblocksize = 0;// in bytes
						while ((currentblocksize < blockSize) && ((line = fbr.readLine()) != null)) {
							tmplist.add(line);
							currentblocksize += sizeOfTheReadLine(line);

						}
						files.add(sortFileAndSaveInTempDir(tmplist, tempDirectory));
						System.out.println(files.size());
						tmplist.clear();
					}
					// Finally merge the sorted files and write in given
					// outFile.
					mergeFilesAfterSortingAndSaveInNewfile(files, outFile);
				} catch (EOFException oef) {

				} catch (Exception e) {
					e.printStackTrace();
				}
			} finally {
				fbr.close();
			}

		} catch (Exception e) {
			e.printStackTrace();

		}

	}

	public static File sortFileAndSaveInTempDir(List<String> tmplist, File tmpDirectory) throws IOException {

		tmplist = tmplist.parallelStream().sorted(lineComparator)
				.collect(Collectors.toCollection(ArrayList<String>::new));

		if (!tmpDirectory.exists())
			tmpDirectory.mkdirs();
		File newtmpfile = File.createTempFile("SortedFile", ".txt", tmpDirectory);
		newtmpfile.deleteOnExit();
		OutputStream out = new FileOutputStream(newtmpfile);
		BufferedWriter bufferWriter = null;
		try {
			bufferWriter = new BufferedWriter(new OutputStreamWriter(out));

			for (String r : tmplist) {
				bufferWriter.write(r);
				bufferWriter.newLine();
			}

		} catch (Exception e) {

			e.printStackTrace();
		} finally {
			if (bufferWriter != null)
				bufferWriter.close();
		}
		return newtmpfile;
	}

	public static void mergeFilesAfterSortingAndSaveInNewfile(List<File> files, File outFile) throws IOException {
		System.out.println("Merge and Sort");

		ArrayList<FileWrapper> buffers = new ArrayList<>();

		for (File f : files) {
			InputStream in = new FileInputStream(f);
			BufferedReader bufferReader = new BufferedReader(new InputStreamReader(in));
			FileWrapper fileBuffer = new FileWrapper(bufferReader);
			buffers.add(fileBuffer);
		}

		BufferedWriter bufferWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile, false)));

		PriorityQueue<FileWrapper> priorityQueue = new PriorityQueue<>(11, new Comparator<FileWrapper>() {
			@Override
			public int compare(FileWrapper i, FileWrapper j) {
				return lineComparator.compare(i.peek(), j.peek());
			}
		});

		for (FileWrapper buffer : buffers) {
			if (!buffer.empty()) {
				priorityQueue.add(buffer);
			}
		}
		try {

			while (priorityQueue.size() > 0) {
				FileWrapper binaryFileBuffer = priorityQueue.poll();
				String r = binaryFileBuffer.pop();
				bufferWriter.write(r);
				bufferWriter.newLine();
				if (binaryFileBuffer.empty()) {
					binaryFileBuffer.fbr.close();
				} else {
					priorityQueue.add(binaryFileBuffer); // add it back
				}
			}

		} finally {
			bufferWriter.close();
			for (FileWrapper fileBuffer : priorityQueue) {
				fileBuffer.close();
			}
		}
		for (File f : files) {
			f.delete();
		}

	}

	public static double currentAvailableMemory() {
		System.gc();
		Runtime r = Runtime.getRuntime();
		System.out.println("Total Memory :" + r.totalMemory() / TO_GB);
		System.out.println("Max Memory :" + r.maxMemory() / TO_GB);
		System.out.println("Free Memory :" + r.freeMemory() / TO_GB);

		long allocatedMemory = r.totalMemory() - r.freeMemory();
		float presFreeMemory = (float) ((r.maxMemory() - allocatedMemory) / TO_GB);
		return presFreeMemory;
	}

	public static long bestSizeOfMemoryBlock(final long fileSize) {

		long blocksize = fileSize / MAX_TEMP_FILE + (fileSize % MAX_TEMP_FILE == 0 ? 0 : 1);
		if (blocksize < MAX_MEMORY / 2) {
			blocksize = MAX_MEMORY / 2;
		}
		return blocksize;
	}

	public static Comparator<String> lineComparator = new Comparator<String>() {
		@Override
		public int compare(String r1, String r2) {
			return r1.compareTo(r2);
		}
	};

	public static long sizeOfTheReadLine(String s) {
		int objHeader = 16;
		int arrayHeader = 24;
		int objRef = 8;
		int initFields = 12;
		return (s.length() * 2) + objHeader + arrayHeader + objRef + initFields;
	}

}

final class FileWrapper {
	public FileWrapper(BufferedReader r) throws IOException {
		this.fbr = r;
		reload();
	}

	public void close() throws IOException {
		this.fbr.close();
	}

	public boolean empty() {
		return this.cache == null;
	}

	public String peek() {
		return this.cache;
	}

	public String pop() throws IOException {
		String answer = peek().toString();// make a copy
		reload();
		return answer;
	}

	private void reload() throws IOException {
		this.cache = this.fbr.readLine();
	}

	public BufferedReader fbr;

	private String cache;

}