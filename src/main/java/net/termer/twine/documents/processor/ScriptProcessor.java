package net.termer.twine.documents.processor;

import java.util.HashMap;

import bsh.EvalError;
import bsh.Interpreter;
import net.termer.twine.Twine;
import net.termer.twine.documents.DocumentOptions;
import net.termer.twine.documents.DocumentProcessor;
import net.termer.twine.documents.Documents.Out;

/**
 * DocumentProcessor that runs embedded Java scripting
 * @author termer
 * @since 1.0
 */
public class ScriptProcessor implements DocumentProcessor {
	public void process(DocumentOptions ops) {
		String cont = ops.content();
		HashMap<String, Object> vars = new HashMap<String, Object>();
		
		// Check if scripting is enabled
		if((Boolean) Twine.config().get("scripting")) {
			if(cont.startsWith("<!--TES-->")) {
				cont = cont.substring(11);
				if(cont.startsWith("!")) cont = cont.substring(1);
				
				// Process scripts
				if(cont.contains("<?java") && cont.contains("?>")) {
					vars.put("domain", ops.domain());
					vars.put("name", ops.name());
					vars.put("request", ops.route().request());
					vars.put("response", ops.route().response());
					vars.put("route", ops.route());
					
					int index = 0;
					boolean proceed = true;
					Interpreter inter = new Interpreter();
					
					// Add variables
					String[] keys = vars.keySet().toArray(new String[0]);
					Object[] values = vars.values().toArray(new Object[0]);
					
					for(int i = 0; i < keys.length; i++) {
						try {
							inter.set(keys[i], values[i]);
						} catch (EvalError e) {
							ops.fail(e);
						}
					}
					
					// While there are more scripts available
					while(proceed) {
						int opening = cont.indexOf("<?java", index)+6;
						int closing = cont.indexOf("?>", index);
						if(opening > 5 && closing > -1) {
							String script = cont.substring(opening, closing);
							
							Out result = new Out(ops.domain());
							try {
								inter.set("out", result);
								inter.eval(script.trim());
							} catch (Exception e) {
								// Append error if enabled
								if((Boolean) Twine.config().get("scriptExceptions")) {
									result.append(e.getMessage());
								} else {
									ops.fail(e);
								}
							}
							cont = cont.replace("<?java"+script+"?>", result.toString());
							index++;
						} else {
							proceed = false;
						}
					}
				}
			}
		}
	}
}
