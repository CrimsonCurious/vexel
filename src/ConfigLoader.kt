// ============================================================
//						ConfigLoader.kt
// ============================================================
package app.pie.vexel

fun loadConfig() {
    val configFile = when {
    	checkFile(Vexel.Config.file) -> Vexel.Config.file
    	checkFile(Vexel.Config.fileFallBack) -> Vexel.Config.fileFallBack
    	else -> null
    }
    
    val config = if (configFile != null) { 
    	loadConfig(configFile)
    } else { mutableMapOf<String, MutableMap<String, Any>>() }
    
    val tools = config["tools"] ?: mutableMapOf()
	val java = config["java"] ?: mutableMapOf()
	val log = config["logs"] ?: mutableMapOf()

	fun Map<*, *>?.string(key: String): String? =
		(this?.get(key) as? String) ?.takeUnless { it.equals("default", ignoreCase = true) }
		
	fun Map<String, Any>.boolean(key: String): Boolean? =
		this[key] as? Boolean
		
	Vexel.Config.apply {
    	aapt2 = tools.string("aapt2")
    	zipalign = tools.string("zipalign")
    	sdk = tools.string("androidJar")
    	ndk = tools.string("ndk")
    	javaHome = java.string("home") ?: System.getenv("JAVA_HOME")
    	enableColor = log.boolean("enableColor") ?: false
    	enableTimeStamp = log.boolean("enableTimeStamp") ?: false
    	enableDebugMode = log.boolean("enableDebugMode") ?: false
	}
}