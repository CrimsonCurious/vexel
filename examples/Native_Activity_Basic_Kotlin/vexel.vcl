build: {
    app: {
		id: "com.example.nativec.kt"
        name: "Native Example"
        version: "1.0.0"
        buildCode: 1
    }

    sdk: {
        minSdk: 29
        targetSdk: 34
    }

    native: {
        enabled: true
		api: 29
		abi: [
			"arm64-v8a"
			"armeabi-v7a"
			"x86"
			"x86_64"
		]
		
		buildScript: "src/cpp/native.vcl"
	}
}