#!/bin/lua

XBUILD = require '../xbuild/xbuild'
OS_NAME = XBUILD.OS_NAME
OPEN_MODE = XBUILD.OPEN_MODE

local function download_jdk(config)
   local jdk_dir = "dist/tools/jdk/" .. config.os_name
   if XBUILD.exists(jdk_dir .. "/bin/java") then
      XBUILD.warn("JDK already exists, skipping download")
      return
   end

   XBUILD.info("Downloading OpenJDK " .. config.jdk_version .. "...")

   if config.os_name == OS_NAME.LINUX then
      XBUILD.run("mkdir -p dist/tools/jdk")
      XBUILD.run("wget -O jdk.tar.gz https://api.adoptium.net/v3/binary/latest/".. config.jdk_version .."/ga/linux/x64/jdk/hotspot/normal/eclipse")
      XBUILD.run("tar -xf jdk.tar.gz -C dist/tools/jdk")
      XBUILD.run("mv dist/tools/jdk/jdk-".. config.jdk_version .."* " .. jdk_dir)
      XBUILD.run("rm jdk.tar.gz")
   elseif config.os_name == OS_NAME.WINDOWS then
      XBUILD.run("mkdir dist\\tools\\jdk")
      XBUILD.run("curl -L -o jdk.zip https://api.adoptium.net/v3/binary/latest/".. config.jdk_version .."/ga/windows/x64/jdk/hotspot/normal/eclipse")
      XBUILD.run("tar -xf jdk.zip -C dist/tools/jdk")
   elseif config.os_name == OS_NAME.MACOS then
      XBUILD.run("mkdir -p dist/tools/jdk")
      XBUILD.run("wget -O jdk.tar.gz https://api.adoptium.net/v3/binary/latest/".. config.jdk_version .."/ga/mac/x64/jdk/hotspot/normal/eclipse")
      XBUILD.run("tar -xf jdk.tar.gz -C dist/tools/jdk")
   end
end

local function build_app(config)
   if config.os_name == XBUILD.OS_NAME.WINDOWS then
      XBUILD.run("set JAVA_HOME=%CD%\\dist\\tools\\jdk\\windows && set PATH=%JAVA_HOME%\\bin;%PATH% && mvn clean install -DskipTests")
   elseif config.os_name == XBUILD.OS_NAME.MACOS then
      XBUILD.run("export JAVA_HOME=\"$PWD/dist/tools/jdk/macos\" && export PATH=\"$JAVA_HOME/bin:$PATH\" && mvn clean install -DskipTests")
   else
      XBUILD.run("export JAVA_HOME=\"$PWD/dist/tools/jdk/linux\" && export PATH=\"$JAVA_HOME/bin:$PATH\" && mvn clean install -DskipTests")
   end
end

local function package(config)
   XBUILD.run("mkdir -p dist/app")
   XBUILD.run("mkdir -p dist/lib")
   XBUILD.run("mkdir -p dist/plugins")
   
   XBUILD.run("cp xide-app/target/xide-app-0.1.0.jar dist/app/xide.jar")
   
   XBUILD.run("cp xide-core/target/xide-core-0.1.0.jar dist/lib/")
   XBUILD.run("cp xide-ui/target/xide-ui-0.1.0.jar dist/lib/")
   
   XBUILD.run("mvn dependency:copy-dependencies -DoutputDirectory=../dist/lib -DincludeScope=runtime -DexcludeGroupIds=org.xast.xide -f xide-app/pom.xml")
   
   -- Copy plugins
   XBUILD.run("cp xide-standard-plugins/xide-folder-tree-plugin/target/xide-folder-tree-plugin-0.1.0.jar dist/plugins/")
   XBUILD.run("cp xide-standard-plugins/xide-terminal-plugin/target/xide-terminal-plugin-0.1.0.jar dist/plugins/")
   XBUILD.run("cp xide-standard-plugins/xide-settings-plugin/target/xide-settings-plugin-0.1.0.jar dist/plugins/")
   XBUILD.run("cp xide-standard-plugins/xide-rust-file-plugin/target/xide-rust-file-plugin-0.1.0.jar dist/plugins/")
   XBUILD.run("cp xide-standard-plugins/xide-logs-plugin/target/xide-logs-plugin-0.1.0.jar dist/plugins/")

   if config.os_name == OS_NAME.WINDOWS then
      local launcher = XBUILD.open("dist/xide.bat", OPEN_MODE.WRITE)
      launcher:write(
         "@echo off\n" ..
         "set DIR=%~dp0\n" ..
         "set CLASSPATH=%DIR%\\app\\xide.jar;%DIR%\\lib\\*;%DIR%\\plugins\\*\n" ..
         "\"%DIR%\\tools\\jdk\\windows\\bin\\java.exe\" -Dsun.java2d.opengl=true --enable-native-access=ALL-UNNAMED -Xms128m -Xmx1024m -cp \"%CLASSPATH%\" org.xast.xide.app.Main %*"
      )
      launcher:close()
   else
      local launcher = XBUILD.open("dist/xide.sh", OPEN_MODE.WRITE)
      launcher:write(
         "#!/usr/bin/env bash\n" ..
         "DIR=\"$(cd \"$(dirname \"$0\")\" && pwd)\"\n" ..
         "CLASSPATH=\"$DIR/app/xide.jar:$DIR/lib/*:$DIR/plugins/*\"\n" ..
         "\"$DIR/tools/jdk/linux/bin/java\" -Dsun.java2d.opengl=true --enable-native-access=ALL-UNNAMED -Xms128m -Xmx1024m -cp \"$CLASSPATH\" org.xast.xide.app.Main \"$@\""
      )
      launcher:close()
      XBUILD.run("chmod +x dist/xide.sh")
   end
end

XBUILD.build({
   project_name = "xide",
   version = "0.1.0",
   jdk_version = "25",
   pipeline = {
      download_jdk,
      build_app,
      package,
   }
})