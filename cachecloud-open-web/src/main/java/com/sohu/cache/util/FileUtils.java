package com.sohu.cache.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.sohu.cache.protocol.MachineProtocol;

public class FileUtils {
	public static void copyStreamToFile(InputStream in, File file)
			throws IOException {
		BufferedInputStream bfIn = new BufferedInputStream(in);
		FileOutputStream output = new FileOutputStream(file);
		try {
			byte b;
			while (true) {
				b = (byte) in.read();
				if (b == -1) {
					break;
				}
				output.write(b);
			}
			output.flush();
		} finally {
			in.close();
			output.close();
		}

	}

	public static void createTmpScript(File tmpFile, String resourceName)
			throws Exception {
		Resource resource = new ClassPathResource(resourceName);
		if (!resource.exists()) {
			throw new Exception("Can't find the file " + resourceName);
		}

		File tmpDir = new File(MachineProtocol.TMP_DIR);
		if (!tmpDir.exists()) {
			if (!tmpDir.mkdirs()) {
				throw new Exception("cannot create " + MachineProtocol.TMP_DIR
						+ " directory.");
			}
		}

		if (!tmpFile.createNewFile()) {
			throw new Exception("cannot create " + tmpFile.getPath() + " .");
		}

		FileUtils.copyStreamToFile(resource.getInputStream(), tmpFile);
	}

}
