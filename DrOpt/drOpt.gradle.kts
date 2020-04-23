/*
 * Copyright 2020 See AUTHORS file
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

@file:Suppress("UnstableApiUsage")

import org.gradle.internal.jvm.Jvm

group = rootProject.group
version = "1.1.1"

plugins {
   `cpp-library`
   xcode
   `microsoft-visual-cpp-compiler`
   `visual-studio`
   `maven-publish`
}

repositories {
   jcenter()
}

/**
 * Path to the JVM Gradle is running in
 */
val javaHomePathString: String = Jvm.current().javaHome.absolutePath

tasks.withType(CppCompile::class).configureEach {
   source.from(fileTree(file("dropt/src")) {
      include("**/*.c")
      exclude("**/*.cpp")
      exclude("test_dropt.c")
   })
}

library {
   linkage.set(listOf(Linkage.STATIC))

   publicHeaders.from(file("dropt/include"))

   targetMachines.set(listOf(machines.windows.x86_64, machines.linux.x86_64, machines.macOS.x86_64))

   toolChains.forEach { toolChain ->
      if (toolChain is VisualCpp) {
         toolChain.setInstallDir(File("C:/Program Files (x86)/Microsoft Visual Studio/2017/Community"))
         toolChain.setWindowsSdkDir(File("C:/Program Files/Microsoft SDKs/Windows/v7.1"))
      }
   }

   binaries.configureEach(CppStaticLibrary::class.java) {
      val binaryToolChain = toolChain

      val binaryCompileTask = compileTask.get()

      binaryCompileTask.includes(file("$javaHomePathString/include"))

      if (binaryToolChain is VisualCpp) {
         binaryCompileTask.includes(file("$javaHomePathString/include/win32"))

         binaryCompileTask.macros["DLL_EXPORT"] = null
         binaryCompileTask.macros["_WIN32"] = null
         binaryCompileTask.macros["WIN32"] = null
         binaryCompileTask.macros["UNICODE"] = null
         binaryCompileTask.macros["_UNICODE"] = null
         binaryCompileTask.macros["_WIN32_WINNT_WINXP=0x0501"] = null // target windows xp

         binaryCompileTask.compilerArgs.add("/EHsc")
         binaryCompileTask.compilerArgs.add("/nologo")
         binaryCompileTask.compilerArgs.add("/GF")
         binaryCompileTask.compilerArgs.add("/Gy")
         binaryCompileTask.compilerArgs.add("/W4")
         binaryCompileTask.compilerArgs.add("/wd4100")
         binaryCompileTask.compilerArgs.add("/DSTRICT")
         binaryCompileTask.compilerArgs.add("/TC")
      } else if (binaryToolChain is Gcc) {
         binaryCompileTask.compilerArgs.add("-fPIC")
         binaryCompileTask.compilerArgs.add("-c")
         binaryCompileTask.compilerArgs.add("-fmessage-length=0")
         binaryCompileTask.compilerArgs.add("-Wwrite-strings")
         binaryCompileTask.compilerArgs.add("-x")
         binaryCompileTask.compilerArgs.add("c")
         binaryCompileTask.compilerArgs.add("-std=c11")

         // compiler linux
         if (targetPlatform.targetMachine.architecture.name == MachineArchitecture.X86) {
            binaryCompileTask.compilerArgs.add("-m32")
         }
         if (targetPlatform.targetMachine.architecture.name == MachineArchitecture.X86_64) {
            binaryCompileTask.compilerArgs.add("-m64")
         }

         if (targetPlatform.targetMachine.operatingSystemFamily.isLinux) {
            binaryCompileTask.includes(file("/usr/lib/jvm/java-8-openjdk-amd64/include/"))
            binaryCompileTask.includes(file("/usr/lib/jvm/java-8-openjdk-amd64/include/linux/"))
         }

         // compiler osx
         if (targetPlatform.targetMachine.operatingSystemFamily.isMacOs) {
            binaryCompileTask.includes(file("/System/Library/Frameworks/JavaVM.framework/Versions/A/Headers"))
         }
      } else if (binaryToolChain is Clang) {
         binaryCompileTask.compilerArgs.add("-fPIC")
         binaryCompileTask.compilerArgs.add("-c")
         binaryCompileTask.compilerArgs.add("-fmessage-length=0")
         binaryCompileTask.compilerArgs.add("-Wwrite-strings")
         binaryCompileTask.compilerArgs.add("-x")
         binaryCompileTask.compilerArgs.add("c")
         binaryCompileTask.compilerArgs.add("-std=c11")

         binaryCompileTask.includes(file("$javaHomePathString/include"))
         binaryCompileTask.includes(file("$javaHomePathString/include/darwin"))
      }
   }
}
