/**
 * Copyright 2011 Michael Zoech
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bitbrothers.android.cleaner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class JarCleaner {

    public static void main(String[] args) {
        if (!checkArgs(args)) {
            return;
        }
        if (new File(args[0]).isFile()) {
            try {
                cleanJar(args[0], args[1]);
            } catch (IOException e) {
                println("ERROR: Cleaning of file '" + args[0] + "' failed!");
            }
        } else {
            // TODO: maven mode
        }
    }

    private static void cleanJar(String infile, String outfile) throws IOException {
        ZipFile zip = new ZipFile(infile);
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outfile));
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            ZipEntry outEntry = new ZipEntry(entry.getName());
            if (entry.getName().endsWith(".class")) {
                out.putNextEntry(outEntry);
                ClassCleaner.clean(zip.getInputStream(entry), out);
                out.closeEntry();
            }
        }
        zip.close();
        out.close();
    }

    private static boolean checkArgs(String[] args) {
        if (args.length != 2) {
            println("jarcleaner - cleans methods of class of any bytecode and returns default values");
            println("USAGE:");
            println("    jarcleaner <in.jar> <out.jar>");
            println("    jarcleaner <groupId> <artifactId>");
            return false;
        } else if (new File(args[0]).isFile() && new File(args[args.length - 1]).exists()) {
            println("ERROR: out.jar exists already");
            return false;
        }
        return true;
    }

    private static void println(String out) {
        System.out.println(out);
    }
}
