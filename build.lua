#!/bin/lua

XBUILD = require 'xbuild'
OS_NAME = XBUILD.OS_NAME
OPEN_MODE = XBUILD.OPEN_MODE

XBUILD.build({
   project_name = "xide",
   version = "0.1.0",
   jdk_version = "21",
   pipeline = {
      download_jdk = function(config)
         local jdk_dir = "dist/tools/jdk/" .. config.os_name
         if XBUILD.exists(jdk_dir .. "/bin/java") then
            XBUILD.warn("JDK already exists, skipping download")
            return
         end

         XBUILD.info("Downloading OpenJDK " .. config.jdk_version .. "...")

         if config.os_name == OS_NAME.LINUX then
            XBUILD.run("mkdir -p dist/tools/jdk")
            XBUILD.run("wget -O jdk.tar.gz https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse")
            XBUILD.run("tar -xf jdk.tar.gz -C dist/tools/jdk")
            XBUILD.run("mv dist/tools/jdk/jdk-21* " .. jdk_dir)
            XBUILD.run("rm jdk.tar.gz")
         elseif config.os_name == OS_NAME.WINDOWS then
            XBUILD.run("mkdir dist\\tools\\jdk")
            XBUILD.run("curl -L -o jdk.zip https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse")
            XBUILD.run("tar -xf jdk.zip -C dist/tools/jdk")
         elseif config.os_name == OS_NAME.MACOS then
            XBUILD.run("mkdir -p dist/tools/jdk")
            XBUILD.run("wget -O jdk.tar.gz https://api.adoptium.net/v3/binary/latest/21/ga/mac/x64/jdk/hotspot/normal/eclipse")
            XBUILD.run("tar -xf jdk.tar.gz -C dist/tools/jdk")
         end
      end,

      build_app = function(_)
         XBUILD.run("mvn clean package -DskipTests")
      end,

      package = function (config)
         XBUILD.run("mkdir -p dist/app")
         XBUILD.run("cp xide-app/target/*jar-with-dependencies.jar dist/app/xide.jar")

         if config.os_name == OS_NAME.WINDOWS then
            local launcher = XBUILD.open("dist/xide.bat", OPEN_MODE.WRITE)
            launcher:write(
               "@echo off\n" ..
               "set DIR=%~dp0\n" ..
               "\"%DIR%\\tools\\jdk\\windows\\bin\\java.exe\" -Xms64m -Xmx256m -jar \"%DIR%\\app\\xide.jar\""
            )
            launcher:close()
         else
            local launcher = XBUILD.open("dist/xide.sh", OPEN_MODE.WRITE)
            launcher:write(
               "#!/usr/bin/env bash\n" ..
               "DIR=\"$(cd \"$(dirname \"$0\")\" && pwd)\"\n" ..
               "\"$DIR/tools/jdk/linux/bin/java\" -Xms64m -Xmx256m -jar \"$DIR/app/xide.jar\""
            )
            launcher:close()
            XBUILD.run("chmod +x dist/xide.sh")
         end
      end
   }
})