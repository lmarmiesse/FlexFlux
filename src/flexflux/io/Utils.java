package flexflux.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Utils {

	
	/**
	 * Copy a file that is in the project in a directory outside the project
	 * @param path : path of the project file to copy
	 * @param directoryPath : destination path
	 * @param newName : name of the new file
	 * @return the absolute path of the new file
	 * @throws IOException
	 */
	public static String copyProjectResource(String path, String directoryPath, String newName) throws IOException
	{
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();		
		
		InputStream stream = classLoader.getResourceAsStream(path);
		
		File directory = new File(directoryPath);
		
		File file = new File(directory+"/"+newName);
		
		OutputStream outStream = new BufferedOutputStream(new FileOutputStream(file, false));
		byte[] bucket = new byte[32*1024];
        int bytesRead = 0;
        while(bytesRead != -1){
          bytesRead = stream.read(bucket); //-1, 0, or more
          if(bytesRead > 0){
            outStream.write(bucket, 0, bytesRead);
          }
        }
		
        outStream.close();
        stream.close();
        
        return file.getAbsolutePath();
        
	}
	
	
	
}
