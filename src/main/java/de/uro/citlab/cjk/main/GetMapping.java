/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uro.citlab.cjk.main;

import de.uro.citlab.cjk.Decomposer;
import de.uro.citlab.cjk.types.Sign;
import de.uro.citlab.cjk.util.DecomposerUtil;
import de.uro.citlab.cjk.util.FileUtil;
import de.uro.citlab.cjk.util.Gnuplot;
import eu.transkribus.errorrate.util.ObjectCounter;
import java.io.File;
import java.util.List;

/**
 *
 * @author gundram
 */
public class GetMapping {

    public static void main(String[] args) {
        for (Decomposer.Coding coding : Decomposer.Coding.values()) {
            File root = new File(".");
            File folder = new File(root, coding.toString());
            folder.mkdir();
            /////////////////////////////
            /// create raw decomposer ///
            /////////////////////////////
            Decomposer dec = new Decomposer(coding);
            List<File> listFiles = FileUtil.listFiles(new File(root, "docs"), "txt", true);
            ObjectCounter<Character> oc = new ObjectCounter<>();
            for (File listFile : listFiles) {
                List<String> readLines = FileUtil.readLines(listFile);
                for (String readLine : readLines) {
                    for (char sign : readLine.toCharArray()) {
                        oc.add(sign);
                        ///////////////////
                        /// count signs ///
                        ///////////////////
                        dec.count(String.valueOf(sign));
                    }
                }
            }
            //dump some stuff
            DecomposerUtil.saveCharSet(dec, new File(folder, "leaves.txt"), true);
            DecomposerUtil.saveMap(dec, new File(folder, "map.txt"), true, true);

            System.out.println("number distinct signs in text: " + oc.getMap().size());
            ////////////////////////////////
            /// prune decomposition tree ///
            ////////////////////////////////
            List<double[]> reduceDecomposer = DecomposerUtil.reduceDecomposer(dec, 0.005, coding.toString().endsWith("IDC") ? 7 : 11);

            //dump some stuff
            DecomposerUtil.saveCharSet(dec, new File(folder, "leaves_after.txt"), true);
            DecomposerUtil.saveMap(dec, new File(folder, "map_after.txt"), true, true);

//            boolean consistentWithoutFormatSigns = DecomposerUtil.isConsistentWithoutFormatSigns(dec);
//            System.out.println("is consistent with lower format: " + consistentWithoutFormatSigns);

            //show some stuff
            try {
//                Gnuplot.plot(reduceDecomposer);
            } catch (Throwable e) {
                System.out.println("GNUplot not installed correctly (or windows is used) - skip display");
            }
//        System.out.println((int) '土');
//        System.out.println((int) '士');
        }
    }

}
