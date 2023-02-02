/***
 * ASM examples: examples showing how ASM can be used
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.hasrat.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader

import java.util.zip.ZipEntry
import java.util.zip.ZipFile
/**
 * DependencyTracker
 *
 * @author Eugene Kuleshov
 *
 * @see "http://www.onjava.com/pub/a/onjava/2005/08/17/asm3.html"
 */
class DependencyTracker extends DefaultTask {

    @InputFile
    File input

    @TaskAction
    void run() throws IOException {
        DependencyVisitor v = new DependencyVisitor()

        ZipFile f = new ZipFile(input)

        long l1 = System.currentTimeMillis()
        Enumeration<? extends ZipEntry> en = f.entries()
        while (en.hasMoreElements()) {
            ZipEntry e = en.nextElement()
            String name = e.getName()
            if (name.endsWith(".class")) {
                new ClassReader(f.getInputStream(e)).accept(v, 0)
            }
        }
        long l2 = System.currentTimeMillis()

        Set<String> classPackages = v.getPackages()
        Arrays.sort(classPackages)
        FileWriter fileWriter = new FileWriter("unused.txt")
        classPackages.forEach(name -> {
            if (name.startsWith("a/")) {
                fileWriter.append(name)
                fileWriter.append("\n")
            }
        })
        fileWriter.flush()
        fileWriter.close()
        int size = classPackages.size()
        System.err.println("time: " + (l2 - l1) / 1000f + "  " + size)
    }
}
