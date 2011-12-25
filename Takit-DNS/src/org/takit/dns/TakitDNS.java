package org.takit.dns;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.takit.dns.services.Afraid;
import org.takit.dns.services.Dyn;

public class TakitDNS extends JavaPlugin {
	public static final String FREEDNS_AFRAID_ORG = "freedns.afraid.org";
	public static final String DYNS_DNS = "dyn.com";
	
	public static Logger log = Logger.getLogger("Minecraft");
	
	private String username;
	private String password;
	private String domain;
	private long interval;
	private String host;
	
	public void onDisable() {
		log.info(String.format(Messages.PLUGIN_DISABLE, getDescription().getName()));
	}
	public void onEnable() {
		initConfig();
		
		host = host.toLowerCase();
		if ( host.equals(FREEDNS_AFRAID_ORG) ) {
			this.getServer().getScheduler().scheduleAsyncRepeatingTask(
				this, 
				new Afraid(this, username, password, domain),
				(interval*20)*60,
				(interval*20)*60
			);
		}
		else if ( host.equals(DYNS_DNS) ) {
			this.getServer().getScheduler().scheduleAsyncRepeatingTask(
					this, 
					new Dyn(this, username, password, domain),
					(interval*20)*60,
					(interval*20)*60
				);
		}
		else {
			log.log(Level.WARNING, String.format(
				Messages.HOST_NOT_FOUND, 
				getDescription().getName(),
				host
			));
		}
		
		log.info(String.format(
				Messages.PLUGIN_ENABLE, 
				getDescription().getName()
		));
	}
	
	
	public void initConfig() {
		File file = new File("plugins"+File.separator+"Takit"+File.separator+"dns.yml");
		FileConfiguration config = YamlConfiguration.loadConfiguration(file);
		
		if ( !file.exists() ) {
			config.set("dns.domain", "your-domain");
			config.set("dns.username", "username");
			config.set("dns.password", "password");
			config.set("dns.interval", 10);
			config.set("dns.host", "dns-service");
			try {
				config.save(file);
				config.load(file);
			}
			catch ( Exception e ) {
				e.printStackTrace();
			}
		}
		
		domain = config.getString("dns.domain");
		username = config.getString("dns.username");
		password = config.getString("dns.password");
		interval = config.getInt("dns.interval");
		host = config.getString("dns.host");
	}
	
	public static String getIP() {
		String ret = getURL("http://checkip.dyndns.com/");
		if ( ret!=null ) {
			ret = ret.substring(ret.indexOf("Current IP Address: ")+20, ret.indexOf("</body>"));
		}
		return ret;
	}
	public static String getURL(String url) {
		try {
			int needsAuthentication = url.indexOf("@");
			if ( needsAuthentication>-1 ) {
				int start = url.indexOf("://")+3;
				int startOfPassword = url.indexOf(":", start);
				
				final String username = url.substring(start, startOfPassword);
				final String password = url.substring(startOfPassword+1, needsAuthentication);
				Authenticator.setDefault(new Authenticator() {
				    protected PasswordAuthentication getPasswordAuthentication() {
				        return new PasswordAuthentication (username, password.toCharArray());
				    }
				});
				
				url = url.substring(0, start) + url.substring(needsAuthentication+1);
			}
			
			URL u = new URL(url);
			URLConnection conn = u.openConnection();
			conn.setRequestProperty("User-Agent", "org.takit.dns.TakitDNS/0.2");
			BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
			Reader in = new InputStreamReader(bis);
			StringWriter fileContents = new StringWriter();
			for (int b; (b = in.read()) != -1;) {
				fileContents.write(b);
	        }
			bis.close();
			
			return ((StringWriter)fileContents).getBuffer().toString();
		}
		catch ( Exception ignore ) { ignore.printStackTrace(); }
		
		return null;
	}
}
