package com.hasrat.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

import static org.codehaus.groovy.runtime.IOGroovyMethods.readLines

class DeleteZipEntryTask extends DefaultTask {

    @Input
    String input

    @Input
    String unusedTxtFile

    protected HashMap<String, byte[]> maps = new HashMap<>()

    @TaskAction
    protected void doAction() {
        loadJarFile()
        deleteEntry()
        saveJarFile()
    }

    protected void loadJarFile() {
        JarFile jarFile = new JarFile(input)
        Enumeration<JarEntry> jarEntryEnumeration = jarFile.entries()
        while (jarEntryEnumeration.hasMoreElements()) {
            JarEntry jarEntry = jarEntryEnumeration.nextElement()
            if (!jarEntry.isDirectory()) {
                maps.put(jarEntry.getName(), jarFile.getInputStream(jarEntry).readAllBytes())
            }
        }
    }

    protected void deleteEntry() {
        int removedCount = 0, notFound = 0
        List<String> unusedClasses = readLines(new FileInputStream(unusedTxtFile))

        for (String className : unusedClasses) {
            if (maps.containsKey(className)) {
                maps.remove(className)
                removedCount++
            } else {
                notFound++
            }
        }
        println("$removedCount unused classes are removed. And $notFound classes are not found")
    }

    protected void saveJarFile() {
        int i = 0

        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(input))
        for (Map.Entry<String, byte[]> jarMap : maps.entrySet()) {
            jarOutputStream.putNextEntry(new JarEntry(jarMap.getKey()))
            jarOutputStream.write(jarMap.getValue())
            jarOutputStream.closeEntry()
            i++
        }
        jarOutputStream.close()

        println("total $i item saved.")
    }
}

