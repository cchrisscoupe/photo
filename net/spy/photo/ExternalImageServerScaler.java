// Copyright (c) 1999 Dustin Sallings <dustin@spy.net>
// $Id: ExternalImageServerScaler.java,v 1.4 2003/07/26 08:38:27 dustin Exp $

package net.spy.photo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

import java.util.Random;

/**
 * Get an image scaler that scales via an external program.
 */
public class ExternalImageServerScaler extends ImageServerScaler {

	/**
	 * Get it.
	 */
	public ExternalImageServerScaler() {
		super();
	}

	private String dumpIS(InputStream s) {
		String out="";
		try {
			byte b[]=new byte[s.available()];

			s.read(b);
			out=new String(b);
		} catch(Exception e) {
			log("Error dumping InputStream:  " + e);
			e.printStackTrace();
		}
		out=out.trim();
		if(out.length() < 1) {
			out=null;
		}
		return(out);
	}

	/**
	 * Scale an image with an external command.
	 */
	public PhotoImage scaleImage(PhotoImage in, PhotoDimensions dim)
		throws Exception {

		Random r = new Random();
		String part = "/tmp/image." + Math.abs(r.nextInt());
		String thumbfilename = part + ".tn." + in.getFormatString();
		String tmpfilename=part + "." + in.getFormatString();
		byte b[]=null;

		// OK, got our filenames, now let's calculate the new size:
		PhotoDimensions imageSize=new PhotoDimensionsImpl(in.getWidth(),
			in.getHeight());
		PhotoDimScaler pds=new PhotoDimScaler(imageSize);
		PhotoDimensions newSize=pds.scaleTo(dim);

		FileInputStream fin=null;

		try {
			// Need these for the process.
			InputStream stderr=null;
			InputStream stdout=null;
			String tmp=null;

			// Make sure we have a defined convert command.
			tmp=getConf().get("scaler.convert.cmd");
			if(tmp==null) {
				throw new Exception("No convert command has been defined!");
			}

			// Write the image data to a temporary file.
			FileOutputStream f = new FileOutputStream(tmpfilename);
			f.write(in.getData());
			f.flush();
			f.close();

			String command=tmp + " -geometry "
				+ newSize.getWidth() + "x" + newSize.getHeight()
				+ " " + tmpfilename + " " + thumbfilename;
			log("Running " + command);
			Runtime run = Runtime.getRuntime();
			Process p = run.exec(command);
			stderr=p.getErrorStream();
			stdout=p.getInputStream();
			p.waitFor();

			// Process status.
			if(p.exitValue()!=0) {
				log("Exit value was " + p.exitValue());
			}
			tmp=dumpIS(stderr);
			if(tmp!=null) {
				log("Stderr was as follows:\n" + tmp);
				log("------");
			}
			tmp=dumpIS(stdout);
			if(tmp!=null) {
				log("Stdout was as follows:\n" + tmp);
				log("------");
			}

			File file=new File(thumbfilename);
			b=new byte[(int)file.length()];

			fin = new FileInputStream(file);

			fin.read(b);

		} catch(Exception e) {
			log("Error scaling image:  " + e);
			throw e;
		} finally {
			try {
				if(fin != null) {
					fin.close();
				}
				File f = new File(tmpfilename);
				f.delete();
				f = new File(thumbfilename);
				f.delete();
			} catch(IOException e2) {
				// No need to do anything, that's just cleanup.
			}
		}
		return(new PhotoImage(b));
	}
}
