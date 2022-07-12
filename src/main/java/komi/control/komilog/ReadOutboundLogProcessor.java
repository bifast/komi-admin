package komi.control.komilog;

import java.io.File;
import java.io.FileFilter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReadOutboundLogProcessor implements Processor{
	@Autowired ReadLogFile readLogfile;
	
	@Value("${outbound.log.dir}")
	String outboundlogdir;
	
    private String sysdate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    private String tgl;

	FileFilter logFilefilter = new FileFilter() {
		public boolean accept(File pathname) {
			if (pathname.getName().equals("komi-outbound.log")) 
				return true;
			if ((pathname.getName().startsWith("komi-outbound.log." + sysdate + ".")) &&
				(pathname.getName().endsWith(".gz")) )
				return true;
			
			return false;
		}
	};

	FileFilter gzFilefilter = new FileFilter() {
		public boolean accept(File pathname) {
			if (pathname.getName().startsWith("komi-outbound.log." + tgl))
				if (pathname.getName().endsWith(".gz"))
					return true;
			return false;
		}
	};
	
	
	@Override
	public void process(Exchange exchange) throws Exception {
		
		try {
            this.tgl = exchange.getMessage().getHeader("tanggal", String.class);
			if (null != tgl) 
				LocalDate.parse(tgl);
        }
        catch (DateTimeParseException e) {
            exchange.getMessage().setBody("<--- Format tanggal salah --->");
            return;
        }

        File logDir = new File(outboundlogdir);
        
        StringBuffer sb = new StringBuffer();
        List<File> files = new ArrayList<File>();

    	FileFilter fileFilter = null;
        if ((null == tgl) || (sysdate.equals(tgl)))
        	fileFilter = logFilefilter;
        else 
        	fileFilter = gzFilefilter;
     
        for (File f : logDir.listFiles(fileFilter)) 
        		files.add(f);

        Comparator<File> cmp = (o1, o2) -> {
    		int k = 1;
    		if ((o1.getName().endsWith("log")) || (o2.getName().endsWith("log")))
    			k = -1;
    		return o1.getName().compareTo(o2.getName()) * k;
        };
        Collections.sort(files, cmp);
      
        if (files.size()==0) 
        	sb.append("<--- Logfile tidak ditemukan --->");
        
        for (File f : files) 
        	sb.append(readLogfile.read(f));

        exchange.getMessage().setBody(sb);
	}

	
}
