package com.hasrat.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.analysis.AnalyzerException

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

import static org.codehaus.groovy.runtime.IOGroovyMethods.readLines

class DeObfuscator2 extends DefaultTask {


    protected ArrayList<ClassNode> classNodes = new ArrayList<>()

    @InputFiles
    File[] inputFiles

    @Input
    String mappingFile

    @OutputDirectory
    String outputDir

    protected HashMap mapEntry = new HashMap<>()

    @TaskAction
    void runTask() {
        readMapping()
        for (File file : inputFiles) {
            loadJar(file)
            run()
            saveJarFile(file)
        }
    }

    protected void run() {
        ClassNameTransformer classNameTransformer = new ClassNameTransformer()
        try {
            classNameTransformer.visit(classNodes)
        } catch (AnalyzerException analyzerException) {
            analyzerException.printStackTrace(System.err)
        }
    }

    private void readMapping() {
        println("reading mapping")
        int i = 0
        List<String> strs = readLines(new FileInputStream(mappingFile))
        for (String str : strs) {
            if (!str.startsWith("#")) {
                String[] split = str.split("->")
                mapEntry.put(split[0].trim(), split[1].trim())
                i++
            }
        }
        println(i + " mapping found")
    }

    private void loadJar(File inputFile) {
        try (JarFile jarFile = new JarFile(inputFile)) {
            final Enumeration<JarEntry> entries = jarFile.entries()
            while (entries.hasMoreElements()) {
                final JarEntry jarEntry = entries.nextElement()
                if (!jarEntry.isDirectory()) {
                    try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                        final byte[] bytes = inputStream.readAllBytes()
                        try {
                            final ClassNode classNode = new ClassNode()
                            new ClassReader(bytes).accept(classNode, ClassReader.EXPAND_FRAMES)
                            classNodes.add(classNode)
                        } catch (Exception ignored) {
                            ignored.printStackTrace(System.err)
                            System.err.println("[ERROR] There was an error loading " + jarEntry.getName())
                        }
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace()
        }
        println("nodes -> " + classNodes.size())
    }

    protected void saveJarFile(File inputFile) {
        try (JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(inputFile))) {
            for (ClassNode classNode : classNodes) {
                if (classNode != null) {
                    jarOutputStream.putNextEntry(new JarEntry(classNode.name + ".class"))
                    jarOutputStream.write(toByteArray(classNode))
                    jarOutputStream.closeEntry()
                } else {
                    System.err.println("[Error] classNode is null")
                }
            }
            System.out.println("[INFO] Successful saved " + inputFile.getName())
        } catch (Exception e) {
            e.printStackTrace(System.err)
        }
        classNodes.clear()
    }

    static byte[] toByteArray(ClassNode classNode) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS)
        try {
            classNode.accept(writer)
            return writer.toByteArray()
        } catch (Throwable ignored) {
            writer = new ClassWriter(0)
            classNode.accept(writer)
            return writer.toByteArray()
        }
    }

    class ClassNameTransformer {

        ClassNameTransformer() {
        }

        void visit(List<ClassNode> classMap) throws AnalyzerException {
            applyMappings(classMap)
        }

        private void applyMappings(List<ClassNode> classMap) {
            for (ClassNode node : new ArrayList<>(classMap)) {
                ClassNode copy = new ClassNode()

                ClassRemapper adapter = new ClassRemapper(copy, new RemapperImpl())
                node.accept(adapter)
                classMap.remove(node)
                classMap.add(copy)
            }
        }
    }

    class AndroidxRemapper extends Remapper {
        private String superName
        private ArrayList<String> strs = new ArrayList<>()
        private String[] arr

        AndroidxRemapper(String superName) {
            this.superName = superName
            arr = [
                    "androidx/recyclerview/widget/RecyclerView\$Adapter",
                    "androidx/recyclerview/widget/RecyclerView\$a",
                    "androidx/recyclerview/widget/RecyclerView\$ViewHolder",
                    "androidx/recyclerview/widget/RecyclerView\$v",
                    "androidx/recyclerview/widget/RecyclerView\$m",
                    "androidx/viewpager/widget/ViewPager\$m"
            ]
        }

        @Override
        String mapMethodName(String owner, String name, String descriptor) {
            if (superName in arr) {
                switch (name) {
                    case "a":
                    case "b":
                        println("$owner#$name$descriptor -> ")
                        break
                }
            }
            return super.mapMethodName(owner, name, descriptor)
        }
    }

    class RemapperImpl extends Remapper {

        RemapperImpl() {
        }

        @Override
        String mapFieldName(String owner, String name, String descriptor) {
            //println("field = owner -> " + owner + ": method -> " + name + ": descriptor -> " + descriptor)
            return mapEntry.getOrDefault(owner + ":" + name + descriptor, name)
            //return super.mapFieldName(owner, name, descriptor)
        }

        @Override
        String mapMethodName(String owner, String name, String descriptor) {
            //println("method = owner -> " + owner + ": method -> " + name + ": descriptor -> " + descriptor)
            try {
                return mapEntry.getOrDefault(owner + "#" + name + descriptor, name)
            } catch (Exception e) {
                print(e)
            }
            return super.mapMethodName(owner, name, descriptor)
        }

        @Override
        String mapType(String internalName) {
            //println("internal type name:" + internalName)
            try {
                return super.mapType(mapEntry.getOrDefault(internalName, internalName).toString())
            } catch (Exception e) {
                print(e)
            }
            return super.mapType(internalName)
        }

        @Override
        String map(String internalName) {
            return mapEntry.getOrDefault(internalName, internalName)
        }

        @Override
        String mapInnerClassName(String name, String ownerName, String innerName) {
            //println("name:" + name + "-> ownerName:" + ownerName + "-> innerName:" + innerName)
            name = mapEntry.getOrDefault(name, name)
            return super.mapInnerClassName(name, ownerName, innerName)
        }
    }
}