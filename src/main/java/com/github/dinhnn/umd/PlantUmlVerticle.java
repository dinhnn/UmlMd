package com.github.dinhnn.umd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

public class PlantUmlVerticle extends AbstractVerticle {
	private final static Logger LOG = LoggerFactory.getLogger(PlantUmlVerticle.class);
	private final static String DEFAULT_MIME = "application/octet-stream";
	private String webRoot;
	private Map<String,String> extensionMimetype;
	@Override
	public void start(Future<Void> startFuture) throws Exception {
		System.setProperty("GRAPHVIZ_DOT","vizjs");
		webRoot = config().getString("www","www");
		int port = config().getInteger("port",8888);
		extensionMimetype = new HashMap<>();
		extensionMimetype.put("html","text/html");
		extensionMimetype.put("text","text/plain");
		extensionMimetype.put("js","application/javascript");
		extensionMimetype.put("css","text/css");
		extensionMimetype.put("jpg","image/jpeg");
		extensionMimetype.put("png","image/png");
		extensionMimetype.put("icon","image/x-icon");
		JsonObject ext = config().getJsonObject("ext");
		if(ext!=null){
			for(String field:ext.fieldNames()){
				extensionMimetype.put(field, ext.getString(field));
			}
		}
		vertx.createHttpServer().requestHandler(this::service).listen(port, ar -> {
			if (ar.succeeded())
				startFuture.complete();
			else
				startFuture.fail(ar.cause());
		});
	}
	private void service(HttpServerRequest req){
		String path = req.path();
		switch(path){
		case "/svg":
			plantuml(req,FileFormat.SVG,"image/svg+xml",false);
			break;
		case "/jpg":
			plantuml(req,FileFormat.MJPEG,"image/jpeg",false);
			break;
		case "/png":
			plantuml(req,FileFormat.PNG,"image/png",false);
			break;
		case "/text":
			plantuml(req,FileFormat.ATXT,"image/svg+xml",true);
			break;
		default:
			staticFile(req,path);
		}
	}
	private void staticFile(HttpServerRequest req,String path){
		String ext = path.substring(path.lastIndexOf('.')+1).toLowerCase();
		req.response().putHeader(HttpHeaders.CONTENT_TYPE,extensionMimetype.getOrDefault(ext,DEFAULT_MIME)).sendFile(webRoot+path);
	}
	private void plantuml(HttpServerRequest req,FileFormat format,String mimeType,boolean js) {
		String uml = req.query();
		if(uml==null){
			req.response().setStatusCode(404).end();
			return;
		}
		try{
			uml = URLDecoder.decode(uml,"UTF-8").replace(';','\n');
		}catch(UnsupportedEncodingException e){
			LOG.error(e);
		}
    if(!uml.startsWith("@startuml\n")){
        uml="@startuml\n"+uml;
    }
    if(!uml.startsWith("\n@enduml")){
        uml=uml+"\n@enduml";
    }
    SourceStringReader reader = new SourceStringReader(uml);
    vertx.<Buffer>executeBlocking(fut->{
    	BufferOutputStream out = new BufferOutputStream();
    	try{
	      reader.generateImage(out, new FileFormatOption(format, false));
	      if(js){
					String text = out.getBuff().toString(StandardCharsets.UTF_8);
					BufferedReader br = new BufferedReader(new StringReader(text));
					String line;
					int width = 0;
					int lines = 0;
					while ((line = br.readLine()) != null) {
						width = Math.max(width, line.length());
						lines++;
					}
					width *= 8;
					lines *= 15;
					String svg = "<?xml version=\"1.0\" standalone=\"yes\"?><svg xmlns = \"http://www.w3.org/2000/svg\" width=\""
							+ width + "\" height=\"" + lines + "\" viewBox=\"0 0 " + width + " " + lines
							+ "\"><foreignObject x=\"0\" y=\"0\" width=\"" + width + "\" height=\"" + lines
							+ "\"><pre xmlns=\"http://www.w3.org/1999/xhtml\"><![CDATA[" + text + "]]></pre></foreignObject></svg>";
					fut.complete(Buffer.buffer(svg));
	      } else {
	        fut.complete(out.getBuff());
	      }
    	}catch(IOException e){
    		fut.fail(e);
    	}
    },ar->{
    	if(ar.succeeded()){
    		req.response().end(ar.result());
    	} else {
    		req.response().setStatusCode(500).end();
    	}
    });
	}
}
