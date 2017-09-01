package com.chbrown13.pull_rec;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * The Utils class contains various helper methods and variables for the PullRecommender project.
 */
public class Utils {

	public static String BASE_COMMENT = "You fixed the following error in this Pull Request:\n{fixed}\nGoogle's Error Prone static analysis tool can be used to find more issues similar to this{errors}Check out http://errorprone.info for more information.";

	public static String ERROR_PRONE_CMD = "java -Xbootclasspath/p:error_prone_ant-2.1.0.jar com.google.errorprone.ErrorProneCompiler {file}";
	
	/**
	 * Download contents of a file from a web url, similar to wget command
	 *
	 * @param fileUrl   String url of the file to download
	 * @param output    Name of file to store contents
	 */
	public static void wgetFile(String fileUrl, String output) {
		String s;

		try {
			URL url = new URL(fileUrl);
			InputStream in = url.openStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			BufferedWriter out = new BufferedWriter(new FileWriter(output));
			while ((s = br.readLine()) != null) {
				out.write(s+"\n");
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Retrieve Github username and password from a file. Expects file to contain the username on the first line and password on the second line.
	 *
	 * @param filename   Name of credentials file
	 * @return           Array containing the Github username and password
	 */
	public static String[] getCredentials(String filename) {
		String[] creds = new String[2];
		File file = new File(filename);
		try {
			int i = 0;
		    Scanner sc = new Scanner(file);
		    while (i < 2) {
		        creds[i] = sc.nextLine();
				i++;
		    }
		    sc.close();
		} 
		catch (FileNotFoundException e) {
		    e.printStackTrace();
		}
		return creds;
	}
}
